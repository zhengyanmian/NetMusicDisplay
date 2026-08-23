package com.netmusicdisplay;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * 客户端专用模组入口。
 *
 * NeoForge 1.21.1 起，模组要在「模组列表 → Config 按钮」里显示配置界面，
 * 必须显式注册 IConfigScreenFactory 扩展点。否则 Config 按钮是灰色的。
 * 这里注册 NeoForge 内置的 ConfigurationScreen，自动解析本模组的配置文件。
 */
@Mod(value = NetMusicDisplay.MODID, dist = Dist.CLIENT)
public class NetMusicDisplayClient {

    public NetMusicDisplayClient(ModContainer container) {
        // 注册 NeoForge 内置的配置界面，让 Config 按钮可点击
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (modContainer, parent) -> new ConfigurationScreen(modContainer, parent));
    }
}
