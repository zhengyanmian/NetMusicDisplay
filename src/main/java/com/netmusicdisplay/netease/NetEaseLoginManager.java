package com.netmusicdisplay.netease;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.netmusicdisplay.NetMusicDisplay;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 网易云二维码登录管理器。
 *
 * 流程（网易云原生接口，明文无需加密）：
 * 1. GET /api/login/qrcode/unikey 获取二维码 key
 * 2. 在聊天框发送可点击的扫码链接（点击打开 music.163.com/login?codekey=xxx）
 * 3. 后台轮询 /api/login/qrcode/client/login 检查扫码状态
 *    801=等待扫码，802=已扫码待确认，803=登录成功，800=过期
 * 4. 成功后从响应头提取 Cookie（MUSIC_U 等）并应用到 Net Music API
 */
public class NetEaseLoginManager {
    private static final Logger LOGGER = LogManager.getLogger("NetMusicDisplay");
    private static final String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64)";
    private static final String REFERER = "https://music.163.com";

    /** 防止重复启动登录流程 */
    private static volatile boolean loginInProgress = false;

    /**
     * 启动二维码登录流程。
     * 在独立线程执行网络请求和轮询，消息通过服务器主线程发送。
     */
    public static void startQrLogin(CommandSourceStack source) {
        if (loginInProgress) {
            source.sendFailure(Component.literal("§c已有登录流程进行中，请稍候。"));
            return;
        }
        loginInProgress = true;
        CompletableFuture.runAsync(() -> qrLoginFlow(source));
    }

    private static void qrLoginFlow(CommandSourceStack source) {
        try {
            // 1. 获取二维码 key
            String unikey = fetchUnikey();
            if (unikey == null) {
                sendMessage(source, Component.literal("§c获取二维码失败，请检查网络后重试。"), false);
                return;
            }

            // 2. 发送可点击的扫码链接
            String loginUrl = "https://music.163.com/login?codekey=" + unikey;
            Component link = Component.literal("§e§n[点击这里打开网页扫码]")
                    .withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, loginUrl)));
            sendMessage(source, Component.literal("§a请用手机网易云音乐 APP 扫码登录："), false);
            sendMessage(source, link, false);
            sendMessage(source, Component.literal("§7（二维码约 2 分钟有效，扫码后请在手机上确认）"), false);

            // 3. 轮询扫码状态（每 3 秒一次，最多 40 次 = 120 秒）
            for (int i = 0; i < 40; i++) {
                Thread.sleep(3000);
                int code = checkStatus(unikey);

                if (code == 800) {
                    sendMessage(source, Component.literal("§c二维码已过期，请重新执行 /netmusicdisplay qrlogin。"), false);
                    return;
                }
                if (code == 802) {
                    sendMessage(source, Component.literal("§e已扫码，请在手机上点击确认。"), false);
                }
                if (code == 803) {
                    // 登录成功，提取 Cookie
                    String cookie = extractCookie(unikey);
                    if (cookie != null && !cookie.isEmpty()) {
                        applyLogin(source, cookie);
                    } else {
                        sendMessage(source, Component.literal("§c登录成功但获取 Cookie 失败，请重试。"), false);
                    }
                    return;
                }
            }
            sendMessage(source, Component.literal("§c登录超时，请重新执行 /netmusicdisplay qrlogin。"), false);
        } catch (Exception e) {
            LOGGER.error("[NetEaseLogin] QR login flow failed", e);
            sendMessage(source, Component.literal("§c登录流程出错：" + e.getMessage()), false);
        } finally {
            loginInProgress = false;
        }
    }

    /** 获取二维码 key */
    private static String fetchUnikey() throws Exception {
        String json = httpGet("https://music.163.com/api/login/qrcode/unikey?type=1");
        if (json == null) return null;
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        int code = obj.has("code") ? obj.get("code").getAsInt() : -1;
        if (code != 200 || !obj.has("unikey")) return null;
        return obj.get("unikey").getAsString();
    }

    /** 检查扫码状态，返回状态码（800/801/802/803） */
    private static int checkStatus(String unikey) throws Exception {
        String json = httpGet("https://music.163.com/api/login/qrcode/client/login?key=" + unikey + "&type=1");
        if (json == null) return -1;
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        return obj.has("code") ? obj.get("code").getAsInt() : -1;
    }

    /** 从登录响应中提取 Cookie（MUSIC_U、__csrf 等） */
    private static String extractCookie(String unikey) throws Exception {
        HttpURLConnection conn = null;
        try {
            URL url = new URL("https://music.163.com/api/login/qrcode/client/login?key=" + unikey + "&type=1");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", UA);
            conn.setRequestProperty("Referer", REFERER);
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);

            List<String> cookies = new ArrayList<>();
            // 读取所有 Set-Cookie 头
            java.util.Map<String, java.util.List<String>> headers = conn.getHeaderFields();
            java.util.List<String> setCookies = headers.get("Set-Cookie");
            if (setCookies != null) {
                for (String sc : setCookies) {
                    if (sc != null && sc.contains("=")) {
                        // 只取 "key=value" 部分（去掉 path/domain 等属性）
                        String part = sc.split(";")[0].trim();
                        cookies.add(part);
                    }
                }
            }

            if (cookies.isEmpty()) {
                // 兜底：从响应体里找 cookie 字段
                try (InputStream in = conn.getInputStream()) {
                    String json = new String(in.readAllBytes());
                    JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                    if (obj.has("cookie")) {
                        return obj.get("cookie").getAsString();
                    }
                }
                return null;
            }
            return String.join("; ", cookies);
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /** 应用登录结果：写入配置、热更新 API、持久化 */
    private static void applyLogin(CommandSourceStack source, String cookie) {
        // 需要回到主线程执行（配置写入 + 消息发送）
        source.getServer().execute(() -> {
            try {
                com.netmusicdisplay.config.Config.NETEASE_COOKIE.set(cookie);
                NetMusicDisplay.applyCookie(cookie);
                com.netmusicdisplay.config.Config.SPEC.save();
                source.sendSuccess(() -> Component.literal("§a登录成功！网易云 Cookie 已生效并保存。"), true);
            } catch (Exception e) {
                LOGGER.error("[NetEaseLogin] Failed to apply login", e);
                source.sendFailure(Component.literal("§c登录成功但应用 Cookie 失败：" + e.getMessage()));
            }
        });
    }

    /** 在主线程发送消息 */
    private static void sendMessage(CommandSourceStack source, Component message, boolean broadcast) {
        try {
            source.getServer().execute(() -> source.sendSuccess(() -> message, broadcast));
        } catch (Exception e) {
            LOGGER.error("[NetEaseLogin] Failed to send message", e);
        }
    }

    /** 简单 HTTP GET，返回响应体字符串 */
    private static String httpGet(String urlStr) throws Exception {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", UA);
            conn.setRequestProperty("Referer", REFERER);
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            try (InputStream in = conn.getInputStream()) {
                return new String(in.readAllBytes());
            }
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}
