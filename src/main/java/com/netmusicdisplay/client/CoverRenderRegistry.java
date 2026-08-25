package com.netmusicdisplay.client;

import net.minecraft.core.BlockPos;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端封面渲染注册表（仅客户端使用）。
 *
 * 服务端通过 CoverDisplayPacket 把「翻牌坐标 → 歌曲信息」同步过来，
 * 这里按翻牌坐标缓存，供 FlapDisplayRenderer 的 Mixin 在渲染时查询。
 */
public final class CoverRenderRegistry {

    private CoverRenderRegistry() {
    }

    private static final Map<BlockPos, CoverInfo> INFO = new ConcurrentHashMap<>();

    /** 写入/更新某翻牌的封面信息；songId<0 或歌名为空时视为清空 */
    public static void put(BlockPos pos, long songId, boolean isPlaying, String songName) {
        if (pos == null || songId < 0 || songName == null || songName.isEmpty()) {
            INFO.remove(pos);
        } else {
            INFO.put(pos, new CoverInfo(songId, isPlaying, songName));
        }
    }

    public static CoverInfo get(BlockPos pos) {
        return INFO.get(pos);
    }

    public static void remove(BlockPos pos) {
        INFO.remove(pos);
    }

    public static final class CoverInfo {
        public final long songId;
        public final boolean isPlaying;
        public final String songName;

        public CoverInfo(long songId, boolean isPlaying, String songName) {
            this.songId = songId;
            this.isPlaying = isPlaying;
            this.songName = songName;
        }
    }
}
