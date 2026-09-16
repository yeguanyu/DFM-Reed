# 风簧（DFM Reed）

![风簧产品首页](docs/images/homepage.png)

三角洲行动移动端口风琴的自动演奏工具。应用标定 8 个音键，以及低八度、升半音、
高八度 3 个修饰键，通过 Android 无障碍服务的
`dispatchGesture` 模拟点击，不读取界面内容，也不需要悬浮窗权限或网络权限。

官方网站：[https://1nvweb.top/dfm](https://1nvweb.top/dfm)  
QQ 交流群：`1065234120`

## 使用

1. 打开应用并开启“风簧演奏服务”。
2. 保存曲谱，进入游戏的口风琴界面。
3. 点悬浮球，选择“标定”，把 1–8 和低/升/高圆点拖到对应按键中心并保存。
4. 展开悬浮球，点“播放”。

悬浮播放器支持上一首、播放/暂停、下一首、停止、标定，以及播放中实时调节
`0.25×–4.00×` 倍速。自定义歌曲支持 `20–480 BPM`。

曲谱规则：`1` 到 `7` 是自然音键，`^1` 是上方带点的高音 1（旧谱 `8` 仍兼容）；
`L1` 低八度、`H1` 高八度、`#1` 升半音、`L#1`
组合修饰；`0` 和 `-` 空一拍；`.` 空半拍；`[135]` 同时点击，同样可写 `H[135]`。
空格、换行和 `|` 仅用于排版，不占时间。

## 架构

- `core/ScoreParser.java`：数字谱解析，不依赖 Android UI。
- `service/HarmonicaAccessibilityService.java`：音乐时钟、播放状态和手势派发。
- `overlay/`：游戏内悬浮控制与 8 键标定。
- `MainActivity.java`：曲谱与演奏设置。
- `core/SongStore.java`：预设曲库、自定义曲谱与选歌状态。

修饰键会比音键提前 8–70ms 按下，并保持到音键释放之后。参考 HarpAutoPlayer 的输入
时序思想重新实现；没有复制其桌面 UI 或 Windows 输入代码。

## 独立测试场 APK

`simulator` 模块是无需安装游戏即可使用的被演奏目标。它模拟横屏界面，包含 8 个音键、
半音/升调/自然音/降调区域、多点触控、合成口风琴音色，以及最近音符和修饰键提前量
显示。包名为 `com.dfm.reedsim`，与主工具可同时安装。

## 构建

需要 JDK 17 和 Android SDK 36：

```bash
./gradlew :app:assembleDebug :simulator:assembleDebug
```

主工具输出位于 `app/build/outputs/apk/debug/`，测试场输出位于
`simulator/build/outputs/apk/debug/`。

## 官网文件

`index.html` 是无外部资源依赖的单文件首页，截图已以内嵌 Base64 保存。
`download.php` 会下载同目录的 `app.apk`；部署时将安装包另行上传到该文件名即可。
