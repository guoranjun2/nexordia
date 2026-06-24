# 资源、偏好、菜单与翻译

Freeplane/Nexordia 的大量 UI 文本、菜单结构、偏好项、默认属性和外部模板都来自资源文件。资源层和 Java 控制器强绑定：action key、菜单 XML、翻译 key、默认 properties、偏好 XML 必须彼此一致。

## 资源目录

| 路径 | 内容 |
| --- | --- |
| `freeplane/src/viewer/resources` | viewer 资源、`freeplane.properties`、版本、viewer 翻译 |
| `freeplane/src/editor/resources` | 编辑器资源、图片、主题、翻译、accelerators |
| `freeplane/src/external/resources` | XML 菜单、偏好、导出模板、XSLT 等 |

常见关键文件：

```text
freeplane/src/viewer/resources/freeplane.properties
freeplane/src/viewer/resources/version.properties
freeplane/src/viewer/resources/translations/Resources_en.properties
freeplane/src/editor/resources/translations/Resources_*.properties
freeplane/src/external/resources/xml/mindmapmodemenu.xml
freeplane/src/external/resources/xml/filemodemenu.xml
freeplane/src/external/resources/xml/preferences.xml
freeplane/src/external/resources/xml/stylemodemenu.xml
freeplane/src/external/resources/xml/codeexplorermodemenu.xml
```

## `ResourceController`

主要路径：

```text
freeplane/src/main/java/org/freeplane/core/resources/ResourceController.java
```

职责：

- 读取属性。
- 读取 resource bundle 文本。
- 管理 icon cache。
- 管理 accelerator manager。
- 通知 property listener。
- 提供 secure property 抽象接口。
- 提供 `loadString` 等资源读取辅助。

开发注意：

- action 文本通常不是硬编码，而是通过 key 读取。
- icon 路径和 tooltip 也常由 properties 提供。
- 修改 action key 时必须同步资源 key。

## `ApplicationResourceController`

主要路径：

```text
freeplane/src/main/java/org/freeplane/main/application/ApplicationResourceController.java
```

职责：

- 从 `freeplane.properties` 读取默认属性。
- 处理 `load_next_properties`。
- 维护默认属性、auto properties、secrets properties、secured properties。
- 保存用户属性。
- 支持用户资源优先、安装资源其次、classpath/resource loader 兜底。
- 提供 `isPropertySetByUser()` 判断用户是否显式设置过属性。
- 迁移旧属性 key，例如 `keepSelectedNodeVisibleAfterZoom` 到 `keepSelectedNodeVisible`。

属性文件保存使用 ISO-8859-1 语义。不要用不理解 Java properties 编码的工具批量改属性。

## 默认属性

默认属性位于：

```text
freeplane/src/viewer/resources/freeplane.properties
```

适合放：

- 系统默认偏好。
- UI 默认行为。
- 插件默认开关。
- 兼容迁移前的默认值。

新增属性时要确认：

1. 是否需要偏好面板入口。
2. 是否需要迁移旧 key。
3. 是否应通过 `isPropertySetByUser()` 避免覆盖用户设置。
4. 是否需要翻译 `OptionPanel.*` 文本。
5. 是否需要测试默认值和用户覆盖行为。

## 菜单 XML

菜单和工具栏主要由 XML 描述：

```text
freeplane/src/external/resources/xml/mindmapmodemenu.xml
freeplane/src/external/resources/xml/filemodemenu.xml
freeplane/src/external/resources/xml/stylemodemenu.xml
freeplane/src/external/resources/xml/codeexplorermodemenu.xml
```

XML entry 通常引用 action key。action key 必须已在对应 controller 中注册。

排查菜单问题：

- XML 是否加载到当前 mode。
- entry key 是否等于 action key。
- action 是否被注册。
- 插件 action 是否在菜单构建前注册。
- 翻译 key 是否存在。
- accelerator 是否冲突。

## 偏好面板 XML

偏好面板入口：

```text
freeplane/src/external/resources/xml/preferences.xml
```

构建器：

```text
freeplane/src/main/java/org/freeplane/core/ui/preferences/OptionPanelBuilder.java
```

支持控件类型包括：

- boolean
- color
- combo
- radiobuttons
- languages
- font
- key
- number
- 其他专用选项控件

翻译 key 约定：

- 偏好项：`OptionPanel.<key>`
- 分组标题：`OptionPanel.separator.<name>`
- combo/radio 选项：通常使用属性值同名 key

新增偏好项流程：

1. 在 `freeplane.properties` 添加默认值。
2. 在 `preferences.xml` 放到合适分组。
3. 在 `Resources_en.properties` 添加英文文本。
4. 如需要，为其他语言添加翻译。
5. 修改读取属性的 Java 代码。
6. 添加测试或手动验证。
7. 若改翻译，运行 `gradle format_translation`。

## 翻译文件

翻译文件位置：

```text
freeplane/src/editor/resources/translations/Resources_*.properties
freeplane/src/viewer/resources/translations/Resources_en.properties
```

仓库支持多语言和 RTL 语言。翻译文件必须遵守 Java properties 编码规则：

- 文件应为 ASCII text。
- 非 ASCII 字符必须写成 Unicode escape。
- 例如中文、俄文、阿拉伯文、带重音字符都应使用 `\uXXXX`。

严禁直接把 UTF-8 非 ASCII 文本写入 `.properties` 翻译文件。

## `native2ascii` 工作流

修改翻译时必须使用：

```bash
native2ascii input-utf8.txt output.properties
```

推荐流程：

1. 在临时 UTF-8 文件写自然语言翻译。
2. 用 `native2ascii` 转为 properties escape。
3. 合并到目标 `Resources_*.properties`。
4. 运行 `file` 确认目标仍是 ASCII text。
5. 运行 `gradle format_translation`。
6. 检查 diff，确认没有整文件重排或破坏 escape。

验证命令：

```bash
file freeplane/src/editor/resources/translations/Resources_*.properties | grep -v "ASCII text"
cd freeplane/src/editor/resources/translations/
grep -l 'u[0-9][0-9][0-9][0-9]' *.properties
git diff | head -20
gradle format_translation
```

第二条 grep 用于捕捉缺失反斜杠的破损 Unicode，例如 `u0159`。

## 翻译格式化

构建脚本：

```text
freeplane/gradle/format_translation.gradle
```

格式化类：

```text
freeplane_ant
```

格式化行为：

- 按 key 忽略大小写排序。
- 删除重复条目。
- 删除空值、`[translate me]`、`[auto]` 等低质量条目。
- 统一换行。

任何翻译文件改动后必须运行：

```bash
gradle format_translation
```

## 资源开发常见错误

| 错误 | 后果 |
| --- | --- |
| action key 与 XML entry 不一致 | 菜单项无效或缺失 |
| 添加偏好但没默认值 | 行为依赖 null/空字符串 |
| 添加默认值但没 UI 文本 | 偏好面板显示 key |
| 直接写 UTF-8 到 properties | 翻译损坏，Weblate/Java 读取异常 |
| 忘跑 format_translation | CI 或后续 diff 混乱 |
| 覆盖用户属性 | 用户升级后配置被重置 |

