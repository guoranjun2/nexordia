# 测试与验证指南

本仓库测试覆盖不均衡：核心 `freeplane` 有一批聚焦测试，AI 插件测试较密，部分老插件测试较少。开发时应按改动风险选择最小但有效的验证集。

## 测试框架

根构建脚本配置了：

- JUnit 4.13.2
- Hamcrest
- Mockito 5.18.0
- AssertJ 3.27.3

可用测试命令：

```bash
gradle :freeplane:test
gradle :freeplane_plugin_ai:test
gradle :freeplane_plugin_<name>:test
gradle test
```

需要完整日志时可使用：

```bash
gradle :freeplane:test -PTestLoggingFull
```

## 测试目录分布

核心测试：

```text
freeplane/src/test/java
```

覆盖区域包括：

- 菜单构建。
- HTML 渲染。
- icon。
- 加密。
- map/model。
- bookmarks。
- explorer。
- background/video。
- zoom 计算。
- URL/style/tag 等局部功能。

AI 插件测试：

```text
freeplane_plugin_ai/src/test/java
```

覆盖区域包括：

- chat。
- AI tools。
- read/edit/create/move/search。
- prompt/history。
- MCP 相关行为。

其他插件根据模块有少量测试。

## 按改动类型选择验证

| 改动类型 | 建议命令 |
| --- | --- |
| 核心控制器/模型 | `gradle :freeplane:compileJava` + `gradle :freeplane:test` |
| Swing UI 计算逻辑 | 抽测试 + `gradle :freeplane:test`，再手动启动验证 |
| 插件 Java 代码 | `gradle :freeplane_plugin_<name>:compileJava` + 该插件测试 |
| AI 插件 | `gradle :freeplane_plugin_ai:compileJava` + `gradle :freeplane_plugin_ai:test` |
| 翻译 | `gradle format_translation` + `gradle check_translation` |
| OSGi/build.gradle | 受影响模块 compile，检查 `BIN` manifest/classpath |
| 启动链 | compile 后手动启动 GUI/headless |
| 导入导出 | 相关单测 + 手动 round-trip 文件验证 |

## 功能开发测试原则

仓库要求功能开发遵守“抽取-测试-接入”：

1. 先从 UI 或控制器中抽出业务逻辑。
2. 立即写聚焦单元测试。
3. 用已测试组件实现功能。
4. UI/action/listener 只做薄接线。

适合抽取的逻辑：

- 节点选择规则。
- 样式解析和合并。
- 属性默认值迁移。
- 坐标和缩放计算。
- 读写转换。
- AI 工具参数和结果构造。
- clone 节点影响范围判断。
- 菜单/偏好状态计算。

不适合只靠手动验证的逻辑：

- clone/tree 操作。
- undo/redo。
- 文件读写 round-trip。
- 翻译格式化。
- 缩放坐标。
- filter 可见性。
- secret property。

## UI 测试策略

Swing UI 本身难以完全单测，因此建议：

- 把计算逻辑移出 component。
- 用模型/controller 测试覆盖状态变化。
- UI 层测试只验证 wiring 或轻量行为。
- 对绘制/布局进行手动视觉验证。

对于 `MapView`、`NodeView`、编辑器、背景图、缩放等功能，至少手动检查：

- 默认缩放。
- 低缩放。
- 高缩放。
- 有长文本节点。
- 有图片/HTML 内容节点。
- 有折叠节点。
- 有 filter。
- 有 clone 或 summary 节点。

## 读写测试策略

新增持久化字段或扩展时，应测试：

- 读取旧文件没有字段。
- 读取新文件有字段。
- 保存后再读取值不丢失。
- export/clipboard/style 模式是否符合预期。
- unknown elements 是否保留。
- 插件关闭时是否不会破坏文件。

## 翻译验证策略

翻译文件改动必须验证：

```bash
file freeplane/src/editor/resources/translations/Resources_*.properties | grep -v "ASCII text"
cd freeplane/src/editor/resources/translations/
grep -l 'u[0-9][0-9][0-9][0-9]' *.properties
gradle format_translation
gradle check_translation
```

如果 `file` 显示 binary、HTML、UTF-8 或其他非 ASCII 异常，应停止继续修改并恢复正确编码。

## OSGi 验证策略

插件相关改动需要检查：

- bundle symbolic name。
- bundle activator。
- Require-Bundle。
- Import-Package。
- Export-Package。
- Bundle-ClassPath。
- `BIN/plugins/<id>/lib` 中是否有依赖 jar。
- 启动日志是否有 resolve/start 错误。

AI 插件额外检查：

- Java 8 bootstrap class major version。
- Java 17 主体 class major version。
- bootstrap 不直接引用主体类型。

## 提交前建议

最小提交前检查：

```bash
git status --short
git diff --stat
gradle :freeplane:compileJava
```

按实际改动追加：

- core 测试：`gradle :freeplane:test`
- 插件测试：`gradle :freeplane_plugin_<name>:test`
- 翻译格式化：`gradle format_translation`
- 翻译检查：`gradle check_translation`

如果仓库已有无关脏改动，提交前要确认只 stage 自己负责的文件。

