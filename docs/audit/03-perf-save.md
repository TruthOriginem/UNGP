# 03 存档兼容、监听器生命周期与性能审计

- 审计对象: UnofficialNewGamePlus 2.6.0(mod_info.json 的 version;HEAD = 08482f7 "2.6.0 update",2026-11-03)
- 范围: 只读。未修改 src/、data/、build.gradle、mod_info.json;未构建、未运行游戏。
- 方法: 逐行精读 + 全项目 grep + data/campaign/UNGP_rules.csv 统计 + 对 starsector-core/xstream-1.4.10.jar 做常量池/字节码核对(用于确认别名与字段解析的真实语义,避免凭印象下结论)。
- 级别: P0=存档损坏/崩溃/数据错误;P1=功能正确性或明显性能问题;P2=可维护性/扩展性;P3=风格与锦上添花。

---

## 0. 结论摘要

### P0

| # | 位置 | 问题 |
|---|------|------|
| P0-1 | src/ungp/scripts/UNGP_modPlugin.java:77-104 + src/ungp/scripts/campaign/UNGP_SharedData.java:13,50-56 | UNGP_SharedData 被写进 persistentData,@但 configureXStream 未给它注册 alias@,存档里以 FQCN 元素名保存。该类承载 10+ 条规则的运行时数据,且历史上已经历过一次包名迁移(data.scripts → ungp.scripts),迁移即意味着静默丢数据。 |

### P1

| # | 位置 | 问题 |
|---|------|------|
| P1-1 | src/ungp/scripts/plugins/UNGP_SpecialistCombatPlugin.java:35-37,39-86,90-91 | init() 从不重置 tags/bonusMessages/notBonusMessages/init/isSpecialistMode/isRulesBlocked。该插件由 data/config/settings.json:3 声明为全局插件;若引擎在同一会话的多场战斗间复用同一实例,第二场起战斗 tag 会重复叠加(每帧重复调用 apply*)、开场提示重复。 |
| P1-2 | src/ungp/impl/rules/member/UNGP_SlipperyModification.java:290-294 | cleanUp() 只清两个数据槽,不回收已施加的 D-插件/内置 D 插件;玩家重选规则后舰船的 DMod 永久残留(数据错误),triggered 集合也只增不减。 |
| P1-3 | src/ungp/impl/rules/other/UNGP_BountyHunter.java:58-70(工作区新增,git status A,未发布) | 把 WeakHashMap<BaseIntelPlugin,Integer>(游戏 intel 对象)经 UNGP_SharedData 写进 persistentData;并把 4 次 intelManager 查询放进每帧路径。发布前应改为存稳定 id。 |
| P1-4 | src/ungp/scripts/campaign/UNGP_InGameData.java:97-102,111-119 + UNGP_RulesManager.java:122-143 | 规则 id 从 CSV 下线后:旧 id 被永久丢弃(saveActivatedRules 只回写本次解析到的 id),且该规则的 effect 已不在 ALL_RULES 中 → updateRulesCache 的全量 unapply 循环覆盖不到它,已生效效果无法回退。 |
| P1-5 | src/ungp/scripts/campaign/specialist/economy/UNGP_EconomyListener.java:31-44,52-54 | economyUpdated() 每次经济更新全量重算所有市场 × 所有经济 tag(每市场 3 次 tag 调用)。市场交互频繁的存档里是明显的 CPU 尖峰。 |
| P1-6 | src/ungp/scripts/campaign/UNGP_InGameData.java:27-29,68-71,74-76,86-87,114 | XStream 反序列化不执行构造器,缺失属性 = null/0(不是类声明处的初值)。三个字段(activatedRuleIDs/completedChallenges/changeTimeStat)被直接解引用,任意一个缺失即读档 NPE。后果等级 P0,当前存量存档尚未出现该组合。 |
| P1-7 | src/ungp/scripts/campaign/specialist/intel/UNGP_SpecialistBackgroundUI.java:186-201 | advance() 每帧调用 Global.getSoundPlayer().playUILoop(...) 与 getCampaignUI().suppressMusic(0.8f)。不是"每帧一次的状态更新"而是每帧一次的音频调用。 |
| P1-8 | src/ungp/scripts/campaign/specialist/rules/UNGP_RulesManager.java:103-220 | updateRulesCache() 无 try/finally:中途抛异常时第 219 行 needUpdateCache=false 不可达 → CampaignPlugin.advance 的 89-91 每帧重试、每帧再抛,形成日志/卡顿雪崩,且静态缓存停留在半清理半应用状态。 |
| P1-9 | src/ungp/scripts/plugins/UNGP_SpecialistCombatPlugin.java:116-128 | 战斗逐帧 ships × tags 双循环;33 个战斗 tag 全激活、40 艘舰船时 = 每帧 1320 次虚调用(60fps → 7.9 万次/秒)。 |

### P2(择要)
- UNGP_UITimeScript 的 addInterval/removeInterval 无任何调用者 → RuleDescBackgroundPlugin:39 取到的动画因子恒为 0,规则描述背景的闪烁动画是静止的,整套 UITimeScript 目前是纯死重。
- UNGP_CampaignPlugin:43 oneMonthChecker 是唯一"有字段但没 aliasAttribute"的持久字段(modPlugin:82-87 未覆盖),老档读到 0 → 读档首帧必然误判"月已过"。
- UNGP_SpecialistWidgetPlugin:192-211 悬停时每帧 2 次 new ArrayList + 2 次 Collections.sort + 每图标一次 getSprite。
- modPlugin:54,55,58 三处 addTransientScript/addTransientListener 无存在性检查(52 行 addScript 有)。
- ChallengeManager:182 static TEMPORARY_COMPLETED_CHALLENGES 从不在 onGameLoad 重置,且其 setter 目前无调用者(半成品)。
- UNGPFont:19 无界静态 Drawable 缓存 + clearDynamicDrawable 无调用者(死代码)。
- UNGP_InteractionDialog:433-435 把同一个 pickTrigger 同时作为 onConfirm/onCancel 传入,确认路径会执行两次回调(见 4.2)。

---

## 1. Q1 存档兼容

### 1.1 被 alias 的类型与实际字段逐项比对

configureXStream(src/ungp/scripts/UNGP_modPlugin.java:77-104)共为 5 个类注册了别名:

