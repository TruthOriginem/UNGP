# UnofficialNewGamePlus 开发说明

本文件适用于本 mod 的开发、修改、审查和构建任务。项目专属的规则应集中维护在这里，避免与工作区级配置重复或冲突。

## 项目概览

- Starsector 0.98a mod，使用 Java 17 和 Gradle 构建。
- 游戏根目录为 `D:\StarsectorDevelopment\098\Starsector`，本仓库位于其 `mods/UnofficialNewGamePlus` 子目录。
- Java 源码位于 `src/`；游戏数据、图形和声音分别位于 `data/`、`graphics/` 和 `sounds/`。
- 版本信息以 `mod_info.json` 为准；jar 文件名由其中的 `jars` 字段决定。

## 构建与依赖

- 使用本机 Gradle 9.6.1；项目没有 Gradle wrapper。
- Gradle 需要完整 JDK 17（包含 `javac`）。Starsector 自带的 `jre/` 只有运行时，不能用于编译。
- 编译期依赖来自游戏的 `starsector-core/`，以及 `mods/LazyLib/jars`、`mods/Lunalib/jars` 和 `mods/MagicLib/jars`。
- `gradle compileJava` 编译源码，`gradle jar` 生成 `jars/ungp.jar`，`gradle distZip` 生成 `dist/` 下的分发包。
- `UnofficialNewGamePlus.iml` 保留给 IntelliJ IDEA 的模块识别；Gradle 配置是构建任务的权威来源。

## 文件格式

- 文本文件统一使用 UTF-8、无 BOM、LF 行尾和文件末尾单个空行。
- Java 使用 4 个空格缩进；不要引入仅适用于本机的绝对路径或构建产物。
- 可使用 `gradle formatProject` 格式化，使用 `gradle verifyFormat` 检查格式。

## 修改与验证

- 修改前先检查相关源码、数据文件、资源和 `mod_info.json` 的联动关系；保持改动范围在本仓库内。
- 优先复用现有结构和工具，避免为一次性需求引入新的构建系统或重复脚本。
- 完成功能修改后，至少运行与改动直接相关的 Gradle 任务；格式、编译和资源问题分别判断，不要混淆错误来源。
- 不要提交编译生成的 jar、分发包或 Gradle 缓存；除非用户明确要求，不执行提交、推送或其他不可逆的 Git 操作。
