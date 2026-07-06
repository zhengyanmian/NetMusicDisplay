package com.netmusicdisplay;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.github.tartaricacid.netmusic.init.InitBlocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * 模组主类。
 *
 * 作用：把一个自定义的 Create 显示数据源（NetMusicSongNameSource）注册进 Create 的
 * DISPLAY_SOURCE 注册表，并把它关联到 Net Music 的 CD 播放机方块实体上。
 * 这样玩家用显示链接器指向 CD 播放机时，就能在翻牌显示器上看到当前唱片的名字。
 */
@Mod(NetMusicDisplay.MODID)
public class NetMusicDisplay {
    public static final String MODID = "netmusicdisplay";

    public NetMusicDisplay(IEventBus modBus) {
        // 注册自定义数据源到 Create 的注册表
        ModDisplaySources.register(modBus);
        // 在通用初始化阶段，把数据源关联到 Net Music 的 CD 播放机方块实体
        modBus.addListener(NetMusicDisplay::commonSetup);
    }

    private static void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // DisplaySource.BY_BLOCK_ENTITY 是 Create 提供的「方块实体类型 -> 数据源」映射表。
            // 它是 Multi 类型，可以对同一个方块实体注册多个数据源。
            // 玩家用显示链接器指向 Net Music CD 播放机时，会看到六个选项：
            //   单行：歌曲名 / 播放状态 / 原歌词 / 翻译歌词
            //   多行：综合（状态+歌名+原歌词+翻译）/ 双行歌词（原歌词+翻译）
            DisplaySource.BY_BLOCK_ENTITY.add(
                    InitBlocks.MUSIC_PLAYER_TE.get(),
                    ModDisplaySources.NETMUSIC_SONG_NAME.get()
            );
            DisplaySource.BY_BLOCK_ENTITY.add(
                    InitBlocks.MUSIC_PLAYER_TE.get(),
                    ModDisplaySources.NETMUSIC_PLAY_STATUS.get()
            );
            DisplaySource.BY_BLOCK_ENTITY.add(
                    InitBlocks.MUSIC_PLAYER_TE.get(),
                    ModDisplaySources.NETMUSIC_LYRIC.get()
            );
            DisplaySource.BY_BLOCK_ENTITY.add(
                    InitBlocks.MUSIC_PLAYER_TE.get(),
                    ModDisplaySources.NETMUSIC_TRANS_LYRIC.get()
            );
            DisplaySource.BY_BLOCK_ENTITY.add(
                    InitBlocks.MUSIC_PLAYER_TE.get(),
                    ModDisplaySources.NETMUSIC_ALL_IN_ONE.get()
            );
            DisplaySource.BY_BLOCK_ENTITY.add(
                    InitBlocks.MUSIC_PLAYER_TE.get(),
                    ModDisplaySources.NETMUSIC_DUAL_LYRIC.get()
            );
        });
    }
}
