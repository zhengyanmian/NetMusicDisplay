package com.netmusicdisplay.mixin;

import com.github.tartaricacid.netmusic.item.ItemMusicCD;
import com.github.tartaricacid.netmusic.tileentity.TileEntityMusicPlayer;
import com.netmusicdisplay.config.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 暂停续播 Mixin：拦截 CD 播放机的「从头播放」逻辑。
 *
 * 原理：
 * Net Music 的 setPlayToClient() 会在异步回调里调用 setCurrentTime(songTime*20+64)，
 * 把剩余时间重置为整首歌的时长——这就是「暂停后继续播放从头开始」的根因。
 *
 * 这里在 setPlayToClient() 开头检测是否为「暂停续播」（未播放且处于歌曲中途），
 * 若是，则把下一次 setCurrentTime() 的重置拦截掉，让剩余时间保持暂停时的位置。
 * 这样翻牌显示器上的歌词/时间会从暂停位置继续（服务端驱动）。
 */
@Mixin(TileEntityMusicPlayer.class)
public abstract class TileEntityMusicPlayerMixin {

    /** 标记下一次 setCurrentTime() 是否为「从头播放的重置」，需要被拦截 */
    @Unique
    private volatile boolean netmusicdisplay$resuming = false;

    @Shadow
    public abstract boolean isPlay();

    @Shadow
    public abstract int getCurrentTime();

    /**
     * 拦截 tickTime()：暂停时冻结剩余时间，不再递减。
     *
     * 根因：Net Music 的 tickTime() 无条件递减 currentTime，不检查 isPlay。
     * 导致暂停后（isPlay=false）剩余时间仍在走，歌词/时间显示继续跳，
     * 而客户端音频已停——表现为「歌停了但时间还在走」。
     */
    @Inject(method = "tickTime", at = @At("HEAD"), cancellable = true)
    private void netmusicdisplay$freezeWhenPaused(CallbackInfo ci) {
        if (!isPlay()) {
            ci.cancel();
        }
    }

    /**
     * 在 setPlayToClient() 开头判断本次是否为暂停续播。
     * 条件是：暂停续播开关开启、当前未播放、且剩余时间处于 (0, 总时长) 区间。
     */
    @Inject(method = "setPlayToClient", at = @At("HEAD"))
    private void netmusicdisplay$detectResume(ItemMusicCD.SongInfo info, CallbackInfo ci) {
        if (!Config.PAUSE_RESUME.get()) {
            netmusicdisplay$resuming = false;
            return;
        }
        int total = info.songTime * 20 + 64;
        int current = getCurrentTime();
        // 未在播放且处于歌曲中途 → 视为续播，拦截即将发生的重置
        netmusicdisplay$resuming = !isPlay() && current > 0 && current < total;
    }

    /**
     * 拦截「从头播放」的剩余时间重置。
     * 仅在检测到续播时生效，取消本次重置，保持暂停位置不变。
     */
    @Inject(method = "setCurrentTime", at = @At("HEAD"), cancellable = true)
    private void netmusicdisplay$blockTimeReset(int time, CallbackInfo ci) {
        if (netmusicdisplay$resuming) {
            ci.cancel();
            netmusicdisplay$resuming = false;
        }
    }
}