| 类 | 别名 | aliasAttribute | 实际持久字段 | 覆盖率 |
|---|---|---|---|---|
| UNGP_CampaignPlugin(modPlugin:81) | ungp_camplugin | ic/odc/oyc/ngcd/ngc/ssd(82-87) | inGameData(38)、inheritChecker(40)、oneDayChecker(41)、oneYearChecker(42)、@偏 oneMonthChecker(43)@、newGameCheckDays(44)、newGameChecked(45)、shouldShowDialog(46) | 6/8 有属性别名;oneMonthChecker 无别名,inGameData 无别名(其类型已 alias,元素名仍正确) |
| UNGP_InGameData(modPlugin:92) | ungp_igd | cC/d/pIT/iR/ied/iHM/tTCSM/cTS/aRIDs/cChs/bCRNB(93-103) | curCycle(20)、difficulty(21)、isRecorded(22)、inherited(23)、passedInheritTime(24)、isHardMode(25)、timesToChangeSpecialistMode(26)、changeTimeStat(27)、activatedRuleIDs(28)、completedChallenges(29)、blockCombatRulesInNextBattle(31) | 11/11 全覆盖 |
| UNGP_PlayerFleetMemberBuff(modPlugin:89) | ungp_pfmb | 无 | id(11)、dur(12)、tag(10,transient 不入档) | 无需别名(无改名) |
| UNGP_UITimeScript(modPlugin:90) | ungp_ut | 无 | intervalMap(16,HashMap) | 无需别名 |
| Military/UnderworldCustomBounty(modPlugin:79-80,106-110) | 旧名 + 现名双注册 | 无 | — | 见 1.2 |

结论:
- 有字段但没 aliasAttribute 的只有 UNGP_CampaignPlugin.oneMonthChecker(:43)。它不是错误,但它暴露了真实风险类型:新增持久字段时开发者忘记同步属性别名,而字段名就是序列化名,一旦以后重命名该字段就静默失配。
- UNGP_InGameData 的 11 个字段全部有别名,说明"加字段时同步加别名"在本项目里是有纪律的;真正的缺口不在别名覆盖率,而在 1.6.0 之前没有别名的历史段(见 1.3)与根本没注册别名的类(见 1.6)。

### 1.2 僵尸别名判断:没有僵尸,但旧名语义需要说清

- modPlugin:79-80 + 106-110 的 registerLegacyBountyAlias 是正向兼容:先取当前序列化名,再同时注册旧名与现名。旧名(ungp_militaryBounty/ungp_underworldBounty)不是"删掉的字段留下的僵尸",而是"供更早存档读取"的入口。
- 我从 XStream 1.4.10 字节码确认了二次 alias 的安全性,而不是靠猜:
  - com/thoughtworks/xstream/mapper/ClassAliasingMapper.class 的 addClassAlias(String,Class)(31 字节)对 classToName 与 nameToType 两个 Map 各 put 一次 → 同一 Class 注册第二个名字后,旧名仍能反序列化,序列化使用最后注册的名字。所以先 alias 旧名再 alias 现名这个顺序是正确的。
  - com/thoughtworks/xstream/mapper/FieldAliasingMapper.class 的 realMember(Class,String) 在别名表未命中时回退调用 MapperWrapper.realMember(serializedMember 对称)→ 后期才加 aliasAttribute 的字段,老存档里以真字段名写的属性依然能被解析。
- 由此可知 1.6.0(commit 873a02c,2022-02-01,首次引入 ungp_igd 与 d/cC 等属性别名)之后的存档在别名层是自洽的:老属性名能读,新属性名也能读。

### 1.3 真正的历史断点(旧档读不进来)

git 版本史给出的时间线(可核实:commit 4f38210 的 src/data/scripts/campaign/UNGP_InGameData.java vs 现在的 src/ungp/scripts/campaign/UNGP_InGameData.java:19-31):

| 版本 | 类元素名 | 字段形态 |
|---|---|---|
| 1.0-1.5 | FQCN data.scripts.campaign.UNGP_InGameData(无 alias) | int difficultyLevel=0;无 changeTimeStat、无 completedChallenges;timesToChangeSpecialistMode 是 int |
| 1.6.0 起 | ungp_igd | difficulty 变 enum Difficulty;新增 cTS/cChs;timesToChangeSpecialistMode 变 float |
| 2.4/2.5 起 | ungp_igd(类搬到 ungp.scripts 包) | 同 1.6.0 |

断点结论:
1. 1.0-1.5 存档的类名是旧包名,当前 jar 里没有该 FQCN → 游戏侧无法解析。后果不是抛异常,而是被忽略:starfarer-core 的 com/fs/starfarer/campaign/save/CampaignGameManager.class 常量池里同时出现 ignoreUnknownElements 与 configureXStream,说明读档前调用了 x.ignoreUnknownElements()。所以旧档表现为"UNGP 数据凭空消失/被重建",而不是"读档报错"。
2. 该段历史与项目自身策略一致:changelog.txt:97-98/108(2.0.0,2022-06-07)"本次更新需要新存档,请用旧版重生记录重生点后再更新";changelog.txt:333-334/347(1.2.0)"本次更新存档不兼容,请先继承后再更新"。作为设计取舍可以接受,但代码侧没有任何版本标记或提示(见 1.5)。

### 1.4 Difficulty 枚举与规则 id 下线

- Difficulty 枚举自 1.6.0 起四个常量的字段值未变(src/ungp/scripts/campaign/specialist/UNGP_SpecialistSettings.java:15-18:GAMMA legacyLevel=1/min4/max8、BETA 10/8/20、ALPHA 20/12/42、OMEGA 30/20/64)。1.6.0 之后存档里存的 d 属性是枚举名(GAMMA 等),重排常量顺序在 XStream 下是安全的(它按名字解析,不按 ordinal),但改常量名会静默失配;Difficulty.convertLegacyLevelToDifficulty(UNGP_SpecialistSettings.java:72-84)只服务旧的 int 接口(UNGP_BaseRuleEffect.java:57-60 那个已废弃的空实现),不再参与存档解析。
- 一个可量化的上限:最多同时激活 OMEGA 的 64 条规则(:18),data/campaign/UNGP_rules.csv 共 102 行、其中 16 行为空行,即 86 条真实规则。下面所有"86 / 64"的估算都基于这两个数。
- 规则 id 下线后的行为(具体断点):
  - UNGP_InGameData.getActivatedRules():111-119 只返回能在 ALL_RULES 里找到的规则 → 下线 id 被静默跳过,不报错也不提示。
  - saveActivatedRules():85-104 先 oldRules = new ArrayList<>(activatedRuleIDs) 再 activatedRuleIDs.clear(),最后只 addAll(ruleSet)(86-87、103)→ 旧 id 一旦不进本次 newRules 就从此消失。
  - 97-102 对剩余旧 id 调用 URule.getByID(ruleId)(UNGP_RulesManager.java:718-725,线性扫描 ALL_RULES)并 cleanUp();id 下线时返回 null,if (rule != null) 直接跳过 → 该规则的效果不会 cleanUp。
  - updateRulesCache():122-143 的"全量 unapply"循环只遍历 ALL_RULES,已删除的规则没有 effect 实例可 unapply → 它在本次 JVM 会话中留下的全局修改(舰队 stats、listener、经济市场加成)无法被撤销。
  - 判定:P1-4。规则下线 = 存档数据永久丢该规则 + 残留效果无法回退;两者都不会崩,但属"数据错误"范畴。修复方向:读档时对比 CSV 已知 id 集合,把无法解析的 id 记日志并保留在存档里(不要 clear 掉),或提供一次性迁移表。

