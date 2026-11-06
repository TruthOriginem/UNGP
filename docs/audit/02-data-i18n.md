# 02 数据与国际化一致性审计(UNGP / Starsector 0.98a)

- 审计对象:`data/**` 全部 22 个数据文件 + 对应 `src/` 读取端 + `graphics/ungp/**` 图标资源
- 审计方式:**脚本交叉核对**(Python `csv` 模块 + 全仓正则扫描),不依赖目测;关键结论均给出 `path:line`
- 审计基线:工作区当前未提交改动(含本轮新增规则与图标)纳入审计范围
- 严重度:P0 存档损坏/崩溃/数据错误 · P1 功能正确性或明显性能问题 · P2 可维护性/扩展性 · P3 风格与锦上添花

---

## 0. 结论摘要

**未发现 P0。** 数据文件的类引用、资源引用、字符串键三方一致性整体质量很高:86 条规则的 `effectPlugin` 与 `src/ungp/impl/rules/**` 是**完美 1:1**(缺失 0 / 孤儿 0),`spritePath` 缺失 0,图标孤儿 0,153 处静态字符串键在 CHS/ENG 两侧**全部命中**,CSV `tags` 列的 2 个 token 与枚举定义**零悬空**。

唯一的功能性数据错误集中在 **`UNGP_challenges_ENG.csv` 缺列**(P1),它会让英文构建中的 `7_kan` 挑战被静默丢弃。

| 维度 | 统计 | 结论 |
|---|---|---|
| `UNGP_rules.csv` / `_ENG` | 103 行 / 103 行,14 列 / 14 列,86 条规则 | 行数、列数、表头、id 顺序、逐列 `%s` **0 差异** |
| `UNGP_backgrounds.csv` / `_ENG` | 40 行 / 40 行,12 列 / 12 列(9 条启用) | **0 差异** |
| `UNGP_challenges.csv` / `_ENG` | 6 行 / 6 行,**9 列 / 8 列** | **列数不一致 1 处**,见 P1-1 |
| `LunaSettings.csv` / `_ENG` | 8 行 / 8 行,9 列 / 9 列(6 条设置) | **0 差异** |
| `effectPlugin` ↔ 实现类 | 86 ↔ 86 | 缺失类 **0**、孤儿类 **0**、重复引用 **0** |
| `spritePath` → png | 86 条规则 | 缺失 **0**(1 条复用原版资源,合法) |
| `graphics/ungp/**` png | 118 个 | 孤儿 **0**、引用不存在 **0** |
| `strings.json` / `_ENG` 叶子键 | 178 / 179 | 代码侧静态键缺失 **0 / 0**;疑似死键 **10** 个 |
| `getString` 调用 | 静态 153 处 + 动态 6 处 | CHS 缺 0、ENG 缺 0 |
| CSV `tags` 列 ↔ 枚举 | 2 token(milestone / no_roll) | 悬空 **0**、未定义 **0** |
| `tag_data.json` ↔ 代码 intel 标签 | 1 个键 `ungp` | 代码侧 `tags.add("ungp")` **3 处**,对齐 |
| `extra1`/`extra2` 双向契约 | 32 条规则使用 | 冲突 **0** |
| `desc` `%s` ↔ `getDescriptionParams` index | 86 条 | 覆盖不足 **0** |
| LunaSettings fieldID ↔ 代码读取 | 4 个被读取 | 悬空 **0 / 4** |
| 本轮新增规则 | 任务描述 7 条 | **实测 6 条**,清单全部齐全 |

问题计数:**P0 = 0,P1 = 1,P2 = 5,P3 = 8**。详见 §8。

---

## 1. 审计方法与可复现性

所有脚本均为一次性脚本,放在系统临时目录 `C:\Users\55381\AppData\Local\Temp\ungp-audit\`,**未写入仓库**。所用解释器:`C:/Users/55381/.dsh/dsh-runtimes/dsh-primary-runtime/dependencies/python/python.exe`(Python 3)。脚本正文见 §9 附录 A。

CSV 一律用 Python `csv` 模块解析(能正确处理引号包裹的逗号与 `""` 转义),JSON 的宽松解析(去 `#` 注释、去尾逗号)在脚本内显式实现,因为游戏侧 `org.json` 走的就是宽松路径(实测见附录 A.4)。

---

## 2. Q1:CHS/ENG 两份文件的行数、列数、id 顺序、占位符是否一致

### 2.1 `data/campaign/UNGP_rules.csv` vs `UNGP_rules_ENG.csv` —— 完全一致

| 检查项 | CHS | ENG | 结果 |
|---|---|---|---|
| 物理行数 | 103 | 103 | 一致 |
| 列数(表头) | 14 | 14 | 一致 |
| 表头 | `id,name,short,desc,cost,isBonus,isGolden,spritePath,effectPlugin,extra1,extra2,source,tags,spriteAuthor` | 同左 | 逐字段一致 |
| 真实规则数 | 86 | 86 | 一致 |
| 空白分隔行 | 16 | 16 | 一致 |
| id 逐行顺序 | 86 个 | 86 个 | **逐行完全相同** |
| 逐列 `%s` 个数 | — | — | **0 处差异** |

结构说明:103 行 = 1 行表头 + 86 条规则 + 16 行空白分隔行(全空 14 字段)。空白行由 `UNGP_RuleInfoLoader` 的 `if (isEmpty(id)) continue;`(`src/ungp/scripts/campaign/specialist/rules/UNGP_RuleInfoLoader.java:120`)跳过。

> 口径说明:任务描述给的是"各 103 行",与实测(`wc -l` = 103)一致;若按"以 `\n` 切分后的数组长度"会得到 104,那是末行空串,不要误读。

占位符口径为**每一列**分别统计 `%s`/`%d` 个数再逐行比对,不只比对 `desc`。`desc`/`short`/`extra1`/`extra2` 的占位符在 86 条规则上 CHS 与 ENG **逐条完全相等**。

