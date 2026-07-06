package com.netmusicdisplay;

import com.github.tartaricacid.netmusic.init.InitBlocks;
import com.netmusicdisplay.source.NetMusicAllInOneSource;
import com.netmusicdisplay.source.NetMusicDualLyricSource;
import com.netmusicdisplay.source.NetMusicLyricSource;
import com.netmusicdisplay.source.NetMusicPlayStatusSource;
import com.netmusicdisplay.source.NetMusicSongNameSource;
import com.netmusicdisplay.source.NetMusicTransLyricSource;
import com.simibubi.create.content.redstone.displayLink.AllDisplayBehaviours;
import com.simibubi.create.content.redstone.displayLink.source.DisplaySource;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * 模组主类（Forge 1.20.1 版）。
 *
 * 作用：把自定义的 Create 显示数据源注册进 Create 的 AllDisplayBehaviours，
 * 并关联到 Net Music 的 CD 播放机方块实体上。
 *
 * 与 1.21.1 NeoForge 版的区别：
 * - 1.20.1 的 Create 0.5.1 用 AllDisplayBehaviours.register() + assignBlockEntity() 注册，
 *   不需要 DeferredRegister + CreateRegistries（那是 6.0.x 的方式）。
 * - NetMusic API 完全一致（同为 1.5.1 版本），source 代码直接复用。
 */
@Mod(NetMusicDisplay.MODID)
public class NetMusicDisplay {
    public static final String MODID = "netmusicdisplay";

    public NetMusicDisplay() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(NetMusicDisplay::commonSetup);
    }

    private static void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // 注册六个数据源并关联到 Net Music CD 播放机方块实体。
            // 玩家用显示链接器指向 CD 播放机时，会看到六个选项：
            //   单行：歌曲名 / 播放状态 / 原歌词 / 翻译歌词
            //   多行：综合（状态+歌名+原歌词+翻译）/ 双行歌词（原歌词+翻译）
            register("netmusic_song_name", new NetMusicSongNameSource());
            register("netmusic_play_status", new NetMusicPlayStatusSource());
            register("netmusic_lyric", new NetMusicLyricSource());
            register("netmusic_trans_lyric", new NetMusicTransLyricSource());
            register("netmusic_all_in_one", new NetMusicAllInOneSource());
            register("netmusic_dual_lyric", new NetMusicDualLyricSource());
        });
    }

    /**
     * 注册一个 DisplaySource 并关联到 Net Music CD 播放机方块实体。
     * 1.20.1 的 Create 用 AllDisplayBehaviours 管理，不需要注册表。
     */
    private static void register(String name, DisplaySource source) {
        AllDisplayBehaviours.register(new ResourceLocation(MODID, name), source);
        AllDisplayBehaviours.assignBlockEntity(source, InitBlocks.MUSIC_PLAYER_TE.get());
    }
}
