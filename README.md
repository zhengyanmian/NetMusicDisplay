# Net Music Display | 不是显示来源

> 让机械动力（Create）的显示链接器能读取网络音乐机（Net Music）里唱片的歌曲名、播放状态和歌词，并显示在翻牌显示器 / 霓虹灯管 / 牌子上。

An addon for **Create** that adds **Net Music** display sources to the Display Link, letting you show song name, play status, and lyrics on Display Boards / Nixie Tubes / Signs.

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

## 前置依赖 / Dependencies

| 模组 | 1.21.1 NeoForge | 1.20.1 Forge |
|------|-----------------|--------------|
| Minecraft | 1.21.1 | 1.20.1 |
| Create（机械动力） | 6.0.10+ | 0.5.1.f |
| Net Music（网络音乐机） | 1.5.1+ | 1.5.1+ |

## 安装 / Installation

1. 确认已安装上述前置模组
2. 将本模组 jar 文件放入 `mods` 文件夹
3. **服务端和客户端都需要安装**

## 使用方法 / Usage

1. 放置一个 **显示链接器（Display Link）**
2. 用显示链接器右键点击 **网络音乐机的 CD 播放机**
3. 选择你想要的数据源（歌曲名 / 播放状态 / 歌词等）
4. 在翻牌显示器、霓虹灯管或牌子上即可看到信息

## 从源码构建 / Building from Source

```bash
git clone https://github.com/<your-username>/NetMusicDisplay.git
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

| 分支 | MC 版本 | 模组加载器 |
|------|---------|-----------|
| `main` | 1.21.1 | NeoForge |
| `1.20.1-forge` | 1.20.1 | Forge |

## 技术细节 / Technical Details

- Create 的显示链接器是**服务端方块系统**，`DisplaySource` 的数据采集逻辑在服务端运行
- Net Music 的歌词原本是**纯客户端字段**，服务端无法直接获取
- 本模组通过 `LyricCache` 让服务端自行调用网易云歌词 API，异步获取并缓存歌词，根据 CD 播放机的播放进度（tick）计算当前歌词行
- 两个版本的 Net Music API 完全一致（同为 1.5.1），Create 的 DisplaySource API 包名不同，已分别适配

## 开源协议 / License

[MIT License](LICENSE) - 可自由使用、修改、分发，只需保留版权声明。

## 致谢 / Credits

- [Create](https://github.com/Creators-of-Create/Create) - 机械动力，由 simibubi 等开发
- [Net Music](https://modrinth.com/mod/net-music) - 网络音乐机，由 TartaricAcid 开发

本模组不包含也不分发上述模组的任何代码或资源文件。
