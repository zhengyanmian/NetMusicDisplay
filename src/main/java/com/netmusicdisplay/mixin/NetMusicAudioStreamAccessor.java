package com.netmusicdisplay.mixin;

/**
 * 通过 Mixin 注入到 {@code NetMusicAudioStream} 的访问器接口。
 *
 * Mixin 会让 Net Music 的 NetMusicAudioStream 在运行时实现本接口，
 * 从而让其它 Mixin（如 NetMusicSoundMixin）能够调用 {@link #netmusicdisplay$seekTo(int)}
 * 把底层 AudioInputStream 跳到指定位置。
 */
public interface NetMusicAudioStreamAccessor {

    /**
     * 把音频流 seek 到指定 tick（已播放的 tick 数）。
     * seek 失败（底层流不支持 skip）时静默退化为从头播放。
     */
    void netmusicdisplay$seekTo(int tick);
}
