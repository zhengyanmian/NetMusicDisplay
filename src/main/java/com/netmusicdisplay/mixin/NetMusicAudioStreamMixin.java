package com.netmusicdisplay.mixin;

import com.github.tartaricacid.netmusic.client.audio.NetMusicAudioStream;
import com.netmusicdisplay.client.ResumeTracker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import java.net.URL;

/**
 * 给 Net Music 的 NetMusicAudioStream 注入 seek 能力。
 *
 * NetMusicAudioStream 内部持有转码成 PCM 的 AudioInputStream（final stream 字段），
 * 音频数据由 loadAudioData() 懒加载（第一次 read 时才在异步线程读 stream）。
 *
 * 续播位置通过普通类 ResumeTracker.pendingSeekTick 传递：
 * NetMusicSound 构造时写入，本类构造完成后（首次 read 之前）消费并 seek。
 */
@Mixin(NetMusicAudioStream.class)
public abstract class NetMusicAudioStreamMixin {

    private static final Logger LOGGER = LogManager.getLogger("NetMusicDisplay");

    @Shadow
    @Final
    private AudioInputStream stream;

    /** 构造完成后：若存在待续播位置，则 seek 音频流到该位置 */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void netmusicdisplay$applySeek(URL url, CallbackInfo ci) {
        int seek = ResumeTracker.pendingSeekTick;
        if (seek > 0) {
            ResumeTracker.pendingSeekTick = 0;
            netmusicdisplay$seek(seek);
        }
    }

    /**
     * 把底层音频流 seek 到指定 tick。
     * 用 read 精确丢弃字节（skip 对转换流可能跳过整帧导致位置不准）。
     * 换算：字节数 = (tick / 20 秒) × 采样率 × 每帧字节数。
     */
    @Unique
    private void netmusicdisplay$seek(int tick) {
        try {
            AudioFormat format = this.stream.getFormat();
            int frameSize = format.getFrameSize();
            float frameRate = format.getFrameRate();
            if (frameSize <= 0 || frameRate <= 0) {
                return;
            }
            long targetBytes = (long) ((tick / 20.0) * frameRate * frameSize);
            long discarded = 0;
            byte[] buf = new byte[8192];
            while (discarded < targetBytes) {
                int want = (int) Math.min(buf.length, targetBytes - discarded);
                int n = this.stream.read(buf, 0, want);
                if (n <= 0) {
                    break; // 流已读完，退化为从头
                }
                discarded += n;
            }
            LOGGER.info("[NetMusicDisplay] 音频续播 seek：tick={} 目标字节={} 实际丢弃={} (frameRate={} frameSize={})",
                    tick, targetBytes, discarded, frameRate, frameSize);
        } catch (Exception e) {
            LOGGER.error("[NetMusicDisplay] 音频续播 seek 失败，退化为从头播放", e);
        }
    }
}