### 2.2 `data/campaign/UNGP_backgrounds.csv` vs `UNGP_backgrounds_ENG.csv` —— 完全一致

40 行 / 40 行,12 列 / 12 列,表头相同,id 顺序一致,`%s` 0 差异。

结构:1 行表头 + 第 2 行 `#id,...`(列说明,被 `#` 注释)+ **9 条启用背景**(`nothing`、`poor_scientist`、`hacker`、`genius`、`seeker`、`smuggler`、`admiral`、`capitalist`、`the_chosen_one`)+ 29 行 `#` 注释行(停用的备选背景,如 `#avenger`、`#the_cursed_one`)+ 1 行空行 = 40。

`#` 开头的行会被引擎电子表格加载器跳过:证据是原版 `starsector-core/data/campaign/rules.csv` 本身有 887 行以 `#` 开头,且本 mod 若不跳过则 `UNGP_BackgroundManager` 会把 `#avenger` 当作 id 加载并在 `src/ungp/scripts/campaign/background/UNGP_BackgroundManager.java:78-81` 抛 `RuntimeException` 导致游戏崩溃 —— 现状不崩,即证明 `#` 被跳过。

### 2.3 `data/campaign/UNGP_challenges.csv` vs `UNGP_challenges_ENG.csv` —— **列数不一致(P1)**

| 项 | CHS | ENG |
|---|---|---|
| 行数 | 6(1 表头 + 5 挑战) | 6 |
| 列数 | **9** | **8** |
| 表头 | `id,name,rulesRequired,positiveLimitation,durationByMonth,dpToDefeat,needMaxLevel,canReselectRules,milestoneToUnlock` | `id,name,rulesRequired,positiveLimitation,durationByMonth,needMaxLevel,canReselectRules,milestoneToUnlock`(**缺 `dpToDefeat`**) |

逐行核对(两份文件行号一致,数据行 2–6):

| 行 | id | CHS 字段数 | ENG 字段数 | ENG 映射结果 |
|---|---|---|---|---|
| 2 | `seafood_fleet` | 9 | 8 | 正确(仅少一个空 `dpToDefeat`) |
| 3 | `starfarer_alone` | 9 | 8 | 正确 |
| 4 | `dao_of_randomness` | 9 | 8 | 正确 |
| 5 | `fortune_is_variant` | 9 | 8 | 正确 |
| 6 | `7_kan` | 9 | 9 | **值错位** |

`7_kan` 是唯一带非空 `dpToDefeat` 的行(CHS 值 `5000`)。ENG 行虽然也写了 `5000`,但表头少一列,于是按位置映射成 `needMaxLevel="5000"`、`canReselectRules="TRUE"`、`milestoneToUnlock="FALSE"`,而 `symbol_perseus` 变成第 9 个多余字段被丢弃。

后果链(全部可指到行):

1. `src/ungp/scripts/campaign/specialist/challenges/UNGP_ChallengeLoader.java:39` 的 `row.optInt("dpToDefeat", -1)` 在 ENG 侧取默认值 **-1**;
2. `src/ungp/scripts/campaign/specialist/challenges/UNGP_ChallengeInfo.java:44` 的 `URule.getByID("FALSE")` 返回 `null`;
3. `UNGP_ChallengeInfo.java:47-50` 因此把 `isValid` 置 false;
4. `UNGP_ChallengeLoader.java:44-45` 的 `if (challengeInfo.isValid())` 不成立 → **`7_kan` 这条挑战在英文构建里被静默丢弃**,不报错、不进 `challengeInfoMap`;
5. 即便忽略丢弃,`5000 DP` 击破要求也丢失(`UNGP_ChallengeIntel.java:89-90` 用 `getDpToDefeat() > 0` 判断是否作为完成条件,tooltip 同理见 `UNGP_ChallengeInfo.java:176-180`)。

另外 4 条挑战在 ENG 侧只是丢掉了本为空的 `dpToDefeat` 列,`optInt` 默认 -1 与 CHS 空值的解析结果一致(`getInt` 对空串抛异常 → `optInt` 返回默认值),**行为等价,不构成差异**。

### 2.4 `data/config/LunaSettings.csv` vs `LunaSettings_ENG.csv` —— 完全一致

8 行 / 8 行,9 列 / 9 列,表头相同,0 差异。结构:1 表头 + `ungp_generalHeader` + `ungp_noLevelLimit` + `ungp_noTimesLimit` + 1 空行 + `ungp_uiHeader` + `ungp_noSpecialistWidget` + `ungp_noRuleMessageWhileBattleStart`。

### 2.5 其余文件

| 文件 | 记录 | 说明 |
|---|---|---|
| `data/campaign/UNGP_dataSavers.csv` | 3 行(1 表头 + 2),无末尾换行 | 无 `_ENG` 对 |
| `data/campaign/abilities.csv` | 2 行(1 表头 + 1) | 无 `_ENG` 对 |
| `data/characters/skills/skill_data.csv` | 3 行(1 表头 + 2),无末尾换行 | 无 `_ENG` 对 |
| `data/config/version/version_files.csv` | 2 行(1 表头 + 1) | 指向 `UNGP.version`,根目录文件存在(266 字节) |

---

## 3. Q2:effectPlugin / 孤儿类 / spritePath / 孤儿图标

### 3.1 `effectPlugin` ↔ 实现类:**完美 1:1**

| 检查 | 结果 |
|---|---|
| CSV 中去重后的 `effectPlugin` 全限定名 | 86 个(86 条规则,重复 0) |
| `src/ungp/impl/rules/**` 下 `.java` | 86 个 |
| CSV 有、类不存在(加载必崩) | **0** |
| 类存在、CSV 未引用(孤儿类) | **0** |
| 一对多 / 多对一 | **0** |

