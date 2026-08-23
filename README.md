# Net Music Display | 不是显示来源

> 让机械动力（Create）的显示链接器读取网络音乐机（Net Music）的播放数据，在翻牌显示器 / 霓虹灯管 / 牌子上显示歌曲名、播放状态、歌词、封面，并支持网易云登录、音质选择与暂停续播。

An addon for **Create** that adds **Net Music** display sources to the Display Link — showing song name, play status, lyrics, album cover, and more from a Net Music CD player, with NetEase login, audio quality selection, and pause-resume support.

---

## 功能 / Features

用显示链接器（Display Link）指向网络音乐机的 CD 播放机，可选择以下 **6 种数据源**：

| 数据源 | 说明 |
|--------|------|
| 歌曲名 | 当前播放唱片的歌曲名称 |
| 播放状态 | 正在播放 / 已停止 |
| 原歌词 | 根据播放进度同步显示当前原歌词行 |
| 翻译歌词 | 根据播放进度同步显示当前翻译歌词行 |
| 综合信息（多行） | 播放状态 + 歌曲名 + 原歌词 + 翻译歌词 |
| 双行歌词（多行） | 原歌词 + 翻译歌词上下排列 |

歌词由服务端直接从网易云 API 获取并缓存，不依赖客户端数据。

### 进阶功能

- **网易云登录**：游戏内登录界面，支持 **扫码二维码**（界面直接显示，无需浏览器）、**邮箱 + 密码**、**手机 + 验证码** 三种方式。登录后可播放 VIP 歌曲、解锁无损 / Hi-Res 音质。
- **音质选择**：在配置界面选择 `standard / higher / exhigh / lossless / hires`，VIP 账号可解锁高品质。
- **暂停续播**：暂停后再次播放，会 **从暂停的位置精确接播**（声音与歌词同步，位置精确），而非从头开始，也不会丢失片段。
- **封面显示**：翻牌显示器可显示当前歌曲的专辑封面。
- **动力臂交互**：可通过 Create 动力臂与 CD 播放机交互。
- **红石改进**：基于 Create 6.0.x 显示链接器的新 API。
- **配置系统**：中文配置界面，可设置音质、歌词偏移等。

## 前置依赖 / Dependencies

| 模组 | 1.21.1 NeoForge | 1.20.1 Forge |
|------|-----------------|--------------|
| Minecraft | 1.21.1 | 1.20.1 |
| Create（机械动力） | 6.0.10+ | 6.0.8+ |
| Net Music（网络音乐机） | 1.5.1+ | 1.5.1+ |

## 安装 / Installation

1. 确认已安装上述前置模组
2. 将本模组 jar 文件放入 `mods` 文件夹
3. **服务端和客户端都需要安装**

## 使用方法 / Usage

### 基础显示

1. 放置 **翻牌显示器**
2. 用 **显示链接器（Display Link）** 右键点击，放置在 **网络音乐机的 CD 播放机** 上
3. 选择你想要的数据源（歌曲名 / 播放状态 / 歌词等）
4. 即可在翻牌显示器上看到信息

### 网易云登录

1. 在游戏内打开登录界面（配置界面中的登录入口，或 `/netmusicdisplay qrlogin` 指令生成二维码）
2. 选择登录方式：
   - **扫码**：界面直接显示二维码，用网易云 App 扫码授权
   - **邮箱**：输入邮箱 + 密码（密码以 MD5 传输）
   - **手机**：输入手机号，获取验证码后填写
3. 登录成功后即可播放 VIP 歌曲、解锁高品质音质

### 暂停续播

- 播放中暂停，再次播放时会 **从暂停位置精确接播**，歌词同步前进，不会从头播放，也不会丢失片段。
- 注意：首次续播可能有极短的同步延迟，属正常现象。

## 从源码构建 / Building from Source

```bash
git clone https://github.com/zhengyanmian/NetMusicDisplay.git
cd NetMusicDisplay

# 下载 Net Music 的 jar 到 libs/ 目录
# NeoForge 1.21.1 版: https://modrinth.com/mod/net-music/versions
# 放到 libs/ 目录下

# 构建
./gradlew build

# 产物在 build/libs/ 目录
```

> 需要 JDK 21（1.21.1 NeoForge）或 JDK 17（1.20.1 Forge）。

## 分支说明 / Branches

| 分支 | MC 版本 | 模组加载器 | 模组版本 |
|------|---------|-----------|---------|
| `main` | 1.21.1 | NeoForge | 2.3.0 |
| `1.20.1-forge` | 1.20.1 | Forge | 1.1.0 |

## 技术细节 / Technical Details

- Create 的显示链接器是 **服务端方块系统**，`DisplaySource` 的数据采集逻辑在服务端运行
- Net Music 的歌词原本是 **纯客户端字段**，服务端无法直接获取
- 本模组通过 `LyricCache` 让服务端自行调用网易云歌词 API，异步获取并缓存歌词，根据 CD 播放机的播放进度（tick）计算当前歌词行
- **暂停续播**：通过 Mixin 拦截 Net Music 的 `tickTime()` 冻结服务端播放进度，并在客户端于 `NetMusicAudioStream` 的 `pumpBuffers` 之前精确 seek 音频流，实现声音与歌词同步续播
- 两个版本的 Net Music API 完全一致（同为 1.5.1）；Create 6.0.x 的 DisplaySource API 在 1.20.1 与 1.21.1 间已统一，数据源代码可直接复用

## 开源协议 / License

[MIT License](LICENSE) - 可自由使用、修改、分发，只需保留版权声明。

## 致谢 / Credits

- [Create](https://github.com/Creators-of-Create/Create) - 机械动力，由 simibubi 等开发
- [Net Music](https://modrinth.com/mod/net-music) - 网络音乐机，由 TartaricAcid 开发
- 网易云登录功能参考开源模组 [NetMusic-BetterLogin](https://github.com/ming-sc/NetMusic-BetterLogin)（MIT）

本模组不包含也不分发上述模组的任何代码或资源文件。
