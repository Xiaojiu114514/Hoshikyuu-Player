# Hoshikyuu Player

**Hoshikyuu Player** 是一款极简风格的 Android 音乐播放器，专注于在线音乐搜索与高品质流媒体播放。采用 Kotlin + Jetpack Compose + Material 3 原生开发，以 MVVM + Clean Architecture 分层架构组织代码。

> 本项目由 AI 辅助完成全栈开发。

---

## 功能一览

| 功能 | 状态 |
|------|------|
| 多源音乐搜索（网易云） | ✅ |
| 在线流媒体播放（后台 + 锁屏） | ✅ |
| 全屏播放器（毛玻璃背景 + 封面 + 歌词 + 进度控制） | ✅ |
| LRC 滚动歌词（自动同步高亮） | ✅ |
| 三态循环模式（顺序 / 单曲 / 列表） | ✅ |
| 歌曲收藏（Room 持久化） | ✅ |
| 播放队列管理（插播 / 移除 / 跳转） | ✅ |
| 迷你播放条（底部悬浮） | ✅ |
| 通知栏控制（MediaSessionService） | ✅ |
| 深色 / 浅色主题（跟随系统 + 手动切换） | ✅ |
| 播放历史（Room 持久化） | ✅ |
| 自定义歌单（多对多关联） | ✅ |
| 离线下载（OkHttp 流式下载） | ✅ |
| 本地播放缓存（自动缓存已播歌曲） | ✅ |
| 个人头像（相册选取 / Bitmap 压缩） | ✅ |
| 搜索历史（最近 10 条标签） | ✅ |
| 首页推荐（热歌榜 + 新歌榜） | ✅ |
| 桌面歌词 | ✅ |
| 通知栏控制 | ✅ |
| 系统媒体控制中心（MediaSession）对接 | ✅ |

---

## 技术栈

| 层次 | 技术选型 |
|------|---------|
| 语言 | Kotlin 100%（Coroutines + Flow） |
| UI | Jetpack Compose + Material 3 |
| 架构 | MVVM + Clean Architecture（data / domain / ui 三层） |
| 依赖注入 | Hilt (Dagger) |
| 网络 | Retrofit + OkHttp + Kotlin Serialization |
| 播放器 | Media3 ExoPlayer + MediaSessionService |
| 图片加载 | Coil Compose |
| 数据库 | Room（7 张 Entity 表，版本 6） |
| 构建 | Gradle KTS（AGP 8.7.3, Kotlin 2.0.21, Compose BOM 2024.11） |
| 最低 SDK | 26 (Android 8.0) |
| 目标 SDK | 35 (Android 15) |

---

## 项目架构

采用 Clean Architecture 三层架构，目前为单一 Gradle 模块：

```
com.hoshikyuu.player/
├── data/                          # 数据层
│   ├── local/
│   │   ├── dao/                   #   5 个 DAO 接口
│   │   ├── entity/                #   7 个 Entity 类
│   │   └── AppDatabase.kt        #   Room 数据库（版本 6）
│   ├── remote/
│   │   ├── MusicApi.kt           #   Retrofit API 定义
│   │   ├── ApiModels.kt          #   网络请求 / 响应模型
│   │   ├── NetworkClient.kt      #   OkHttp + Retrofit 客户端
│   │   └── RankingResponse.kt   #   排行榜响应模型
│   ├── repository/               #   9 个 Repository
│   │   ├── MusicRepository       #   搜索 + 详情 + 排行榜
│   │   ├── FavoriteRepository    #   收藏 CRUD
│   │   ├── PlaylistRepository    #   歌单（多对多）
│   │   ├── HistoryRepository     #   播放历史
│   │   ├── DownloadRepository    #   下载记录
│   │   ├── SongCacheRepository   #   歌曲缓存元数据
│   │   ├── RankingCacheRepository#   排行榜缓存
│   │   ├── SettingRepository     #   设置持久化
│   │   ├── AvatarRepository      #   头像管理
│   │   └── LocalSongRepository   #   本地歌曲扫描
├── di/                            # Hilt 依赖注入（16 个 Provider）
├── domain/
│   ├── Models.kt                 # Song / Playlist / UiState 核心模型
│   └── usecase/                  # （预留，尚无实现）
├── player/
│   ├── PlayerManager.kt          # ExoPlayer 状态管理器
│   ├── MusicService.kt           # MediaSessionService 后台播放
│   ├── PlayerController.kt       # MediaController 封装
│   ├── DownloadManager.kt        # OkHttp 流式下载缓存
│   ├── DesktopLyricsManager.kt   # 桌面歌词状态管理
│   └── LyricsOverlayService.kt   # 桌面歌词悬浮窗服务
├── ui/
│   ├── components/
│   │   ├── MiniPlayerBar         #   底部悬浮播放条
│   │   ├── SongItem              #   歌曲卡片
│   │   ├── QueueSheet            #   播放队列弹窗
│   │   └── CommonComponents      #   Loading / Error / List 包装
│   ├── navigation/               #   导航图 + Screen 定义
│   ├── screens/
│   │   ├── home/                 #   首页探索（排行榜）
│   │   ├── search/               #   搜索（历史标签 + 结果列表）
│   │   ├── player/               #   全屏播放器
│   │   ├── library/              #   音乐库（收藏 / 歌单 / 历史 / 下载）
│   │   ├── playlist/             #   歌单列表 + 歌单详情
│   │   ├── download/             #   下载管理
│   │   ├── setting/              #   设置
│   │   └── local/                #   本地歌曲列表
│   └── utils/
│       └── LyricParser.kt        #   LRC 歌词解析器
├── HoshikyuuApp.kt               # Application 入口
└── MainActivity.kt               # 主 Activity + 底部导航 + 迷你播放条
```

