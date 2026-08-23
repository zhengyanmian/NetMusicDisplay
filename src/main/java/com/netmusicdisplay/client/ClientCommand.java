package com.netmusicdisplay.client;

import com.mojang.brigadier.CommandDispatcher;
import com.netmusicdisplay.NetMusicDisplay;
import com.netmusicdisplay.client.gui.LoginScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * 客户端指令：打开登录界面。
 *
 * /netmusicdisplay gui —— 打开游戏内登录界面（扫码/邮箱/手机验证码）。
 */
@EventBusSubscriber(modid = NetMusicDisplay.MODID, value = Dist.CLIENT)
public class ClientCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(
                Commands.literal("netmusicdisplay")
                        .then(Commands.literal("gui")
                                .executes(ctx -> {
                                    Minecraft.getInstance().setScreen(new LoginScreen());
                                    return 1;
                                }))
        );
    }
}
