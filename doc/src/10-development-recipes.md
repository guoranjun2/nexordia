# 常见开发任务路线图

本文按实际开发任务整理入口和注意事项。每条路线都应结合源码再确认，避免只按文档机械修改。

## 新增一个 MindMap 动作

适用场景：菜单项、工具栏按钮、快捷键触发的编辑或命令。

路线：

1. 找到同类 action 所在 package。
2. 创建继承 `AFreeplaneAction` 的 action。
3. 如果有业务逻辑，先抽到独立类并写测试。
4. 在 `MModeControllerFactory`、相关 feature controller 或插件 provider 注册 action。
5. 在 `mindmapmodemenu.xml` 添加菜单/工具栏 entry。
6. 添加 `<ActionKey>.text`、tooltip、icon 等资源。
7. 如需快捷键，添加 accelerator 配置。
8. 运行 `gradle :freeplane:compileJava` 和相关测试。

注意：

- action key、菜单 XML、资源 key 必须一致。
- 可撤销编辑应走 `MModeController.execute` 和 actor。
- actionPerformed 不应包含复杂业务逻辑。

## 新增一个偏好项

路线：

1. 在 `freeplane.properties` 添加默认值。
2. 在 `preferences.xml` 的合适分组添加控件。
3. 在 `Resources_en.properties` 添加 `OptionPanel.<key>`。
4. 如果涉及其他语言，使用 `native2ascii` 流程添加翻译。
5. 在 Java 代码中通过 `ResourceController` 读取属性。
6. 如需要，添加 property listener。
7. 写测试覆盖默认值、用户覆盖或迁移。
8. 如果改翻译，运行 `gradle format_translation`。

注意：

- 不要覆盖用户已经设置的属性。
- 迁移旧 key 时使用清晰方法名。
- combo/radio 的选项值也可能需要翻译 key。

## 修改翻译

路线：

1. 在临时 UTF-8 文件编辑自然语言文本。
2. 用 `native2ascii` 转换。
3. 合并到目标 `Resources_*.properties`。
4. 运行 `file` 确认 ASCII text。
5. 检查是否有破损 `uXXXX`。
6. 运行 `gradle format_translation`。
7. 检查 diff。

命令：

```bash
native2ascii input-utf8.txt output.properties
gradle format_translation
gradle check_translation
```

注意：

- 不要把 UTF-8 中文、俄文、阿拉伯文直接写入 properties。
- 不要让编辑器自动转换编码。
- 不要忽略格式化带来的排序变化。

## 新增节点模型字段或扩展

路线：

1. 判断数据是否应属于 `NodeModel` 字段、`SharedNodeData`、node extension 或 map extension。
2. 判断 clone 节点语义：共享还是独立。
3. 通过 controller 修改数据，触发 node change event。
4. 如需持久化，注册 read/write handler。
5. 写模型测试。
6. 写读写 round-trip 测试。
7. 最后接入 UI 或 action。

注意：

- content clone 会共享 `SharedNodeData`。
- tree clone 可能影响整棵子树。
- 直接改模型字段可能绕过 dirty flag 和 undo。

## 新增 `.mm` 持久化内容

路线：

1. 设计 XML tag/attribute。
2. 注册 `ReadManager` handler。
3. 注册 `WriteManager` writer。
4. 确认 clipboard/file/export/style 模式差异。
5. 确认 unknown elements 不被破坏。
6. 写 round-trip 测试。
7. 手动打开保存真实地图验证。

注意：

- 插件数据优先放插件扩展，不要污染核心模型。
- 导出模式可能不应写内部状态。
- 读取旧文件时要有默认行为。

## 新增插件

路线：

1. 创建 `freeplane_plugin_<name>` 子项目。
2. 在 `settings.gradle` include。
3. 配置 `build.gradle`。
4. 实现 `org.freeplane.plugin.<name>.Activator`。
5. 注册 controller 或 mode provider。
6. 如果有 MindMap UI，在 provider 中注册 action/控制器/读写器。
7. 添加资源、菜单、偏好。
8. 编写测试。
9. 运行 `gradle :freeplane_plugin_<name>:compileJava`。
10. 检查 `BIN/plugins/org.freeplane.plugin.<name>`。

注意：

- 需要随插件发布的依赖应进入 `lib`/Bundle-ClassPath。
- 只编译需要的依赖才用 `compileOnly`。
- 插件启动失败不一定阻止核心启动，必须看日志。

## 修改启动流程

路线：

1. 区分修改点属于 launcher、核心 Activator、GUI starter 还是插件加载。
2. launcher 相关看 `freeplane_framework`。
3. OSGi 生命周期看 `ActivatorImpl`。
4. GUI 初始化看 `FreeplaneGUIStarter`。
5. headless 初始化看 `FreeplaneHeadlessStarter`。
6. 修改后同时考虑 GUI 和 headless。
7. 编译并手动启动验证。

注意：

- 启动阶段异常可能发生在 Swing EDT 或 OSGi bundle thread。
- 插件扫描路径变化会影响用户插件。
- 资源路径变化会影响菜单、翻译和图标。

## 修改 Swing 绘制或交互

路线：

1. 明确是选择、折叠、缩放、绘制、编辑器还是布局。
2. 找同类逻辑：`NodeSelector`、`NodeFolder`、`MapView`、`NodeView`、editor controller。
3. 抽出可测试计算逻辑。
4. 写单元测试。
5. UI 层只调用新逻辑。
6. 手动验证多缩放、多节点形态和 filter。

注意：

- 绘制方法中不要加入复杂业务决策。
- filter 会影响可见节点。
- 缩放坐标要覆盖低缩放。
- repaint 不能代替模型事件。

## 修改 AI 工具

路线：

1. 找到对应 tools 子包。
2. 判断工具只读还是写入。
3. 业务逻辑放到服务类。
4. 写参数校验和成功/失败测试。
5. 写入地图时走 controller/editor。
6. 如暴露给 MCP，补充 JSON request/response 测试。
7. 验证 secret 和 token 不泄露。
8. 运行 `gradle :freeplane_plugin_ai:test`。

注意：

- AI 工具返回内容要控制大小。
- 写操作要保留 undo/事件。
- MCP server stop 必须释放端口。
- bootstrap 不能引用 Java 17 主体类。

## 修改样式或主题

路线：

1. 判断是 node style、map style、logical style 还是 UI theme。
2. 找对应 controller/model/action。
3. 若涉及样式继承或默认值，抽测试。
4. 修改资源默认值或偏好项时同步翻译。
5. 验证普通节点、样式节点、logical style、clone 节点。
6. 手动检查不同 Look & Feel。

注意：

- 样式可能来自多个层级合并。
- 用户自定义样式不应被默认迁移覆盖。
- 背景色/文字色等视觉属性要考虑主题变化。

## 修改导入导出

路线：

1. 找导入导出 controller 或插件。
2. 判断是 core map IO、图片/PDF/SVG export、外部格式导入还是 clipboard。
3. 读写转换抽测试。
4. 写 round-trip 或 golden-like 测试。
5. 手动验证真实文件。

注意：

- export 模式不一定等同保存文件。
- clipboard 模式可能过滤节点。
- SVG/PDF export 可能依赖插件和外部库。