### 1.5 老存档缺字段时的默认值:不安全(XStream 语义)

这一条是本报告最重要的机制性结论,因为它决定了"以后还能不能安全加字段":

- XStream 反序列化不调用构造器 UNGP_InGameData()(:33-34),字段值来自流里的属性;属性缺失 = JVM 默认值(null / 0 / false),而不是源码里写的初值。
- 因此 :27-29 声明的三份"一定有初值"的容器/对象,在读档后可能是 null,而后续代码全部直接解引用:
  - activatedRuleIDs(:28) → :114 activatedRuleIDs.contains(...)、:87 activatedRuleIDs.clear()
  - completedChallenges(:29) → :75 completedChallenges.add(id)、:76 传参
  - changeTimeStat(:27) → :69 changeTimeStat.getModifiedValue()
- 目前这三个属性自 1.6.0 起就存在,存量存档都不会缺,所以没有可复现路径;但一旦某个属性的序列化名变化(改名而没补 aliasAttribute)、或将来给"读档会走到的类"再加集合字段并直接解引用,后果就是读档 NPE(P0 级后果)。
- 同一个机制解释了 P2 的 oneMonthChecker:新增字段在旧档里是 0,而 CampaignPlugin.advance():153-156 用 currentMonth != oneMonthChecker 判月变化 → 读档首帧必判"月已过",给 StillGrowingUp(:43-51) 白送一个月进度。属于"默认值无害但不是正确的初始值"的典型。
- 修复方向:凡是"读档会用到且不能为 null"的字段,二选一 ——
  1. 加 private Object readResolve()(或实现 Serializable 的 XStream 等价物)做兜底初始化;
  2. 在 visitor / resolve 之后集中校验,缺字段时补默认值并打日志。
  最小改动是把 :27-29 的访问点改成 null 兜底,但要连 :123 的 addTimesToChangeSpecialistMode 等调用链一起看。

### 1.6 P0-1:UNGP_SharedData 没有 alias

- 事实:src/ungp/scripts/campaign/UNGP_SharedData.java:13 是普通 final 类,实例在 :50-56 被 put 进 Global.getSector().getPersistentData(),SHARED_DATA_KEY="UNGP_sharedData"。而 UNGP_modPlugin.java:77-104 只 alias 了 4 个类 + 2 个 bounty 兼容名,没有 UNGP_SharedData。
- 落盘形式:persistentData 的 value 由 XStream 按真实类名写元素 → 元素名是 ungp.scripts.campaign.UNGP_SharedData。对比 UNGP_InGameData 有 ungp_igd,说明这不是风格差异,而是遗漏。
- 影响面(ruleDataMap 的实际使用者,grep 84 处命中,经 api/rules/UNGP_BaseRuleEffect.java:208-241 的 getDataInCampaign/saveDataInCampaign 封装):
  TechRevolution(Long)、StillGrowingUp(int[], Set<String>)、BirthdayPresent(int[])、EntropiusMind(Integer)、MonotonousTeller(Integer)、BustlingFungus(Object/float)、BloodLust(Integer)、HumanNatureTest(Integer)、GLaDos(Boolean)、SlipperyModification(Set<String>)、MakeAnException(List<HegemonyInspectionIntel>)、BountyHunter(WeakHashMap<BaseIntelPlugin,Integer>)。
- 后果:类名 = 存档键。任何一次包名/类名调整、甚至 IDE 重构移动文件,都会让 10+ 条规则的进度数据静默消失(用户看到的是"规则效果突然从头开始"),且无日志、无检测。
- 历史证据:该类由 commit a4664ba(2023-05-07,"2.2.0 compatible with 096")引入;它的引入早于 data.scripts→ungp.scripts 的包名迁移,也就是这条路径已经踩过一次(changelog.txt:6-19 的 2.5.0 只写"代码结构进行了部分更改",没有提到规则数据丢失)。2.2.0 及更早存档里该类的 FQCN 与今天不同 → 那批数据在当前版本必然读不回来。
- 修复方向(成本极低):
```java
// UNGP_modPlugin.configureXStream
x.alias("ungp_shared_data", UNGP_SharedData.class);
// 兼容 2.5.0 及更早:旧 FQCN 仍可读(alias 名带点需实测一次读档)
x.alias("ungp.scripts.campaign.UNGP_SharedData", UNGP_SharedData.class);
```
- 同类检查项:除本类外,persistentData 里还写入了 "UNGP_cam"(CampaignPlugin:32,69)与 "UNGP_challenge_start"(ChallengeManager:58-77 区间),key 是字符串常量不算风险,但 value 类型 UNGP_CampaignPlugin 已 alias。

### 1.7 缺属性的具体崩溃链

之所以给 P1-6 而非 P0,是因为没有存量触发路径;但一旦触发,链是确定的:

```
读档 → UNGP_modPlugin.onGameLoad:47 → UNGP_RulesManager.updateRulesCache():104-118
     → inGameData.getActivatedRules():112-114 → activatedRuleIDs == null → NPE
```

另一条健康度相关的链(difficulty 为 null):

```
UNGP_RulesManager.updateRulesCache():164  Difficulty difficulty = inGameData.getDifficulty();
                            :169  effect.updateDifficultyCache(difficulty);
UNGP_BaseRuleEffect:63-64                 updateDifficultyCache(difficulty.legacyLevel)  ← 无判空
```

difficulty 声明即 null(UNGP_InGameData.java:21),只有玩家在对话框里选过难度才会被赋值(UNGP_InteractionDialog.java:511 等)。"有激活规则 + difficulty 为 null"这个组合在当前版本里我没找到形成路径(新档在选规则前 rules 为空,循环不进入),所以按 P1-6 归为防御缺口;但注意 setStaticDifficulty(difficulty)(RulesManager:210、892-894)会把 null 写进 globalDifficulty,而该静态值被多处工具方法读取(例如 URule 内 getGlobalDifficulty() 的多个调用点),null 一旦流入就是 NPE 或空指针级联。建议在 :164 之后加 null 判空兜底并打日志。

