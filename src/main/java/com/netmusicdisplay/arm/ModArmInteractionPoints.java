package com.netmusicdisplay.arm;

import com.netmusicdisplay.NetMusicDisplay;
import com.simibubi.create.api.registry.CreateRegistries;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 注册动力臂交互点类型。
 *
 * 让 Create 的机械臂（Mechanical Arm）能够识别 Net Music 的 CD 播放机，
 * 从而支持自动放入/取出唱片。
 */
public class ModArmInteractionPoints {
    public static final DeferredRegister<ArmInteractionPointType> ARM_INTERACTION_POINT_TYPES =
            DeferredRegister.create(CreateRegistries.ARM_INTERACTION_POINT_TYPE, NetMusicDisplay.MODID);

    public static final DeferredHolder<ArmInteractionPointType, MusicPlayerArmInteractionPointType> MUSIC_PLAYER =
            ARM_INTERACTION_POINT_TYPES.register("music_player", MusicPlayerArmInteractionPointType::new);

    public static void register(IEventBus modBus) {
        ARM_INTERACTION_POINT_TYPES.register(modBus);
    }
}
