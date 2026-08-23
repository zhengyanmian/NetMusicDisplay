package com.netmusicdisplay.netease;

import com.github.tartaricacid.netmusic.api.NetWorker;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.netmusicdisplay.NetMusicDisplay;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 网易云登录管理器。
 *
 * 参照开源模组「网络音乐机:更好的登录」(ming-sc/NetMusic-BetterLogin, MIT) 的实现。
 * 关键结论：网易云登录接口全部为明文 GET，无需 weapi 加密：
 * - 二维码：/api/login/qrcode/unikey?type=3 + /api/login/qrcode/client/login?key=xxx&type=3
 * - 邮箱登录：/api/w/login/?username=xxx&password=md5(xxx)&type=0
 * - 手机验证码：/api/sms/captcha/sent?cellphone=xxx + /api/w/login/cellphone?phone=xxx&captcha=xxx&type=1
 * - 邮箱/手机登录密码需 MD5（邮箱），手机验证码明文
 */
public class NetEaseLoginManager {
    private static final org.apache.logging.log4j.Logger LOGGER =
            org.apache.logging.log4j.LogManager.getLogger("NetMusicDisplay");

    private static final Map<String, String> HEADERS = new HashMap<>() {{
        put("Content-Type", "application/x-www-form-urlencoded");
        put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Safari/537.36 Chrome/91.0.4472.164 NeteaseMusicDesktop/3.1.6");
        put("Referer", "https://music.163.com");
    }};

    /** 防止重复启动登录流程 */
    private static volatile boolean loginInProgress = false;
    /** 待验证的手机号（发送验证码后保存，验证码登录时使用） */
    private static volatile String pendingPhone = null;

    // ==================== 二维码登录 ====================

