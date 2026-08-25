package com.netmusicdisplay.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.netmusicdisplay.client.CoverCache;
import com.netmusicdisplay.client.CoverRenderRegistry;
import com.simibubi.create.content.trains.display.FlapDisplayBlockEntity;
import com.simibubi.create.content.trains.display.FlapDisplayRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 翻牌显示器封面图文 Mixin（客户端）。
 *
 * 注入到 FlapDisplayRenderer.renderSafe 的【外层 popPose 之前】——
 * 此时 PoseStack 仍处于 Create 自己搭好的「翻牌正面 2D 平面」坐标系内，
 * 我们只需抵消 Create 循环累积的逐行 translate，即可在同一平面
 * 叠加绘制「播放状态符号 → 封面缩略图 → 歌名」。
 *
 * 这样完全复用 Create 的面部朝向变换，不自己重建矩阵，避免坐标错乱。
 */
@Mixin(FlapDisplayRenderer.class)
public abstract class FlapDisplayRendererMixin {

    private static final Logger LOG = LoggerFactory.getLogger("NetMusicDisplay");
    private static long lastDrawLog = 0L;

    @Inject(method = "renderSafe",
            at = @At(value = "INVOKE",
                     target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V",
                     ordinal = 1, shift = At.Shift.BEFORE))
    private void netmusicdisplay$renderCover(FlapDisplayBlockEntity be, float partialTicks,
                                             PoseStack ms, MultiBufferSource buffer,
                                             int light, int overlay, CallbackInfo ci) {
        if (be == null) {
            return;
        }
        CoverRenderRegistry.CoverInfo info = CoverRenderRegistry.get(be.getBlockPos());
        if (info == null) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastDrawLog > 2000L) {
            lastDrawLog = now;
            LOG.info("[CoverDebug] drawing overlay pos={} name={} playing={}", be.getBlockPos(), info.songName, info.isPlaying);
        }

        // 此时 ms 已在 Create 的正面平面内；抵消循环累积的逐行偏移，回到第 0 行顶部
        int lines = be.getLines().size();
        ms.pushPose();
        ms.translate(0.0f, -16.0f * lines, 0.0f);

        Matrix4f pose = ms.last().pose();
        Font font = Minecraft.getInstance().font;

        // 1) 播放状态：左侧符号（绿 ▶=播放，橙 ■=暂停），与现有数据源符号一致
        int statusColor = info.isPlaying ? 0x55FF66 : 0xFFAA33;
        font.drawInBatch(info.isPlaying ? "\u25B6" : "\u25A0", 1.0f, 9.0f, statusColor, false,
                pose, buffer, Font.DisplayMode.NORMAL, 0, light);

        // 2) 封面：紧贴状态右侧；未加载时留空（歌名仍显示）。y∈[1,15] 适配单行高度
        ResourceLocation cover = CoverCache.getLocation(info.songId);
        if (cover != null) {
            VertexConsumer vcCover = buffer.getBuffer(RenderType.entityCutoutNoCull(cover));
            drawTexturedRect(vcCover, pose, 10.0f, 1.0f, 24.0f, 15.0f, 0.02f, light);
        }

        // 3) 歌名：封面右侧
        int textColor = info.isPlaying ? 0xCCFFCC : 0xFFE0B0;
        font.drawInBatch(info.songName, 27.0f, 9.0f, textColor, false,
                pose, buffer, Font.DisplayMode.NORMAL, 0, light);

        ms.popPose();
    }

    /** 纹理矩形（用于封面图，V 坐标取反以适配 Y 翻转的正面坐标系，使图片正向显示） */
    private static void drawTexturedRect(VertexConsumer vc, Matrix4f pose,
                                         float x0, float y0, float x1, float y1, float z, int light) {
        vc.addVertex(pose, x0, y0, z).setColor(1.0f, 1.0f, 1.0f, 1.0f).setUv(0.0f, 1.0f).setLight(light).setNormal(0.0f, 0.0f, 1.0f);
        vc.addVertex(pose, x1, y0, z).setColor(1.0f, 1.0f, 1.0f, 1.0f).setUv(1.0f, 1.0f).setLight(light).setNormal(0.0f, 0.0f, 1.0f);
        vc.addVertex(pose, x1, y1, z).setColor(1.0f, 1.0f, 1.0f, 1.0f).setUv(1.0f, 0.0f).setLight(light).setNormal(0.0f, 0.0f, 1.0f);
        vc.addVertex(pose, x0, y1, z).setColor(1.0f, 1.0f, 1.0f, 1.0f).setUv(0.0f, 0.0f).setLight(light).setNormal(0.0f, 0.0f, 1.0f);
    }
}
