# BiliClient

> 一个极其轻量级的 B 站（哔哩哔哩）第三方客户端，使用 Java 编写，支持 Android 4.2（API 17）及以上设备（主要面向手表等小屏设备）。

这是一个极其轻量级的B站客户端，使用Java写成，支持至安卓4.2，借鉴了WearBili和腕上哔哩的部分开源代码和它们项目中收集的API，程序逻辑和数据处理还是自己写的，界面暂时使用WearBili的布局。播放视频需要使用小电视播放器或凉腕播放器。

我尽量不往里面塞太多东西，优先保证可用性、流畅性，字体啥的我再怎么说也不会放进去40M（

我也尽量把代码写得好看了，该分类的地方都有分类，重要部分都有注释。自学的安卓开发，代码可能有很多明病/暗病，我尽力了，轻喷QwQ。

## 特性

- 视频信息浏览与播放（需配合小电视 / 凉腕等外置播放器）
- 番剧 / 影视
- 动态、关注、粉丝
- 搜索（视频 / 用户 / 番剧 / 专栏等）
- 个人主页、用户信息
- 收藏、稍后再看、历史记录
- 评论 / 弹幕
- 消息、私信
- 专栏 / 文章
- 二维码扫描登录
- 图片查看（支持缩放）
- 创作中心

## 技术栈 / 第三方库

- 开发语言：Java（Android）
- 最低支持：Android 4.2（API 17）
- 构建：Gradle + Android Gradle Plugin 7.3.1
- 主要第三方库：
  - [ZXing](https://github.com/zxing/zxing) —— 二维码生成 / 扫描
  - [OkHttp](https://square.github.io/okhttp/) 3.12.1 —— 网络请求
  - [Glide](https://github.com/bumptech/glide) 4.13.2 —— 图片加载
  - [PhotoView](https://github.com/chrisbanes/PhotoView) 2.3.0 —— 图片缩放查看
  - [Jsoup](https://jsoup.org/) 1.10.2 —— HTML 解析
  - AndroidX AppCompat / Material Components / SwipeRefreshLayout —— UI 组件

## 构建

环境要求：

- Android Studio（或支持 AGP 7.3.1 的 IDE）
- JDK 8
- Android SDK（compileSdk 32）

步骤：

1. 克隆仓库
2. 用 Android Studio 打开项目，等待 Gradle 同步完成
3. 生成安装包：
   - Android Studio：`Build > Build Bundle(s) / APK(s) > Build APK(s)`
   - 或 Gradle 命令行：

   ```bash
   ./gradlew assembleDebug
   ```

## 致谢 / 借鉴

本项目的部分开源代码与所收集的 API 借鉴自以下两位前辈的项目：

- **WearBili**
- **腕上哔哩**（luern 的 Lson 库相关开源代码）

> 为啥不是在那两位前辈的基础上改？
>
> 1. 腕上哔哩的开源代码不完整，它的数据处理部分多处用到 luern 自己的 Lson 库，然而 github 上的版本似乎不管用。
> 2. WearBili 的界面确实好看，但是体积大、在许多手表上卡顿严重，而且仅支持安卓 7.1 以上，最重要的是我看不懂 kotlin。

## 开发者碎碎念

api逻辑十分甚至九分清晰（指直接一层一层拆json）（有在自己尝试写json拆解函数，后续版本可能会逐渐替换原有方式）

依赖库少，可以快速嫁接到其他工程里（大嘘

请注意：此工程的某些部分存在复用以及有一些奇怪的写法以及可能存在暗病和屎山！很多结构相同的页面（如稍后再看页面、收藏页面等，都是只有一个RecyclerView）我都直接使用了共用的一套界面布局。动态和视频的Adapter和Holder我并没有按照常规套路来写，而是将Holder独立出来。因为有些页面如搜索页、个人信息页也用到了相同的代码，我就选择了把这些共用代码统一放在同一个类里。这可以减小一部分资源浪费，也易于整体修改。

有任何疑问可以问我。

此项目正在更新中，若有问题和建议欢迎提出。作者事学生，上学期间不能更新，请勿催更，因为催了也大概率没用（

# 听说WearBili即将推出重制版了！XC说会支持安卓5.0，流畅度也有改善。可以小小期待一下哦！

## 许可证

本项目基于 [GNU General Public License v3.0](./LICENSE) 开源。

## 获取最新源码 / 联系

*Gitee / GitHub 可能不能够即时更新，下面两种方式可以获得最新源码：*
*交流群：482091687*
*作者QQ：1707106142*