### 1.8 存档线断点汇总

| 场景 | 位置 | 现象 | 级别 |
|---|---|---|---|
| 1.0-1.5 存档(旧 FQCN) | modPlugin:92 + CampaignGameManager(ignoreUnknownElements) | UNGP 数据静默消失、被重建 | 已知取舍,建议加提示(P2) |
| 任何类移动/改包名 | modPlugin:77-104(缺 SharedData 别名) | 规则运行时数据静默丢失 | P0-1 |
| 属性缺失(未来新增字段) | InGameData:27-29 → 68/74/87/114 | 读档 NPE | P1-6 |
| 规则 id 下线 | InGameData:97-102/111-119 + RulesManager:122-143 | 规则永久消失 + 效果无法回退 | P1-4 |
| 新增字段忘记 aliasAttribute | CampaignPlugin:43(oneMonthChecker) | 旧档读到 0,首帧误判月切换 | P2 |
| 规则切换/转生后 buff | PlayerFleetMemberBuff:10-11,50-55 | tag 是 transient,读档/重建后由 CampaignPlugin:193-196 重新挂接 | 设计正确 |

---

## 2. Q2 监听器与脚本生命周期

### 2.1 注册/注销全表(grep 全项目)

| 注册点 | 对象 | 是否有存在性检查 | 注销点 |
|---|---|---|---|
| modPlugin:52 addScript(new UNGP_CampaignPlugin()) | 生涯脚本(存档内) | 有(51 行 hasScript) | 无(常驻) |
| modPlugin:54 addTransientScript(new UNGP_UITimeScript()) | 瞬时脚本 | 无 | 无 |
| modPlugin:55 addTransientListener(new UNGP_CampaignEventListener()) | 事件监听 | 无 | 无(瞬时监听随 Sector 丢弃) |
| modPlugin:58 addTransientScript(new UNGP_SpecialistWidgetPlugin()) | UI 脚本 | 无 | 无 |
| SpecialistWidgetPlugin:65 getListenerManager().addListener(this, true) | UI 渲染/输入监听 | 有(true) | 无 |
| RulesManager:173 addListener(effect.getListener(), true) | 规则监听 | 有(true) | RulesManager:131 removeListenerOfClass |
| RulesManager:207 UNGP_EconomyListener.addListener() | 经济监听 | 有(instanceof 扫描,EconomyListener:14-21) | isEconomyListenerExpired()(EconomyListener:57-59) |
| InteractionDialog:522-546 addTransientScript(匿名) | 一次性延迟脚本 | — | 自己在 :543 移除 |
| GLaDos:21-44 | 一次性瞬时脚本 | — | 自己在 :44 移除 |
| ChallengeIntel:35/44 | intel 脚本 | 有 notifyEnded | 自己移除 |
| 战斗监听器(Collector:40、CriticalHit:43、IHaveActivated:107、LetThereBeLight:44、LuckySeven:44、Order66:42、SymbolPerseus:42、VengefulSpirit:30) | ship/engine 监听 | — | 部分有 removeListenerOfClass(CriticalHit:46、IHaveActivated:110/140、LetThereBeLight:68、LuckySeven:47) |

结论:行为上不会跨存档泄漏(transient 脚本/监听随 Sector 销毁;战斗监听随 CombatEngine 销毁),但 54/55/58 三行缺存在性检查是唯一的结构性隐患:本项目中 addTransientScript 的重复保护全靠调用点自觉(UNGP_UITimeScript:22-31 就是"先扫描再创建"的写法,说明作者知道这个坑),而 addScriptsIfNeeded 只对 :52 做了保护。若未来出现同一 Sector 上重复 onGameLoad(例如某些 e2e 流程),UNGP_SpecialistWidgetPlugin 会双份挂载(它 ctor 还会再注册一次 UI 监听)且 :69-77 的 updateUI() 只能找到一个实例。级别 P2,修复:三行都改成显式的存在性判断。

### 2.2 规则切换时的清理:正确

- 每次 updateRulesCache() 先在 108-116 清空 9 个静态列表,再 122-143 对全部 86 条规则执行 unapply:
  - 130-132 removeListenerOfClass(listenerClass)(对实现 UNGP_CampaignListenerTag 的 effect 逐个类移除)
  - 134-138 舰队 tag 的 unapplyPlayerFleetStats
  - 140-142 经济 tag 的 unapplyMarkets
  然后 165-205 对激活规则重新 apply 并 addListener(..., true)。
- 这套"全量 unapply → 全量 apply"是幂等重建,方向正确。两个可维护性注意点:
  - :131 是按 Class 移除,且 ListenerManager.removeListenerOfClass(Class) 走 ObjectRepository.getList(Class):FastIterationClassifier 会把对象按"自身类 + 父类 + 全部接口"建索引(apis/starsector-deobf-renamed/src/com/fs/util/container/repo/FastIterationClassifier.java:26-45,Lead 已核实),因此传接口类会精确命中全部实现者。当前每个 listener 接口都是规则私有的(SlipperyModification:55-56 的 SlipperyListener 即是),不会误删;但**如果将来出现"两个规则共用同一个 listener 接口"的设计,一条规则下线就会把另一条的监听一起移除**。P2(扩展性约束,非当前缺陷)。
  - :122-143 的循环把"清理"和"锁 milestone roll"(:124)混在一起,如果中途异常,静态列表已清空但未重新应用(见 P1-8)。

### 2.3 战斗侧:真正的问题(候选 P1-1)

- 插件由 data/config/settings.json:3 的 plugins 声明。vanilla 的同一个 section 里有官方注释 # add EveryFrameCombatPlugins here (with unique keys)(starsector-core/data/config/settings.json,与 genericCombatPlugin 相邻),所以这是"全局插件实例"通道,而不是"每场战斗 new 一个"。
- 而 UNGP_SpecialistCombatPlugin 的字段 35-37(tags/bonusMessages/notBonusMessages)只在构造时 new,init():39-86 全程 append(69、78、80),没有任何 clear;init(:31)、isSpecialistMode(:32)、isRulesBlocked(:33)也不复位。唯一的 clear 是 advance 首帧对两个消息表清空(:102-103),且还被 isSpecialistMode 分支包着。
- 后果(若实例跨战斗复用):第二场战斗起 tags 里同时存在上一场的同名 tag → 112-114 与 120-127 的逐帧调用翻倍(战斗规则效果重复叠加,取决于各 tag 是否 set 语义);开场提示重复;isRulesBlocked 永久为 true(只影响 :104 的一条消息)。
- 反证:代码里 if (!init) { init = true; ... }(:90-91)这个写法只有在"每场战斗都是新实例"的前提下才成立 —— 说明作者假设的是 per-combat 实例。这两种语义的分歧必须用显式重置来消除,否则就成了"取决于引擎实现"的隐式契约。
- 修复(6 行,零风险):
```java
public void init(CombatEngineAPI engine) {
    this.engine = engine;
    tags.clear(); bonusMessages.clear(); notBonusMessages.clear();
    init = false; isSpecialistMode = false; isRulesBlocked = false;
    ...
```
  说明:本条在静态审计下无法 100% 确认实例是否复用(见第 6 节),但上述修复在两种语义下都正确,属于"先修掉不确定性"。

