package com.netmusicdisplay.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 模组配置类。
 *
 * 所有可调参数集中在这里，通过 NeoForge 的 ModConfigSpec 机制管理。
 * 配置文件生成在 config/netmusicdisplay-common.toml。
 *
 * 配置分组：
 * - lyric：歌词相关（偏移量）
 * - display：显示相关（暂停时是否显示歌词/时间）
 * - redstone：红石信号模式
 * - playback：播放行为（暂停续播）
 * - netease：网易云 Cookie 登录
 */
public class Config {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // ===== 歌词设置 =====
    public static final ModConfigSpec.IntValue LYRIC_OFFSET_TICKS;

    // ===== 显示设置 =====
    public static final ModConfigSpec.BooleanValue SHOW_LYRIC_WHEN_PAUSED;
    public static final ModConfigSpec.BooleanValue SHOW_PAUSE_TIME;
    public static final ModConfigSpec.BooleanValue PAUSE_SYMBOL_ENABLED;

    // ===== 红石模式 =====
    public static final ModConfigSpec.EnumValue<RedstoneMode> REDSTONE_MODE;

    // ===== 播放行为 =====
    public static final ModConfigSpec.BooleanValue PAUSE_RESUME;

    // ===== 网易云 Cookie =====
    public static final ModConfigSpec.ConfigValue<String> NETEASE_COOKIE;

    static {
        // --- 歌词 ---
        BUILDER.push("lyric");

        LYRIC_OFFSET_TICKS = BUILDER
                .comment("歌词时间偏移（tick）。正数=歌词提前显示，负数=歌词延后显示。1 秒 = 20 tick。")
                .comment("Lyric time offset in ticks. Positive = lyrics appear earlier, negative = later. 1 second = 20 ticks.")
                .defineInRange("lyric_offset_ticks", 0, -600, 600);

        BUILDER.pop();

        // --- 显示 ---
        BUILDER.push("display");

        SHOW_LYRIC_WHEN_PAUSED = BUILDER
                .comment("暂停时是否继续显示当前歌词行（而不是显示 ~）")
                .comment("Show current lyric line when paused instead of ~")
                .define("show_lyric_when_paused", true);

        SHOW_PAUSE_TIME = BUILDER
                .comment("暂停时是否在播放状态行显示暂停位置时间")
                .comment("Show pause position time in play status line when paused")
                .define("show_pause_time", true);

        PAUSE_SYMBOL_ENABLED = BUILDER
                .comment("播放状态行是否显示播放/暂停符号（▶/■）")
                .comment("Show play/pause symbol (▶/■) in play status line")
                .define("pause_symbol_enabled", true);

        BUILDER.pop();

        // --- 红石 ---
        BUILDER.push("redstone");

        REDSTONE_MODE = BUILDER
                .comment("红石信号模式")
                .comment("EDGE_TOGGLE: 边沿触发切换（原版行为，红石信号上升沿切换播放/暂停）")
                .comment("CONTINUOUS: 持续模式（有信号=播放，无信号=暂停，需配合暂停续播）")
                .comment("Redstone signal mode")
                .comment("EDGE_TOGGLE: Toggle on rising edge (vanilla behavior)")
                .comment("CONTINUOUS: Powered = play, unpowered = pause (requires pause_resume)")
                .defineEnum("redstone_mode", RedstoneMode.EDGE_TOGGLE);

        BUILDER.pop();

        // --- 播放 ---
        BUILDER.push("playback");

        PAUSE_RESUME = BUILDER
                .comment("暂停后继续播放时从暂停位置恢复，而非从头开始")
                .comment("Resume from pause position instead of restarting from beginning")
                .define("pause_resume", true);

        BUILDER.pop();

        // --- 网易云 ---
        BUILDER.push("netease");

        NETEASE_COOKIE = BUILDER
                .comment("网易云音乐 Cookie（用于访问 VIP 歌曲、私人 FM 等）。留空=不使用。")
                .comment("获取方式：浏览器登录网易云网页版，F12 打开开发者工具，Network 面板找任意请求的 Cookie 头。")
                .comment("NetEase Music cookie for VIP song access. Leave empty to disable.")
                .define("cookie", "");

        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    /**
     * 红石信号模式枚举。
     */
    public enum RedstoneMode {
        /** 边沿触发切换（原版行为） */
        EDGE_TOGGLE,
        /** 持续模式：有信号=播放，无信号=暂停 */
        CONTINUOUS
    }
}
