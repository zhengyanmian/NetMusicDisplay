package com.netmusicdisplay.source;

import com.github.tartaricacid.netmusic.api.lyric.LyricRecord;
import com.github.tartaricacid.netmusic.item.ItemMusicCD;
import com.github.tartaricacid.netmusic.tileentity.TileEntityMusicPlayer;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlockEntity;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * 综合数据源：一个显示链接器同时输出多行，减少所需链接器数量。
 *
 * 输出行：
 *   行1：▶ 歌曲名 [剩余时间]  或  ■ 歌曲名（未播放）
 *   行2：当前原歌词行（播放中）
 *   行3：当前翻译歌词行（播放中，有翻译才显示）
 *
 * 直接继承 DisplaySource（而非 SingleLineDisplaySource），通过 provideText 返回多行 List。
 * 翻牌显示器至少需要 3 行高才能完整显示。
 */
public class NetMusicAllInOneSource extends DisplaySource {

    @Override
    public List<MutableComponent> provideText(DisplayLinkContext context, DisplayTargetStats stats) {
        List<MutableComponent> lines = new ArrayList<>();

        DisplayLinkBlockEntity gatherer = context.blockEntity();
        BlockPos sourcePos = gatherer.getSourcePosition();
        BlockEntity be = context.level().getBlockEntity(sourcePos);

        if (!(be instanceof TileEntityMusicPlayer musicPlayer)) {
            return lines;
        }

        ItemStack cd = musicPlayer.getPlayerInv().getStackInSlot(0);
        if (cd.isEmpty()) {
            return lines;
        }

        ItemMusicCD.SongInfo info = ItemMusicCD.getSongInfo(cd);
        if (info == null || info.songName == null || info.songName.isEmpty()) {
            return lines;
        }

        // 行1：播放状态 + 歌曲名
        if (musicPlayer.isPlay()) {
            int remainingTicks = musicPlayer.getCurrentTime();
            int remainingSeconds = Math.max(0, remainingTicks / 20);
            int min = remainingSeconds / 60;
            int sec = remainingSeconds % 60;
            lines.add(Component.literal(String.format("\u25B6 %s [%d:%02d]", info.songName, min, sec)));
        } else {
            lines.add(Component.literal("\u25A0 " + info.songName));
            return lines; // 没在播放就不显示歌词
        }

        // 歌词部分
        long songId = LyricCache.extractSongId(info.songUrl);
        if (songId < 0) {
            lines.add(Component.literal("\u4EC5\u652F\u6301\u7F51\u6613\u4E91\u6B4C\u8BCD"));
            return lines;
        }

        LyricRecord record = LyricCache.getLyric(songId, info.songName);
        if (record == null) {
            lines.add(Component.literal("\u52A0\u8F7D\u6B4C\u8BCD\u4E2D..."));
            return lines;
        }

        int totalTicks = info.songTime * 20 + 64;
        int playedTicks = totalTicks - musicPlayer.getCurrentTime();
        if (playedTicks < 0) playedTicks = 0;

        // 行2：原歌词
        String originLine = LyricCache.getCurrentLyricLine(record, playedTicks);
        lines.add(Component.literal((originLine != null && !originLine.isEmpty()) ? originLine : "~"));

        // 行3：翻译歌词（有翻译才加，节省行数）
        if (LyricCache.hasTranslation(record)) {
            String transLine = LyricCache.getCurrentTransLyricLine(record, playedTicks);
            lines.add(Component.literal((transLine != null && !transLine.isEmpty()) ? transLine : "~"));
        }

        return lines;
    }

    @Override
    protected String getTranslationKey() {
        return "netmusic_all_in_one";
    }
}
