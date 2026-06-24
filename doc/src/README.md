# Nexordia 源码开发文档索引

本文档集基于对仓库源码、Gradle 构建脚本、OSGi 清单配置、核心 Java 包、插件入口、资源文件和测试目录的静态阅读整理。它面向继续开发 Nexordia/Freeplane 的维护者，目标是把“从哪里入手、关键链路在哪里、改动后如何验证”集中到 `doc/src`。

源码仍然是最终权威。本文档记录的是本次阅读到的结构和开发约定，用于缩短定位时间、降低误改风险，并提醒后续开发遵守仓库已经形成的模式。

## 文档导航

| 文件 | 主题 |
| --- | --- |
| `01-build-and-repository.md` | 仓库结构、Gradle 子项目、Java 版本、构建输出和常用命令 |
| `02-startup-and-osgi.md` | Launcher、Knopflerfish、核心 Activator、GUI/headless 启动、插件加载 |
| `03-core-controllers-and-modes.md` | `Controller`、`ModeController`、MindMap/File 模式、动作和撤销体系 |
| `04-map-model-and-io.md` | `MapModel`、`NodeModel`、克隆节点、扩展、读写管理和 `.mm` 文件流程 |
| `05-swing-view-and-interaction.md` | Swing 视图层、`MapView`、`NodeView`、编辑器、选择、缩放和绘制 |
| `06-resources-preferences-translations.md` | 配置、资源、菜单 XML、偏好面板、翻译文件和格式化流程 |
| `07-plugin-development-map.md` | OSGi 插件开发模式、现有插件地图、Bundle-ClassPath 和 Activator |
| `08-ai-plugin-and-mcp.md` | AI 插件、Java 8 bootstrap、LangChain4j 工具集和 MCP 服务 |
| `09-testing-and-verification.md` | 测试分布、按改动类型选择验证命令、功能开发测试策略 |
| `10-development-recipes.md` | 常见开发任务路线图：动作、偏好项、翻译、插件、节点模型和 UI 功能 |

## 本次阅读覆盖

主要读取范围包括：

- 根目录 `settings.gradle`、`build.gradle` 和各子项目 `build.gradle`
- `freeplane_framework` 启动器
- `freeplane` 核心启动、控制器、模式、模型、IO、资源、Swing 视图代码
- `freeplane_api` 公共 API 暴露方式
- `freeplane_plugin_*` 插件的 Activator、构建脚本和主要包结构
- `freeplane/src/editor/resources`、`freeplane/src/viewer/resources`、`freeplane/src/external/resources`
- `freeplane/src/test/java` 与插件测试目录

未执行源码修改。本文档集只新增 Markdown 文件。

## 使用原则

开发时优先按下面顺序定位：

1. 先确认改动属于构建、启动、模型、视图、资源、插件还是测试。
2. 在对应文档中找到入口类和验证命令。
3. 回到源码阅读调用关系，再做最小范围改动。
4. 如果业务逻辑将进入 UI 事件处理器，先抽出可测试类，再写聚焦单元测试，最后接入 UI。
5. 涉及翻译文件时必须使用 `native2ascii` 流程并运行 `gradle format_translation`。

## 仓库内关键约束

- 开发命令使用 `gradle`，不要使用 `gradlew` 或 Maven。
- Java 兼容性以 Java 8 为主，少数模块有明确例外，例如 AI 主体代码和 macOS 模块。
- Freeplane/Nexordia 是 OSGi + Swing 架构，插件通过 Bundle 和服务提供者接入。
- `BIN/` 是全局构建输出目录，核心和插件都会复制到该目录。
- `.properties` 翻译文件必须是 ISO-8859-1/ASCII 形式，非 ASCII 文本使用 Unicode 转义。
- 已有源码中存在大量历史模式，开发时优先跟随局部代码风格，不做无关重构。