### 2.4 静态集合/缓存普查(是否持有 Sector / FleetMember)

grep static (final)?(Map|List|Set|...) 共 29 处,逐个评估:

| 静态 | 位置 | 内容 | 跨存档风险 |
|---|---|---|---|
| ALL_RULES + 9 个 *_ITG 列表 | UNGP_RulesManager:46,51-60 | URule / 规则 effect 单例 | 每次 updateRulesCache 清空重建(108-116);不持有游戏对象 |
| needUpdateCache / isSpecialistMode / globalDifficulty | RulesManager:61,63,64 | 布尔/枚举 | 由 onGameLoad 链重建;见 P1-8 的异常路径 |
| CACHED_SPECIALIST_MODE | UNGP_SpecialistManager:12,17-19,36-38 | int 三态 | onGameLoad:44 显式 resetCache |
| ID_TO_CHALLENGE_INFO_MAP | ChallengeManager:16 | 挑战配置(CSV 模型) | 由 onApplicationLoad 重建;不含运行时状态 |
| TEMPORARY_COMPLETED_CHALLENGES | ChallengeManager:182 | List<String> | 从不在 onGameLoad 重置;目前 setter(:184-187)无任何调用者,恒为空 → 半成品 |
| SAVER_INSTANCES | InheritData:23 | DataSaver 实例 | 配置级单例,无游戏对象 |
| BACKGROUND_MAP | BackgroundManager:20 | 背景模型 | 同上 |
| DYNAMIC_DRAWABLE_MAP | UNGPFont:19 | Drawable 缓存 | 无界增长,getDynamicDrawable(:43-51)无上限,clearDynamicDrawable(:53-59)无调用者 → 死代码 + 潜在泄漏(P3) |
| BASE_SPAWN_TIME | DangerZone:25 | HullSize→Float 纯数值 | 无风险 |
| WIDGET_RECT | SpecialistWidgetPlugin:45-47 | Rectangle,类初始化时读屏幕高度 | 分辨率改变后不更新(P3) |
| ticking | SpecialistBackgroundUI:203 | static boolean | UI 静态状态,跨 intel 打开残留(P3) |

结论:没有任何 static 集合持有 Sector/CampaignUIAPI/FleetMemberAPI,不存在典型的"跨存档 NG 对象泄漏"。唯一的"游戏对象入档"路径是 UNGP_SharedData 的 ruleDataMap(见 P0-1 与 2.5),那是存档内容而不是静态缓存。

### 2.5 游戏对象写进 persistentData:两条路径

1. UNGP_MakeAnException:41-72 在 params.isOneDayPassed() 时把 List<HegemonyInspectionIntel>(vanilla intel 对象)通过 saveDataInCampaign(0, ...) 写进 persistentData(43/46);cleanUp 时清槽(74-77)。
2. UNGP_BountyHunter(工作区新增,未发布):58-70 把 WeakHashMap<BaseIntelPlugin,Integer> 写入 slot 0,并且发现从存档读回的不是 WeakHashMap 时会 new WeakHashMap<>(旧 map) 重新保存(:60-63)—— 这段"类型自愈"说明作者已经踩到过序列化类型变化。
   - 每帧成本::44-52 的 advanceInCampaign 无条件调 updateBounties(),而 65-69 每帧做 4 次 intelManager 查询(getIntel(PersonBountyIntel.class)、getCommQueue(...)、getIntel(BaseCustomBounty.class)、getCommQueue(...))→ 每条查询都要扫 intel 列表,数量随游戏进程增长。
   - 存档风险:把 live intel 对象作为 map key。XStream 在同一流内通过引用表保持对象同一性,所以"读档后 map key 就是 intel manager 里那个实体的副本还是本体"取决于两者是否写在同一份流里。如果是同一份,map 指向本体,弱引用与 isActive 判断都成立;如果是分开的流,读到的是副本,setReward(98-104)会改在副本上 → 悬赏金额静默不生效。
   - 建议(发布前):不要存对象,存稳定键 —— 例如 intel 的 id / getIcon / getIntelName + 自己维护的递增序号,把"弱引用自动回收"换成显式清理(反正 64 行已经在 keySet().removeIf(!isActive))。这样同时消掉每帧 4 次查询的必要性(可以只在 intel 变化/每天结算时跑)。
- modPlugin:78 的 x.registerConverter(new MapConverter(x.getMapper(), WeakHashMap.class), XStream.PRIORITY_LOW) 意图是修复 WeakHashMap 序列化。从字节码看 MapConverter.canConvert 是 this.type.equals(clazz),而 DefaultConverterLookup.registerConverter 走 PrioritizedList.add(converter, priority),高优先级先匹配 —— XStream 内置的通用 Map 转换器默认优先级更高,所以这行很可能不生效(仅作提醒,未实测;见第 6 节)。真正的隐患不在转换器,而在"存对象"这个设计。

---

## 3. Q3 性能(逐帧开销与量化)

### 3.1 每帧会跑到的代码(生涯)

UNGP_CampaignPlugin.advance()(:87-209)是唯一常驻的生涯逐帧逻辑,runWhilePaused() 返回 true(:81-85),暂停时也逐帧执行。逐段成本:

