package com.netmusicdisplay.source;

import com.github.tartaricacid.netmusic.item.ItemMusicCD;
import com.github.tartaricacid.netmusic.tileentity.TileEntityMusicPlayer;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlockEntity;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.source.SingleLineDisplaySource;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 自定义显示数据源：显示 Net Music CD 播放机的播放状态。
 *
 * 显示格式：
 * - 正在播放：▶ 歌曲名 [剩余分:秒]
 * - 已停止：■ 歌曲名
 * - 无唱片：空行
 *
 * isPlay() 和 getCurrentTime() 都是服务端字段，可安全读取。
 */
public class NetMusicPlayStatusSource extends SingleLineDisplaySource {

    @Override
    protected MutableComponent provideLine(DisplayLinkContext context, DisplayTargetStats stats) {
        DisplayLinkBlockEntity gatherer = context.blockEntity();
        BlockPos sourcePos = gatherer.getSourcePosition();
        BlockEntity be = context.level().getBlockEntity(sourcePos);

        if (be instanceof TileEntityMusicPlayer musicPlayer) {
            ItemStack cd = musicPlayer.getPlayerInv().getStackInSlot(0);
            if (cd.isEmpty()) {
                return EMPTY_LINE;
            }

            ItemMusicCD.SongInfo info = ItemMusicCD.getSongInfo(cd);
            if (info == null || info.songName == null || info.songName.isEmpty()) {
                return EMPTY_LINE;
            }

            if (musicPlayer.isPlay()) {
                // 正在播放：▶ 歌曲名 [剩余时间]
                int remainingTicks = musicPlayer.getCurrentTime();
                int remainingSeconds = Math.max(0, remainingTicks / 20);
                int min = remainingSeconds / 60;
                int sec = remainingSeconds % 60;
                return Component.literal(String.format("▶ %s [%d:%02d]", info.songName, min, sec));
            } else {
                // 已停止：■ 歌曲名
                return Component.literal("■ " + info.songName);
            }
        }

        return EMPTY_LINE;
    }

    @Override
    protected String getTranslationKey() {
        return "netmusic_play_status";
    }

    @Override
    protected boolean allowsLabeling(DisplayLinkContext context) {
        return true;
    }
}
