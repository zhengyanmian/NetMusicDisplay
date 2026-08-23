package com.netmusicdisplay.mixin;

import com.github.tartaricacid.netmusic.api.lyric.LyricRecord;
import com.github.tartaricacid.netmusic.client.audio.NetMusicSound;
import com.github.tartaricacid.netmusic.tileentity.TileEntityMusicPlayer;
import com.netmusicdisplay.client.ResumeTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.URL;

/**
 * 客户端声音 Mixin：续播位置 + 暂停停止。
 *
 * 两个功能：
 * 1. 构造时从 ResumeTracker 取出续播起始 tick，设置 tick 初值，
 *    并把起始位置写入 ResumeTracker.pendingSeekTick 供 NetMusicAudioStream 消费。
 * 2. tick() 时检查方块实体的 isPlay，若已暂停则把 tick 设到结尾之后，
 *    让原版 tick() 的 stop() 逻辑自然触发，停止声音和粒子。
 *
 * 第 2 点的根因：Net Music 的 setPlay(false) 只设置服务端 isPlay 字段，
 * 客户端的 NetMusicSound 仍在声音管理器中继续 tick（吐粒子+继续播放）。
 * 原版靠 tickTime() 递减 currentTime 到 <16 触发 tick() 停止逻辑，
 * 但我们的 TileEntityMusicPlayerMixin 冻结了 tickTime()，导致停止逻辑永不触发。
 * 后果：暂停后旧声音不停 → 粒子残留 + 下次续播旧声音盖住新声音 → 歌词对不上。
 */
@Mixin(NetMusicSound.class)
public abstract class NetMusicSoundMixin {

    private static final Logger LOGGER = LogManager.getLogger("NetMusicDisplay");

    @Shadow
    private int tick;

    @Shadow
    @Final
    private BlockPos pos;

    @Shadow
    @Final
    private int tickTimes;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void netmusicdisplay$initResume(BlockPos pos, URL url, int timeSecond, LyricRecord record, CallbackInfo ci) {
        int startTick = ResumeTracker.take(pos);
        if (startTick > 0) {
            this.tick = startTick;
        }
        // 无条件写入（含 0），既传递续播位置，也清掉上次可能残留的 seek 值
        ResumeTracker.pendingSeekTick = startTick;
    }

    /**
     * 暂停时停止客户端声音。
     *
     * 检查方块实体的 isPlay，若已暂停则把 tick 设到 tickTimes + 51，
     * 让原版 tick() 的 tick++ → 检查 tick > tickTimes + 50 → stop() 逻辑自然触发。
     * 不 cancel，让原版代码继续跑（它会执行 stop() + 清理 lyricRecord）。
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void netmusicdisplay$stopWhenPaused(CallbackInfo ci) {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        BlockEntity be = level.getBlockEntity(this.pos);
        if (be instanceof TileEntityMusicPlayer player && !player.isPlay()) {
            LOGGER.info("[NetMusicDisplay] 暂停检测：停止声音 pos={} tick={}/{}", this.pos, this.tick, this.tickTimes);
            this.tick = this.tickTimes + 51;
        }
    }
}