| 行 | 动作 | 每帧次数 | 备注 |
|---|---|---|---|
| 89-91 | isCacheNeedUpdate() | 1 | O(1) 静态布尔 |
| 93-95 | Global.getSector() ×1、getClock() ×1 | 2 | 引用读取 |
| 97-121 | 继承时间窗口判定:仅当 !passedInheritTime | 0-1 次/帧 | 正常存档长期为 false,不再进入 |
| 102-119 | newGameCheckDays -= days、inheritChecker.advance(days) | 同上 | 无分配 |
| 122-133 | sector.getCampaignUI().isShowingDialog() ×1;Keyboard.isKeyDown ×2(仅在无对话框时) | 1 + 2 | 每帧的 UI/输入查询 |
| 135 | UNGP_SpecialistManager.isSpecialistMode() | 1 | 非专家模式在此尽早返回 |
| 137-140 | new TempCampaignParams() | 1 次分配/帧 | 60 fps → 60 对象/秒,3600/分钟 |
| 142-162 | 日/月/年比较(3 次 clock getter + 3 次 int 比较) | 0-3 | 只在切换帧置位 |
| 170-172 | for (CAMPAIGN_TAGS_ITG) advanceInCampaign | 已激活的 CampaignTag 数(现库 16 个实现,典型 3-8) | 每个 tag 通常 1-2 次 SharedData Map 读(getDataInCampaign) |
| 186 | playerFleet.getFleetData().getMembersListCopy() | 1 次 List 分配/帧 + N 元素 | 20 成员 → 60 个 List 对象/秒 |
| 187-202 | 成员 × PLAYER_FLEET_MEMBER_TAGS_ITG | N × M(当前 M=1,DeepSpaceFear) | 每格 1 次 buffManager.getBuff(String) + 2 次 stat 访问 |
| 204-207 | playerFleet.forceSync() | 0-1(仅当 needsSync/强制标志) | 重同步在切规则首帧才发生 |

量化结论(20 成员、6 个已激活 CampaignTag、1 个成员 tag、60fps):
每帧 ≈ 2 次堆分配 + 6 次 tag 虚调用 + 20 次 buff 查询 + 3 次 UI/输入查询 ≈ 每秒 120 个短命对象。这不足以构成可感知的卡顿,但 TempCampaignParams 完全可以是 4 个字段的静态复用对象(它是纯数据载体,CampaignPlugin:225-241),getMembersListCopy 也可换成只读视图。级别 P2。

### 3.2 战斗(真正的热点候选)

UNGP_SpecialistCombatPlugin.advance():109-128:

- 112-114:for (tags) tag.advanceInCombat(...) → tags 数(≤33,现库实现数,见下)
- 116-128:engine.getShips()(一次 List 引用)→ 逐舰 × 逐 tag:玩家舰走 applyPlayerShipInCombat(120-122),敌舰走 applyEnemyShipInCombat(124-126)
- 量化:5 艘玩家舰 + 20 艘敌舰、8 个战斗 tag 全激活 → 25 × 8 = 200 次虚调用/帧;极端自建军(40 舰、33 tag)→ 1320 次/帧 = 79,200 次/秒。每次调用内部多为 MutableStat.modifyMult/setStat 或 buff 判重(不同 tag 实现不同,未逐个展开),但循环次数本身是可量化的确定开销。
- 一条容易忽略的:107-114 与 116-128 是两套遍历,tags 里的同一个对象每帧被调用 1 + 舰船数 次;若某个 tag 的 applyXxxInCombat 是重活(改 stat、查 buff),成本按舰船数线性放大。建议在 P1-1 的 init 重置之外,顺手给"每帧只该跑一次"的 tag 与"每舰一次"的 tag 做接口区分(现有 UNGP_CombatTag 把两类方法塞在一个接口里)。
- 待确认:现库有多少 effect 实现 UNGP_CombatTag/UNGP_CombatInitTag?grep 到 33 个实现(impl/rules/combat/ 32 个 + impl/rules/member/UNGP_CruisingHunting:14)。由于同时最多激活 64 条规则、且战斗类只是子集,实战 tags 数通常 3-10。

### 3.3 getActivatedRules() 的真实调用频率(纠正一个常见假设)

任务书里提到的 UNGP_InGameData.getActivatedRules():111-119 内部确实是 new ArrayList(:112)+ getAllRulesCopy() 再 new ArrayList(UNGP_RulesManager:249-251)+ contains 线性扫描(activatedRuleIDs 是 List)→ 单次成本 O(86 × 已激活数) 次 String.equals,最坏 86×64 = 5504 次字符串比较 + 2 次 List 分配(约几微秒级)。

但全项目 grep 后,getActivatedRules 的调用点只有 6 个,且没有任何每帧调用:

| 调用点 | 触发频率 |
|---|---|
| UNGP_RulesManager:118 | 每次 updateRulesCache(读档/切规则/repick) |
| UNGP_ChallengeManager:88 | getRunnableChallenges,在规则选择面板构建时(:107、:672 附近) |
| UNGP_ChallengeIntel:56 | updateChallengeProgress,由 RulesManager:213 在每次缓存更新时调 |
| UNGP_BirthdayPresent:52 | 仅在 params.isOneDayPassed() 的那一帧 |
| UNGP_StillGrowingUp:53 | 仅在 params.isOneMonthPassed() 的那一帧 |
| UNGP_SpecialistIntel:365 | 玩家点击"反馈"按钮 |

结论:这不是性能热点,O(rules×ids) 发生在极少数帧/事件里。但仍有两处值得改(P2):(a) 把 activatedRuleIDs 改成 HashSet<String> 或维护一份 Set<String> 视图,把 114 行的 O(n) 变成 O(1);(b) getAllRulesCopy() 每次分配 86 元素的 List,可返回 Collections.unmodifiableList 视图。顺带:112-118 没有 null 保护(见 1.5)。

### 3.4 经济线:最容易被忽视的一次性大开销

UNGP_EconomyListener.applyMarkets():31-44:
- :32 new ArrayList(...)(市场副本)→ 每次经济更新一次分配
- :33-38 每个市场 × ECONOMY_TAGS_ITG(现库 6 个经济 tag 实现,同时最多激活数受规则数限制)→ 3 次 tag 调用(unapplyAllMarket/unapplyPlayerMarket/applyAllMarket)
- :39-42 玩家市场再来一轮 applyPlayerMarket
- :52-54 economyUpdated() 直接调 applyMarkets() → 每次经济更新都全量重算

量化:50 个市场 × 4 个经济 tag × 3 = 600 次 tag 调用/次经济更新,再加玩家市场 200 次;若 tag 实现内部还遍历市场的 commodity 列表(如 GLaDos),实际是 600 × O(commodities)(~20)≈ 1.2 万次操作。触发点不只是"每天一次",玩家每次买卖/建造/市场易手都会触发 economyUpdated 语义的刷新 → 频繁市场交互时是可感知的尖峰。级别 P1-5。修复方向:缓存市场列表、只对 isPlayerOwned() 与规则关心的市场做重算、或给 tag 加 dirty 标记。

### 3.5 UI 线的每帧成本(详见第 4 节)