引用入口:`src/ungp/scripts/campaign/specialist/rules/UNGP_RuleInfoLoader.java:133`(`row.getString("effectPlugin")`,缺列抛 `JSONException`)→ `:144` `classLoader.loadClass(...)` → `:146` `newInstance()`。异常在 `:160` 统一包成 `throw new RuntimeException("Failed to load UNGP rules:", e)`,**fail-loud,不静默**。

### 3.2 `spritePath`:缺失 0

86 条规则的 `spritePath` 中 85 个解析到仓库内真实文件;1 个解析到 mod 目录之外:

| 规则 | spritePath | 说明 |
|---|---|---|
| `lobster_perday` | `graphics/icons/cargo/lobster.png` | 该路径**在 mod 内不存在**,但原版 `D:\StarsectorDevelopment\098\Starsector\starsector-core\graphics\icons\cargo\lobster.png` 存在(10483 字节),属合法的原版资源复用 |

反向检查:`data/`、`src/`、`graphics/` 文本中被引用但**文件不存在**的 `graphics/...png` = **0**。

### 3.3 图标孤儿:0

| 检查 | 结果 |
|---|---|
| `graphics/ungp/**` 下 png | 118 个 |
| 全仓文本引用扫描后"无任何引用"的 png | 1 个:`graphics/ungp/fonts/ungp_orbitron.png` |
| 其真实引用方 | `graphics/ungp/fonts/ungp_orbitron.fnt`(`page id=0 file="ungp_orbitron.png"`)→ 有效位图字体页,**非孤儿** |
| 结论:孤儿图标 | **0** |

`data/config/settings.json` 注册的图标/特效/插画(spritePath 引用目标)全部存在:`UNGP_specialist_mode_logo`、`UNGP_intel_icon`、`rule_negative/positive/golden/milestone/golden_milestone/default`、`UNGP_difficulty_gamma/beta/alpha/omega`、`UNGP_logo`、`UNGP_frame`、`UNGP_specialist_bg_corner/seamless/ring0-2`、`UNGP_specialist_icon_layer0/1`、`UNGP_headhunting_target_edge`。

初筛时被判为"疑似孤儿"的 22 个 png(`UNGP_difficulty_*.png`、`UNGP_icon.png`、`ungp_backgrounds/*.png`、`ungp_rules/default.png` 等)经逐一定位,引用方分别是 `data/config/settings.json`、`data/campaign/UNGP_backgrounds.csv` 的 `spritePath` 列或 Java 字面量 → **全部非孤儿**。

---

## 4. Q3:strings.json / strings_ENG.json key 集合与代码引用

### 4.1 双语文档结构

| 检查 | CHS | ENG |
|---|---|---|
| 叶子键数 | 178 | 179 |
| 顶层分类集合 | 完全相同 | 完全相同 |
| 键差异 | — | ENG 多 `.UNGP.inGameData`(CHS 无) |
| 占位符差异 | — | 0 处 |
| CJK 残留 | — | 0 处 |

CHS 侧"无中文"的叶子仅 5 个,均合法:2 个 URL(`UNGP_rules.button_feedback_url`、`UNGP_rules.pick_times_collection_url`)与 `UNGP_rule_impl.ct_7/ct_77/ct_777`(彩蛋文本 `+7%` / `+77%!` / `+777%!!`)。

`.UNGP_rules.middle_space`:CHS 是空串、ENG 是单空格。**不是缺陷**:两处使用点 `src/ungp/scripts/campaign/specialist/rules/UNGP_RulesManager.java:763` 与 `:777` 都用 `sb.append(rules_i18n.get("middle_space"))` 拼接规则名,空串/空格正是中英文排版所需。

ENG 多出的 `.UNGP.inGameData` 键无代码/CHS 对应,属**孤儿键**(P3),但不影响运行。

### 4.2 代码侧 key 完整性

| 检查 | 结果 |
|---|---|
| 裸 `Global.getSettings().getString("cat","key")` 字面量调用 | **0 处**(全部经 `ungp.scripts.utils.SimpleI18n`) |
| 静态可解析 `(category, key)` 调用 | 153 个 |
| 其中 CHS 缺失 | **0** |
| 其中 ENG 缺失 | **0** |
| 动态 key 调用 | 6 处 |

6 个 `I18nSection` 实例(声明于 `src/ungp/scripts/utils/Constants.java:5-9` 与 `src/ungp/api/rules/UNGP_BaseRuleEffect.java:28`):

| 字段 | category |
|---|---|
| `root_i18n` | `UNGP` |
| `ability_i18n` | `UNGP_abilities` |
| `rules_i18n` | `UNGP_rules` |
| `backgrounds_i18n` | `UNGP_backgrounds` |
| `codex_i18n` | `UNGP_codex` |
| `i18n` | `UNGP_rule_impl` |

LunaSettings 相关不读 `strings.json`,而是走 Lunalib(`src/ungp/scripts/utils/lunalib/UNGP_LunaSettingCompatible.java:19` → `LunaSettings.getBoolean(Constants.MOD_ID, fieldId)`),其 fieldID 与 `LunaSettings.csv` 的对齐见 §5.4。

6 处动态 key 正好解释了静态扫描里"未命中"的 `inGameData_*` / `inheritData_*` / `unmet_*` 家族,它们不是死键:

| 位置 | 动态拼接 |
|---|---|
| `src/ungp/scripts/campaign/inherit/UNGP_BlueprintsDataSaver.java:278` | `descKey + "_3"` |
| `src/ungp/scripts/campaign/inherit/UNGP_CreditsDataSaver.java:67` | `descKey + "_2"` |
| `src/ungp/scripts/campaign/inherit/UNGP_InheritData.java:93/94/103` | `descKey + "_0/_1/_4"` |
| `src/ungp/scripts/campaign/ui/UNGP_RulePickPanelPlugin.java:721` | `unmetReason.getType().getI18nKey()` |

