# AI 插件与 MCP 工具

AI 插件是当前仓库中结构最复杂的插件。它同时处理 Java 版本兼容、AI provider 配置、聊天 UI、地图读取/编辑工具、prompt 管理、secret properties 和本地 MCP server。

## 模块位置

```text
freeplane_plugin_ai
```

构建特点：

- 主体代码使用 Java 17。
- bootstrap source set 使用 Java 8。
- Gradle 校验 class 文件 major version。
- bnd 配置使用特殊 activator。
- 依赖 LangChain4j OpenAI/Ollama/Gemini，以及 markdown 插件。

## Bootstrap 设计

Java 8 兼容入口：

```text
org.freeplane.plugin.ai.bootstrap.Java8BootstrapActivator
```

职责：

- 在 Java 8 运行时仍可被 OSGi 加载。
- 检查当前 Java major version。
- 如果低于 Java 17，注册不兼容提示 UI。
- 如果满足要求，反射加载真正的 `org.freeplane.plugin.ai.Activator`。

这个设计避免旧 Java 在类加载阶段直接解析 Java 17 class 失败。

开发注意：

- bootstrap 包不能引用 Java 17 主体类的类型。
- bootstrap 代码必须保持 Java 8 bytecode。
- 主体实现可以使用 Java 17，但要留在 main source set。

## 主 Activator

AI 主 Activator 负责：

- 为 MindMap 模式注册 provider。
- 添加 AI 默认属性。
- 注册 secret/secure properties。
- 持久化 AI edits、chat history 和 prompt。
- 注册 AI icon provider。
- 注册 AI action。
- 添加 AI tab。
- 安装 prompt menu。
- 根据属性启动 MCP server。
- 停止时保存状态并关闭 MCP server。

涉及的 secret keys 包括：

- `ai_openrouter_key`
- `ai_gemini_key`
- `ai_ollama_api_key`
- `ai_mcp_token`

不要把 secret 写入普通 properties 或日志。

## 包结构概览

AI 插件包大致分为：

| 包/区域 | 作用 |
| --- | --- |
| `chat` | 聊天 UI、消息、会话和模型交互 |
| `chat/history` | 聊天历史保存和恢复 |
| `model` | AI provider/model 配置 |
| `prompt` | prompt 菜单、prompt 持久化 |
| `tools` | LangChain4j 工具入口 |
| `tools/read` | 地图/节点读取 |
| `tools/edit` | 节点编辑 |
| `tools/create` | 创建节点/结构 |
| `tools/move` | 移动节点 |
| `tools/search` | 搜索 |
| `tools/content` | 文本和内容处理 |
| `tools/selection` | 当前选择 |
| `tools/tagcategories` | 标签分类 |
| `edits` | AI 编辑记录和应用 |
| `mcpserver` | 本地 MCP server |
| `utilities` | 辅助函数 |

## `AIToolSet`

`AIToolSet` 聚合一组 LangChain4j `@Tool` 方法，让 AI 能读取和编辑思维导图。

能力包括：

- 读取当前选择。
- 读取节点及后代。
- 读取纯文本。
- 为编辑获取节点信息。
- 搜索节点。
- 选择节点。
- 创建节点。
- 创建 summary。
- 移动节点。
- 移动节点到 summary。
- 删除节点。
- 创建或编辑 connector。
- 批量编辑。
- 管理标签分类。

设计上它不是直接操作 Swing UI，而是组合 controller、reader、editor、selection 等服务。新增工具时应保持这个边界。

## MCP Server

主要类：

```text
freeplane_plugin_ai/src/main/java/org/freeplane/plugin/ai/mcpserver/ModelContextProtocolServer.java
```

特征：

- 本地 HTTP server。
- 默认端口读取属性，常见默认值为 6298。
- 端口合法范围 1024-65535。
- 启动前检查端口可用。
- 支持 MCP protocol `2024-11-05`。
- 使用 token 保护。

认证 header：

- `X-Nexordia-MCP-Token`
- `Authorization`

支持的方法包括：

- `initialize`
- `tools/list`
- `tools/call`
- `resources/list`
- `resources/read`
- `templates/list`

MCP server 通过 registry/dispatcher 暴露 AI 工具。

## MCP 开发注意事项

- server 必须只绑定本地或明确安全的地址。
- token 不能出现在日志。
- `tools/call` 必须校验参数。
- 对地图的写操作应走已有 controller/editor，保留 undo 和事件。
- 任何长耗时工具都应考虑 UI 线程和取消/超时。
- stop 生命周期必须关闭 HTTP server。

## AI 工具设计原则

新增 AI tool 前先回答：

1. 该工具是只读还是写入？
2. 写入是否需要 undo？
3. 写入是否应尊重 filter、selection、hidden/folded 状态？
4. 是否可能影响 clone 节点？
5. 返回内容是否可能过大？
6. 是否会泄露本地路径、secret 或用户隐私？
7. 是否需要批处理以避免多轮调用效率低？

推荐结构：

- tool method 只做参数解析和结果包装。
- 业务逻辑放在可测试服务类。
- 地图修改通过 controller/editor。
- 复杂 diff/apply 行为写测试。
- UI 只负责展示状态和错误。

## AI 插件测试重点

AI 插件已有较多测试，尤其集中在 chat 和 tools。新增功能应补充：

- 参数校验。
- 成功路径。
- 节点不存在/选择为空。
- 权限或只读地图。
- clone/summary 节点特殊情况。
- 大地图输出限制。
- MCP JSON request/response。
- Java 8 bootstrap 不引用 Java 17 类。

## 常见故障定位

| 症状 | 优先检查 |
| --- | --- |
| 插件在旧 Java 下崩溃 | bootstrap 是否引用主体类或 Java 17 API |
| AI tab 不显示 | Activator、MindMap provider、菜单/面板注册 |
| provider key 不生效 | secure property、默认属性、用户属性覆盖 |
| MCP 端口启动失败 | 端口范围、端口占用、属性值 |
| MCP 调用未授权 | token header、secret 存储、客户端配置 |
| 工具改图后 UI 不刷新 | 是否走 controller、是否触发 node/map event |
| 工具改图不可撤销 | 是否绕过 `MModeController.execute`/undo actor |