    public static void startQrLogin(CommandSourceStack source) {
        if (!beginLogin(source)) return;
        CompletableFuture.runAsync(() -> {
            try {
                String unikey = getQRKey();
                if (unikey == null) {
                    send(source, Component.literal("§c获取二维码失败，请检查网络后重试。"), false);
                    return;
                }
                String loginUrl = "https://music.163.com/login?codekey=" + unikey;
                Component link = Component.literal("§e§n[点击这里打开网页扫码]")
                        .withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, loginUrl)));
                send(source, Component.literal("§a请用手机网易云音乐 APP 扫码登录："), false);
                send(source, link, false);
                send(source, Component.literal("§7（二维码约 2 分钟有效，扫码后请在手机上确认）"), false);

                for (int i = 0; i < 40; i++) {
                    Thread.sleep(3000);
                    HttpResponse<String> resp = checkQRStatus(unikey);
                    int code = parseCode(resp);
                    if (code == 800) {
                        send(source, Component.literal("§c二维码已过期，请重新执行 /netmusicdisplay qrlogin。"), false);
                        return;
                    }
                    if (code == 802) {
                        send(source, Component.literal("§e已扫码，请在手机上点击确认。"), false);
                    }
                    if (code == 803) {
                        String cookie = extractCookie(resp);
                        if (cookie != null && !cookie.isEmpty()) {
                            applyLogin(source, cookie);
                        } else {
                            send(source, Component.literal("§c登录成功但获取 Cookie 失败，请重试。"), false);
                        }
                        return;
                    }
                }
                send(source, Component.literal("§c登录超时，请重新执行 /netmusicdisplay qrlogin。"), false);
            } catch (Exception e) {
                LOGGER.error("[NetEaseLogin] QR login failed", e);
                send(source, Component.literal("§c登录流程出错：" + e.getMessage()), false);
            } finally {
                loginInProgress = false;
            }
        });
    }

    // ==================== 邮箱登录 ====================

    public static void emailLogin(CommandSourceStack source, String email, String password) {
        if (!beginLogin(source)) return;
        CompletableFuture.runAsync(() -> {
            try {
                String url = String.format(
                        "https://music.163.com/api/w/login/?username=%s&rememberLogin=true&password=%s&https=true&type=0",
                        urlEncode(email), md5(password));
                HttpResponse<String> resp = httpGet(url);
                String cookie = extractCookie(resp);
                if (cookie != null && !cookie.isEmpty()) {
                    applyLogin(source, cookie);
                } else {
                    send(source, Component.literal("§c邮箱登录失败，请检查邮箱和密码是否正确。"), false);
                }
            } catch (Exception e) {
                LOGGER.error("[NetEaseLogin] Email login failed", e);
                send(source, Component.literal("§c邮箱登录出错：" + e.getMessage()), false);
            } finally {
                loginInProgress = false;
            }
        });
    }

    // ==================== 手机验证码登录 ====================

    public static void sendPhoneCaptcha(CommandSourceStack source, String phone) {
        CompletableFuture.runAsync(() -> {
            try {
                String url = String.format("https://music.163.com/api/sms/captcha/sent?cellphone=%s&ctcode=86", phone);
                String body = NetWorker.get(url, HEADERS);
                pendingPhone = phone;
                send(source, Component.literal("§a验证码已发送到手机 " + phone + "，请查收短信。"), false);
                send(source, Component.literal("§7收到后执行 /netmusicdisplay login code <验证码> 完成登录。"), false);
            } catch (Exception e) {
                LOGGER.error("[NetEaseLogin] Send captcha failed", e);
                send(source, Component.literal("§c发送验证码失败：" + e.getMessage()), false);
            }
        });
    }

    public static void phoneCaptchaLogin(CommandSourceStack source, String captcha) {
        if (pendingPhone == null) {
            source.sendFailure(Component.literal("§c请先执行 /netmusicdisplay login phone <手机号> 获取验证码。"));
            return;
        }
        if (!beginLogin(source)) return;
        String phone = pendingPhone;
        CompletableFuture.runAsync(() -> {
            try {
                String url = String.format(
                        "https://music.163.com/api/w/login/cellphone?phone=%s&countrycode=86&rememberLogin=true&captcha=%s&https=true&type=1",
                        phone, urlEncode(captcha));
                HttpResponse<String> resp = httpGet(url);
                String cookie = extractCookie(resp);
                if (cookie != null && !cookie.isEmpty()) {
                    pendingPhone = null;
                    applyLogin(source, cookie);
                } else {
                    send(source, Component.literal("§c手机验证码登录失败，请检查验证码是否正确。"), false);
                }
            } catch (Exception e) {
                LOGGER.error("[NetEaseLogin] Phone captcha login failed", e);
                send(source, Component.literal("§c手机验证码登录出错：" + e.getMessage()), false);
            } finally {
                loginInProgress = false;
            }
        });
    }

    // ==================== 内部工具 ====================

    private static boolean beginLogin(CommandSourceStack source) {
        if (loginInProgress) {
            source.sendFailure(Component.literal("§c已有登录流程进行中，请稍候。"));
            return false;
        }
        loginInProgress = true;
        return true;
    }

    private static String getQRKey() throws Exception {
        String body = NetWorker.get("https://music.163.com/api/login/qrcode/unikey?type=3", HEADERS);
        JsonObject obj = JsonParser.parseString(body).getAsJsonObject();
        return obj.has("unikey") ? obj.get("unikey").getAsString() : null;
    }

    private static HttpResponse<String> checkQRStatus(String key) throws Exception {
        return httpGet("https://music.163.com/api/login/qrcode/client/login?key=" + key + "&type=3");
    }

    private static int parseCode(HttpResponse<String> resp) {
        try {
            JsonObject obj = JsonParser.parseString(resp.body()).getAsJsonObject();
            return obj.has("code") ? obj.get("code").getAsInt() : -1;
        } catch (Exception e) {
            return -1;
        }
    }

    /** 发起 GET 请求并返回完整响应（含响应头，用于提取 Cookie） */
    private static HttpResponse<String> httpGet(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(java.net.URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("User-Agent", HEADERS.get("User-Agent"))
                .header("Referer", HEADERS.get("Referer"))
                .GET()
                .build();
        return NetWorker.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /** 从响应头 Set-Cookie 提取 Cookie，组合成 "key=value; key=value" 字符串 */
    private static String extractCookie(HttpResponse<String> resp) {
        List<String> setCookies = resp.headers().allValues("Set-Cookie");
        if (setCookies == null || setCookies.isEmpty()) return null;
        Map<String, String> cookies = new HashMap<>();
        for (String sc : setCookies) {
            String body = sc.split(";")[0].trim();
            String[] parts = body.split("=", 2);
            if (parts.length == 2) {
                cookies.put(parts[0].trim(), parts[1].trim());
            }
        }
        if (cookies.isEmpty()) return null;
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : cookies.entrySet()) {
            if (sb.length() > 0) sb.append("; ");
            sb.append(e.getKey()).append("=").append(e.getValue());
        }
        return sb.toString();
    }

    private static void applyLogin(CommandSourceStack source, String cookie) {
        source.getServer().execute(() -> {
            try {
                com.netmusicdisplay.config.Config.NETEASE_COOKIE.set(cookie);
                NetMusicDisplay.applyCookie(cookie);
                com.netmusicdisplay.config.Config.SPEC.save();
                source.sendSuccess(() -> Component.literal("§a登录成功！网易云 Cookie 已生效并保存，VIP 歌曲可正常播放。"), true);
            } catch (Exception e) {
                LOGGER.error("[NetEaseLogin] Failed to apply login", e);
                source.sendFailure(Component.literal("§c登录成功但应用 Cookie 失败：" + e.getMessage()));
            }
        });
    }

    private static void send(CommandSourceStack source, Component message, boolean broadcast) {
        try {
            source.getServer().execute(() -> source.sendSuccess(() -> message, broadcast));
        } catch (Exception e) {
            LOGGER.error("[NetEaseLogin] Failed to send message", e);
        }
    }

    private static String md5(String str) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] array = md.digest(str.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : array) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    private static String urlEncode(String s) {
        return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
    }
}
