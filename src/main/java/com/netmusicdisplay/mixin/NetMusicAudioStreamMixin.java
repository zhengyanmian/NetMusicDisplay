package com.netmusicdisplay.mixin;

import com.github.tartaricacid.netmusic.client.audio.NetMusicAudioStream;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

/**
 * 给 Net Music 的 NetMusicAudioStream 注入 seek 能力。
 *
 * NetMusicAudioStream 内部持有转码成 PCM 的 AudioInputStream（final stream 字段），
 * 音频数据由 loadAudioData() 懒加载（第一次 read 时才开始读）。
 *
 * 这里在首次 read 之前调用 skip() 跳到指定位置，实现「从暂停点继续播放声音」。
 */
@Mixin(NetMusicAudioStream.class)
public abstract class NetMusicAudioStreamMixin implements NetMusicAudioStreamAccessor {

    @Shadow
    private AudioInputStream stream;

    /**
     * 把底层音频流 seek 到指定 tick。
     * 换算：字节数 = (tick / 20 秒) × 采样率 × 每帧字节数。
     */
    @Unique
    @Override
    public void netmusicdisplay$seekTo(int tick) {
        if (tick <= 0) {
            return;
        }
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
