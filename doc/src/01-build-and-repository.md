# 构建系统与仓库结构

Nexordia 继承 Freeplane 的多模块 Gradle 结构。仓库根目录是所有开发路径的基准，主应用代码位于 `freeplane/` 子项目，插件以 `freeplane_plugin_*` 子项目组织，运行时产物集中输出到根目录 `BIN/`。

## 根目录结构

核心目录：

| 路径 | 作用 |
| --- | --- |
| `freeplane/` | 核心应用 bundle，包含主模型、控制器、Swing UI、资源、菜单、翻译、导入导出 |
| `freeplane_api/` | 面向脚本和插件的公共 API |
| `freeplane_framework/` | 启动器、Knopflerfish OSGi 框架封装和平台脚本 |
| `freeplane_ant/` | 构建辅助任务，翻译格式化/检查依赖此模块 |
| `freeplane_mac/` | macOS 专用集成 |
| `freeplane_plugin_*` | 功能插件，例如 AI、脚本、LaTeX、Markdown、SVG、公式、OpenMaps |
| `JOrtho_0.4_freeplane/` | 拼写检查组件 |
| `doc/` | 版本开发记录和本文档集 |
| `BIN/` | 全局构建输出目录，编译/打包后生成 |

`settings.gradle` 注册的主要子项目包括：

- `freeplane`
- `freeplane_api`
- `freeplane_ant`
- `freeplane_framework`
- `freeplane_mac`
- `freeplane_plugin_ai`
- `freeplane_plugin_bugreport`
- `freeplane_plugin_codeexplorer`
- `freeplane_plugin_formula`
- `freeplane_plugin_jsyntaxpane`
- `freeplane_plugin_latex`
- `freeplane_plugin_markdown`
- `freeplane_plugin_openmaps`
- `freeplane_plugin_script`
- `freeplane_plugin_svg`
- `freeplane_debughelper`
- `JOrtho_0.4_freeplane`

## 常用命令

仓库要求使用系统 `gradle` 命令：

```bash
gradle :freeplane:compileJava
gradle :freeplane:test
gradle :freeplane_plugin_ai:compileJava
gradle check_translation
gradle format_translation
gradle dist
gradle mac.dist
gradle win.dist
gradle linux-packages
```

按改动选择命令：

| 改动类型 | 建议验证 |
| --- | --- |
| 核心 Java 代码 | `gradle :freeplane:compileJava`，必要时 `gradle :freeplane:test` |
| 某个插件 | `gradle :freeplane_plugin_<name>:compileJava`，必要时该插件测试 |
| 翻译文件 | `gradle format_translation`，再运行 `gradle check_translation` |
| 构建脚本或 OSGi 清单 | 对受影响子项目执行 `compileJava`，检查 `BIN/plugins` 或 manifest 输出 |
| 分发脚本 | 运行对应平台的 dist 任务，至少先跑相关 compile 任务 |

## 版本来源

根构建脚本从 `freeplane/src/viewer/resources/version.properties` 读取版本字段：

- `freeplane_version`
- `freeplane_version_status`

本次阅读到的版本文件中 `freeplane_version=0.1.0`。所有子项目的 Gradle `version` 由根构建统一设置。

## Java 版本

默认约束：

- 大多数子项目设置 `sourceCompatibility = 1.8`
- 大多数子项目设置 `targetCompatibility = 1.8`
- 开发时应避免 Java 9+ 语言特性，除非模块构建脚本明确允许

例外：

| 模块 | 版本约束 |
| --- | --- |
| `freeplane_plugin_ai` 主体代码 | Java 17，`options.release = 17` |
| `freeplane_plugin_ai` bootstrap source set | Java 8，确保旧运行时可加载并给出兼容提示 |
| `freeplane_mac` | Java 15 source/target |

AI 插件有额外校验任务检查 class 文件主版本：

- bootstrap 应为 major version 52
- 主体代码应为 major version 61

## 全局构建输出

根构建脚本定义：

```text
globalBin = rootDir.path + '/BIN'
```

主要输出形态：

| 输出 | 位置 |
| --- | --- |
| 核心 bundle jar | `BIN/core/org.freeplane.core/lib/` |
| 插件 bundle jar 与依赖 | `BIN/plugins/<plugin.id>/lib/` |
| 启动器和脚本 | `BIN/` 及平台子目录 |
| API 文档和脚本 API | `BIN/doc/api` 等文档目录 |
| 资源文件 | 从各 source set 复制到对应 runtime 目录 |

