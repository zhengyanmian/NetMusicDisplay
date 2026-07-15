package com.netmusicdisplay;

import com.github.tartaricacid.netmusic.init.InitBlocks;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * 模组主类（Forge 1.20.1 版，支持 Create 6.0.x）。
 *
 * 作用：把自定义的 Create 显示数据源注册进 Create 的 DISPLAY_SOURCE 注册表，
 * 并关联到 Net Music 的 CD 播放机方块实体上。
 *
 * Create 6.0.x 的注册方式与 1.21.1 NeoForge 版一致：
 * - 用 DeferredRegister + CreateRegistries.DISPLAY_SOURCE 注册数据源类型
 * - 在 commonSetup 里用 DisplaySource.BY_BLOCK_ENTITY.add() 关联到方块实体
 *
 * 与旧版 0.5.1 的区别：
 * - 0.5.1 用 AllDisplayBehaviours.register() + assignBlockEntity()（已废弃）
 * - 6.0.x 用 DeferredRegister + DisplaySource.BY_BLOCK_ENTITY（新 API）
 */
@Mod(NetMusicDisplay.MODID)
public class NetMusicDisplay {
    public static final String MODID = "netmusicdisplay";

    public NetMusicDisplay() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        // 注册自定义数据源到 Create 的注册表
        ModDisplaySources.register(modBus);
        modBus.addListener(NetMusicDisplay::commonSetup);
    }

    private static void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // 把数据源关联到 Net Music CD 播放机方块实体。
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
