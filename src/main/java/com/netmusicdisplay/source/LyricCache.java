package com.netmusicdisplay.source;

import com.github.tartaricacid.netmusic.NetMusic;
import com.github.tartaricacid.netmusic.api.lyric.LyricParser;
import com.github.tartaricacid.netmusic.api.lyric.LyricRecord;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 歌词缓存管理器。
 *
 * Net Music 的歌词原本只在客户端获取（从网易云 API 拉取 LRC 歌词，解析成 LyricRecord）。
 * 但 Create 的显示链接器是服务端方块系统，服务端拿不到客户端的歌词。
 *
 * 解决方案：服务端自己也去调网易云歌词 API，拿到歌词后缓存起来。
 * - 第一次请求时异步获取（不阻塞服务端 tick）
 * - 后续请求直接从缓存读取
 * - 根据 CD 播放机的播放进度（currentTime）计算当前歌词行
 *
 * 歌词时间轴单位是 tick（50ms），和 Minecraft tick 一致（1 秒 = 20 tick）。
 */
public class LyricCache {
    private static final Logger LOGGER = LogManager.getLogger("NetMusicDisplay");

    /** 歌词缓存：歌曲 ID -> 歌词加载 Future */
    private static final ConcurrentHashMap<Long, CompletableFuture<LyricRecord>> CACHE = new ConcurrentHashMap<>();

    /** 网易云歌曲 URL 格式：https://music.163.com/song/media/outer/url?id=123456.mp3 */
    private static final Pattern SONG_ID_PATTERN = Pattern.compile("^.*?\\?id=(\\d+)\\.mp3$");

    /**
     * 从 songUrl 提取网易云歌曲 ID。
     * @return 歌曲 ID，如果不是网易云歌曲返回 -1
     */
    public static long extractSongId(String songUrl) {
        if (songUrl == null || songUrl.isEmpty()) {
            LOGGER.warn("[LyricCache] songUrl is null or empty");
            return -1;
        }
        Matcher matcher = SONG_ID_PATTERN.matcher(songUrl);
        if (matcher.find()) {
            try {
                long id = Long.parseLong(matcher.group(1));
                LOGGER.info("[LyricCache] Extracted song ID: {} from URL: {}", id, songUrl);
                return id;
            } catch (NumberFormatException e) {
                LOGGER.warn("[LyricCache] Failed to parse song ID from URL: {}", songUrl);
                return -1;
            }
        }
        LOGGER.warn("[LyricCache] URL does not match NetEase pattern: {}", songUrl);
        return -1;
    }

    /**
     * 获取歌词（非阻塞）。
     * 第一次请求时异步获取歌词，后续从缓存读取。
     * 如果获取失败，不会缓存 null 结果，下次请求会重试。
     *
     * @param songId   网易云歌曲 ID
     * @param songName 歌曲名（用于歌词第一行补全）
     * @return LyricRecord，如果还在加载或获取失败返回 null
     */
    public static LyricRecord getLyric(long songId, String songName) {
        if (songId < 0) return null;

        CompletableFuture<LyricRecord> future = CACHE.computeIfAbsent(songId, id ->
                CompletableFuture.supplyAsync(() -> {
                    try {
                        if (NetMusic.NET_EASE_WEB_API == null) {
                            LOGGER.error("[LyricCache] NET_EASE_WEB_API is null! NetMusic mod may not be fully initialized.");
                            return null;
                        }
                        LOGGER.info("[LyricCache] Fetching lyric for song ID: {}, name: {}", id, songName);
                        String json = NetMusic.NET_EASE_WEB_API.lyric(id);
                        if (json == null || json.isEmpty()) {
                            LOGGER.warn("[LyricCache] API returned empty response for song ID: {}", id);
                            return null;
                        }
                        LOGGER.debug("[LyricCache] API response (first 200 chars): {}",
                                json.length() > 200 ? json.substring(0, 200) + "..." : json);
                        LyricRecord record = LyricParser.parseLyric(json, songName);
                        if (record == null) {
                            LOGGER.warn("[LyricCache] LyricParser returned null for song ID: {}", id);
                        } else {
                            LOGGER.info("[LyricCache] Successfully parsed lyric for song ID: {}, lyric lines: {}",
                                    id, record.getLyrics() != null ? record.getLyrics().size() : 0);
                        }
                        return record;
                    } catch (Exception e) {
                        LOGGER.error("[LyricCache] Failed to fetch lyric for song ID: " + id, e);
                        return null;
                    }
                })
        );

        // 非阻塞：如果还没完成，返回 null（DisplaySource 会显示"加载中"）
        if (future.isDone()) {
            try {
                LyricRecord result = future.get();
                if (result == null) {
                    // 获取失败，清除缓存以便下次重试
                    CACHE.remove(songId, future);
                    LOGGER.warn("[LyricCache] Lyric fetch returned null, removed from cache for retry. Song ID: {}", songId);
                }
                return result;
            } catch (Exception e) {
                CACHE.remove(songId, future);
                LOGGER.error("[LyricCache] Exception getting lyric result, removed from cache. Song ID: " + songId, e);
                return null;
            }
        }
        return null;
    }

    /**
     * 线程安全地查找当前原歌词行（只读，不修改 LyricRecord 数据）。
     *
     * 注意：LyricRecord.updateCurrentLine() 是破坏性操作（会移除已过的歌词行），
     * 在多线程环境下使用会导致数据损坏。这里用只读遍历替代。
     *
     * @param record      歌词记录
     * @param playedTicks 已播放的 tick 数
     * @return 当前原歌词行文本，如果没有匹配的返回 null
     */
    public static String getCurrentLyricLine(LyricRecord record, int playedTicks) {
        if (record == null) return null;
        return findCurrentLine(record.getLyrics(), playedTicks);
    }

    /**
     * 线程安全地查找当前翻译歌词行（只读，不修改 LyricRecord 数据）。
     *
     * @param record      歌词记录
     * @param playedTicks 已播放的 tick 数
     * @return 当前翻译歌词行文本；如果该歌曲没有翻译歌词返回 null
     */
    public static String getCurrentTransLyricLine(LyricRecord record, int playedTicks) {
        if (record == null) return null;
        Int2ObjectSortedMap<String> transLyrics = record.getTransLyrics();
        if (transLyrics == null || transLyrics.isEmpty()) return null;
        return findCurrentLine(transLyrics, playedTicks);
    }

    /**
     * 判断歌词记录是否包含翻译歌词。
     */
    public static boolean hasTranslation(LyricRecord record) {
        return record != null && record.getTransLyrics() != null && !record.getTransLyrics().isEmpty();
    }

    /**
     * 通用：在按 tick 升序排列的歌词 map 中，查找 <= playedTicks 的最大 key 对应的歌词行。
     * 只读遍历，不修改数据，线程安全。
     */
    private static String findCurrentLine(Int2ObjectSortedMap<String> lyrics, int playedTicks) {
        if (lyrics == null || lyrics.isEmpty()) return null;
        String result = null;
        for (Int2ObjectMap.Entry<String> entry : lyrics.int2ObjectEntrySet()) {
            if (entry.getIntKey() <= playedTicks) {
                result = entry.getValue();
            } else {
                break; // 后面的 key 都更大，不需要继续
            }
        }
        return result;
    }
}
