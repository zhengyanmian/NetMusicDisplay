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
 * 客户端歌词续播 Mixin。
 *
 * Net Music 的 NetMusicSound.tick 字段在构造器里硬编码为 0，音频流也从头播放。
 * 这里在构造时从 ResumeTracker（普通类）取出续播起始 tick，设置 tick 初值，
 * 并把起始位置写入 ResumeTracker.pendingSeekTick，供随后创建的 NetMusicAudioStream 消费。
 *
 * 注意：跨 Mixin 传递数据一律用普通类 ResumeTracker，绝不用 Mixin 类的静态字段
 * （无 refmap 下会被错误映射，导致 NoSuchFieldError）。
 */
@Mixin(NetMusicSound.class)
public abstract class NetMusicSoundMixin {

    @Shadow
    private int tick;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void netmusicdisplay$initResume(BlockPos pos, URL url, int timeSecond, LyricRecord record, CallbackInfo ci) {
        int startTick = ResumeTracker.take(pos);
        if (startTick > 0) {
            this.tick = startTick;
        }
        // 无条件写入（含 0），既传递续播位置，也清掉上次可能残留的 seek 值
        ResumeTracker.pendingSeekTick = startTick;
    }
}