插件构建通常会生成两个层级：

1. 模块自己的 OSGi bundle jar，包含 `META-INF/MANIFEST.MF`。
2. `plugin.jar` 和第三方依赖，通过 `Bundle-ClassPath` 暴露给 bundle。

## 根构建脚本的公共约定

根 `build.gradle` 为子项目提供统一配置：

- Java 插件和编译选项
- UTF-8 源码编译
- JUnit、Hamcrest、Mockito、AssertJ 测试依赖
- bnd OSGi bundle 配置
- 插件打包、复制和清理任务
- `check_translation` 与 `format_translation`
- 分发任务和平台产物任务

OSGi bundle 配置要点：

- 核心 bundle symbolic name 是 `org.freeplane.core`
- 插件 bundle symbolic name 由项目名转换得到，例如 `freeplane_plugin_latex` -> `org.freeplane.plugin.latex`
- 插件默认 activator 是 `org.freeplane.plugin.<name>.Activator`
- 插件通常声明 `Require-Bundle: org.freeplane.core`
- 插件依赖通过 `Bundle-ClassPath` 外置到 `plugin.jar` 和 `lib/*.jar`

## `freeplane` 子项目

`freeplane/build.gradle` 是核心应用配置。它定义多个 source set：

| source set | 内容 |
| --- | --- |
| `main` | 核心 Java 代码 |
| `viewer` | viewer 资源和 viewer jar |
| `editor` | 编辑器资源、图片、翻译、主题 |
| `external` | XML 菜单、偏好、导出模板、XSLT 等外部资源 |

核心依赖包括：

- JavaFX 21.0.5 平台相关模块
- FlatLaf
- imageio、svgSalamander
- commons 系列库
- jgoodies
- simplyhtml
- JOrtho
- `freeplane_api`

该子项目会构建：

- `freeplaneeditor`
- `freeplaneosgi`
- `freeplaneviewer.jar`
- core bundle 运行目录
- 翻译和外部资源

## `freeplane_api` 子项目

`freeplane_api` 提供插件和脚本可依赖的公共接口。它也会构建 viewer API jar，包含一小部分可在 viewer 场景使用的类型，例如：

- `LengthUnit`
- `Quantity`
- `EdgeStyle`
- 其他公开模型/样式相关 API

开发原则：

- 不要为了内部实现便利扩大 API 暴露面。
- 公共 API 改动要考虑脚本兼容性和插件兼容性。
- 修改 API 后应检查脚本插件和依赖 API 的插件是否需要适配。

## `freeplane_framework` 子项目

该模块负责编译启动器：

- `freeplanelauncher.jar`
- 平台启动脚本
- 在 Nexordia 中复制为 `nexordia.sh`、`.bat`、`.exe` 等运行入口
- Knopflerfish 框架 jar

它不承载业务逻辑，但任何启动参数、系统属性、OSGi xargs、single instance 行为都可能需要从这里开始追踪。

## 插件子项目概览

| 子项目 | 主要功能 |
| --- | --- |
| `freeplane_plugin_ai` | AI 对话、地图编辑工具、MCP 服务、多模型提供商 |
| `freeplane_plugin_script` | Groovy 脚本、插件脚本 API、加载附加组件 |
| `freeplane_plugin_formula` | 节点文本公式解析和自动更新 |
| `freeplane_plugin_latex` | LaTeX 内容类型、渲染、动作和偏好 |
| `freeplane_plugin_markdown` | Markdown/PlantUML 渲染和内容类型 |
| `freeplane_plugin_svg` | SVG/PDF 导出和 SVG viewer factory |
| `freeplane_plugin_codeexplorer` | 代码结构导入、ArchUnit、代码模式 |
| `freeplane_plugin_bugreport` | 日志报告和手动 bug report |
| `freeplane_plugin_jsyntaxpane` | 语法高亮编辑组件 |
| `freeplane_plugin_openmaps` | OpenMaps 注册 |

## 构建脚本开发注意事项

- 修改插件依赖时确认依赖属于 `implementation`、`lib` 还是 `compileOnly`。
- 如果依赖需要进入运行时 bundle classpath，应放入插件打包链路。
- 修改 bundle manifest 时检查 `Import-Package`、`Export-Package`、`Require-Bundle` 和 `Bundle-ClassPath`。
- 对 AI 插件尤其注意 Java 8 bootstrap 与 Java 17 主体的边界。
- 不要在构建脚本里引入需要 Maven 命令的流程，仓库统一使用 Gradle。

