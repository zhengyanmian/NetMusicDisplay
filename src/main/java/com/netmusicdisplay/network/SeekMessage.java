package com.netmusicdisplay.network;

import com.netmusicdisplay.NetMusicDisplay;
import com.netmusicdisplay.client.ResumeTracker;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 续播位置消息：服务端在「暂停续播」时发送给附近玩家，
 * 告知客户端应从哪个 tick 开始播放（声音 + 歌词同步）。
 *
 * 流程：
 * 1. 服务端检测到续播（暂停后重新播放），在发送 MusicToClientMessage 之后补发本消息
 * 2. 客户端收到后，把 pos -> startTick 存入 ResumeTracker
 * 3. 随后 MusicToClientMessage 触发 NetMusicSound 创建时，从 ResumeTracker 取出起始 tick
 *
 * @param pos       播放机方块位置
 * @param startTick 已播放的 tick 数（续播起始位置）
 */
public record SeekMessage(BlockPos pos, int startTick) implements CustomPacketPayload {

    public static final Type<SeekMessage> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(NetMusicDisplay.MODID, "seek"));

    public static final StreamCodec<ByteBuf, SeekMessage> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, SeekMessage::pos,
                    ByteBufCodecs.VAR_INT, SeekMessage::startTick,
                    SeekMessage::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** 客户端处理：把续播位置存入 ResumeTracker，供 NetMusicSound 创建时读取 */
    public static void handle(SeekMessage msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ResumeTracker.put(msg.pos(), msg.startTick()));
    }
}
