package com.netmusicdisplay;

import com.netmusicdisplay.source.NetMusicAllInOneSource;
import com.netmusicdisplay.source.NetMusicCoverSource;
import com.netmusicdisplay.source.NetMusicDualLyricSource;
import com.netmusicdisplay.source.NetMusicLyricSource;
import com.netmusicdisplay.source.NetMusicPlayStatusSource;
import com.netmusicdisplay.source.NetMusicSongNameSource;
import com.netmusicdisplay.source.NetMusicTransLyricSource;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.api.registry.CreateRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 负责把自定义的 DisplaySource 注册到 Create 的 DISPLAY_SOURCE 注册表。
 *
 * 注册了七个数据源：
 * 单行（各需 1 个显示链接器）：
 * - netmusic_song_name：歌曲名 + 歌手
 * - netmusic_play_status：播放状态（▶ 播放中 / ■ 已停止）+ 剩余时间
 * - netmusic_lyric：当前原歌词行
 * - netmusic_trans_lyric：当前翻译歌词行
 * - netmusic_cover：翻牌封面图文（客户端 Mixin 绘制 状态→封面→歌名）
 * 多行（1 个链接器输出多行，省链接器）：
 * - netmusic_all_in_one：综合（状态+歌名 / 原歌词 / 翻译歌词），3 行
 * - netmusic_dual_lyric：原歌词 + 翻译歌词，2 行
 */
public class ModDisplaySources {
    public static final DeferredRegister<DisplaySource> DISPLAY_SOURCES =
            DeferredRegister.create(CreateRegistries.DISPLAY_SOURCE, NetMusicDisplay.MODID);

    public static final DeferredHolder<DisplaySource, NetMusicSongNameSource> NETMUSIC_SONG_NAME =
            DISPLAY_SOURCES.register("netmusic_song_name", NetMusicSongNameSource::new);

    public static final DeferredHolder<DisplaySource, NetMusicPlayStatusSource> NETMUSIC_PLAY_STATUS =
            DISPLAY_SOURCES.register("netmusic_play_status", NetMusicPlayStatusSource::new);

    public static final DeferredHolder<DisplaySource, NetMusicCoverSource> NETMUSIC_COVER =
            DISPLAY_SOURCES.register("netmusic_cover", NetMusicCoverSource::new);

    public static final DeferredHolder<DisplaySource, NetMusicLyricSource> NETMUSIC_LYRIC =
            DISPLAY_SOURCES.register("netmusic_lyric", NetMusicLyricSource::new);

    public static final DeferredHolder<DisplaySource, NetMusicTransLyricSource> NETMUSIC_TRANS_LYRIC =
            DISPLAY_SOURCES.register("netmusic_trans_lyric", NetMusicTransLyricSource::new);

    public static final DeferredHolder<DisplaySource, NetMusicAllInOneSource> NETMUSIC_ALL_IN_ONE =
            DISPLAY_SOURCES.register("netmusic_all_in_one", NetMusicAllInOneSource::new);

    public static final DeferredHolder<DisplaySource, NetMusicDualLyricSource> NETMUSIC_DUAL_LYRIC =
            DISPLAY_SOURCES.register("netmusic_dual_lyric", NetMusicDualLyricSource::new);

    public static void register(IEventBus modBus) {
        DISPLAY_SOURCES.register(modBus);
    }
}
