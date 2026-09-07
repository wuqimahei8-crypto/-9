# 活动提醒 (MoveReminder)

一个安卓 App：在你设定的时间段内，每隔固定分钟数提醒你起来活动一下，提醒方式是**弹通知 + 手机震动**。

## 🚀 最简单的安装方式：用 GitHub 云端自动编译出 APK（推荐，不用装任何软件）

项目里已经配置好了 `.github/workflows/build.yml`，只要把代码传到 GitHub，它的服务器会自动帮你编译出真正能安装的 APK，你直接下载装到手机上就行。步骤如下：

1. **注册/登录 GitHub**：打开 https://github.com ，没有账号就免费注册一个。
2. **新建一个仓库**：右上角 "+" → "New repository"，随便起个名字（比如 `MoveReminder`），Public 或 Private 都可以，其他选项默认，点 "Create repository"。
3. **上传项目文件**：
   - 在新仓库页面点击 "uploading an existing file"（或 "Add file" → "Upload files"）。
   - 把你解压出来的 `MoveReminder` 文件夹里的**所有内容**（包括隐藏的 `.github` 文件夹）整个拖进网页上传区域。
     - 如果浏览器不支持拖文件夹（比如 Safari），可以先把整个 `MoveReminder` 文件夹重新压缩成 zip，用 [GitHub Desktop](https://desktop.github.com/) 或命令行 `git` 上传会更方便；也可以用 Chrome/Edge 浏览器，它们支持直接拖整个文件夹。
   - 拖入后点击底部 "Commit changes" 提交。
4. **等待自动编译**：上传完成后，点击仓库页面上方的 "Actions" 标签页，会看到一个正在运行（黄色圆点）的工作流 "Build APK"，等它变成绿色对勾（一般 2-5 分钟）。
5. **下载 APK**：
   - 点进这个已完成的工作流运行记录，往下滑到 "Artifacts" 区域，会看到一个叫 `MoveReminder-debug-apk` 的压缩包，点击下载。
   - 解压后得到 `app-debug.apk` 文件。
6. **安装到手机**：
   - 把这个 APK 传到手机上（微信传文件、QQ、云盘、数据线都行）。
   - 手机上用文件管理器打开这个 APK 文件点击安装。
   - 因为不是应用商店签名的应用，系统会提示"未知来源"风险，点"仍要安装"/"允许安装"即可（这是正常的，自己编译的 App 都会有这个提示）。

之后如果你想改设置（比如改默认提醒间隔），改完源码文件，重新上传/提交到 GitHub，Actions 会自动重新编译，重复第 4-6 步下载新 APK 即可。

## 功能
- 自定义提醒时间段（如 9:00–18:00）
- 自定义提醒间隔（默认 40 分钟）
- 开关一键启停
- "测试提醒"按钮，立即触发一次通知+震动，方便验证权限是否生效
- 手机重启后自动恢复提醒（如果之前是开启状态）

限制：当前实现假设"开始时间 < 结束时间"，即同一天内的区间，不支持跨零点（比如 22:00–02:00）的区间。

## 备选方式：本地用 Android Studio 编译安装

1. 安装 [Android Studio](https://developer.android.com/studio)（较新版本即可，比如 Hedgehog 或更新）。
2. 用 Android Studio 打开本项目文件夹（`MoveReminder/`）。
   - 首次打开会自动联网下载 Gradle 及依赖，需要能访问 Google 的 Maven 仓库（国内网络可能需要配置代理或镜像，例如在 `settings.gradle.kts` 里把 `google()` / `mavenCentral()` 换成阿里云等镜像地址）。
3. 等待 Gradle 同步完成后，用 USB 连接手机（需开启开发者模式 + USB 调试），点击运行按钮（绿色三角）安装到手机，或者选择 Build > Build Bundle(s)/APK(s) > Build APK(s) 生成 APK 文件，再传到手机上安装。

## 使用说明与重要权限提示

第一次打开开关"开启提醒"时，App 会请求两个权限，请务必都允许，否则可能收不到提醒：

1. **通知权限**（Android 13 及以上系统会弹窗询问，直接点"允许"）。
2. **精确闹钟权限**（Android 12 及以上系统，会跳转到系统设置页面，找到本应用并允许"闹钟和提醒"）。

另外强烈建议手动关闭本应用的**电池优化/后台限制**，否则手机厂商的省电策略可能会导致提醒不准时或被杀掉：
- 设置 > 应用管理 > 活动提醒 > 电池 > 选择"无限制"/"不限制后台活动"
- 部分国产手机（小米/华为/OPPO/vivo 等）还需要额外在"自启动管理"里允许该应用自启动

## 项目结构

```
MoveReminder/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/example/movereminder/
│       │   ├── MainActivity.kt       # 设置界面
│       │   ├── Prefs.kt              # 保存/读取用户设置
│       │   ├── ReminderScheduler.kt  # 计算下一次提醒时间、设置系统闹钟
│       │   ├── ReminderReceiver.kt   # 闹钟触发：弹通知+震动，并安排下一次
│       │   └── BootReceiver.kt       # 开机后恢复提醒
│       └── res/
│           ├── layout/activity_main.xml
│           ├── values/(strings|colors|themes).xml
│           └── drawable/ic_launcher.xml
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## 实现原理简述

- 使用 `AlarmManager.setExactAndAllowWhileIdle` 设置精确闹钟，闹钟触发时弹通知、震动，然后立刻计算并注册"下一次"闹钟（时间 = 本次时间 + 间隔分钟数），如此链式进行。
- 如果下一次触发时间超出了当天的结束时间，则自动跳到"第二天的开始时间"，从而只在你设定的时间段内提醒。
- 所有设置保存在 SharedPreferences 中，App 被杀掉重启，或手机重启后（配合 BootReceiver）都能按设置恢复。