### 4.3 疑似死键(P3)

`src/` 全仓文本检索 0 命中(唯一例外是注释):`inheritedPoints`、`rulepick_cost`、`rulepick_desc`、`rulepick_ruleAmount`、`rulepick_title`、`rulepick_warning`、`startRecord`;另有 `specialistMode` 只在注释行 `src/ungp/scripts/campaign/inherit/UNGP_InheritData.java:80` 出现。`on` / `off` 没有任何 `get("on")` / `get("off")` 形式调用。

| 键 | strings.json | strings_ENG.json |
|---|---|---|
| `specialistMode`(仅注释行引用) | :6 | :6 |
| `startRecord` | :10 | :10 |
| `rulepick_title` | :51 | :53 |
| `rulepick_desc` | :52 | :54 |
| `rulepick_warning` | :53 | :55 |
| `rulepick_cost` | :55 | :57 |
| `rulepick_ruleAmount` | :56 | :58 |
| `inheritedPoints` | :65 | :66 |
| `on` | :67 | :68 |
| `off` | :68 | :69 |

这些键保留成本很低,但建议在确认 UI 不会再启用后清理,或加注释说明保留原因。

### 4.4 本轮新增字符串键

`UNGP.specialist_mode_combat_blocked` 在 `data/strings/strings.json:79` 与 `data/strings/strings_ENG.json:80` 同步新增,且已被 `src/ungp/scripts/plugins/UNGP_SpecialistCombatPlugin.java:105` 引用 —— **新增键完整、无孤儿**。

---

## 5. Q4:tag 三方对齐(需要纠正命题)

任务描述假定 `tag_data.json`、`src/ungp/api/rules/tags` 接口、CSV `tags` 列是同一个 tag 体系。**实测这是三个互不相干的命名空间**,逐个核对如下。

### 5.1 `data/config/tag_data.json` = Starsector 情报(intel)标签配色

`tag_data.json` / `tag_data_ENG.json` 里只有 1 个键 `ungp`(字段 `name`/`color`/`width`/`putFirst`/`sort`)。同名原版文件 `starsector-core/data/config/tag_data.json` 存在且含 `New`/`Important` 等键,mod 文件靠引擎 merge 追加,不是替代。

代码侧对应关系(3 处 `tags.add("ungp")`):

| 位置 | 代码 |
|---|---|
| `src/ungp/scripts/campaign/intel/UNGP_BackgroundIntel.java:60` | `tags.add("ungp")` |
| `src/ungp/scripts/campaign/specialist/intel/UNGP_ChallengeIntel.java:282` | `tags.add("ungp")` |
| `src/ungp/scripts/campaign/specialist/intel/UNGP_SpecialistIntel.java:420` | `tags.add("ungp")` |

配置 1 个 ↔ 代码 3 处引用,**无悬空**。若将来新增 intel 标签字符串,必须在此文件补键,否则过滤面板会显示未着色标签。

### 5.2 `src/ungp/api/rules/tags/` = 规则混入标记接口,与 tag_data.json 无关

12 个接口,实现类统计(脚本按 `implements XxxTag` 统计):

| 接口 | 实现数 |
|---|---|
| `UNGP_CombatTag` | 29 |
| `UNGP_CampaignTag` | 16 |
| `UNGP_PlayerFleetTag` | 11 |
| `UNGP_CombatInitTag` | 7 |
| `UNGP_EconomyTag` | 6 |
| `UNGP_PlayerCharacterStatsSkillTag` | 5 |
| `UNGP_TweakBeforeApplyTag` | 4 |
| `UNGP_CampaignListenerTag` | 3 |
| `UNGP_PlayerFleetMemberTag` | 2 |
| `UNGP_PlayerShipSkillTag` | 1 |
| `UNGP_TweakBeforeApplyCombatTag` | **0** |
| `UNGP_SkillTag` | 被 2 个 tags 子接口 `extends`(父接口,不直接实现) |

`UNGP_TweakBeforeApplyCombatTag` 在本 mod 内 0 实现,但被 `src/ungp/scripts/plugins/UNGP_SpecialistCombatPlugin.java:61-62` 以 `instanceof` 作为扩展点调用 → **属公开扩展点,不算缺陷**(P3 提示)。

### 5.3 CSV `tags` 列 ↔ `URule.Tags` 枚举

实测取值只有 2 个 token,定义在 `src/ungp/scripts/campaign/specialist/rules/UNGP_RulesManager.java:309-317`(`NO_ROLL("no_roll")`、`MILESTONE("milestone")`),读取逻辑是同文件 `:701-715` 的 `hasTag(String)`,以及 `:746-747` 的 `isMilestone() = hasTag(Tags.MILESTONE)`。

| token | 使用行数 | 规则 id |
|---|---|---|
| `milestone` | 5 | `lobster_perday`、`birthday_present`、`entropius_mind`、`still_growing_up`、`symbol_perseus` |
| `no_roll` | 5 | `ying`、`yang`、`rain_pour`、`amnesia`、`entropius_mind` |

(`entropius_mind` 的 `tags` 是 `milestone,no_roll`,两 token 共用。)CHS/ENG 两份文件 tag 列**逐行相同**。拆列使用 `tags.split(",")`(`UNGP_RuleInfoLoader.java:138`),未 `trim`,但当前数据无空格,故无实际影响 —— 这是脆弱点,建议未来加 `.trim()`。

**结论:三方各自一致,无悬空 token、无未定义 token。**

### 5.4 LunaSettings fieldID 对齐(顺带核对)

`data/config/LunaSettings.csv` 定义 6 个 fieldID,其中被代码读取的 4 个:

