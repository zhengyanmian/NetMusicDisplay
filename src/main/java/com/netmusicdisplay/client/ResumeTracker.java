package com.netmusicdisplay.client;

import net.minecraft.core.BlockPos;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 续播位置暂存器（客户端）。
 *
 * 服务端的 SeekMessage 先于 MusicToClientMessage 到达，这里把「播放机位置 -> 起始 tick」
 * 暂存起来；随后 NetMusicSound 构造时通过 take() 取出并清除，避免跨消息的时序错乱。
 */
public final class ResumeTracker {

    private static final Map<BlockPos, Integer> PENDING = new ConcurrentHashMap<>();

    private ResumeTracker() {
    }

    /** 存入续播位置 */
    public static void put(BlockPos pos, int startTick) {
        if (startTick > 0) {
            PENDING.put(pos, startTick);
        }
    }

    /** 取出并清除续播位置（返回 0 表示没有待续播位置） */
    public static int take(BlockPos pos) {
        Integer v = PENDING.remove(pos);
        return v == null ? 0 : v;
    }
}