### 播放流程

```
用户点击播放
  ├─ 检查本地缓存（下载/缓存目录）
  │   ├─ 找到 → 直接播放（含歌词加载）
  │   └─ 未找到 → 检查是否为本地文件路径
  │       ├─ 是 → 加载歌词（同目录 .lrc）→ 播放
  │       └─ 否 → 网络请求详情 API（mp3Url + lrc）
  │           ├─ 成功 → 播放 + 异步缓存
  │           └─ 失败 → 显示错误
  ├─ 更新播放状态（currentSong / isPlaying / progress / duration）
  └─ 同步到系统媒体控制中心（MediaSession）
```

PlayerManager 通过 StateFlow 暴露全部播放状态：`currentSong` / `isPlaying` / `progress` / `duration` / `queue` / `currentIndex` / `repeatMode` / `favoriteIds` / `favoriteSongs` / `playHistory` / `errorMessage`。

---

## Room 数据库结构（版本 6）

### Entity 关系

```
┌──────────────┐    ┌──────────────────────┐    ┌────────────────┐
│   favorites  │    │      playlists       │    │    history     │
├──────────────┤    ├──────────────────────┤    ├────────────────┤
│ songId (PK)  │    │ id (PK, autoGen)     │    │ songId (PK)    │
│ songName     │    │ name                 │    │ songName       │
│ songArtist   │    │ createdAt            │    │ songArtist     │
│ songAlbum    │    └──────────┬───────────┘    │ playedAt       │
│ songCoverUrl │               │                └────────────────┘
│ songMp3Url   │   ┌──────────────────────────┐
│ source       │   │    playlist_songs        │
│ addedAt      │   ├──────────────────────────┤
└──────────────┘   │playlistId(FK → playlists)│
                   │songId / songName / artist│
┌──────────────┐   │ album / coverUrl / mp3Url│
│  song_cache  │   │ lrc / source / addedOrder│
├──────────────┤   └──────────────────────────┘
│ songId (PK)  │
│ name / album │   ┌──────────────┐    ┌───────────────────┐
│ artist       │   │  downloads   │    │  ranking_cache    │
│ coverUrl     │   ├──────────────┤    ├───────────────────┤
│ mp3Url / lrc │   │ songId (PK)  │    │ rankingId (PK)    │
│ source       │   │ filePath     │    │ rankingName       │
│ cachedAt     │   │ lrc / source │    │ songsJson         │
└──────────────┘   └──────────────┘    │ cachedDate / At   │
                                       └───────────────────┘
```

数据库共经历 5 次迁移（1→2→3→4→5→6）。

---

## API 接口

项目使用第三方音乐聚合 API（base URL: `https://???/`），封装在 `MusicApi` 与 `MusicRepository` 中。

| API | 参数 | 说明 |
|-----|------|------|
| `???` | `token`, `name`, `type`, `page`, `limit` | 歌曲搜索 |
| `???` | `token`, `id`, `type` | 歌曲详情（封面 / mp3Url / LRC） |
| `???` | `token`, `id`, `type` | 排行榜数据 |

支持的音乐源：`wy`（网易云）。API 模型通过 `FlexibleIdSerializer` 兼容数字型（网易云）与字符串型（QQ 音乐）ID。

### 数据流

```
搜索 → API 获取基本信息（id / name / artist / album）
  ↓
点击播放 → 触发详情 API 获取 mp3Url + lrc
  ↓
PlayerManager 开始流式播放 + 异步缓存至本地
```

---

## 页面导航

底部导航三页：**探索（首页）** → **搜索** → **我的（音乐库）**
全屏播放器、歌单列表、歌单详情、下载管理、设置通过路由跳转。

| Screen | 路由 | 说明 |
|--------|------|------|
| Home | `/home` | 热歌榜 + 新歌榜 |
| Search | `/search` | 搜索 + 历史标签 |
| Library | `/library` | 收藏 / 歌单 / 下载 / 历史 / 头像 |
| FullPlayer | `/player/{songId}` | 全屏播放器 |
| PlaylistList | `/playlists` | 歌单列表 |
| PlaylistDetail | `/playlist/{id}/{name}` | 歌单详情 |
| Setting | `/setting` | 设置 |
| DownloadManagement | `/download_management` | 下载管理 |

---

## 构建与运行

### 环境要求

- Android Studio Ladybug 或更新版本
- JDK 17+
- Android SDK 35
- Android 8.0 (API 26) 或更高版本设备

### 构建步骤

1. 克隆项目
2. 在 `app/build.gradle.kts` 中配置 `API_BASE_URL`（当前为 `https://???/`）
3. 在 `MusicRepository.kt` 中配置 `API_TOKEN`
4. 使用 Android Studio 打开项目，等待 Gradle 同步完成
5. 点击 Run 或执行：

```bash
./gradlew assembleDebug
```

### 构建类型

- **Debug** — 默认，保留日志输出
- **Release** — 启用 ProGuard 混淆压缩（`proguard-rules.pro`）

---

## 项目规模

- 约 65 个 Kotlin 源文件，约 5,800 行代码
- 单一 `main` 分支

---

## 已知技术债务

| 项目 | 说明 |
|------|------|
| PlayerManager 职责较重 | 同时管理播放状态、队列、缓存、收藏、历史、歌词加载（~650 行） |
| domain/usecase/ 为空 | 部分业务逻辑分散在 ViewModel 与 PlayerManager 中 |
| 测试 | 尚未编写单元测试与 UI 测试 |
| 多模块拆分 | 可拆分为 `:core-player`、`:feature-home` 等独立模块 |
| 崩溃收集 | 可接入 Firebase Crashlytics 或 Sentry |

---