| fieldID | 读取位置 |
|---|---|
| `ungp_noLevelLimit` | `src/ungp/scripts/campaign/UNGP_Settings.java:19` |
| `ungp_noTimesLimit` | `src/ungp/scripts/campaign/UNGP_Settings.java:25` |
| `ungp_noSpecialistWidget` | `src/ungp/scripts/campaign/UNGP_Settings.java:39` |
| `ungp_noRuleMessageWhileBattleStart` | `src/ungp/scripts/campaign/UNGP_Settings.java:45` |

**4/4 对齐,0 悬空**;`ungp_generalHeader` / `ungp_uiHeader` 是 Lunalib 的分组标题行,本就不被代码读取。另 `UNGP_Settings.java:53-58` 还从 `data/config/settings.json` 读 `noLevelLimit`/`noTimesLimit`/`noRuleMessageWhileBattleStart` 作为 legacy 回落。

---

## 6. Q5:extra1/extra2 与 desc 参数契约(是否会静默失败)

### 6.1 覆盖情况

| 检查 | 结果 |
|---|---|
| 使用 `extra1`/`extra2` 的规则 | 32 条 |
| CSV `extra1/extra2` 有值但实现类完全不引用 | **0** |
| 实现类引用 `getExtra*` 但 CSV 两列皆空 | **0** |
| `desc` 中 `%s` 个数 | 0–5(分布与 CHS/ENG 逐条一致) |
| `desc` `%s` 个数 > `getDescriptionParams` 覆盖的 index | **0** |
| 5 个 `%s` 的规则 | `danger_zone`(`UNGP_DangerZone.java:50-58` 覆盖 0..4)、`stay_calm`(`UNGP_StayCalm.java:102-109` 覆盖 0..4) |

`getDescriptionParams` 默认实现返回 `null`(`src/ungp/api/rules/UNGP_BaseRuleEffect.java:78-83`),而 `UNGP_RulesManager.java:469-475` 固定构造 `String[10]` 并逐 index 取值,`addDesc` 在 `:433-442` 把该数组交给 `tooltip.addPara(prefix + ruleInfo.getDesc(), pad, highlightColor, values)`。因此**多给的参数无害**,但**少给/给 null 会把字面 `null` 渲染进 tooltip**。

### 6.2 静默失败点(真实存在,P2)

| 位置 | 行为 |
|---|---|
| `src/ungp/scripts/utils/SimpleI18n.java:33-41` | `format()` 用 `try/catch (Exception)` 包住 `String.format`,**任何异常都 `return null`** |
| `src/ungp/scripts/utils/SimpleI18n.java:23-25` | `I18nSection.get()` 直接调 `Global.getSettings().getString(category, keyPrefix+key)`,**不捕获** |
| `UNGP_RulesManager.java:433-442` | `addPara(null, ...)` 的行为取决于游戏 UI 实现,存在 NPE 风险 |

即:若 `desc` 的 `%s` 数量与实参数组长度不符 → `IllegalFormatException` → `format()` 返回 `null` → tooltip 可能 NPE 或渲染异常。当前 86 条规则实测 **0 处不匹配**,所以这是"埋着的坑"而非现存故障。

缺 key 的情况反而安全:反编译 `com/fs/starfarer/loading/StarfarerStrings.String(String,String)` 在缺 key 时返回字面 `"Missing string: [<key>]"`,**不崩溃**,但会在 UI 上露出来。

### 6.3 其它解析端的静默默认值(P2,并与 P1-1 直接相关)

| 位置 | 静默行为 |
|---|---|
| `src/ungp/scripts/campaign/specialist/challenges/UNGP_ChallengeLoader.java:39` | `optInt("dpToDefeat", -1)`:缺列 → -1,无告警 |
| `UNGP_ChallengeLoader.java:40-42` | `optBoolean`/`optString` 同样静默默认 |
| `src/ungp/scripts/campaign/background/UNGP_BackgroundManager.java:57-62` | `effectPlugin` 为空 → 静默回落 `new UNGP_Nothing()` |
| `UNGP_BackgroundManager.java:64` | `spritePath` 为空 → 回落 `graphics/portraits/portrait_generic.png` |
| `UNGP_RuleInfoLoader.java:148` | `spritePath` 为空 → 回落 `graphics/ui/icons/64x_xcircle.png` |

这些回落本身是合理的防御式设计,但**列名拼错/列缺失**会与"值确实为空"走同一条静默路径,正是 P1-1 能潜伏至今的原因。

---

## 7. Q6:未提交改动新增规则的齐全性

### 7.1 数量口径:实测 **6 条**(任务描述写"7 条")

`git diff` 显示 `data/campaign/UNGP_rules.csv` 与 `UNGP_rules_ENG.csv` 各新增 **6 行**,无删除、无修改行。新增 id 为:`bounty_hunter`、`human_nature_test`、`i_have_activated`、`keep_the_way_clear`、`slippery_modification`、`vengeful_spirit`。按 7 条核对会"多找一条",本报告以 6 条为准。

### 7.2 逐条齐全性核对(全部通过)

| id | cost | isBonus | isGolden | tags | desc %s | 覆盖 index | extra1/2 | ENG 行 | 图标 |
|---|---|---|---|---|---|---|---|---|---|
| `bounty_hunter` | -2 | TRUE | (空) | — | 2 | 0,1(`UNGP_BountyHunter.java:38-41`) | 空/空 | ✓ | ✓ 5026B 64×64 |
| `human_nature_test` | 2 | FALSE | (空) | — | 3 | 0,1,2(`UNGP_HumanNatureTest.java:49-52`) | 有值/有值 | ✓ | ✓ 4646B 64×64 |
| `i_have_activated` | -3 | TRUE | (空) | — | 4 | 0,1,2,3(`UNGP_IHaveActivated.java:91-95`) | 有值/有值 | ✓ | ✓ 4588B 64×64 |
| `keep_the_way_clear` | -3 | TRUE | (空) | — | 0 | 无需覆盖 | 空/空 | ✓ | ✓ 3548B 64×64 |
| `slippery_modification` | 2 | FALSE | (空) | — | 1 | 0(`UNGP_SlipperyModification.java:135-141`) | 标题/行格式 | ✓ | ✓ 5672B 64×64 |
| `vengeful_spirit` | 1 | FALSE | (空) | — | 0 | 无需覆盖 | 空/空 | ✓ | ✓ 4226B 64×64 |

