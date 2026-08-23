package com.netmusicdisplay.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.netmusicdisplay.NetMusicDisplay;
import com.netmusicdisplay.config.Config;
import com.netmusicdisplay.netease.NetEaseLoginManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * 网易云登录指令。
 *
 * 用法（需要 OP 权限）：
 * - /netmusicdisplay qrlogin                    扫码登录（推荐，聊天框点链接扫码）
 * - /netmusicdisplay login email <邮箱> <密码>    邮箱登录
 * - /netmusicdisplay login phone <手机号>         发送手机验证码
 * - /netmusicdisplay login code <验证码>          用验证码完成登录
 * - /netmusicdisplay login <cookie>              手动粘贴 Cookie
 * - /netmusicdisplay logout                      清除 Cookie，恢复匿名 API
 * - /netmusicdisplay status                      查看当前登录状态
 */
@EventBusSubscriber(modid = NetMusicDisplay.MODID)
public class NetMusicCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
                Commands.literal("netmusicdisplay")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("qrlogin")
                                .executes(ctx -> qrLogin(ctx.getSource())))
                        .then(Commands.literal("login")
                                .then(Commands.literal("email")
                                        .then(Commands.argument("email", StringArgumentType.word())
                                                .then(Commands.argument("password", StringArgumentType.greedyString())
                                                        .executes(ctx -> emailLogin(ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "email"),
                                                                StringArgumentType.getString(ctx, "password"))))))
                                .then(Commands.literal("phone")
                                        .then(Commands.argument("phone", StringArgumentType.word())
                                                .executes(ctx -> sendPhoneCaptcha(ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "phone")))))
                                .then(Commands.literal("code")
                                        .then(Commands.argument("captcha", StringArgumentType.word())
                                                .executes(ctx -> phoneCaptchaLogin(ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "captcha")))))
                                .then(Commands.argument("cookie", StringArgumentType.greedyString())
                                        .executes(ctx -> login(ctx.getSource(), StringArgumentType.getString(ctx, "cookie")))))
                        .then(Commands.literal("logout")
                                .executes(ctx -> logout(ctx.getSource())))
                        .then(Commands.literal("status")
                                .executes(ctx -> status(ctx.getSource())))
        );
    }

    /** 扫码登录 */
    private static int qrLogin(CommandSourceStack source) {
        NetEaseLoginManager.startQrLogin(source);
        return 1;
    }

    /** 邮箱登录 */
    private static int emailLogin(CommandSourceStack source, String email, String password) {
        NetEaseLoginManager.emailLogin(source, email, password);
        return 1;
    }

    /** 发送手机验证码 */
    private static int sendPhoneCaptcha(CommandSourceStack source, String phone) {
        NetEaseLoginManager.sendPhoneCaptcha(source, phone);
        return 1;
    }

    /** 手机验证码登录 */
    private static int phoneCaptchaLogin(CommandSourceStack source, String captcha) {
        NetEaseLoginManager.phoneCaptchaLogin(source, captcha);
        return 1;
    }

    /** 设置 Cookie 并立即生效 */
    private static int login(CommandSourceStack source, String cookie) {
        if (cookie == null || cookie.trim().isEmpty()) {
            source.sendFailure(Component.literal("Cookie 不能为空。用法：/netmusicdisplay login <cookie>"));
            return 0;
        }
        // 写入配置并热更新 API
        Config.NETEASE_COOKIE.set(cookie.trim());
        NetMusicDisplay.applyCookie(cookie);
        saveConfig(source);
        source.sendSuccess(() -> Component.literal("§a已设置网易云 Cookie 并生效。"), true);
        return 1;
    }

    /** 清除 Cookie，恢复匿名 API */
    private static int logout(CommandSourceStack source) {
        Config.NETEASE_COOKIE.set("");
        NetMusicDisplay.applyCookie("");
        saveConfig(source);
        source.sendSuccess(() -> Component.literal("§e已清除网易云 Cookie，恢复匿名 API。"), true);
        return 1;
    }

    /** 查看当前登录状态 */
    private static int status(CommandSourceStack source) {
        String cookie = Config.NETEASE_COOKIE.get();
        if (cookie != null && !cookie.trim().isEmpty()) {
            source.sendSuccess(() -> Component.literal("§a网易云 Cookie 已配置（长度 " + cookie.trim().length() + " 字符）。"), false);
        } else {
            source.sendSuccess(() -> Component.literal("§7未配置网易云 Cookie，当前使用匿名 API。"), false);
        }
        return 1;
    }

    /** 把配置改动保存到文件 */
    private static void saveConfig(CommandSourceStack source) {
        try {
            Config.SPEC.save();
        } catch (Exception e) {
            source.sendFailure(Component.literal("§c配置保存失败（本次设置仍已生效，但重启后可能丢失）：" + e.getMessage()));
        }
    }
}
