# UnofficialNewGamePlus 全项目审计报告

> 审计对象:UNGP 2.6.0(Starsector 0.98a-RC8),工作区 HEAD `08482f7` + 未提交在途改动
> 审计时间:2026-11-05
> 审计范围:172 个 `.java`(16,861 行)、22 个数据文件、构建脚本与工具链、未提交的 2.6.0 在途功能(11 文件 / +954 行)
> 审计方式:4 条独立线并行只读审计(架构 / 存档性能 / 数据 i18n / 构建仓库)+ Lead 交叉复核
> 分级:P0=存档损坏·崩溃·数据错误;P0★=当前不触发但爆炸半径为存档数据的高危潜伏项;P1=功能正确性或明显性能问题;P2=可维护性/扩展性;P3=风格与锦上添花

---

## 0. 一页结论

### 0.1 总评

**这个项目的工程质量高于同类 Starsector mod 的平均水平,没有"要崩了"的迹象。** 编译干净、jar 可复现、86 条规则的类引用与图标引用**零悬空**、双语 CSV **逐列零差异**、监听器注册/注销配对正确。真正的问题不在"坏",而在**四类结构性欠账**:补偿式而非成对式的效果卸载模型、规则框架的静态状态与 instanceof 分发、未发布代码里的两处数据账本设计、以及一条会在英文发行时静默丢内容的 CSV 列错位。

| 维度 | 评分 | 一句话 |
|---|---|---|
| 功能完整度 | 8.5/10 | 86 条规则 + 5 挑战 + 背景/继承/Codex,体系完整 |
| 编译与打包 | 8.5/10 | clean 编译通过、jar 两次重建 sha256 相同、class 版本 61 与游戏 JRE 17 匹配 |
| 存档安全性 | 6.0/10 | **1 条高危潜伏项**(SharedData 无 alias)+ 规则下线无回退路径 |
| 数据/i18n 一致性 | 8.0/10 | 结构性缺陷仅 1 处(ENG 挑战 CSV 缺列),其余维度零差异 |
| 性能 | 7.0/10 | 无全局性热点,但有 5 处逐帧/全量路径偏重 |
| 可维护性 | 5.5/10 | `UNGP_RulesManager` 952 行、`URule` 14 类职责、29 处 instanceof 分发跨 10 个文件 |
| 仓库与发布卫生 | 5.5/10 | 构建产物入库、格式门禁在本机恒红、英文发行元数据过期、distZip 内容错 |

### 0.2 最该做的 6 件事(按性价比降序)

| # | 事项 | 成本 | 收益 |
|---|---|---|---|
| 1 | 给 `UNGP_SharedData` 补 XStream alias | **2 行** | 拆掉全项目唯一一颗"重构即丢档"的地雷 |
| 2 | `UNGP_challenges_ENG.csv` 表头补 `dpToDefeat` | **1 行** | 英文发行恢复 `7_kan` 挑战与 5000 DP 条件 |
| 3 | `UNGP_StillGrowingUp.java:71` 的 `Math.min` 改 `Math.max` | **1 字符** | 让"低 cost 更易被抽中"的加权机制真正生效 |
| 4 | `UNGP_SpecialistCombatPlugin.init()` 重置实例状态 + 改用 `getDataInSaveOrNull()` | ~10 行 | 消除跨战斗叠加与战斗开场崩溃路径 |
| 5 | `UNGP_BountyHunter` 的存档账本从 `WeakHashMap<intel 对象>` 改为稳定 id | ~40 行(**发布前**,否则发布即背债) | 消除赏金随读档膨胀 |
| 6 | `.gitattributes` + `jars/*.jar` 出库 | 1 文件 + 2 命令 | 让格式门禁可用;止住每次功能提交都改二进制 |

> 1~3 合计 4 行代码,能消掉本报告里最贵的三个风险。**先做这三件,再谈重构。**

### 0.3 问题总量

| 级别 | 数量 | 说明 |
|---|---|---|
| P0(当前可触发) | **0** | 未发现当前版本可复现的存档损坏/崩溃/数据错误 |
| P0★(高危潜伏) | **2** | `UNGP_SharedData` 无 alias;规则下线/字段缺失无兜底 |
| P1 | **16** | 1 条数据、4 条未发布代码、6 条框架契约、5 条性能/生命周期 |
| P2 | **21** | 架构分层、静态状态、i18n 机制、打包与工具链 |
| P3 | **20+** | 风格、死键、尾随空格、冗余 alias 等 |

---

## 1. 审计方法与可信度