补充核对:

- **CSV 行**:6 条在 CHS/ENG 均为 14 列,id 逐行对齐,`%s` 个数逐条相等(3=3、1=1、4=4、0=0、0=0、2=2)。
- **ENG 行**:ENG 侧 6 行的 `name/short/desc/extra1/extra2` 全部为英文,无 CJK 残留。
- **实现类**:6 个 `effectPlugin` 全限定名均在 86 个实现类集合内,路径与 CSV 一致(`ungp.impl.rules.other.UNGP_BountyHunter`、`...fleet.UNGP_HumanNatureTest`、`...combat.UNGP_IHaveActivated`、`...fleet.UNGP_KeepTheWayClear`、`...member.UNGP_SlipperyModification`、`...combat.UNGP_VengefulSpirit`)。
- **图标**:6 个 png 均存在,PNG 魔数 `89 50 4E 47 0D 0A 1A 0A` 合法,均为 64×64,体积 3548–5672 字节,与既有 `ungp_rules/` 图标规格一致。
- **strings.json**:6 条规则的文本全部内联在 CSV,不需要新增字符串键;本轮唯一新增键见 §4.4。
- **cost 约定**:3 条 cost 为负(-2/-3/-3)且 `isBonus=TRUE`,3 条 cost 为正且 `isBonus=FALSE`,与全表约定("负 cost = 奖励规则")一致。

参数语义抽查(可与实现常量对齐):

- `i_have_activated`:`25%` 减伤、`CHARGE_TIME=4` 秒充能、`10%` 增伤、`BUFF_DURATION=30` 秒,对应 `UNGP_IHaveActivated.java:91-95`。
- `human_nature_test`:宽限期 GAMMA/BETA/ALPHA/OMEGA = 60/40/30/20 天、`CREW_LOSS_PERCENT=1`、`DRUG_CONSUMPTION_PERCENT=10`,对应 `UNGP_HumanNatureTest.java:35-44` 与 `:49-52`。

### 7.3 未提交改动的残留物

- `git status` 中有两条 `AD` 记录:`graphics/ungp/icons/ungp_rules/keep_the_way_clear_green.png`、`tools/icon_tint/i_have_activated.png` —— 已加入索引但在工作区被删除。当前磁盘不存在、无任何引用,**不会影响运行**,但建议提交前清掉索引记录。
- `tools/icon_tint/`(`icon_tint.py`、`图标着色.bat`、`README.md`、`example.png`)是本轮新增的临时工具目录,不属于发行内容(见 P3-6)。

---

## 8. 问题清单(P0–P3)

### P0(存档损坏 / 崩溃 / 数据错误)—— 0 项

未发现会导致崩溃或存档损坏的问题。所有 CSV 加载路径都是 fail-loud(`UNGP_RuleInfoLoader.java:160`、`UNGP_BackgroundManager.java:78-81`),JSON 全部能被引擎宽松解析(实测见附录 A.4)。

### P1 —— 1 项

**P1-1 `UNGP_challenges_ENG.csv` 缺 `dpToDefeat` 列,导致英文构建中 `7_kan` 挑战被静默丢弃**

- 位置:`data/campaign/UNGP_challenges_ENG.csv:1`(表头 8 列,CHS 为 9 列);受影响数据行 `:6`(`7_kan`,9 个值按 8 列表头错位)
- 证据链:`UNGP_ChallengeLoader.java:39` → `UNGP_ChallengeInfo.java:44-50` → `UNGP_ChallengeLoader.java:44-45`
- 影响:英文部署时 `7_kan` 挑战整条消失(不报错);`5000 DP` 完成条件丢失;`milestoneToUnlock` 变成非法值 `"FALSE"`
- 修复方向:给 ENG 表头补 `dpToDefeat`,并把 `7_kan` 行的 `5000` 放回该列(与 CHS 表头同构);同时建议 `UNGP_ChallengeLoader` 增加"列缺失即 log warn"的保护
- 备注:只在走 `swapLangFile-切换语言.py` 切到英文后触发,CHS 玩家无感 —— 但会让英文分发的挑战内容缺失

### P2 —— 5 项

**P2-1 语言切换完全依赖脚本改名,代码零分支**
`src/` 中 `_ENG` / `language` / `isEnglish` 相关分支命中 **0**;7 对可切换文件(rules / backgrounds / challenges / LunaSettings / strings / tag_data / mod_info)全靠仓库根目录 `swapLangFile-切换语言.py` 用 `os.walk` 全仓把 `xxx.ext` 与 `xxx_ENG.ext` 互换文件名(第 11-30 行,**无白名单**,会走进 `build/`、`.git/`)。原版 `starsector-core` 下 `_ENG.*` 文件数为 0,`localization_version.json` 显示本机为 zh-Hans 汉化包 → 引擎不会自动选 `_ENG`,确认只能靠脚本。风险:误改名会同时破坏构建缓存与 git 索引。

**P2-2 部分数据缺 `_ENG` 对,中英只有一侧**
没有 `abilities_ENG.csv`、`skill_data_ENG.csv`、`UNGP_dataSavers_ENG.csv`。其中 `data/campaign/abilities.csv` 的 `name` 是英文 "Check Savepoint"、`desc` 为空,而原版 `abilities.csv` 的 `name/desc` 是中文 → **中文环境下本 mod 唯一能力显示英文名**;`data/characters/skills/skill_data.csv` 两行 `name` 均为 "UNGP Modifier"(英文,无 `_ENG`)。

