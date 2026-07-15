package com.netmusicdisplay;

import com.netmusicdisplay.source.NetMusicAllInOneSource;
import com.netmusicdisplay.source.NetMusicDualLyricSource;
import com.netmusicdisplay.source.NetMusicLyricSource;
import com.netmusicdisplay.source.NetMusicPlayStatusSource;
import com.netmusicdisplay.source.NetMusicSongNameSource;
import com.netmusicdisplay.source.NetMusicTransLyricSource;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.api.registry.CreateRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * 负责把自定义的 DisplaySource 注册到 Create 6.0.x 的 DISPLAY_SOURCE 注册表。
 *
 * Create 6.0.x 用 DeferredRegister + CreateRegistries.DISPLAY_SOURCE 替代了
 * 旧版 0.5.1 的 AllDisplayBehaviours.register()。
 *
 * 注册了六个数据源：
 * 单行（各需 1 个显示链接器）：
 * - netmusic_song_name：歌曲名 + 歌手
 * - netmusic_play_status：播放状态（▶ 播放中 / ■ 已停止）+ 剩余时间
 * - netmusic_lyric：当前原歌词行
 * - netmusic_trans_lyric：当前翻译歌词行
 * 多行（1 个链接器输出多行，省链接器）：
 * - netmusic_all_in_one：综合（状态+歌名 / 原歌词 / 翻译歌词），3 行
 * - netmusic_dual_lyric：原歌词 + 翻译歌词，2 行
 */
public class ModDisplaySources {
    // Create 6.0.x 的 DISPLAY_SOURCE 注册表键
    // 使用 ResourceLocation 形式兼容 Forge 47.x 的 DeferredRegister
    public static final DeferredRegister<DisplaySource> DISPLAY_SOURCES =
            DeferredRegister.create(new ResourceLocation("create", "display_source"), NetMusicDisplay.MODID);

    public static final RegistryObject<DisplaySource> NETMUSIC_SONG_NAME =
            DISPLAY_SOURCES.register("netmusic_song_name", NetMusicSongNameSource::new);

    public static final RegistryObject<DisplaySource> NETMUSIC_PLAY_STATUS =
            DISPLAY_SOURCES.register("netmusic_play_status", NetMusicPlayStatusSource::new);

    public static final RegistryObject<DisplaySource> NETMUSIC_LYRIC =
            DISPLAY_SOURCES.register("netmusic_lyric", NetMusicLyricSource::new);

    public static final RegistryObject<DisplaySource> NETMUSIC_TRANS_LYRIC =
            DISPLAY_SOURCES.register("netmusic_trans_lyric", NetMusicTransLyricSource::new);

    public static final RegistryObject<DisplaySource> NETMUSIC_ALL_IN_ONE =
            DISPLAY_SOURCES.register("netmusic_all_in_one", NetMusicAllInOneSource::new);

    public static final RegistryObject<DisplaySource> NETMUSIC_DUAL_LYRIC =
            DISPLAY_SOURCES.register("netmusic_dual_lyric", NetMusicDualLyricSource::new);

    public static void register(IEventBus modBus) {
        DISPLAY_SOURCES.register(modBus);
    }
}
