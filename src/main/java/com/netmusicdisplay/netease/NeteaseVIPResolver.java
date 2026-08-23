package com.netmusicdisplay.netease;

import com.github.tartaricacid.netmusic.api.resolver.IAsyncSongUrlResolver;
import com.github.tartaricacid.netmusic.item.ItemMusicCD;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * VIP 歌曲播放解析器。
 *
 * Net Music 原版的 DefaultVipResolver（优先级最低）对 VIP 歌曲返回占位歌曲，
 * 导致 VIP 歌曲无法正常播放。这里注册一个更高优先级的 resolver，
 * 用带 Cookie 的 API 获取真实的高音质播放 URL。
 *
 * 参照开源模组 NetMusic-BetterLogin 的 NeteaseVIPResolver 实现。
 */
public class NeteaseVIPResolver implements IAsyncSongUrlResolver {
    private static final Logger LOGGER = LogManager.getLogger("NetMusicDisplay");
    private static final Pattern PATTERN = Pattern.compile("^.*?\\?id=(\\d+)\\.mp3$");

    /** 默认音质（higher = 较高音质，VIP 可用） */
    private static final String LEVEL = "higher";

    @Override
    public boolean canResolve(ItemMusicCD.SongInfo songInfo) {
        String url = songInfo.songUrl;
        return url != null && url.startsWith("https://music.163.com") && PATTERN.matcher(url).find();
    }

    @Override
    public CompletableFuture<ItemMusicCD.SongInfo> resolve(ItemMusicCD.SongInfo songInfo) {
        return CompletableFuture.supplyAsync(() -> {
            Matcher matcher = PATTERN.matcher(songInfo.songUrl);
            if (!matcher.find()) {
                return songInfo;
            }
            long musicId = Long.parseLong(matcher.group(1));
            try {
                String json = NeteaseApi.getPlayInfo(musicId, LEVEL);
                JsonObject root = JsonParser.parseString(json).getAsJsonObject();
                JsonArray data = root.has("data") ? root.getAsJsonArray("data") : null;
                if (data != null && !data.isEmpty()) {
                    JsonObject song = data.get(0).getAsJsonObject();
                    int code = song.has("code") ? song.get("code").getAsInt() : -1;
                    if (code == 200 && song.has("url") && !song.get("url").isJsonNull()) {
                        String url = song.get("url").getAsString();
                        if (url != null && !url.isEmpty()) {
                            songInfo.songUrl = url;
                            if (song.has("time") && !song.get("time").isJsonNull()) {
                                // time 单位是毫秒，转成秒
                                int timeSeconds = song.get("time").getAsInt() / 1000;
                                if (timeSeconds > 0) {
                                    songInfo.songTime = timeSeconds;
                                }
                            }
                            LOGGER.info("[NetEaseVIPResolver] Resolved VIP song id {} to {}", musicId, url);
                        }
                    } else {
                        LOGGER.warn("[NetEaseVIPResolver] VIP song id {} code={}, may not be available", musicId, code);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("[NetEaseVIPResolver] Failed to resolve VIP song id {}", musicId, e);
            }
            return songInfo;
        });
    }

    @Override
    public int getPriority() {
        // 比 DefaultVipResolver 的 Integer.MIN_VALUE 高，优先处理 VIP 歌曲
        return 10;
    }
}