4 条线并行只读审计,**不修改 src/ 与 data/**,仅 `gradle` 由单一成员串行执行(避免 `.gradle` 锁冲突)。四份分报告见附录 A。

**证据标准**:每条结论必须落到 `path:line`,且引用前实际读过该行;行号以工作区当前状态(含未提交改动)为准。

**Lead 复核**:对 5 条高影响结论做了独立复核,其中 **1 条否决、1 条降级、1 条加强、2 条确认**,记录见第 6 节。审计报告里被我改过的结论,比没被改过的更可信。

---

## 2. 项目现状

| 项 | 值 |
|---|---|
| 代码 | 172 个 `.java` / 16,861 行;最大文件 `UNGP_InteractionDialog.java`(1,153)、`UNGP_RulesManager.java`(952)、`UNGP_RulePickPanelPlugin.java`(776) |
| 数据 | 86 条规则(`UNGP_rules.csv` 103 行含空行/注释)、5 条挑战、9 个背景、8 项 LunaSetting |
| 依赖 | LazyLib、MagicLib(`mod_info.json`);构建期另用 Lunalib |
| 构建 | `gradle compileJava` 通过 / `gradle jar` 通过且可复现 / `gradle verifyFormat` 失败 / `gradle build` 失败(被前者阻断) |
| 在途改动 | 30 条 git status(11 M / 10 AM / 7 A / 2 AD),6 条新规则 + 1 个新公共 API + 图标着色工具 |

**在途功能的定性结论(2.6.0 的"下一场战斗屏蔽战斗规则"):** 语义自洽且实现正确 —— 真实生涯战斗消费、模拟战不消费、自动结算不消费、战役任务 no-op,`bCRNB` 别名已覆盖旧档。**这条功能可以发。** 它的问题全在实现细节(见 P1-8/P1-9)与同批次新增的两条规则(P1-2/P1-3)。

---

## 3. 分维度结论

### 3.1 架构与框架(最值得投入的长期项)

**核心模型**:CSV → 反射实例化 `UNGP_BaseRuleEffect` → `URule`(信息+展示)→ 实现 12 个 tag 接口之一/多 → `RulesManager` 分 9 个静态列表缓存 → 战斗插件/生涯插件/技能二次分发。

三个结构性欠账:

1. **卸载是"全量广播"而非"成对调用"。** `UNGP_RulesManager.java:122-143` 每次刷新都把 **ALL_RULES**(不是"上次激活的集合")全量 `unapplyGlobalStats` 一遍,再 `apply` 回激活集合。这是安全网也是双刃剑:它让"忘记写 unapply"的规则在**缓存刷新时**侥幸被兜住,却在**规则 id 下线时**完全失效(下线规则已不在 `ALL_RULES`,再也遍历不到)。
2. **`cleanUp()` 与 `unapply()` 的时序契约是约定而非机制。** `UNGP_RepickRulesDialog.java:100-107` 先 `saveActivatedRules`(内部触发被移除规则的 `cleanUp`,见 `UNGP_InGameData.java:96-102`)再 `updateRulesCache`。若某条规则的 `cleanUp` 清掉了 `unapply` 需要的数据,这次 unapply 就是空操作 → stat 永久残留。现有规则**全是侥幸正确**,第三方照抄就会踩坑。
3. **`URule` 承担 14 类职责、i18n 在构造期即耦合。** `URule` 同时是数据模型、i18n 入口、配色来源、tooltip 构建器、Codex 链接源、规则筛选器(见 `UNGP_RulesManager.java:300-610`)。叠加 `UNGP_RulesManager` 952 行(加载/缓存/分发/tooltip/枚举/常量),`api` 包对第三方 mod 的承诺实际上被这个类绑架。

**tag 混入体系**:12 个接口 + **29 处 `instanceof UNGP_*` 分发,分散在 10 个文件**(`RulesManager`、`CampaignPlugin`、`SpecialistCombatPlugin`、`SpecialistSkills`、`EconomyListener`、`UITimeScript`、`SpecialistWidgetPlugin`、`InteractionDialog`、`ChallengeIntel`、`BaseBackgroundPlugin`)。新增一个 tag 需要改 4~5 个文件且无编译期保护 —— 这是"扩展性"的真实成本,不是洁癖。

**拆解建议**(按职责,不按行数):
- `UNGP_RuleLoader`(CSV→URule,含校验)、`UNGP_RuleCache`(9 个静态列表 + 失效)、`UNGP_RuleDispatcher`(tag → 消费方,用 `Map<Class<?>, List<Object>>` 注册表替代 instanceof)、`UNGP_RulePresenter`(tooltip/配色/Codex/i18n)、`URule` 退化为不可变数据 + `RuleInfo`。
- `cleanUp()` 在基类给默认实现 = `unapplyGlobalStats()` + `clearAllDataInCampaign()`;`updateRulesCache` 的卸载阶段改为遍历"上次激活 ∪ 本次激活",并持久化"上次激活 id 列表"。

### 3.2 存档与生命周期

- **P0★ `UNGP_SharedData` 无 alias。** 它进 `persistentData`,但 `UNGP_modPlugin.configureXStream:77-104` 只 alias 了 `camplugin/pfmb/ut/igd` 四类。于是它按 **FQCN** 落盘,10+ 条规则的运行时账本全部绑在类全名上。**历史上已经翻过一次车**:`changelog.txt:60` 记 "data.scripts变更为ungp.scripts",`changelog.txt:98` 紧接着写"本次更新需要新存档"。修复 2 行:`x.alias("ungp_sharedData", UNGP_SharedData.class)` 并保留旧 FQCN 兼容。
- **旧档缺字段 = null,不是初值。** XStream 反序列化不走构造器;`UNGP_InGameData` 的 `activatedRuleIDs` / `completedChallenges` / `changeTimeStat` 在 `:68 / :74 / :87 / :114` 被直接解引用。坏档或跨版本裁剪后读档即 NPE。建议 `readResolve()` 兜底。
- **监听器生命周期是正确的。** 注册/注销配对,且 `removeListenerOfClass(接口)` 能精确命中 —— `ObjectRepository` 通过 `FastIterationClassifier` 按"自身类 + 父类 + 全部接口"建索引(`apis/starsector-deobf-renamed/src/com/fs/util/container/repo/FastIterationClassifier.java:26-45`),接口/内部类都能删。**唯一的扩展性约束**:两条规则共用同一个监听器类会互相误删。
- **规则 id 下线无回退路径**(P1):`UNGP_InGameData.saveActivatedRules:85-104` 只回写本次解析到的 id 并丢弃旧 id,`RulesManager:122-143` 又只遍历 `ALL_RULES` → 已生效效果无人回收。

### 3.3 性能

**先说结论,并纠正一个常见假设:不存在"每帧 O(规则²)"。** `getActivatedRules()` 全项目仅 6 个调用点且无一是每帧调用,单次约 86×64 次 `equals` + 2 次 List 分配,微秒级。

真实热点(按危害):

| # | 位置 | 量化 |
|---|---|---|
| 1 | `UNGP_SpecialistCombatPlugin.java:116-128` | ships × tags 双循环:40 舰 × 33 tag = **1,320 次虚调用/帧**,60fps → 7.9 万/秒 |
| 2 | `UNGP_BountyHunter.java:58-70`(未发布) | **每帧** 4 次 intelManager 全量查询 + `keySet().removeIf` |
| 3 | `UNGP_EconomyListener.java:31-44` | 每次经济更新:约 50 市场 × 4 经济 tag × 3 次调用 ≈ **600 次/次** |
| 4 | `UNGP_CampaignPlugin.java:184-208`(每帧) | `getMembersListCopy()` 每帧新 List + 成员 × 成员 tag 的 BuffManager 查询(30 舰 × 11 tag) |
| 5 | `UNGP_SpecialistBackgroundUI.java:186-201` | **每帧** `playUILoop` + `suppressMusic` 音频调用 |
| 6 | `UNGP_KeepTheWayClear.java:24-34` | 每帧遍历 `location.getFleets()` 并写 MemoryAPI,且**无势力过滤** |
| 7 | `UNGP_RulePickPanelPlugin`(悬停) | 每帧两次排序 + 每图标 `getSprite()` |

**修复原则**:"每帧一次"的东西改成"日事件/脏标记触发";`ships × tags` 改成按 tag 类型分桶、只在有对应规则时进循环。

**UI 结论(池子里的好消息)**:`UNGP_InteractionDialog` 1,153 行**不是**性能问题 —— 它的 `advance` 只有 `hasOption` + `setEnabled` 两句,没有每帧文本/布局/字体重建;行数来自委托内联。读档与转生走"清空 + 全量重算",幂等成立。

### 3.4 数据与 i18n

整体质量高得反常,这值得说清楚:**86 条规则的 `effectPlugin` 与 `src/ungp/impl/rules/**` 完美 1:1(缺失 0 / 孤儿 0);图标缺失 0、孤儿 0;153 处静态字符串键中英两侧全命中;双语 rules/backgrounds/LunaSettings 逐列零差异。**

唯一的功能性缺陷就是 P1-1(ENG 挑战 CSV 缺 `dpToDefeat` 列 → 英文构建静默丢掉 `7_kan` 挑战)。而它能潜伏,是因为**缺列与"值为空"走同一条静默默认路径**(P2-5)—— 这是结构性原因,值得在 loader 里加"列缺失即 warn"。

其余:P2 `abilities.csv` / `skill_data.csv` 无 `_ENG` 对(中文环境显示英文名)、`SimpleI18n.format` 吞异常返回 null(潜 NPE)、语言切换全靠根目录脚本 `os.walk` 全仓改名(无白名单,会走进 `build/`、`.git/`,P2-1)。

### 3.5 构建、发布与仓库

**最健康的一环是编译打包**(clean 编译 6s、jar 可复现、class major 61 与游戏 JRE 17.0.10 匹配、172 顶层类 = 172 个 `.java`)。**最不健康的是发布链路:**

- `gradle verifyFormat` 在本机**必然失败**:`core.autocrlf=true` + 仓库无 `.gitattributes`,检出即 CRLF —— 实测工作区 **160/172 个 `.java` 与 187/213 个文本文件是 CRLF**(git 里的 blob 却是纯 LF),而 `checkLf` 检查的是工作区 → "LF needed: 183"。这是环境问题不是源码问题,但后果是**格式门禁形同虚设**,且 `gradle build` 被它连带阻断。
- `mod_info_ENG.json` 仍是 **2.5.0**,且依赖只声明 LazyLib(漏 MagicLib)。英文用户走 `swapLangFile-切换语言.py` 切换后,版本管理器会报版本倒退 + 缺依赖。
- `jars/ungp.jar` **入库**且随功能提交变化(+27KB),直接违反 `AGENTS.md:31`。
- `distZip`(`build.gradle:56-74`)把未入库的 `data/campaign/CSV_id_order.py` 和 `graphics/ungp/icons/UNGP PS Blend.zip` 打进发行包,却漏掉 `changelog.txt:386/392` 明确承诺给用户的 `swapLangFile-切换语言.py`(它还被 `.gitignore:1` 忽略)。
- 索引里有 **2 个 `AD` 文件**(暂存了添加、工作区已删):直接 commit 会把两个已删 png 提交进去。

---

## 4. 全量问题清单

### P0 — 当前可触发:**0 条**

### P0★ — 高危潜伏:**2 条**

| # | 位置 | 机制 | 修法 |
|---|---|---|---|
| P0★-1 | `UNGP_modPlugin.java:77-104` + `UNGP_SharedData.java:13,50-56` | 存档以 FQCN 落盘,类移动/改名即静默丢全部规则数据(历史上已发生一次包迁移) | 补 `x.alias` + 旧 FQCN 兼容,2 行 |
| P0★-2 | `UNGP_InGameData.java:27-29,68-74,86-87,114` | XStream 不走构造器,缺属性 = null;三个集合字段被直接解引用 | `readResolve()` 兜底初始化,~10 行 |

### P1 — 16 条

| # | 位置 | 机制 | 归属 |
|---|---|---|---|
| P1-1 | `data/campaign/UNGP_challenges_ENG.csv:1`(行 `:6`) | 表头缺 `dpToDefeat` → `7_kan` 9 值错位 → `milestoneToUnlock="FALSE"` → `ChallengeInfo.isValid=false` → 英文构建静默丢弃该挑战及其 5000 DP 条件 | 数据 |
| P1-2 | `UNGP_BountyHunter.java:59-63,78` | `WeakHashMap<intel 对象>` 当奖金账本写进 `persistentData`;`:78` 用**对象恒等**判重,引用失配即二次叠加 | 在途代码 |
| P1-3 | `UNGP_BountyHunter.java:50-52,58-70` | `advanceInCampaign` 每帧 4 次 intel 全量扫描 | 在途代码 |
| P1-4 | `UNGP_SlipperyModification.java:290-294` | `cleanUp()` 清空 `triggered` 账本但**不回退已施加的 D-mod** → 重选规则后同一艘船可再次吃 D-mod(不可逆资产变更) | 在途代码 |
| P1-5 | `UNGP_StillGrowingUp.java:71` | `Math.min(6-abs(cost),1)` 恒为 1,应为 `Math.max`;"低 cost 更容易被抽中"从未生效 | 既有 |
| P1-6 | `UNGP_FullOfCompany.java:23-29`、`UNGP_TachLabour.java:23-28` | 直接 `setRelationship`,无 `unapplyGlobalStats` → 规则移除后势力关系永久 FRIENDLY/COOPERATIVE | 既有 |
| P1-7 | `UNGP_RulesManager.java:122-143` + `UNGP_InGameData.java:96-111` | 规则 id 下线:旧 id 被丢弃 + 已不在 `ALL_RULES` → 效果永不回退;含 `NoWayBack` 写进随档序列化的 `changeTimeStat` | 框架 |
| P1-8 | `UNGP_SpecialistCombatPlugin.java:35-37,39-86` | `init()` 不重置 `tags/bonusMessages/init/isRulesBlocked`;该插件由 `data/config/settings.json:3` 声明为全局插件,实例一旦被复用即逐场叠加 | 在途代码 |
| P1-9 | `UNGP_SpecialistCombatPlugin.java:58-59` | `tweakBeforeApply` 第二参传可变 `ArrayList`,违反 `UNGP_TweakBeforeApplyCombatTag.java:14` 的 Unmodifiable 契约(生涯侧传 `List.copyOf`) | 在途代码 |
| P1-10 | `UNGP_RulesManager.java:103-220` | `updateRulesCache` 无 try/finally:中途异常 → `needUpdateCache` 永真 → `CampaignPlugin:89-91` 每帧重试每帧抛;静态缓存停在半清理半应用态 | 框架 |
| P1-11 | `UNGP_RepickRulesDialog.java:100-107` | 先 `saveActivatedRules`(触发 cleanUp)后 `updateRulesCache`(触发 unapply):顺序与"先卸载后清理"的直觉相反,规则正确性靠自觉 | 框架 |
| P1-12 | `UNGP_RulesManager.java:178` | 规则归类 `if CombatTag else Campaign` 互斥:同时实现 combat + campaign 的规则其 `advanceInCampaign` 静默不执行,无告警 | 框架 |
| P1-13 | `UNGP_SpecialistCombatPlugin.java:116-128` | 战斗逐帧 ships × tags(40×33=1,320/帧) | 性能 |
| P1-14 | `UNGP_EconomyListener.java:31-44,52-54` | 每次经济更新全量重算所有市场 × 所有经济 tag(≈600 次/次) | 性能 |
| P1-15 | `UNGP_KeepTheWayClear.java:24-34`、`UNGP_IHaveActivated.java:104-112` | 前者每帧遍历全星系舰队写 MemoryAPI 且**无势力过滤**;后者每帧对全舰做 listener 类扫描 | 性能/正确性 |
| P1-16 | `mod_info_ENG.json:5` | 版本 2.5.0(其余 2.6.0)且依赖缺 MagicLib → 英文发行元数据失真 | 发布 |

### P2 — 21 条(归纳)

**架构**:`RulesManager` 952 行/5 类职责;`URule` 14 类职责 + i18n 构造期耦合;29 处 instanceof 跨 10 文件;9 个 public static 可变列表;`ALL_RULES` 由 daemon 线程写(无同步);`isSpecialistMode` 死字段;`loadAllRules` 内部起 HTTPS daemon 线程。

**存档/生命周期**:`UNGP_InGameData.getDataInSave()` **从不返回 null** —— 缺 plugin 时它抛 `RuntimeException`(`UNGP_CampaignPlugin:48-55`),所以调用方所有 `!= null` 判空都是死代码,`UNGP_SpecialistCombatPlugin:43` 就是在 `init()` 里走这条会抛的路径(应改用 `getDataInSaveOrNull():46-49`);规则下线后 `SharedData` 槽位永久残留(存档缓慢膨胀);`UNGP_SharedData.instance` 静态缓存回主菜单不清理。

**性能**:`CampaignPlugin:140` 每帧 `new TempCampaignParams()`;`:184-208` 每帧 `getMembersListCopy()` + 成员×tag;`SpecialistBackgroundUI:186-201` 每帧音频调用;选择面板悬停每帧两次排序。

**数据/i18n**:`SimpleI18n.format` 吞异常返回 null(潜 NPE);`abilities.csv` / `skill_data.csv` 无 `_ENG`(中文环境显示英文);语言切换脚本 `os.walk` 全仓改名无白名单;可选列缺失全走静默默认值(即 P1-1 的根因)。

**构建/仓库**:无 Gradle wrapper、无 toolchain、依赖目录硬编码且大小写敏感;`MagicLib` 实为无用依赖;mod_info 的 LAX 解析在 `build.gradle` 与 `settings.gradle` 双份实现;`.gitignore` 缺 `dist/` 与 `jars/*.jar`;2 个 `AD` 死文件;`formatProject` 无 dry-run;`Exec` 任务无输入输出声明(永远重跑)。

### P3 — 20+ 条(择要)

`strings.json` 10 个疑似死键;`strings_ENG.json` 多一个 `UNGP.inGameData`;`tag_data.json` 非严格 JSON;`mod_info.json` 尾逗号;`UNGP_rules.csv` 26 处字段尾随空格(会渲染进 tooltip);`CSV_id_order.py` 混入 `data/` 且 `numpy>=2` 下 ImportError;`buffID` 的 `ungp_` 与 `UNGP_` 双前缀;`UNGP_IHaveActivated.java:45-49` 的 `String.format` 未指定 Locale;`MapConverter(WeakHashMap)` 以 PRIORITY_LOW 注册疑为冗余;`rules.csv` 中 3 条 `cost=0 && isBonus=TRUE` 的语义未注释。

---

## 5. 优化方案

### 5.1 冲刺 0:今天,< 2 小时,~40 行(先止血)

| 顺序 | 动作 | 文件 | 成本 |
|---|---|---|---|
| 1 | 补 `x.alias("ungp_sharedData", UNGP_SharedData.class)` + 保留旧 FQCN | `UNGP_modPlugin.java` | 2 行 |
| 2 | ENG 挑战 CSV 表头补 `dpToDefeat`,`7_kan` 行 `5000` 归位 | `UNGP_challenges_ENG.csv` | 1 行 |
| 3 | `Math.min` → `Math.max` | `UNGP_StillGrowingUp.java:71` | 1 字符 |
| 4 | `init()` 重置 `tags/bonusMessages/init/isRulesBlocked`;改用 `getDataInSaveOrNull()`;`tweakBeforeApply` 传 `List.copyOf` | `UNGP_SpecialistCombatPlugin.java` | ~10 行 |
| 5 | `readResolve()` 兜底 3 个集合字段 | `UNGP_InGameData.java` | ~10 行 |
| 6 | `updateRulesCache` 包 try/finally | `UNGP_RulesManager.java:103-220` | ~4 行 |
| 7 | 新增 `.gitattributes`(`* text=auto eol=lf`)后把工作区一次性转 LF。**注意:仓库 blob 本来就是 LF,是 `core.autocrlf=true` 把它检出成 CRLF,所以"重新检出"无效** | 仓库根 | 1 文件 + 1 命令 |
| 8 | `git rm --cached jars/ungp.jar`;`.gitignore` 补 `dist/`、`jars/*.jar`;处理 2 个 `AD` | 仓库 | 2 命令 |
| 9 | `mod_info_ENG.json` 版本改 2.6.0、补 MagicLib 依赖 | 1 文件 | 2 行 |

**验收**:`gradle compileJava`、`gradle verifyFormat`、`gradle jar` 三绿;`gradle build` 通过;英文 CSV 加载日志出现 5 条挑战。

### 5.1.1 执行记录(2026-11-05,已落地)

**结果:第 1、2、3、6、7、8、9 项完成,`verifyFormat` / `compileJava` / `jar` / `build` 全绿。第 4、5 项按维护者决定不做。**

| 项 | 状态 | 实际改动 |
|---|---|---|
| 1 | ✅ | `UNGP_modPlugin.java:89-91` 加 `x.alias("ungp_sharedData", UNGP_SharedData.class)` + 2 行注释。别名是叠加的,XStream 仍能按旧 FQCN 解析老存档,不需要迁移代码 |
| 2 | ✅(**实测 5 行,非 1 行**) | 表头补 `dpToDefeat` 后,前 4 行也各要补一个空列(`-1,-1,,TRUE`),否则它们会跟着右移错位。已用 `csv` 模块核对:CHS/ENG 两侧均为 9 列,仅 `7_kan` 有 `dpToDefeat=5000` |
| 3 | ✅ | `UNGP_StillGrowingUp.java:71` `Math.min` → `Math.max` |
| 6 | ✅ | `UNGP_RulesManager.java:103-113` 拆出 `updateRulesCache()`(try/finally 复位 `needUpdateCache`)+`updateRulesCacheInternal()`(原方法体),删除原尾部赋值。异常仍向上抛(只保证复位),日志只记一次 |
| 7 | ✅ | 新增 `.gitattributes`(`* text=auto eol=lf` + 21 条 binary);`to_lf.py` 把工作区 183 个文件转为 LF(`checkLf` 由 183 → 0)。转换后 157 个文件因 **stat 缓存**显示为 modified,但字节内容与 index 完全一致(`git diff` 为空、`git hash-object` 等于 index blob),已用 `git add` 刷新 stat,**没有任何内容进入暂存区**;`.gitattributes` 自身已入暂存区,随改动提交即生效 |
| 8 | ✅ | `git rm --cached jars/ungp.jar` 与 2 个 `AD` 死条目;`.gitignore` 补 `jars/*.jar`、`dist/`、`__pycache__/`。工作区 jar 保留(仍可 `gradle jar` 复现),只是不再入库 |
| 9 | ✅(仅版本) | `mod_info_ENG.json:5` 2.5.0 → 2.6.0 |
| 4 | ⛔ 不做 | 维护者确认战斗引擎每场战斗新建 plugin 实例,不存在状态累积。**(b)(c) 两条仍成立但降为 P3**:`UNGP_SpecialistCombatPlugin.java:43` 调的是会抛 `RuntimeException` 的 `getDataInSave()`(应 `getDataInSaveOrNull()`,让 `:45/:53` 的判空变成真防线);`:58` 传给 `tweakBeforeApply` 的是可变 `ArrayList`,与接口 javadoc 的 `Unmodifiable` 契约相反(生涯侧 `RulesManager:153` 用 `List.copyOf`)。两条各 1 行,随时可补 |
| 5 | ⛔ 不做 | 维护者判断:项目运行已久未遇到缺属性 NPE,收益不足以动存档类。**保留原结论备查**:字段缺失时 `saveActivatedRules:86` / `getActivatedRules:114` / `completeChallenge:75` / `addTimesToChangeSpecialistMode:69` 均直接解引用 |

**第 9 项的 MagicLib 结论修正**:`grep -ri "magiclib|magiclib" src/` **零命中**,`build.gradle:28` 只是把它放进编译期 `compileOnly` 目录。它是**无用运行时依赖**,不该补进 `mod_info_ENG.json`,正确做法是删掉 `mod_info.json:8` 那一条(顺带让中英两版一致)。本次未动 `mod_info.json`,待维护者决定。

**第 7 项连带发现(纯历史遗留,与 2.6.0 功能无关)**:`checkLf` 修好后 `verifyFormat` 才暴露后面两个关卡,均已一并修好 ——
- `checkJavaSpaces`:7 个文件仍是 tab 缩进(`UNGP_DangerZone` / `ExplosiveSniper` / `FragmentGraveyard` / `LuckySeven` / `StrikerSchool` / `Sneaker` / `Thermodynamics`),已转 4 空格(纯空白 diff,197/197 行);
- `checkJavaEof`:10 个文件缺文件末空行(含 `UNGP_SpecialistBackgroundUI` / `BlueprintsDataSaver` / `RainPour` / `Ying` / `Yang` 等),已补。

> 说明:这三类(CRLF / tab / EOF)此前被"`checkLf` 先失败"层层遮住,所以"格式门禁形同虚设"不只是行尾问题 —— 门禁修好一次,它才第一次真正审视仓库。建议把 `verifyFormat` 接进发布流程,`formatProject` 作为提交前习惯。

### 5.2 冲刺 1:2.6.1 发布前,1~2 天(把在途代码收干净)

1. **`UNGP_BountyHunter` 重做账本**:`Map<String intelId, Integer bonus>` 替代 `WeakHashMap<intel 对象>`;`advanceInCampaign` 改 `params.isOneDayPassed()` 触发;精确类判断改 `isAssignableFrom` + 白名单。
2. **`UNGP_SlipperyModification`**:`cleanUp()` 只清 `PENDING`,**保留 `TRIGGERED`**(它必须随存档持久,否则重拾规则 = 重新发罚);`triggered` 记入"该船已受罚"并在 UI 说明。
3. **`FullOfCompany` / `TachLabour`**:先记录原关系值到 ruleData 槽,`unapplyGlobalStats` 里还原。
4. **框架契约统一**:`UNGP_BaseRuleEffect.cleanUp()` 默认实现 = `unapplyGlobalStats()` + `clearAllDataInCampaign()`;`updateRulesCache` 卸载阶段改用"上次激活 ∪ 本次激活"(需持久化"上次激活 id 列表")。
5. **`SimpleI18n.format`**:`catch` 至少 `log.error` 一次再降级返回原文,别把 null 递给 `addPara`。
6. **`distZip` 修正**:排除 `data/**/*.py`、`*.zip`;纳入 `swapLangFile-切换语言.py`;`.gitignore` 为它开口子。
7. **`UNGP_ChallengeLoader`**:列缺失时 `log.warn`(根治 P1-1 这类静默)。

### 5.2.1 执行记录(2026-11-05,已落地:第 1、2 项)

**第 1 项 `UNGP_BountyHunter` 已重写**,并在查证后做了两处方案调整:

- **key 用什么**:原计划用 person id,查证后改用两种赏金各自的稳定标识 —— `PersonBountyIntel.getPerson().getId()`(人物赏金)与 `HubMission.getMissionId()`(酒馆悬赏,接口方法,公开可见)。人物 id 单独用不足以区分同一个人发出的多个委托,任务 id 更精确。
- **触发时机**:用户要求“如果能找到接取时点,就接取后短时间改赏金,不要全量遍历”。查证结论是**公开 API 里没有“任务被接取 / intel 被加入”的全局事件**(该 API 版本没有 `CampaignEventListener` 这种全局事件总线,intel 相关的监听接口里也没有 add 回调)。可用的最佳时点是 `CoreUITabListener.reportAboutToOpenCoreTab`:引擎确实会派发(`starfarer_obf.jar` 内 7 处引用),原版 `SlipstreamVisibilityManager` 也在用。因此最终是**三触发**:规则生效时一次、每天一次(`isOneDayPassed` 兜底原版定期生成的人物赏金)、玩家打开任意核心界面时一次(赏金面板就在 INTEL 里)。全部只做一次 intel 列表遍历,逐帧扫描已删除。
- **旧存档兼容**:读档时若槽里还是“intel 对象 → bonus”的旧 map,就地迁移成 id 账本(认不出 id 的条目丢弃)。丢弃是刻意的取舍:宁可不回滚,也不要减到错误的赏金上。
- 顺带修正:精确类判断改 `Class.isAssignableFrom`,别的 mod 继承 `MilitaryCustomBounty`/`UnderworldCustomBounty` 的赏金也能吃到加成;`unapplyGlobalStats` 开头加“没有账本就返回”的守卫,避免给从没生效过的规则在存档里创建空槽。

**第 2 项 `UNGP_SlipperyModification` 已改**:`cleanUp()` 只清 `PENDING`,保留 `TRIGGERED`。理由:`saveActivatedRules` 是规则被卸下时唯一调用 `cleanUp()` 的地方,清掉 `TRIGGERED` 等于“卸下再装回 = 再罚一次”。**没有**顺手修剪 `TRIGGERED`(原本考虑过):舰船在殖民地仓储里时不在 `getMembersListCopy()` 里,按舰队修剪会让取回仓储的船重新可罚。

**未执行**:第 3 项按维护者决定保持现状;第 4 项见 5.2.3。

### 5.2.2 执行记录(2026-11-05,已落地:第 5、6、7 项)

**第 5 项 `SimpleI18n` 已改**:`format` 不再在 `catch` 里返回 `null`。

- `getString` 抛异常或返回空 → 记一次 `error` 日志,返回 **key 本身**;
- `String.format` 失败(占位符数量对不上)→ 记日志,返回 **模板原文**(界面上会看到 `%s`,但不会抛 NPE);
- 同一个 key 只报一次(`ConcurrentHashMap.newKeySet()`),避免 Tooltip 每帧刷日志;
- `I18nSection.get` 原来直连 `Global.getSettings().getString`,现在走同一个 `SimpleI18n.get`,两条路径行为一致。

行为变化是刻意的:缺 key 时以前是 `addPara(null)` 在 UI 层崩溃、堆栈指错地方,现在界面显示 key 名 + 日志点名。

**第 6 项 `distZip` 已收紧**:发布包 197 → 195 个文件。

- `include "jars/**"` → `include "jars/${modJarName}"`(`modJarName` 取自 `mod_info.json` 的 `jars` 字段),残留旧 jar 不再进包;
- 新增 `exclude`:`**/*.py`、`**/__pycache__/**`、`**/*.zip`、`**/*.7z`。被清掉的 `data/campaign/CSV_id_order.py`(455 B)和 `graphics/ungp/icons/UNGP PS Blend.zip`(2719 B)都是**未被 git 追踪**的工作区文件,留在包里等于同一版本在不同机器上产出不同内容。
- **没有**把 `swapLangFile-切换语言.py` 加进发布包:它被 `.gitignore` 的 `*.py` 挡在版本库外(`!` 例外只开了 `tools/`),不入库就无法保证包内容可复现。若要随包发布,需 `.gitignore` 加 `!swapLangFile-切换语言.py` 并在 `distZip` 显式 include —— 这会改变玩家拿到的东西,属产品决定,未擅自做。

**第 7 项 `UNGP_ChallengeLoader` 已加告警**:

- 载入后先核对表头:`EXPECTED_COLUMNS` 中任一列不在首行的 key 里就 `warn`,并列出实际列名。`optString/optInt` 遇缺列会静默填默认值,P1-1(英文挑战 CSV 缺 `dpToDefeat` 导致整行错位)正是这样被吞掉的;
- 被 `isValid()` 挡下而跳过的挑战,逐条 `warn` 出 id 与 `rulesRequired`/`milestoneToUnlock`,不再无声消失。

### 5.2.3 执行记录(2026-11-05,已落地:第 4 项 —— 仅 (b) 部分)

**(a) 默认 `cleanUp()` 清数据 —— 实现后撤回,不做**

- 一度实现为 `UNGP_BaseRuleEffect.cleanUp()` 默认 = `unapplyGlobalStats()` + 按规则 id 前缀清空 `UNGP_SharedData`。维护者指出:部分规则的存档数据是**故意跨卸载保留**的(重新选中同一条规则时要接着用),默认清空会毁掉这些规则的状态。该判断成立,已整体回退:默认 `cleanUp()` 保持空实现,数据去留由每条规则自己决定(现状即设计)。
- 连带回退:`UNGP_SharedData.clearRuleDataOfRule(String)`、`UNGP_RulesManager.checkRuleIdPrefixes()` 一并删除(没有调用方就不该留着)。
- 保持的一处文档:`UNGP_RuleEffectAPI.cleanUp()` 的注释补了一句"默认实现是空的、存档数据不会随卸载删除",避免下一个人再把它当成漏洞"修"回去。

**(b) 卸载阶段从"全量 86 条"改成"上次真正生效的 ∪ 本次记录的"**

- 新增持久化字段 `UNGP_InGameData.appliedRuleIDs` + `setAppliedRules()`/`getPreviouslyAppliedRules()`:刷新成功时记录这一轮真正 apply 的规则;旧存档没有该字段时退回 `activatedRuleIDs`(旧存档里两者本来就是同一批)。
- 新增持久化字段 `UNGP_InGameData.ruleCacheDirty`:`apply` 前置真,`saveActivatedRules` + `setAppliedRules` 之后置假。上次刷新中途抛过异常的存档,下一次刷新自动退回全量清扫(`LOGGER.warn` 一次)。
- **listener 清理保持全量**:对话框流程(`UNGP_InteractionDialog:512` → `:520`、`UNGP_RepickRulesDialog:103` → `:106`)会先 `saveActivatedRules` 再刷新缓存,那时"上次生效"的记录已被新集合覆盖,只能在这里对全部规则兜底;同理 `isMilestoneRollLocked` 的锁定循环仍是全量。
- `saveActivatedRules` 仍然在记录新集合时对被移除的规则调 `cleanUp()`(对话框流程依赖它的即时清理);`tweakBeforeApply` 仍在卸载之后、apply 之前执行,它只改规则列表、不读 stats,所以并集用的是 tweak **之前**的列表 —— tweak 新加进来的规则从没被 apply 过,不需要 unapply。

**风险与边界**

- 唯一能绕过并集的是"规则刷新中途抛异常、之后又存了档"的存档:这类存档里写进 stats 的规则没被记录,`ruleCacheDirty` 只兜得住下一次刷新。原实现的全量清扫同样擦不掉它(清扫只能作用于当前存在的规则对象)。
- 未实机验证。建议冒烟:装一条数据型规则(如 `monotonous_teller`)→ 卸下 → 存档读档,确认 `UNGP_SharedData` 里该规则的键与规则效果一起消失;再连续两次在对话框里改规则,确认效果没有叠加。

### 5.3 冲刺 2:2.7,1~2 周(架构偿还)

1. **拆 `RulesManager`(952→5 个类)**:Loader / Cache / Dispatcher / Presenter / URule(纯数据)。这是所有后续重构的前提。
2. **tag 分发改成注册表**:`Map<Class<? extends UNGP_Tag>, List<Object>>` + 启动期扫描 `URule` 的接口集合,消灭 29 处 instanceof 里的绝大部分,新增 tag 从"改 5 个文件"变成"实现接口即可"。
3. **性能三件套**:战斗 `ships × tags` 改分桶 + 有规则才进循环;经济监听增量更新(脏市场集合);每帧路径(成员 buff 同步、背景板音频、巡逻队 MemoryAPI)改日事件/脏标记。
4. **i18n 去耦合**:`URule` 不再在构造期读 i18n,改由 `RulePresenter` 按需取。

### 5.4 明确不建议做的事

- **不要**为了"看起来现代"引入 Kotlin / 新构建系统 / wrapper —— 当前 6 秒编译、jar 可复现,已经是 Starsector mod 里的优等生。
- **不要**顺手 `gradle formatProject` 全仓格式化:34 处违规分散在 32 个文件,会和在途改动打架,先在冲刺 0 里逐文件处理。
- **不要**在没有别名机制保护的前提下移动任何进 `persistentData` 的类(P0★-1)。

---

## 6. 复核记录:我否决/修正的条目

审计的价值一半在于"什么不是问题"。以下 5 条来自分报告,经 Lead 独立复核后调整:

| 原结论 | 复核结果 | 证据 |
|---|---|---|
| "监听器可能泄漏:`removeListenerOfClass(接口)` 能否命中存疑"(01/03 P1) | **否决(不是问题)** | `FastIterationClassifier.classify()` 按"自身类+父类+全部接口"建索引(`apis/starsector-deobf-renamed/.../FastIterationClassifier.java:26-45`),`ObjectRepository.getList(Class)` 精确命中(`:67-74`) |
| "`UNGP_SpecialistCombatPlugin:43` 用会抛异常的方法,战斗开场崩溃"(01 P1) | **降级 P1→P2** | 复核更正:`getInstance()` 缺 plugin 时**确实抛** `RuntimeException`(`UNGP_CampaignPlugin:48-55`),不是静默 new;但该调用被 `:41` 的 `isInCampaign()/isInCampaignSim()` 守卫,而 `UNGP_modPlugin:52` 在 onGameLoad/onNewGame 必然注册 plugin,正常流程不触发。真实问题是"判空死代码 + 用错 API" |
| "`instanceof` 分发 19 处 / 3 个文件"(01) | **加强** | 实测 **29 处 `instanceof UNGP_*` / 10 个文件**(全项目 `instanceof` 共 56 处) |
| "赏金账本 WeakHashMap 是 P0"(01/03) | **定为 P1** | 单次存档流内 XStream 保引用恒等,`containsKey` 不会失配;真正的缺陷是把游戏对象写进 `persistentData`(跨版本脆弱)与每帧扫描。**但发布前必须改**,否则一次 XStream 行为变更即变 P0 |
| "`MapConverter(WeakHashMap)` 以 PRIORITY_LOW 注册疑为死代码"(01 存疑) | **记为 P3 待验证** | `WeakHashMap` 经 `Map.class` 的默认 `MapConverter` 已可序列化,自定义注册大概率被遮蔽;但不影响正确性,不值得为它花时间 |

另有两处口径纠正:`UNGP_StillGrowingUp:71` 的 `Math.min` 缺陷、`UNGP_FullOfCompany/TachLabour` 无 unapply,**均经 Lead 实读源码确认成立**。

---

## 附录 A · 分报告

| 文件 | 内容 |
|---|---|
| [docs/audit/01-architecture.md](docs/audit/01-architecture.md) | 架构与框架:契约模型、apply/cleanUp 泄漏逐字段、tag 分发全图、RulesManager 拆分、公共 API 兼容、在途改动逐文件缺陷 |
| [docs/audit/02-data-i18n.md](docs/audit/02-data-i18n.md) | 数据与 i18n:双语 CSV/JSON 逐列交叉核对、类与图标引用双向差集、字符串键三方对齐、可复现脚本附录 |
| [docs/audit/03-perf-save.md](docs/audit/03-perf-save.md) | 存档/生命周期/性能:XStream 别名逐项、监听器注册注销全表、每帧热点量化、UI 与幂等性 |
| [docs/audit/04-build-quality.md](docs/audit/04-build-quality.md) | 构建与仓库:三条命令实跑记录、格式违规清单、版本一致性、.gitignore 与打包内容、工具链改进 |

## 附录 B · 关键复现命令

```bash
# 构建三连(需设 JAVA_HOME 指向 JDK17;本机 PATH 无 java)
gradle compileJava && gradle verifyFormat && gradle jar

# 双语 CSV 结构比对(见 02 报告附录 A.1)
python -c "import csv,sys;[print(i,len(next(csv.reader(open(f,encoding='utf-8-sig'))))) for i,f in enumerate(sys.argv[1:])]" data/campaign/UNGP_challenges.csv data/campaign/UNGP_challenges_ENG.csv

# 规则类与 CSV 的双向差集(见 02 报告附录 A.2)
grep -o 'ungp\.impl\.rules\.[A-Za-z0-9_.]*' data/campaign/UNGP_rules.csv | tr '.' '/' | sort -u

# 复现 P0★-1:临时把 UNGP_SharedData 改名后读旧档(不要在正式分支上做)
```

---

*本报告由 Lead 汇总 4 份分报告并逐条复核得出。所有 `path:line` 对应 2026-11-05 工作区状态(含未提交改动);未执行任何 git 写操作。*
