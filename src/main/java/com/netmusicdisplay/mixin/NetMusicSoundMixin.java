package com.netmusicdisplay.mixin;

import com.github.tartaricacid.netmusic.api.lyric.LyricRecord;
import com.github.tartaricacid.netmusic.client.audio.NetMusicSound;
import com.netmusicdisplay.client.ResumeTracker;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.URL;

/**
 * 客户端音频续播 Mixin。
 *
 * Net Music 的 NetMusicSound.tick 字段在构造器里硬编码为 0，音频流也从头播放。
 * 这里在构造时从 ResumeTracker 取出续播起始 tick，设置 tick 初值，
 * 并把起始位置写入 NetMusicAudioStreamMixin.pendingSeekTick，
 * 供随后创建的 NetMusicAudioStream 在首次 read 之前 seek。
 */
@Mixin(NetMusicSound.class)
public abstract class NetMusicSoundMixin {

    @Shadow
    private int tick;

    /**
     * 构造完成后：若存在续播位置，把 tick 设为起始位置，
     * 并写入待 seek 位置供音频流消费。
     * 这样歌词同步、播放完成判断、声音都从正确位置开始。
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void netmusicdisplay$initResume(BlockPos pos, URL url, int timeSecond, LyricRecord record, CallbackInfo ci) {
        int startTick = ResumeTracker.take(pos);
        if (startTick > 0) {
            this.tick = startTick;
        }
        // 无条件写入（含 0），既传递续播位置，也清掉上次可能残留的 seek 值
        NetMusicAudioStreamMixin.pendingSeekTick = startTick;
    }
}
