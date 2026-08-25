package com.netmusicdisplay.source;

import com.github.tartaricacid.netmusic.item.ItemMusicCD;
import com.github.tartaricacid.netmusic.network.NetworkHandler;
import com.github.tartaricacid.netmusic.tileentity.TileEntityMusicPlayer;
import com.netmusicdisplay.network.CoverDisplayPacket;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlockEntity;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.source.SingleLineDisplaySource;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 翻牌「封面图文」显示源（服务端）。
 *
 * 设计：翻牌显示器的文字仍由本模组在客户端用 Mixin 直接绘制，
 * 所以这个数据源本身只输出「空行」占位，真正的图文（状态→封面→歌名）
 * 由 CoverDisplayPacket 同步到客户端后，由 FlapDisplayRenderer 的 Mixin 画出来。
 *
 * provideLine 在每次显示链接刷新时被服务端调用，这里顺带把
 * 翻牌坐标 + 歌曲 ID + 播放状态 + 歌名 发给附近客户端（带节流，仅在变化时发送）。
 */
public class NetMusicCoverSource extends SingleLineDisplaySource {

    /** 每个翻牌上次发送的内容指纹，用于节流避免每帧发包 */
    private static final Map<BlockPos, String> LAST_KEY = new ConcurrentHashMap<>();

    @Override
    protected MutableComponent provideLine(DisplayLinkContext context, DisplayTargetStats stats) {
        DisplayLinkBlockEntity gatherer = context.blockEntity();
        BlockPos sourcePos = gatherer.getSourcePosition();
        BlockPos flapPos = context.getTargetPos();
        Level level = context.level();
        BlockEntity be = level.getBlockEntity(sourcePos);

        if (be instanceof TileEntityMusicPlayer musicPlayer) {
            ItemStack cd = musicPlayer.getPlayerInv().getStackInSlot(0);
            if (!cd.isEmpty()) {
                ItemMusicCD.SongInfo info = ItemMusicCD.getSongInfo(cd);
                if (info != null && info.songName != null && !info.songName.isEmpty()) {
                    long songId = LyricCache.extractSongId(info.songUrl);
                    boolean playing = musicPlayer.isPlay();
                    String key = songId + "|" + playing + "|" + info.songName;
                    if (!key.equals(LAST_KEY.get(flapPos))) {
                        LAST_KEY.put(flapPos, key);
                        NetworkHandler.sendToNearby(level, flapPos,
                                new CoverDisplayPacket(flapPos, songId, playing, info.songName));
                    }
                    return EMPTY_LINE;
                }
            }
        }

        // 没有唱片：清空该翻牌的封面叠加层
        if (LAST_KEY.containsKey(flapPos)) {
            LAST_KEY.remove(flapPos);
            NetworkHandler.sendToNearby(level, flapPos,
                    new CoverDisplayPacket(flapPos, -1, false, ""));
        }
        return EMPTY_LINE;
    }

    @Override
    protected String getTranslationKey() {
        return "netmusic_cover";
    }

    @Override
    protected boolean allowsLabeling(DisplayLinkContext context) {
        return true;
    }
}
