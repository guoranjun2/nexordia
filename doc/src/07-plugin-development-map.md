# 插件开发与现有插件地图

Freeplane/Nexordia 使用 OSGi 插件体系。插件通常作为 `freeplane_plugin_<name>` 子项目存在，构建后安装到 `BIN/plugins/org.freeplane.plugin.<name>`。插件通过 Activator 注册 OSGi service，再由核心在启动过程中调用扩展 provider。

## 插件构建模型

插件项目名到 bundle id 的转换：

```text
freeplane_plugin_latex -> org.freeplane.plugin.latex
freeplane_plugin_script -> org.freeplane.plugin.script
freeplane_plugin_ai -> org.freeplane.plugin.ai
```

默认 activator：

```text
org.freeplane.plugin.<name>.Activator
```

默认依赖：

```text
Require-Bundle: org.freeplane.core
```

运行时结构：

```text
BIN/plugins/org.freeplane.plugin.<name>/
  META-INF/
  OSGI-INF/
  lib/
    <bundle jar>
    plugin.jar
    dependency jars...
```

`Bundle-ClassPath` 让 bundle 能看到 `plugin.jar` 和外置依赖。

## Provider 模式

插件通常在 `Activator.start()` 中注册一个或多个 service：

```text
IControllerExtensionProvider
IModeControllerExtensionProvider
```

全局 provider 适合：

- 日志、bug report。
- 新模式注册。
- 全局服务。
- 启动时资源或首选项初始化。

mode provider 适合：

- MindMap 菜单和动作。
- Style 模式扩展。
- CodeExplorer 模式扩展。
- 节点/地图读写扩展。
- 文本 transformer。

mode provider 常通过服务属性绑定模式，例如 `mode=MindMap`。

## 插件开发流程

1. 创建或选择 `freeplane_plugin_<name>` 子项目。
2. 配置 `build.gradle` 的依赖和 Bundle-ClassPath。
3. 实现 `Activator`。
4. 注册 controller 或 mode extension provider。
5. 在 provider 中注册 action、读写器、偏好项或服务。
6. 如需 UI，修改菜单 XML 和资源文本。
7. 编写核心逻辑测试。
8. 运行该插件的 `compileJava` 和测试。
9. 检查 `BIN/plugins/<bundle-id>` 输出。

## 现有插件概览

| 插件 | 入口和主要能力 |
| --- | --- |
| AI | Java 8 bootstrap + Java 17 主体；注册 MindMap 扩展、AI 聊天、地图编辑工具、MCP server、模型提供商配置 |
| Script | 注册 Groovy 脚本引擎、脚本 API、附加组件、脚本相关 UI |
| Formula | 注册公式文本 transformer、公式缓存、节点/地图监听、偏好项 |
| LaTeX | 注册 LaTeX node hook、内容类型、文本 transformer、格式控制器和动作 |
| Markdown | 注册 Markdown renderer、内容类型、transformer，可选 PlantUML |
| SVG | 注册 PDF/SVG 导出引擎和 SVG viewer factory |
| CodeExplorer | Java 11+，创建 CodeExplorer mode、ArchUnit server、代码导入服务和偏好 |
| BugReport | 向 root logger 添加报告生成器和手动 bug report action |
| JSyntaxPane | 初始化 DefaultSyntaxKit、LaTeX/Groovy 语法高亮和主题 |
| OpenMaps | 在 MindMap 模式注册 OpenMaps 能力 |

## AI 插件

子项目：

```text
freeplane_plugin_ai
```

特殊点：

- 主体代码 Java 17。
- bootstrap source set Java 8。
- 旧 Java 运行时只加载 bootstrap，并显示不兼容提示。
- 使用 LangChain4j 集成 OpenAI、Ollama、Gemini 等提供商。
- 保存/保护 AI secret properties。
- 可以启动本地 MCP server。

更详细说明见 `08-ai-plugin-and-mcp.md`。

## Script 插件

主要能力：

- Groovy 4 脚本引擎。
- Ivy 依赖支持。
- 脚本执行和权限策略。
- 通过 `freeplane_api` 暴露脚本对象。
- 注册 API `Controller` service。
- 支持 add-on UI。

脚本插件是很多高级用户功能的基础。修改公共 API、节点模型或控制器行为时，应考虑脚本兼容性。

## Formula 插件

主要能力：

- 根据属性 `parse_formulas` 控制公式解析。
- 注册 `FormulaTextTransformer`。
- 监听节点、地图、生命周期变化。
- 支持缓存和禁用。

公式功能和文本渲染、节点变更事件关系紧密。修改文本 transformer 链或节点刷新逻辑时需要回归公式场景。

## LaTeX 与 Markdown 插件

二者都通过内容类型和文本 transformer 接入。

LaTeX：

- `LatexNodeHook`
- detail/note content type
- format controller formats
- 插入、编辑、删除动作
- MindMap 和 style 模式扩展

Markdown：

- Markdown renderer
- content type/transformer
- 可选 PlantUML extension
- MindMap 和 style 模式扩展

修改 note/detail/text 渲染时要同时考虑这些插件。

## SVG 插件

主要能力：

- PDF export
- SVG export
- SVG viewer factory
- 在 MindMap 和 CodeExplorer 模式安装扩展

它依赖 Batik、FOP、PDFBox 等库，并在构建中排除若干冲突依赖。修改导出路径或依赖版本时要检查 bundle classpath。

## CodeExplorer 插件

主要能力：

- 创建独立 CodeExplorer mode。
- 使用 ArchUnit、JGraphT、Gson。
- 提供代码依赖图和 map 导入。
- 需要 Java 11+。
- 有自己的菜单 XML：`codeexplorermodemenu.xml`。

CodeExplorer 不是普通 MindMap 插件，涉及新 mode，因此要同时看启动注册、mode controller 和菜单。

## BugReport、JSyntaxPane、OpenMaps

BugReport：

- controller provider。
- 向 root logger 添加 `ReportGenerator`。
- 提供手动 bug report。

JSyntaxPane：

- 非 headless 初始化语法组件。
- 注册 LaTeX content type。
- 支持 dark theme。
- 提供 Groovy 相关高亮。

OpenMaps：

- MindMap provider。
- 调用 `OpenMapsRegistration`。

## 插件依赖选择

| Gradle 配置 | 使用场景 |
| --- | --- |
| `implementation` | 编译需要，可能由构建脚本处理到产物 |
| `lib` | 需要复制进插件运行时 `lib` 并出现在 Bundle-ClassPath |
| `compileOnly` | 只编译需要，运行时由其他 bundle 或环境提供 |

依赖放错位置的典型后果：

- 编译通过但运行时报 `ClassNotFoundException`。
- OSGi resolve 失败。
- 插件启动失败但核心仍启动。
- 同一库多版本冲突。

## 插件验证清单

- `gradle :freeplane_plugin_<name>:compileJava`
- 插件测试任务。
- 检查 `BIN/plugins/org.freeplane.plugin.<name>/lib`。
- 检查 manifest 中 `Bundle-SymbolicName`。
- 检查 `Bundle-Activator`。
- 检查 `Bundle-ClassPath`。
- 启动应用确认 Activator 执行。
- 在目标 mode 中确认 action/menu/preference 可见。

