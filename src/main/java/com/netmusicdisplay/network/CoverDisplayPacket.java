package com.netmusicdisplay.network;

import com.netmusicdisplay.NetMusicDisplay;
import com.netmusicdisplay.client.CoverRenderRegistry;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 封面显示同步包：服务端「封面显示源」在玩家把翻牌显示器接到 CD 播放机时，
 * 把翻牌坐标 + 歌曲 ID + 播放状态 + 歌名发给附近客户端。
 *
 * 客户端收到后写入 CoverRenderRegistry，由 FlapDisplayRenderer 的 Mixin
 * 在渲染该翻牌时画出「播放状态 → 封面 → 歌名」的图文混排。
 *
 * @param flapPos   翻牌显示器（显示链接目标）的坐标
 * @param songId    网易云歌曲 ID（-1 表示清空）
 * @param isPlaying 是否正在播放
 * @param songName  歌名（封面加载失败时作为纯文本兜底）
 */
public record CoverDisplayPacket(BlockPos flapPos, long songId, boolean isPlaying, String songName)
        implements CustomPacketPayload {

    public static final Type<CoverDisplayPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(NetMusicDisplay.MODID, "cover_display"));

    public static final StreamCodec<ByteBuf, CoverDisplayPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, CoverDisplayPacket::flapPos,
                    ByteBufCodecs.VAR_LONG, CoverDisplayPacket::songId,
                    ByteBufCodecs.BOOL, CoverDisplayPacket::isPlaying,
                    ByteBufCodecs.STRING_UTF8, CoverDisplayPacket::songName,
                    CoverDisplayPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CoverDisplayPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            CoverRenderRegistry.put(msg.flapPos(), msg.songId(), msg.isPlaying(), msg.songName());
            org.slf4j.LoggerFactory.getLogger("NetMusicDisplay").info(
                    "[CoverDebug] CLIENT recv flapPos={} songId={} playing={} name={}",
                    msg.flapPos(), msg.songId(), msg.isPlaying(), msg.songName());
        });
    }
}
