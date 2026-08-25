package com.netmusicdisplay;

import com.netmusicdisplay.client.gui.ConfigScreen;
import com.netmusicdisplay.qq.QqCredentialManager;
import com.netmusicdisplay.search.SearchSourceManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import java.nio.file.Path;

/**
 * 客户端专用模组入口。
 *
 * 注册：自定义配置界面（IConfigScreenFactory）、多平台搜索源、QQ 登录凭证加载。
 */
@Mod(value = NetMusicDisplay.MODID, dist = Dist.CLIENT)
public class NetMusicDisplayClient {

    public NetMusicDisplayClient(ModContainer container) {
        // 注册自定义配置界面（列表模式 / 每页数量 / QQ 登录），取代 NeoForge 默认配置界面
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (modContainer, parent) -> new ConfigScreen(modContainer, parent));
        // 注册多平台搜索源（网易云 / QQ音乐 / 酷狗 ...）
        SearchSourceManager.registerAll();
        // 加载 QQ 登录凭证（位于游戏配置目录）
        Path configDir = FMLPaths.CONFIGDIR.get();
        QqCredentialManager.init(configDir);
    }
}
