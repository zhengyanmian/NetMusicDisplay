package com.netmusicdisplay;

import com.netmusicdisplay.arm.ModArmInteractionPoints;
import com.netmusicdisplay.config.Config;
import com.github.tartaricacid.netmusic.NetMusic;
import com.github.tartaricacid.netmusic.api.NetEaseMusic;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.github.tartaricacid.netmusic.init.InitBlocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
    private static final Logger LOGGER = LogManager.getLogger("NetMusicDisplay");

    public NetMusicDisplay(IEventBus modBus, ModContainer modContainer) {
        // 注册模组配置
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        // 注册自定义数据源到 Create 的注册表
        ModDisplaySources.register(modBus);
        // 注册动力臂交互点类型（让机械臂能识别 CD 播放机）
        ModArmInteractionPoints.register(modBus);
        // 在通用初始化阶段，把数据源关联到 Net Music 的 CD 播放机方块实体
        modBus.addListener(NetMusicDisplay::commonSetup);
    }

    private static void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // 如果配置了网易云 Cookie，用带认证的 API 替换 Net Music 默认的匿名 API
            initNeteaseCookie();

            LOGGER.info("[NetMusicDisplay] Registering display sources to MUSIC_PLAYER_TE...");

            // 检查 InitBlocks.MUSIC_PLAYER_TE 是否存在
            if (InitBlocks.MUSIC_PLAYER_TE == null) {
                LOGGER.error("[NetMusicDisplay] InitBlocks.MUSIC_PLAYER_TE is null! NetMusic mod may not be loaded.");
                return;
            }
            if (InitBlocks.MUSIC_PLAYER_TE.get() == null) {
                LOGGER.error("[NetMusicDisplay] InitBlocks.MUSIC_PLAYER_TE.get() is null! Registration not ready.");
                return;
            }

            LOGGER.info("[NetMusicDisplay] MUSIC_PLAYER_TE = {}", InitBlocks.MUSIC_PLAYER_TE.get());

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

            LOGGER.info("[NetMusicDisplay] All 6 display sources registered successfully!");
        });
    }

    /**
     * 如果配置了网易云 Cookie，用带认证的 API 替换 Net Music 默认的匿名 API。
     *
     * Net Music 的 NET_EASE_WEB_API 是 public static 且非 final 字段，
     * 这里用 NetEaseMusic(cookie) 构造器创建带认证的实例替换它，
     * 从而支持 VIP 歌曲、私人 FM 等需要登录的功能。
     */
    private static void initNeteaseCookie() {
        String cookie = Config.NETEASE_COOKIE.get();
        if (cookie == null || cookie.trim().isEmpty()) {
            LOGGER.info("[NetMusicDisplay] 未配置网易云 Cookie，使用匿名 API。");
            return;
        }
        try {
            NetMusic.NET_EASE_WEB_API = new NetEaseMusic(cookie.trim()).getApi();
            LOGGER.info("[NetMusicDisplay] 已启用网易云 Cookie 认证 API。");
        } catch (Exception e) {
            LOGGER.error("[NetMusicDisplay] 初始化网易云 Cookie API 失败，回退到匿名 API。", e);
        }
    }
}