- UNGP_SpecialistWidgetPlugin:163-190 每帧 4 次 getSprite + 4 次绘制;192-211 悬停时每帧 2 次 ArrayList 分配、2 次 Collections.sort(≤64 元素)、2 次 renderIcons(101-109 里每个图标一次 getSprite)→ 64 条激活规则时 ≈ 每帧 128 次 sprite 查找 + 2 次排序(每次约 64·log64 ≈ 384 次比较)。级别 P2。
- RuleDescBackgroundPlugin:33-66 每次绘制 1 次 getSprite(48-56)+ 1 次 Misc.renderQuad;:39 每帧调 getFactor("6secs") → UNGP_UITimeScript.getInstance():22-31 线性遍历 transientScripts;而 addInterval 无调用者(:76-85),所以 intervalMap 恒空(:46-48 直接 return)→ 因子恒 0 → 闪烁常量,动画是死的。既是每帧无谓调用,也是功能缺失(P2/P3)。
- UNGP_SpecialistBackgroundUI.advance():186-201:每帧 playUILoop + suppressMusic(P1-7)。
- UNGP_RulePickItemPanelPlugin.advance():84-86 与 render():89-130:规则选择界面最多 86 个 item 同屏,每帧 86 次 advance + 86 次 render(每次若干 GL 调用)。无分配,可接受;但 render():89-130 每帧重算金色扫描条几何,可预计算。P3。

### 3.6 分配与字符串

- 未发现每帧字符串拼接热点:getActivatedRules 的 id 比较是 equals,tooltip/描述文本的构建全部在 UI 构建期(Content/Tooltip 回调),没有在 advance 里拼字符串。
- 每帧确定分配:TempCampaignParams(CampaignPlugin:140)、getMembersListCopy(:186)、widget 悬停时的 2 个 ArrayList(:194-195)。
- 结论:GC 压力低,不需要专项优化;真正的开销是 3.2/3.4 的循环次数与 3.5 的重复查询。

### 3.7 性能结论(一句版)

没有"每帧 O(规则数²)"的热点,任务书假设的 getActivatedRules 每帧调用并不成立;真实热点是(1) 战斗内 ships×tags 双循环(极端 1320 次虚调用/帧)、(2) 每次经济更新 O(markets×tags×3) 的全量重算、(3) 悬停时 widget 每帧两次排序 + 每图标 sprite 查找、(4) specialist 背景面板每帧调用音频 API。

---

## 4. Q4 UI 绘制(1153 行 dialog 与选择面板)

### 4.1 UNGP_InteractionDialog 本体:没有每帧重建问题

- advance() 只有 :587-592 两句(hasOption + setEnabled),没有 textPanel.clear()/addPara/字体度量。
- 所有文本刷新都在事件回调里:optionSelected():246-468 的 :248-252 先 clearOptions()+textPanel.clear()+uiPanelPlugin.update(...) 再重建;updateOptionsFromSettings():597-688 也是事件驱动。
- init():131-147 有一次性 IO 与重型构造,值得注意但不在逐帧路径:
  - :139 UNGP_InheritData.createInheritData(inGameData) → 立刻快照全部 DataSaver(其中 BlueprintsDataSaver:47-57 复制 4 个 faction 的蓝图集合,量级上千)
  - :140 UNGP_InheritManager.loadAllSlots() → 3 次 JSON 文件读取(InheritManager:89-120),同步阻塞在对话框打开时
  - 结论:打开对话框会有一次性卡顿(与存档越老越明显),不是"每帧布局重建"。建议改成异步/懒加载(P2)。
- 每帧只有两处小成本:
  - :847-881(InheritOptionsDelegate 的匿名 CustomUIPanelPlugin)advance 在 :865-869 每帧调 backgroundCheckBoxGroup.updateCheck() 与 settingEntry_difficulty.advance(amount),后者再调一次 updateCheck(:1131-1134)→ 每帧 2 次 checkBox 组刷新;
  - :587-590 每帧 options.setEnabled(...),vanilla 侧会重算按钮状态(可接受,建议仅在状态变化时 set)。
- 字体:全部走 UNGPFont:15-16 的静态 Orbitron,无每帧度量;UNGPFont:43-51 的动态缓存无人清理(见 2.4)。

### 4.2 规则选择面板(UNGP_RulePickPanelPlugin)

- 没有 advance 覆写:每帧只有 renderBelow():488-506 的 4 顶点 GL_QUADS 渐变。updateRuleThings():628-728(会 new ArrayList + createSubPanel×3 + createUIElement×3 + 遍历 86 个按钮重建 tooltip)只由按钮事件触发(:353、:622)。
- 一次性构建成本较大但合理:createContent:100-354 为每个选项建 checkbox/label/tooltip,createRuleRect:365-481 为 86 条规则各建一个 UNGP_RulePickItemPanelPlugin(:401-423、:456-477)。
- 一个功能瑕疵(P2):UNGP_InteractionDialog:389-439 创建 UNGP_RulePickPanelPlugin 时,把同一个 pickTrigger 同时作为 onConfirm 与 onCancel 传入(:433-435);确认路径在 :536 执行 onConfirm.run(),随后 exit():769-775 末尾又执行 onCancel.run() → 回调跑两次。因为回调是幂等的 UI 重建,玩家看不到错误,但每次确认都白做一遍刷新,且若以后有人给回调加非幂等副作用就会出问题。建议 exit 里判断"是否已被 confirm 消费"。

### 4.3 intel / codex

- UNGP_RuleCodexEntryPlugin:63-96 的 detail 只在打开时构建(:79-86),advance/renderBelow/processInput 全空(:108-140)。
- UNGP_SpecialistIntel:71-328 的 createLargeDescription 一次性构建整页(含规则的 desc/tooltip,:226-272),不在逐帧路径。
- 真正的每帧 UI 成本只有 UNGP_SpecialistBackgroundUI.advance():186-201(P1-7)与 widget(3.5)。

### 4.4 UI 结论

1153 行的体积主要是"多个 delegate 内联在一个文件里"(InheritOptionsDelegate:756-882、CustomStoryDialogDelegate:884-920、RecordDialogDelegate:925-1063、InheritSettingEntry:1070-1151),不是逐帧布局重建。每帧 UI 开销集中在 widget 悬停分支、86 个规则 item 的 render、specialist 背景板的音频调用,没有"每帧测字体宽度"的问题。

---

## 5. Q5 读档/转生(cycle)时的状态重建幂等性

### 5.1 读档(幂等,但重建链无护栏)

UNGP_modPlugin.onGameLoad():43-48 的顺序是正确的:resetCache(44)→ SharedData.initialize(45)→ addScriptsIfNeeded(46)→ updateRulesCache(47)。

