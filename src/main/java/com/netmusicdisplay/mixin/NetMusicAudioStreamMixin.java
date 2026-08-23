package com.netmusicdisplay.mixin;

import com.github.tartaricacid.netmusic.client.audio.NetMusicAudioStream;
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
 * 续播位置通过静态字段 {@link #pendingSeekTick} 传递：
 * NetMusicSound 构造时写入，NetMusicAudioStream 构造完成后（首次 read 之前）消费，
 * 在 stream 未被读取前调用 skip() 跳到指定位置。
 */
@Mixin(NetMusicAudioStream.class)
public abstract class NetMusicAudioStreamMixin {

    /**
     * 待 seek 的续播位置（已播放 tick 数）。
     * NetMusicSound 构造器（主线程）写入，NetMusicAudioStream 构造器（后台线程）消费，
     * 跨线程共享，故用 volatile。
     */
    public static volatile int pendingSeekTick = 0;

    @Shadow
    @Final
    private AudioInputStream stream;

    /** 构造完成后：若存在待续播位置，则 skip 音频流到该位置 */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void netmusicdisplay$applySeek(URL url, CallbackInfo ci) {
        int seek = pendingSeekTick;
        if (seek > 0) {
            pendingSeekTick = 0;
            netmusicdisplay$seek(seek);
        }
    }

    /**
     * 把底层音频流 skip 到指定 tick。
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
            long skipped = 0;
            while (skipped < targetBytes) {
                long n = this.stream.skip(targetBytes - skipped);
                if (n <= 0) {
                    break; // 底层流不支持 skip，退化为从头播放
                }
                skipped += n;
            }
        } catch (Exception ignored) {
            // seek 失败则退化为从头播放
        }
    }
}
