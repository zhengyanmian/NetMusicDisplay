package com.netmusicdisplay.mixin;

import com.github.tartaricacid.netmusic.api.lyric.LyricRecord;
import com.github.tartaricacid.netmusic.client.audio.NetMusicAudioStream;
import com.github.tartaricacid.netmusic.client.audio.NetMusicSound;
import com.netmusicdisplay.client.ResumeTracker;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.URL;
import java.util.concurrent.CompletableFuture;

/**
 * 客户端音频续播 Mixin。
 *
 * Net Music 的 NetMusicSound.tick 字段在构造器里硬编码为 0，音频流也从头播放。
 * 这里在构造时从 ResumeTracker 取出续播起始 tick，设置 tick 初值，
 * 并在音频流创建完成后（getStream 返回的 CompletableFuture）执行 seek。
 */
@Mixin(NetMusicSound.class)
public abstract class NetMusicSoundMixin {

    @Shadow
    private int tick;

    /** 待 seek 的续播位置（音频流创建完成后消费） */
    @Unique
    private int netmusicdisplay$resumeTick = 0;

    /**
     * 构造完成后：若存在续播位置，把 tick 设为起始位置。
     * 这样歌词同步、播放完成判断都从正确位置开始。
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void netmusicdisplay$initResume(BlockPos pos, URL url, int timeSecond, LyricRecord record, CallbackInfo ci) {
        int startTick = ResumeTracker.take(pos);
        if (startTick > 0) {
            this.tick = startTick;
            this.netmusicdisplay$resumeTick = startTick;
        }
    }

    /**
     * getStream 返回前：追加一个 thenApply，在音频流创建完成后 seek 到续播位置。
     * 时机：getStream 的 CompletableFuture 先创建 NetMusicAudioStream（后台线程），
     * seek 在其后、声音引擎开始 read 之前执行，此时底层流尚未被消费，skip 有效。
     */
    @Inject(method = "getStream", at = @At("RETURN"), cancellable = true)
    private void netmusicdisplay$applySeek(SoundBufferLibrary library, Sound sound, boolean looping,
                                           CallbackInfoReturnable<CompletableFuture<AudioStream>> cir) {
        int seek = this.netmusicdisplay$resumeTick;
        if (seek <= 0) {
            return;
        }
        this.netmusicdisplay$resumeTick = 0;
        CompletableFuture<AudioStream> future = cir.getReturnValue();
        cir.setReturnValue(future.thenApply(stream -> {
            if (stream instanceof NetMusicAudioStream audioStream) {
                ((NetMusicAudioStreamAccessor) audioStream).netmusicdisplay$seekTo(seek);
            }
            return stream;
        }));
    }
}