- SharedData.initialize():50-56 是"有则取、无则建",幂等。
- updateRulesCache():103-220 是"全量清空 → 全量 unapply → 全量 apply → 回写",重复调用结果相同(97-102 的 cleanUp 只在"本次被移除的旧 id"上跑,第二次调用 oldRules 为空)。
- 不幂等的部分:无 try/finally(P1-8);updateRulesCache 里 214 updateSpecialistSkills(playerStats) 会 setSkillLevel + refreshCharacterStatsEffects(),每次重建都重算一次技能效果(可接受)。
- 读档不重启 JVM:静态列表、静态 difficulty、之前会话残留的对象都还在内存里。108-116 清空 + 165-205 重建能覆盖,但任何"只在 onApplicationLoad 做一次"的初始化(如 UNGPFont:29-40 的字体、ChallengeManager 的 CSV 模型)在读档时不会重跑 —— 目前看没有依赖它的读档路径,属可接受。

### 5.2 转生/继承(继承逻辑幂等)

UNGP_InteractionDialog.inherit():473-548:
- :506-512 一次性写入 curCycle / inherited / hardMode / completedChallenges / difficulty / rules,全部是赋值语义,重复执行结果相同。
- 不幂等的副作用:
  - :514-515 每次执行都 new UNGP_SpecialistIntel() 并 addIntel → 重复确认会产生重复 Intel(P2)
  - :516 confirmChallenges 会写 persistentData 的启动标记(ChallengeManager:58-77 区间),幂等
  - :522-546 每次都挂一个匿名瞬时脚本(0.1 秒后自删,:543),最多短暂叠加几个(P3)
- cycle 递增:UNGP_InheritData.createInheritData():40-51 用 inGameData.getCurCycle()+1;getColorByCycle/getPrefixByCycle:119-149 只做显示分档。若玩家在同一存档反复"记录 → 继承",cycle 会不断 +1 且与真实转生次数脱钩(P3,非幂等但无崩溃)。UNGP_InheritManager.load():89-120 以 jsonObject.has("cycle")(:93-95)判断文件有效性,读失败时 catch(Exception ignored) return null(:117-119)→ 静默吞掉损坏的继承文件,玩家看到的是"重生点消失"。建议至少打日志(P2)。

### 5.3 规则切换的幂等性

- saveActivatedRules():85-104 对同一输入重复调用结果相同。
- 但 97-102 的"清理被移除规则"依赖 URule.getByID 非 null(见 1.4):规则 id 下线时既不清理也不报错;getActivatedRules():111-119 也不会把它显示出来。这是"看起来幂等、实际静默丢数据"的典型。
- updateCacheNextFrame():89-95 是纯标志位(BirthdayPresent:71、StillGrowingUp:87、EntropiusMind:69 会在改规则后置位),下一帧 CampaignPlugin:89-91 消费 → 幂等;但注意它没有"防抖",同一帧多个来源置位只产生一次重建,OK。

### 5.4 读档/转生的幂等性结论

重建路径整体是"清空 + 全量重算",幂等性设计是成立的;缺陷集中在:(a) 异常时不留可恢复状态(P1-8);(b) 规则下线时的静默丢弃(P1-4);(c) Intel 重复添加与继承文件的静默吞异常(P2)。

---

## 6. 未能静态确认 / 需运行验证的点

以下结论我在报告里都标了"可能/候选",请不要当作已验证事实:

1. settings.json 声明的 EveryFrameCombatPlugin 实例是否跨战斗复用(P1-1)。我确认了 vanilla 的 section 语义(# add EveryFrameCombatPlugins here),但没有运行时验证引擎是否 new 多个实例。修复方案对两种语义都安全,建议直接改。
2. XStream MapConverter 的优先级是否真的被内置转换器压制(modPlugin:78)。字节码层面 canConvert 是精确类型相等 + PrioritizedList 按优先级降序匹配,内置 MapConverter 的默认优先级更高;但"内置转换器能否正确处理 WeakHashMap 子类"我没有实证。修复建议不依赖该结论。
3. intel 对象与 persistentData 是否在同一份 XStream 流里(2.5 的 BountyHunter/MakeAnException)。这决定"读到副本还是本体",需运行时打印引用是否相等来确认。
4. 1.0-1.5 存档在 ignoreUnknownElements 下的确切表现(1.3)。我确认了 CampaignGameManager 调用了 ignoreUnknownElements,但"脚本列表/持久数据里出现不可解析类时游戏是跳过还是抛错"由游戏内部实现决定,mod 侧无法控制。
5. 未构建、未运行:所有性能数字都是"调用次数 × 单次成本"的结构性估算,不是 profiler 结果。量化依据已在各条目写明(帧率假设 60、成员数 20、市场 50、舰船 25/40、规则 86、上限 64)。

---

## 7. 修复建议(按性价比排序)

1. (P0,10 分钟) 给 UNGP_SharedData 加 alias(modPlugin:77-104),并保留旧 FQCN 别名;顺手给 UNGP_CampaignPlugin.inGameData 与其余持久字段补 aliasAttribute,建立"加字段即加别名"的检查清单。
2. (P1,10 分钟) UNGP_SpecialistCombatPlugin.init() 开头显式 clear/reset 字段(35-37、31-33),消除跨战斗累积的不确定性。
3. (P1,30 分钟) updateRulesCache 包 try/finally,异常时把 needUpdateCache 置回 false 并记日志,避免每帧重试。
4. (P1,30 分钟) SlipperyModification.cleanUp():除清槽外,对 triggered 内的成员回退 DMod(或至少在规则被移除时提示玩家);把 triggered 从无界 Set 改成与舰队成员同步的集合。
5. (P1,1 小时) BountyHunter:改为存稳定 id;把 4 次 intelManager 查询从每帧挪到"每天/悬赏变化"事件;发布前完成。
6. (P1,半天) 规则 id 下线兼容:读档时保留无法解析的 id 并记日志,提供迁移表;禁止 saveActivatedRules 静默丢弃。
7. (P2) EconomyListener:市场列表缓存 + 只重算玩家市场 / dirty 标记。
8. (P2) CampaignPlugin:复用 TempCampaignParams、避免每帧 getMembersListCopy;给 oneMonthChecker 补 aliasAttribute。
9. (P2) Widget:悬停分支缓存排序结果(规则集变化时才重排)、缓存 sprite;RuleDescBackground 的 UITimeScript 要么接线上 addInterval,要么删掉。
10. (P2) 读档/转生后的 UI 冗余:去掉确认路径的双回调执行;SpecialistIntel 重复添加去重;InheritManager 的 catch(Exception ignored) 至少打日志。
11. (P3) 静态小问题:UNGPFont 无界缓存与死代码、WIDGET_RECT 分辨率快照、ChallengeManager:182 的半成品静态列表。