**P2-3 `SimpleI18n.format()` 吞异常返回 null**
`src/ungp/scripts/utils/SimpleI18n.java:33-41`:`catch (Exception)` → `return null`。任何 `%s`/实参不匹配都会把 `null` 传给 `TooltipMakerAPI.addPara`,存在 NPE 风险且完全无日志。建议至少 `log.error` 一次再降级为原文。

**P2-4 项目格式检查当前必然失败(`gradle verifyFormat`)**
`tools/formatter/to_lf.py --check` 报 30+ 文件不通过,其中 `data/` 下 21 个是 CRLF:`abilities.csv`、`UNGP_backgrounds.csv(_ENG)`、`UNGP_challenges.csv(_ENG)`、`UNGP_dataSavers.csv`、`LunaSettings.csv(_ENG)`、`settings.json`、`sounds.json`、`tag_data.json(_ENG)`、`strings.json(_ENG)`、2 个 `.skill`;`rules.csv(_ENG)` 为 LF;`skill_data.csv`、`version_files.csv`、`challenges` 两份、`LunaSettings` 两份**末尾缺换行**(`LunaSettings` 还以 `。,,,` 结尾)。`utf8_no_bom.py --check` → BOM 0 处。`tools/formatter/common.py:5` 的 `SKIP_DIRS` 不含 `data/`,且 `:73-80` 遍历全仓 → `build.gradle:168-172` 的 `verifyFormat`(被 `:174-176` 挂到 `check`)当前一定失败。注:原版 `rules.csv`/`tag_data.json`/`settings.json` 也是 CRLF,**CRLF 本身对游戏无影响**,这是纯规范问题。

**P2-5 可选列缺失全部走静默默认值**
见 §6.3。缺列/拼错列名与"值确实为空"共用同一条降级路径,是 P1-1 得以潜伏的结构性原因。

### P3 —— 8 项

| 编号 | 位置 | 问题 |
|---|---|---|
| P3-1 | `data/strings/strings.json:6,10,51-53,55-56,65,67-68` 及 `_ENG` 对应行 | 10 个疑似死键(§4.3),`src/` 0 处真实引用 |
| P3-2 | `data/strings/strings_ENG.json` | ENG 多一个 `UNGP.inGameData` 键,CHS 无,疑似孤儿键 |
| P3-3 | `data/config/tag_data.json` / `_ENG` | 严格 JSON 解析失败(`Missing value at 4 [character 1 line 2]`,第 2 行是 `# only applies to intel tags` 注释);末尾还有 3 个多余空行。引擎走宽松解析可正常加载,但 `jq`/`python json` 会报错 |
| P3-4 | `data/strings/strings.json:79`、`data/strings/strings_ENG.json:80`、`mod_info.json`、`data/config/settings.json`、`LunaSettingsConfig.json` | 尾逗号。实测游戏自带 `starsector-core/json.jar`(2010 版 org.json)`new JSONObject("{a:1,}")` **容忍尾逗号**,运行无碍;仅严格工具链会报错 |
| P3-5 | `data/campaign/UNGP_rules.csv` / `_ENG` | 字段值尾随空格 15 处 / 11 处(共 26 处),如 `blocked_flux_conduits.desc`、`glados.desc` 的 ` - %s ` / ` +  %s `;会被原样渲染进 tooltip(多出空格) |
| P3-6 | `data/campaign/CSV_id_order.py` | 开发脚本混入 `data/`,且 `build.gradle:62-73` 的 `distZip` `include "data/**"` 会把它打进发行包;该脚本第 1 行 `from numpy.core.numeric import NaN` 在 numpy≥2(本机 2.3.5,`numpy.core` 已移除)下直接 `ImportError` |
| P3-7 | `data/campaign/UNGP_rules.csv` | 3 条规则 `cost=0` 且 `isBonus=TRUE`(`lobster_perday`、`birthday_present`、`entropius_mind`)。`UNGP_RulesManager.java:690` 用 `cost > 0` 判断是否消耗,`cost=0` 时 `isBonus` 的符号语义不参与判定,数据无害但语义上是"免费 milestone 规则",建议注释说明 |
| P3-8 | `src/ungp/api/rules/tags/UNGP_TweakBeforeApplyCombatTag.java` | 本 mod 内 0 实现,仅被 `UNGP_SpecialistCombatPlugin.java:61-62` 作为扩展点 `instanceof` 调用;保留合理,建议在接口 javadoc 标明"仅供第三方扩展" |

---

## 9. 附录 A:复现脚本

以下脚本均**未写入仓库**(位于系统临时目录)。运行环境:

- 工作目录:`D:\StarsectorDevelopment\098\Starsector\mods\UnofficialNewGamePlus`
- Python:`C:/Users/55381/.dsh/dsh-runtimes/dsh-primary-runtime/dependencies/python/python.exe`
- JDK(JSON 实测用):`C:/Users/55381/.jdks/azul-17.0.20`
- shell:git-bash(`TEMP=/tmp`)

### A.1 双语对逐列比对(`q1_csv_pair.py` 核心)

```python
import csv, os

BASE = r"data"
PAIRS = [
    ("campaign/UNGP_rules.csv",       "campaign/UNGP_rules_ENG.csv"),
    ("campaign/UNGP_backgrounds.csv", "campaign/UNGP_backgrounds_ENG.csv"),
    ("campaign/UNGP_challenges.csv",  "campaign/UNGP_challenges_ENG.csv"),
    ("config/LunaSettings.csv",       "config/LunaSettings_ENG.csv"),
]

def rows(p):
    with open(os.path.join(BASE, p), encoding="utf-8-sig", newline="") as f:
        return list(csv.reader(f))

def ph(s):                      # stats printf placeholders
    return s.count("%s") + s.count("%d") + s.count("%f")

for chs, eng in PAIRS:
    a, b = rows(chs), rows(eng)
    print(chs, "rows", len(a), len(b), "cols", {len(r) for r in a}, {len(r) for r in b})
    print("  header eq:", a[0] == b[0])
    ids_a = [r[0] for r in a if r and r[0].strip()]
    ids_b = [r[0] for r in b if r and r[0].strip()]
    print("  id list eq:", ids_a == ids_b, "n =", len(ids_a))
    diffs = []
    for i, (ra, rb) in enumerate(zip(a, b), 1):
        if len(ra) != len(rb):
            diffs.append((i, "colcount", len(ra), len(rb)))
            continue
        for j, (ca, cb) in enumerate(zip(ra, rb)):
            if ph(ca) != ph(cb):
                diffs.append((i, "ph@col%d" % j, ph(ca), ph(cb)))
    print("  placeholder diffs:", len(diffs), diffs[:10])
```

### A.2 effectPlugin ↔ 实现类双向差集

```python
import csv, os
ROOT = r"D:\StarsectorDevelopment\098\Starsector\mods\UnofficialNewGamePlus"
rs = [r for r in csv.DictReader(open(os.path.join(ROOT, "data/campaign/UNGP_rules_ENG.csv"),
                                    encoding="utf-8-sig", newline="")) if r.get("id")]
fq = {r["effectPlugin"].strip() for r in rs}
missing = [f for f in fq
           if not os.path.exists(os.path.join(ROOT, "src", f.replace(".", os.sep) + ".java"))]
impls = []
for dp, _, names in os.walk(os.path.join(ROOT, "src", "ungp", "impl", "rules")):
    impls += [os.path.join(dp, n) for n in names if n.endswith(".java")]
fqset = {}
for p in impls:
    rel = os.path.relpath(p, os.path.join(ROOT, "src"))[:-5].replace(os.sep, ".")
    fqset[rel] = p
orphans = [k for k in fqset if k not in fq]
print("rules", len(rs), "distinct plugins", len(fq), "impls", len(impls))
print("missing classes", len(missing), missing)
print("orphan classes", len(orphans), orphans)
```

实测输出(逐字):

```
rules 86 effectPlugin missing 0 []
impl java 86 orphans 0 []
```

### A.3 spritePath 存在性 + PNG 规格

```python
import csv, os, struct
ROOT = r"D:\StarsectorDevelopment\098\Starsector\mods\UnofficialNewGamePlus"
rs = [r for r in csv.DictReader(open(os.path.join(ROOT, "data/campaign/UNGP_rules.csv"),
                                    encoding="utf-8-sig", newline="")) if r.get("id")]
for r in rs:
    p = os.path.join(ROOT, r["spritePath"].replace("/", os.sep))
    if not os.path.exists(p):
        print("MISSING", r["id"], r["spritePath"])
    else:
        d = open(p, "rb").read()
        w, h = struct.unpack(">II", d[16:24])
        print(r["id"], os.path.getsize(p), f"{w}x{h}", d[:8] == b"\x89PNG\r\n\x1a\n")
```

实测:86 条中 85 条命中仓库内文件,唯一 MISSING 为 `lobster_perday -> graphics/icons/cargo/lobster.png`(原版存在);6 条新增规则图标均 64x64。

### A.4 JSON 严格性实测(`JsonProbe.java`,用游戏自带 json.jar)

```java
import org.json.JSONObject;
import java.nio.file.*;

public class JsonProbe {
    public static void main(String[] args) throws Exception {
        for (String f : args) {
            try {
                new JSONObject(new String(Files.readAllBytes(Paths.get(f)), "UTF-8"));
                System.out.println("OK   " + f);
            } catch (Exception e) {
                System.out.println("FAIL " + f + " -> " + e.getMessage());
            }
        }
    }
}
```

```bash
javac -cp starsector-core/json.jar JsonProbe.java
java  -cp "starsector-core/json.jar;." JsonProbe \
  data/strings/strings.json data/strings/strings_ENG.json \
  data/config/tag_data.json data/config/tag_data_ENG.json \
  data/config/settings.json data/config/sounds.json \
  data/config/LunaSettingsConfig.json mod_info.json
```

实测输出要点:`tag_data.json` / `tag_data_ENG.json` → `Missing value at 4 [character 1 line 2]`(第 2 行 `# only applies to intel tags`);其余全部 `OK`。

### A.5 格式检查(只读)

```bash
python tools/formatter/to_lf.py --check
python tools/formatter/utf8_no_bom.py --check
wc -l data/campaign/UNGP_rules.csv data/campaign/UNGP_rules_ENG.csv
```

### A.6 其它统计命令

```bash
# 图标总数
find graphics/ungp -name '*.png' | wc -l
# 按接口统计实现数
grep -rn "implements .*UNGP_CombatTag" src/ungp/impl/rules | wc -l
# 动态字符串键
grep -rn 'descKey\|getI18nKey()' src
```

---

## 10. 附录 B:审计边界与未核实项

1. **运行期行为**未实机验证:所有"会不会崩/会不会显示"的判断都基于源码与反编译 API(`StarfarerStrings.String`、`org.json` 行为),未启动游戏实测。P1-1 的最终表现(挑战消失)由 `isValid` 逻辑推定,证据链完整,建议实机确认一次。
2. `getMergedSpreadsheetDataForMod` 的列缺失行为:依据是 `optInt(key, default)` 的常规语义 + 原版用例,未单步调试引擎内部。
3. `#` 注释行被电子表格加载器跳过:以原版 `starsector-core/data/campaign/rules.csv` 887 行 `#` 行与本 mod 现状不崩为依据推断,未读引擎实现源码。
4. 引擎侧 `_ENG` 选择策略:以 `starsector-core` 下 0 个 `_ENG.*` 文件与 `localization_version.json` 为 zh-Hans 为依据,未读 `SettingsAPI` 实现。
5. 本文档为只读审计,**未修改 `src/`、`data/`、`build.gradle`、`mod_info.json` 任何文件**,也未执行任何 git 写操作。
