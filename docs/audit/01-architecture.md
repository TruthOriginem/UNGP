# 01 · 架构与框架审计

> 项目:UnofficialNewGamePlus(Starsector 0.98a) 172 个 .java / 16861 行
> 审计对象:规则框架契约层(`src/ungp/api/rules/**`)、规则管理器(`UNGP_RulesManager`)、加载/存档链路、tag 混入体系,以及 2.6.0 未提交在途改动(11 文件 / +954 行)。
> 审计方式:只读。所有 `path:line` 均来自本次实际读取的源码;行号对应工作区当前状态(含未提交改动)。
> 级别:P0=存档损坏/崩溃/数据错误;P1=功能正确性或明显性能问题;P2=可维护性/扩展性;P3=风格。

---

## 0. 速览

**本轮未发现可确定性复现的 P0。** 有两条"疑似 P0"需要 Lead 复核(涉及 XStream 序列化与玩家不可逆数据):

| 编号 | 位置 | 级别 | 一句话机制 |
| --- | --- | --- | --- |
| S-1 | `src/ungp/impl/rules/other/UNGP_BountyHunter.java:59-63` | P0?/P1 | 把 `WeakHashMap<BaseIntelPlugin,Integer>` 作为"已加奖金"账本写进存档;弱 key 语义与"必须撤销"语义冲突,GC 或引用失配后 `unapplyGlobalStats` 无法减回,赏金永久膨胀 |
| S-2 | `src/ungp/impl/rules/member/UNGP_SlipperyModification.java:290-294` | P0?/P1 | `cleanUp()` 清空 `triggered`(已惩罚舰船)集合,规则被移除后再重拾会**对同一艘船重复施加 D-mod**,且 `DModManager.setDHull` 改的是玩家舰队真实 variant(随存档持久、不可逆) |

P1 结论 12 条,见第 7 节汇总表;正文按 6 个必答问题逐条展开。

---

## 1. 框架模型与数据流

### 1.1 三方契约

```
CSV(UNGP_rules.csv) --反射--> UNGP_BaseRuleEffect 实例 --setRule--> URule(buffID/描述/配色)
                                        |
                          implements 12 个 tag 接口之一/多
                                        |
   RulesManager 分 9 个静态列表缓存 + SpecialistCombatPlugin / SpecialistSkills 二次分发
```

- 规则效果基类:`src/ungp/api/rules/UNGP_BaseRuleEffect.java:29`(`rule` 字段)、`:30`(`buffID`)、`:49`(`setRule` 里 `buffID = "UNGP_" + rule.getId()`)。
- 生命周期接口:`src/ungp/api/rules/UNGP_RuleEffectAPI.java:20 updateDifficultyCache` / `:49 applyGlobalStats` / `:51 unapplyGlobalStats` / `:58 cleanUp` / `:67 addIntelTips`。
- 规则数据槽:`UNGP_BaseRuleEffect.java:219-221 saveDataInCampaign(int slot)`、`:235-237 getDataInCampaign(int slot)`、`:239-241 clearDataInCampaign(int slot)`,键 = `rule.getId() + slot`(`int` 直接字符串拼接,见 6.5)。
- 数据实际落盘位置:`src/ungp/scripts/campaign/UNGP_SharedData.java:17` `ruleDataMap` → `:50-55 initialize()` 挂到 `persistentData["UNGP_sharedData"]`。

### 1.2 加载 → 实例 → 缓存 → 激活/卸载

```
onApplicationLoad (UNGP_modPlugin.java:30)
  └─ loadSettings → UNGPFont.init → InheritData.loadAllSavers
     → BackgroundManager.loadAllBackgrounds
     → RulesManager.initOrReloadRules (:34)      // CSV→URule,loadAllRules:785
     → ChallengeManager.initOrReloadChallengeInfos // 依赖规则已加载
     → tagAllChallengeProviders (:77)

onGameLoad (UNGP_modPlugin.java:43)
  └─ UNGP_SpecialistManager.resetCache → UNGP_SharedData.initialize
     → addScriptsIfNeeded → RulesManager.updateRulesCache()

updateRulesCache (UNGP_RulesManager.java:103)
  1) :104  inGameData = UNGP_InGameData.getDataInSave();:105 null 则整体跳过
  2) :118  activatedRules = inGameData.getActivatedRules()          // 新 ArrayList,按 ALL_RULES 顺序
  3) :122-143 遍历 **ALL_RULES**(不是 activatedRules!)逐一 unapplyGlobalStats
       / CampaignListenerTag→removeListenerOfClass / PlayerFleetTag→unapplyPlayerFleetStats
       / EconomyTag→UNGP_EconomyListener.unapplyMarkets
  4) :146-156 收集 TweakBeforeApplyTag,调 tweakBeforeApply(activatedRules, List.copyOf(...))
  5) :160-162 HashSet 去重后 clear+addAll 9 个静态列表
  6) :165-205 遍历 activatedRules:updateDifficultyCache → applyGlobalStats → addListener(transient=true)
       → :178 instanceof CombatTag||CombatInitTag ? COMBAT_RULES : CAMPAIGN_RULES → 各 tag 列表
  7) :206-209 EconomyListener.addListener()+applyMarkets
  8) :210 setStaticDifficulty;:211 inGameData.saveActivatedRules(activatedRules);
     :212 updateCachedSpecialistMode;:213 updateChallengeProgress;:214 updateSpecialistSkills;:215 playerFleet.forceSync()
  9) :219 needUpdateCache = false
```

**关键结构性事实:卸载是"全量广播"而非"成对调用"。** 每一步卸载都遍历 `ALL_RULES`,而不是"上次激活的规则集合"。这既是安全网(任何一次缓存刷新都会把 167 处 MutableStat 修改全部撤回),也是双刃剑(见 Q1)。

### 1.3 static 可变状态清单

| 位置 | 字段 | 谁写 | 风险 |
| --- | --- | --- | --- |
| `UNGP_RulesManager.java:46` | `ALL_RULES` | `loadAllRules:785`、`loadPickTimes:811`(daemon 线程) | 网络线程与主线程无同步写 |
| `:51-60` | `ACTIVATED_RULES_IN_THIS_GAME`(public)、`COMBAT_RULES_ITG`、`CAMPAIGN_RULES_ITG`、`ECONOMY/CAMPAIGN/PLAYER_FLEET/PLAYER_FLEET_MEMBER/PLAYER_SHIP_SKILL/PLAYER_CHARACTER_SKILL_TAGS_ITG` | `updateRulesCache` | 单线程写,但读写方遍布 6 个文件 |
| `:61` | `needUpdateCache` | `updateCacheNextFrame:89` / `updateRulesCache:219` | — |
| `:63-64` | `isSpecialistMode`(未使用)、`globalDifficulty` | `setStaticDifficulty` | 死字段 |
| `UNGP_SpecialistManager.java:12` | `CACHED_SPECIALIST_MODE = -1` | `:17 resetCache`(仅 onGameLoad) / `:26` | 跨存档不重置时读到上局状态 |
| `UNGP_PlayerFleetMemberBuff.java:9` | `transient static shouldForceSyncNextStep` | CampaignPlugin:204 | 静态 + transient,只在会话内 |
| `UNGP_ChallengeManager.java:182-191` | `TEMPORARY_COMPLETED_CHALLENGES` | 挑战流程 | static 可变集合 |
| `UNGP_SharedData.java:15` | `instance` | `:50 initialize()`(仅 onGameLoad) | 回主菜单后仍指向旧存档 map |
| `UNGP_BaseRuleEffect.java:27` | `protected static Random RANDOM` | 各规则 | 共享随机源,并行/顺序敏感 |

---

## 2. Q1:apply 与 cleanUp 是否严格成对?泄漏在哪?

### 2.1 结论

**不成对。** 当前设计是"补偿式":`updateRulesCache` 每次刷新都把 `ALL_RULES` 全量 `unapply*` 一遍(RulesManager:122-143),然后重新 `apply*`(165-205)。`cleanUp()` 只是"规则被移除"时的一次性钩子,并且它的触发点(见 2.3)**与 unapply 的时机不保证一致**。于是泄漏有两类:

1. **效果直接写在"随存档序列化的公共对象"上,且该对象不参与全量 unapply** → 会永久残留。
2. **cleanUp 先于 unapply 执行,把 unapply 需要的数据清掉** → 该次 unapply 变成空操作。

### 2.2 具体泄漏字段(逐个可指认)

| # | 被修改的字段 | 修改点 | 是否有 unapply | 结论 |
| --- | --- | --- | --- | --- |
| L1 | 势力关系 `FactionAPI.setRelationship`(PIRATES + INDEPENDENT = FRIENDLY) | `src/ungp/impl/rules/other/UNGP_FullOfCompany.java:23-29` | **无**(该类没有 `unapplyGlobalStats` 覆写) | 规则被移除后关系永久保持 FRIENDLY,只有存档里那次 `setRelationship` 生效;后续想还原没有任何代码路径 |
| L2 | 势力关系(REMNANTS = COOPERATIVE) | `src/ungp/impl/rules/other/UNGP_TachLabour.java:23-28` | **无** | 同 L1 |
| L3 | `UNGP_InGameData.changeTimeStat`(MutableStat,`:27`,随存档以 `cTS` 别名序列化) | `src/ungp/impl/rules/other/UNGP_NoWayBack.java:29-36` `modifyMult/unmodify(buffID)` | 有,配对正确 | **但**:unapply 只在"该规则对象存在于 `ALL_RULES`"时才被遍历到。规则 id 被改名/删除后,`getActivatedRules`(:111-119)会静默丢弃它,而 `changeTimeStat` 上仍带着那次 mult → 每年可重置次数永久放大,且 `unmodify` 永远不会有人调用 |
| L4 | 赏金 intel 的 `PersonBountyIntel.bountyCredits` / `BaseCustomBounty.creditReward` | `UNGP_BountyHunter.java:98-104` | 有(:107-117),但依赖 WeakHashMap 账本 | 账本条目一旦丢失(GC / 读档恒等失配 / :64 的 `removeIf`),奖励的增量就再也减不回来 → 永久膨胀(见 S-1) |
| L5 | 玩家舰 `ShipVariantAPI` 的 hullmod 集合 + `DModManager.setDHull` | `UNGP_SlipperyModification.java:221-246` | **设计上不撤销**(D-mod 是规则惩罚) | 不可逆资产变更;重复施加路径见 S-2 |
| L6 | `UNGP_SharedData.ruleDataMap` 各槽位 | 各规则的 `saveDataInCampaign` | 部分规则覆写 `cleanUp` 清理(HumanNatureTest:137-140、SlipperyModification:291-294、BountyHunter:120-122) | 槽位以 `rule.getId() + slot` 为键,**规则删除后槽位永久留在存档里**(无版本/无清理),存档随版本升级缓慢膨胀 |
| L7 | 舰队成员 buff(`UNGP_PlayerFleetMemberBuff`) | CampaignPlugin:184-208 每帧重建 | `PlayerFleetMemberTag` 走 `PLAYER_FLEET_MEMBER_TAGS_ITG`,列表刷新时自然消失 | 安全(`tag` 字段 `transient`,`UNGP_PlayerFleetMemberBuff.java:10`;buff 时长 0.1 秒,不会成为僵尸) |
| L8 | `ListenerManager` 监听器 | RulesManager:173 `addListener(..., true)`(transient) | `removeListenerOfClass`(RulesManager:129) | **待复核**:`UNGP_CampaignListenerTag.getClassOfListener()` 返回的是**接口**(如 `SlipperyListener`、`MarketTransactionListener`),能否被移除取决于 `ObjectRepository.getList(Class)` 是 `isInstance` 还是精确类匹配;该类的反编译源码不在 `apis/starsector-deobf/src` 覆盖范围内,无法在本轮核实 |

### 2.3 反证路径:哪些流程会跳过 cleanUp 或让 cleanUp 无效

**路径 A —— 先 save 再 update(顺序反了)**
`src/ungp/scripts/campaign/specialist/dialog/UNGP_RepickRulesDialog.java:100-107`:`saveActivatedRules(pickedList)` 先执行,随后才 `updateRulesCache()`。
`UNGP_InGameData.java:96-102` 会在 `saveActivatedRules` 内部对"被移除的规则"立刻调 `cleanUp()`。
后果:cleanUp 发生在 RulesManager:122-143 的全量 `unapplyGlobalStats` **之前**。若某规则的 `cleanUp()` 把"待撤销金额/待撤销倍率"存在自己的 ruleData 槽里并清空,随后那次 unapply 就读不到数据 → **stat 永久残留**。
现有规则里 `UNGP_BountyHunter.cleanUp()`(:120-122)是"自己调 unapply",所以侥幸正确;而 `UNGP_HumanNatureTest.cleanUp()`(:137-140)只清数据、它本身没有 stat 修改,也侥幸正确。**但框架把"正确性"寄托在每条规则自觉上**,第三方按 HumanNatureTest 的写法复制就会踩坑。

**路径 B —— cleanUp 只在"从激活集合里被移除"时触发,而 unapply 遍历的是 ALL_RULES**
规则被移除 → `saveActivatedRules` 里 `URule.getByID(ruleId)`(:98)返回对象 → cleanUp 正常。
规则**从 CSV 删除** → `getByID` 返回 null(:99 判空跳过)→ 没有任何 cleanUp,同时 `updateRulesCache` 也遍历不到它 → **L3 类残留无人回收**。

**路径 C —— 缓存刷新本身可能被跳过**
`UNGP_CampaignPlugin.java:89-91`:只有 `isCacheNeedUpdate()` 为真才调 `updateRulesCache`。任何直接改 `activatedRuleIDs` 却没调 `updateCacheNextFrame()` 的代码,都会让"规则集合已变、效果未换"的状态持续到下一次刷新。现有 3 处调用方都配对调用了(`UNGP_StillGrowingUp:86-87`、`RepickRulesDialog:100-107`、`UNGP_InteractionDialog:506-520`),但这是约定而非机制。

**路径 D —— `updateRulesCache` 在 :105 因 `getDataInSave()` 拿不到存档而整体跳过**
此时 `needUpdateCache` 保持 true,不构成泄漏,但意味着"没有存档就没有卸载"。

### 2.4 修复方向

1. 把"需要撤销的量"从"局部缓存字段"改为"在 unapply 里从持久化槽位读取",并在 `UNGP_BaseRuleEffect` 提供 `cleanUp()` 默认实现 = `unapplyGlobalStats()` + `clearAllDataInCampaign()`,消除"忘写 unapply 就没有逆向"这一类缺陷(L1/L2)。
2. `updateRulesCache` 的卸载阶段改为"遍历 `ACTIVATED_RULES_IN_THIS_GAME` ∪ 上次激活集合的并集",让"被删除的规则"仍有最后一次卸载机会(需要把上次激活 id 列表单独持久化)。
3. 强制 `cleanUp` 只在 `updateRulesCache` 末尾(卸载+应用之后)统一触发,或明确规定"cleanUp 内禁止清空 unapply 依赖的数据"。当前两条路径顺序相反(路径 A),属实现不一致。

---

## 3. Q2:tag 混入体系是否可扩展?instanceof 是否爆炸?有无隐式顺序依赖?

### 3.1 分发点清单(全部 instanceof 位置)

| 文件 | 行 | 判断的 tag |
| --- | --- | --- |
| `UNGP_RulesManager.java` | :129 | `CampaignListenerTag` |
| | :135 | `PlayerFleetTag` |
| | :140 | `EconomyTag` |
| | :147 | `TweakBeforeApplyTag` |
| | :172 | `CampaignListenerTag`(注册) |
| | :178 | `CombatTag` / `CombatInitTag`(决定进 COMBAT 还是 CAMPAIGN 列表) |
| | :184 | `PlayerFleetTag` |
| | :190 | `PlayerFleetMemberTag` |
| | :193 | `EconomyTag` |
| | :196 | `CampaignTag` |
| | :199 | `PlayerShipSkillTag` |
| | :202 | `PlayerCharacterStatsSkillTag` |
| `UNGP_SpecialistCombatPlugin.java` | :61 | `TweakBeforeApplyCombatTag` |
| | :68 / :72 | `CombatTag` / `CombatInitTag` |
| `UNGP_SpecialistSkills.java` | :32 / :39 / :56 / :63 | `PlayerShipSkillTag` / `PlayerCharacterStatsSkillTag` |

合计 **19 处 instanceof,分布在 3 个文件**(RulesManager 12 / SpecialistCombatPlugin 3 / SpecialistSkills 4)。**问题不在"数量"而在"散落"**:新增一个 tag 必须同步改 3 个分发文件(RulesManager 的填充列表 + CombatPlugin 的战斗循环 + Skills 的技能循环),编译器不会提醒漏改;漏改的表现是"规则被加载、被显示、但永不生效"——静默失效。

另外,消费侧还有 2 个文件:`PLAYER_FLEET_MEMBER_TAGS_ITG` / `PLAYER_FLEET_TAGS_ITG` / `CAMPAIGN_TAGS_ITG` 由 `UNGP_CampaignPlugin.java:170-208` 消费,`ECONOMY_TAGS_ITG` 由 `UNGP_EconomyListener` 消费。**新增 tag 的最小改动面:3 个分发文件 + 1~2 个消费文件 = 4~5 个文件。**

### 3.2 隐式依赖(按危险度排序)

| # | 依赖 | 位置 | 违反后果 |
| --- | --- | --- | --- |
| D1 | `updateDifficultyCache` 必须先于 `applyGlobalStats`(规则把难度换算结果存进字段) | RulesManager:169→170;每个规则如 `UNGP_HumanNatureTest.java:29`、`UNGP_SlipperyModification.java:106-109` | 用了未初始化/上一难度的字段值,数值错误且无报错 |
| D2 | 卸载全部 → tweak → 应用,三步顺序不可交换 | RulesManager:122→146→165 | tweak 改出来的列表不含卸载副作用,或干净的 stat 被叠加两次 |
| D3 | `TweakBeforeApplyTag` 的第二参"不可变" | `UNGP_TweakBeforeApplyTag.java:14` javadoc 写死 `Unmodifiable`,实现侧 `RulesManager:153` 用 `List.copyOf`;**但战斗侧 `UNGP_SpecialistCombatPlugin:58-59` 传的是可变 `ArrayList`** | 同一个接口在生涯/战斗两种场合行为不同:第三方 tweak 若尝试修改"原始列表",生涯路径抛 `UnsupportedOperationException`,战斗路径静默成功。**这是明确的契约违反(P1),见 6.2** |
| D4 | 规则归类 = `instanceof CombatTag || CombatInitTag` | RulesManager:178 | 一条规则若同时实现 `CombatTag` 和 `CampaignTag`,只会进 `COMBAT_RULES_ITG`,`advanceInCampaign` **永远不会被调用**;反之若只实现 `CampaignTag` 且忘了生命周期,战斗内无表现。归类是"互斥二选一",但没有任何编译期或运行期校验 |
| D5 | `UNGP_StillGrowingUp` 在 `advanceInCampaign` 内改规则集合并依赖 `updateCacheNextFrame()` 延后一帧 | `UNGP_StillGrowingUp.java:78-87` | 若某个 tweak 直接在 `advanceInCampaign` 里增删 `CAMPAIGN_TAGS_ITG`/激活列表而不走延后刷新,会在 `UNGP_CampaignPlugin:170-172` 的 for-each 期间抛 `ConcurrentModificationException` |
| D6 | `URule` 构造期即计算 `isDescAffectedByLevel`(4 个难度 × 取描述) | `UNGP_RulesManager.java:335`、`:480` | 依赖 i18n 已加载;缺行时 `SimpleI18n.get` 无 null 保护(`src/ungp/scripts/utils/SimpleI18n.java:23`)→ 加载期 NPE,而不是"这条规则缺描述" |
| D7 | `isDescAffectedByLevel` 依赖 4 个难度描述**互不相同** | `:480` | 全是常数文本的规则被判为"与难度无关",UI 上的难度切换按钮消失;纯视觉,但判定靠字符串比较,改文案会改行为 |
| D8 | `UNGP_SpecialistSkills` 的 apply 只遍历"生效规则",unapply 遍历"全部规则" | `UNGP_SpecialistSkills.java:32` vs `:39`(`:56/:63` 同) | 这是刻意的补偿;代价是 unapply 时任何一条规则抛异常会中断后续全部撤回 → stat 残留。建议加 per-rule try/catch |
| D9 | `BackgroundManager` 的 skill 取最高等级依赖 `LinkedHashMap` 插入顺序 | `src/ungp/api/backgrounds/UNGP_BaseBackgroundPlugin.java:266-273` | 见 6.5(P2):`if (level > targetLevel) put(skillId, targetLevel)` 存的是旧值,随后又无条件 `put(skillId, level)`,最终等级 = 插入顺序里最后一个被遍历到的等级,可能比应有的更低 |

### 3.3 结论

- 分发集中但不内聚:13 处 instanceof 分布在 5 个文件,新增 tag 无编译期保护 → 建议引入 `RuleEffectDispatcher`(单点注册:tag 接口 → 应用阶段 + 目标列表),把 `updateRulesCache`/`SpecialistCombatPlugin`/`SpecialistSkills`/`UNGP_CampaignPlugin` 里的 instanceof 全部收敛。
- 顺序依赖正常但**没有文档化**:D1/D2/D3 应写进 `UNGP_RuleEffectAPI` 的 javadoc;D4 的归类互斥应加运行期断言(同时实现两组接口时日志告警)。
- 至少存在一处**契约违反**:D3(战斗侧可变)。这是本次在途改动引入的,建议本轮修掉。

---

## 4. Q3:`URule` 缓存与 i18n 耦合 + `RulesManager` 952 行拆分

### 4.1 缓存结构

- 缓存实体是"tag 静态列表"(9 个静态 `List`),不是 `Map<规则, 状态>`;因此 **同一规则不可能同时出现在两个 tag 列表里**(按 D4 的互斥归类),但**同一规则可以出现在"COMBAT 列表"和"PLAYER_FLEET 列表"两处**(两者互不排斥)——这是有意的,问题在于没有任何地方记录"本规则属于哪些通道",排障时必须靠读代码。
- 缓存失效是"整表重建"(`needUpdateCache` → `updateRulesCache`),没有局部失效;成本可接受(规则数 ~100),但**重建副作用大**(全量 unapply + apply + `playerFleet.forceSync()`),因此不能在高频路径触发。`UNGP_CampaignPlugin:89-91` 已做到"只在标志位为真时刷新"。

### 4.2 i18n 耦合(这是 `URule` 设计的最大问题)

```
UNGP_BaseRuleEffect.java:28  static I18nSection i18n = new I18nSection("UNGP_rule_impl","",false)
UNGP_RulesManager.java:335   构造器内即:effectPlugin.setRule(this) + 计算 isDescAffectedByLevel
UNGP_RulesManager.java:469   getDescriptionParams(index) 固定取 10 个参数
UNGP_RulesManager.java:629   generateCombatTips 按 "%s" split desc 生成战斗提示
```

后果:
1. **数据类依赖 UI 层**:`URule` 同时承担 CSV 数据、i18n 文本拼装、配色(`:872-890`)、tooltip 渲染(`:593`)、Codex 关联(`:91/:99` 在 CodexManager)、roll 描述(`:507`)、挑战关联(`:513/:582`)。任何一处 i18n 缺行都会在**加载期**炸,而不是渲染期降级。
2. **描述参数数量硬编码 10**(`:469`):第 11 个参数会被静默丢弃;返回 null(`:96` 处的 `getDescriptionParams` 默认分支)与 `String.format` 的 `%s` 数量由规则作者自行保证,框架不校验。
3. **规则 id 是 i18n 键**:`I18nSection` 用 `rule.getId()` 索引文本;改名规则 = 同时改 CSV、两个 strings 文件、规则数据槽键、旧存档里的 id(见 Q4)—— 一处漏改就静默显示空白。

### 4.3 建议拆分(按职责,不是按行数)

| 现状职责 | 建议归属 | 理由 |
| --- | --- | --- |
| CSV 解析、反射实例化、`ALL_RULES`、`getByID`、排序(:785/:897/:924) | `UNGP_RuleRegistry` | 与游戏生命周期无关,可单测 |
| 9 个 tag 列表 + `updateRulesCache`(:103-219) | `UNGP_RuleCacheService` | 唯一可变状态的家 |
| 文本/配色/tooltip/Codex(`:469-709`、`:269/:273/:292`) | `URule` 保持数据 + `UNGP_RuleTextProvider` | 把 i18n 从"构造期"挪到"渲染期",缺行降级为占位符而不是 NPE |
| 网络反馈 IO(`:796-850`) | `UNGP_RuleUsageReporter`,且**不要在 `loadAllRules` 里启动线程** | 现在 `loadAllRules:785` 内部调 `loadPickTimes:796`,后者起 daemon 线程做 HTTPS 并**无同步地写 `ALL_RULES` 里的 `pickedTimes`**(:811 遍历 `ALL_RULES`)——跨线程写可变列表里的可变字段,且失败只 catch `JSONException/IOException` |
| 难度静态量(`:210 setStaticDifficulty`、`:64 globalDifficulty`) | 保留(全局单例语义) | — |

补充:`:63` 的 `isSpecialistMode` 字段已无使用点(死字段);`:356` `getBuffID()` 已 `@Deprecated` 但仍与 `:319` 的 `buffID = "ungp_" + id`(小写)和 `UNGP_BaseRuleEffect:49` 的 `"UNGP_" + id`(大写)三套命名并存 —— 后者是 `modifyMult` 的 key,必须精确匹配,任何规则若混用会导致"改了但撤不掉"。

---

## 5. Q4:公共 API 向后兼容

| 项 | 结论 | 证据 |
| --- | --- | --- |
| 2.6.0 是否破坏规则 API 二进制兼容 | **否**。`git diff HEAD -- src/ungp/api/` 只有新增文件 `UNGP_CombatRulesAPI.java`,`UNGP_RuleEffectAPI`/`UNGP_BaseRuleEffect`/12 个 tag 接口均未改动 | `git diff HEAD --stat` |
| 新增存档字段是否兼容旧档 | **是**。`blockCombatRulesInNextBattle` 加了 `aliasAttribute(...,"bCRNB")`(`UNGP_modPlugin.java:103`);缺属性时 XStream 用构造器默认 `false`(`UNGP_InGameData.java:31`) | `UNGP_modPlugin.java:92-103` |
| `getDataInSave()` 抛异常的旧约定 | **对第三方不友好**:`UNGP_InGameData.java:39-42` 抛 `RuntimeException`。本次新增 `getDataInSaveOrNull():46` 是正确方向,但**战斗插件仍在用抛异常版本**(`UNGP_SpecialistCombatPlugin.java:43`),见 6.1 | — |
| 规则 id 改名/删除 | **半兼容**。`getActivatedRules:111-119` 静默丢弃不存在的 id(安全);`saveActivatedRules:98-101` 对 `getByID==null` 判空跳过(安全)。但"写到公共对象上的效果"无法回收(见 L3),且 `ruleDataMap` 槽位永久残留 | — |
| 新增 tag 接口 | 破坏面 = 0(空标记接口,实现方只在自身类上加 `implements`)。但新增 tag **不会自动生效**,必须改 4~5 个文件(见 3.1) | — |
| `UNGP_BackgroundPluginAPI`(`src/ungp/api/backgrounds/UNGP_BackgroundPluginAPI.java:13`) | 纯 interface,未来新增方法会破坏第三方实现;当前 2.6.0 未新增 | 该文件不在 diff 中 |
| `getDescriptionParams` 契约 | 返回 `null` 表示"没有该参数"(`UNGP_BaseRuleEffect` 默认分支);而 `URule:469` 固定取 10 个 → 第三方无法扩展参数个数 | — |
| `UNGP_CombatRulesAPI` 语义 | `blockInNextBattle()` 在非生涯静默 no-op(`getDataInSaveOrNull()==null`),与 javadoc 一致 ✔ | `UNGP_CombatRulesAPI.java:30-35` |

---

## 6. Q5:2.6.0 在途改动缺陷清单

在途文件:`git status` 显示 11 个 src 文件(+954 行)。逐文件结论如下。

### 6.1 战斗规则屏蔽通道(核心新功能)

`src/ungp/scripts/plugins/UNGP_SpecialistCombatPlugin.java:39-86`:

```java
43: UNGP_InGameData inGameData = UNGP_InGameData.getDataInSave();       // ← 抛异常版本
45: if (inGameData != null && engine.isInCampaign() && inGameData.isBlockCombatRulesInNextBattle()) {
46:     inGameData.setBlockCombatRulesInNextBattle(false);                 // ← 先消费
47:     if (inGameData.isHardMode()) { isRulesBlocked = true; ...; return; }
52: }
53: if (inGameData != null && inGameData.isHardMode()) { ... }
```

| 编号 | 级别 | 位置 | 机制 |
| --- | --- | --- | --- |
| F1 | **P1** | `:43` | 用的是会抛 `RuntimeException` 的 `getDataInSave()`(实现在 `UNGP_InGameData.java:39-42`:`Global.getSector().getPersistentData()` 缺 `UNGP_cam` 就抛)。战斗引擎初始化路径上抛未捕获异常 = 战斗开始崩溃。本次已经为 API 新增了 `getDataInSaveOrNull()`,这里却没换 → 建议改为 `getDataInSaveOrNull()` 并在 null 时跳过 |
| F2 | P2 | `:45-52` | 标记在"是否硬模式"判断**之前**就被消费。非硬模式下:标记清空、`isRulesBlocked` 不置位、无任何提示,而 `isBlockedInNextBattle()` 已返回 false → 调用方无法得知"我已消费"。当前非硬模式本来就不应用战斗规则,所以行为等价,但 API 语义被静默破坏 |
| F3 | **P1** | `:58-59` | `originalRules = new ArrayList<>(...)` 后直接 `tweakBeforeApply(activatedRules, originalRules)`,与 `UNGP_TweakBeforeApplyCombatTag.java:14` javadoc 的 `Unmodifiable` 契约冲突(生涯侧 `RulesManager:153` 传的是 `List.copyOf`)。同一接口两种可变性 |
| F4 | P2 | `:60-64` | 只遍历 `originalRules` 收集 tweak,但 tweak 可能**新增**带 tweak 的规则 → 新增的那条不会被 ask 第二次。一阶层级足够,可接受,建议注释说明"tweak 不级联" |
| F5 | P2 | `:65-83` | `tags.add((UNGP_CombatTag) ruleEffect)` 装的是**规则单例**,跨战斗复用;规则若有实例字段存战斗状态会串场。本次新规则都用 `engine.getCustomData()` 存状态(`UNGP_IHaveActivated.java:32`、`UNGP_VengefulSpirit.java:39-55`)✔,但框架层没有强制 |
| F6 | P2 | `:112-128` | 每帧 `ships × tags` 双重循环;100 舰 × 8 tag × 60fps ≈ 4.8 万次虚调用/秒。当前可接受,建议按 tag 分桶只在有规则时进循环 |
| F7 | P3 | `:31 / :90` | 字段 `init` 与父类方法 `init(CombatEngineAPI)` 同名,可读性差;`:109` 用的是本类字段 `isSpecialistMode`,与 `UNGP_SpecialistManager.isSpecialistMode()` 同名不同义 |

**一次性消费语义核对(simulated / auto-resolve)**:
- 真实生涯战斗:`engine.isInCampaign()==true` → 消费 ✔(`:45`)
- 战斗模拟(`isInCampaignSim()`):不消费 ✔(与 javadoc `UNGP_CombatRulesAPI.java:10` 一致)
- 自动结算(不创建 `CombatEngineAPI`):`init` 不调用 → 不消费 ✔(与 javadoc 一致)
- 战役任务/主菜单战斗:`isInCampaign()` 为 false,且 `getDataInSaveOrNull()` 返回 null → API no-op ✔
**结论:通道语义与文档自洽,无功能错误。** 唯一真实缺陷是 F1 与 F3。

### 6.2 `UNGP_BountyHunter`(`src/ungp/impl/rules/other/UNGP_BountyHunter.java`)

| 编号 | 级别 | 位置 | 机制 |
| --- | --- | --- | --- |
| F8 | **P1** | `:50-52` + `:58-70` | `advanceInCampaign` **每帧**调 `updateBounties()`,每次做 4 次全量扫描(`getIntel(PersonBountyIntel)`、`getCommQueue(PersonBountyIntel)`、`getIntel(BaseCustomBounty)`、`getCommQueue(BaseCustomBounty)`)+ `keySet().removeIf`。intel 数百时每帧 O(4N);且 `applyGlobalStats:45-47` 又调一次。应改到 `params.isOneDayPassed()` 或 `economyUpdated` |
| F9 | **P1/P0?** | `:59-63` | 用 `WeakHashMap<BaseIntelPlugin,Integer>` 做"已加奖金"账本并 `saveDataInCampaign(0, ...)` 写进存档。弱引用语义与"必须能减回"的账本语义冲突:key 被 GC 回收后,`unapplyGlobalStats:110-115` 遍历不到该项,而奖励已永久加到 intel 上 → 信用点膨胀。`:60-63` 的类型纠正 + 复制还把"序列化回来的旧 key"一起复制(复制后 key 仍是反序列化实例) |
| F10 | **P1** | `:78` | `appliedBonuses.containsKey(bounty)` 依赖**对象恒等**(`BaseIntelPlugin` 未覆写 `equals`)。读档后若 key 实例与 `IntelManager` 中的实例不重合(XStream 引用跟踪未保住时),`containsKey` 返回 false → 同一赏金**二次加奖金**。这是 F9 的同源风险,建议改成用 `intel.getId()` 之类稳定键 |
| F11 | P2 | `:64` | `removeIf(!isActive)` 会丢弃"暂时不活跃"赏金的记录,但奖励增量已写进 intel → 记录丢失即无法撤回;若该 intel 之后重新变活跃(`isActive` 再次为真),`containsKey` 为 false → 再加一次。当前 `BaseCustomBounty.Stage` 单向推进使该路径**基本不可达**,但账本设计脆弱 |
| F12 | P2 | `:74-76` | 用精确类比较(`!= PersonBountyIntel.class`),子类(intel 继承/extend 的第三方赏金)全部跳过。扩第三方不友好,建议 `isAssignableFrom` + 白名单配置 |

### 6.3 `UNGP_SlipperyModification`(`src/ungp/impl/rules/member/UNGP_SlipperyModification.java`)

| 编号 | 级别 | 位置 | 机制 |
| --- | --- | --- | --- |
| F13 | **P1/P0?** | `:290-294` `cleanUp` | 清空 `TRIGGERED_SLOT`(已惩罚舰船 id 集合)。规则被移除(`UNGP_RepickRulesDialog` 改规则 / 难度切换)时 cleanUp 触发;之后再重拾同一规则,`applyGlobalStats:145-152` 重建空集合 → **同一艘船会再吃一次 D-mod**。D-mod 与 `setDHull` 是玩家舰队真实 variant 的持久变更(`:221-246`),叠加不可逆 |
| F14 | P2 | `:76-80` | 结算挂在 `reportPlayerOpenedMarket`(进港)与 `:157` 每日任务上,但 `applyDMods` 会调 `DModManager.setDHull` + `member.updateStats()` + `setSyncNeeded`(`:236/:242/:244`)。在"打开市场"回调里改舰队数据,若市场界面已缓存 fleet 信息可能出现刷新时序问题(建议在 `advanceInCampaign` 的日事件里做,`:155-159` 已有该入口) |
| F15 | P2 | `:58` `getListener():96-98` | `Listener` 是非静态内部类,且 `getListener()` **每次 new**;移除靠 `removeListenerOfClass(getClassOfListener())`(`RulesManager:129`),而 `getClassOfListener()` 返回**接口** `SlipperyListener.class`(:101)。能否移除成功取决于 `ObjectRepository.getList(Class)` 的匹配语义(本轮无法从可用的反编译源码核实,列**待复核**) |
| F16 | P3 | `:275-288` | `showGains` 用 `rule.getExtra2()` 做 `addLine` 的格式串,CSV 里 `"%s：%s"` 已是全角冒号,英文 CSV 用半角 —— 一致 ✔ |

### 6.4 `UNGP_HumanNatureTest` / `UNGP_KeepTheWayClear` / `UNGP_IHaveActivated` / `UNGP_VengefulSpirit`

| 编号 | 级别 | 位置 | 机制 |
| --- | --- | --- | --- |
| F17 | P2 | `UNGP_KeepTheWayClear.java:24-34` | 每帧遍历 `location.getFleets()` 并对每个 `Misc.isPatrol` 舰队写 `MEMORY_KEY_PATROL_ALLOW_TOFF` + `$ungp_keepTheWayClear`(`:14`)。**没有势力过滤** → 中立/友方巡逻队也被改;每帧写 MemoryAPI 产生大量短命条目。建议只对敌对势力 + 改到日事件 |
| F18 | P2 | `UNGP_IHaveActivated.java:104-112` | 每帧对**每艘非玩家舰**调 `removeListenerOfClass(ActivationListener.class)`,对玩家舰 `hasListenerOfClass`;internally 都是遍历 listener 仓库。可只在"玩家舰变化"时执行一次 |
| F19 | P3 | `UNGP_IHaveActivated.java:45-49` | `String.format(rule.getExtra2(), String.format("%.1f", ...))` 未指定 `Locale`,小数逗号 locale 下显示 `12,3` |
| F20 | P3 | `UNGP_IHaveActivated.java:114` | `ActivationListener` 是非静态内部类,持有规则单例隐式引用;被加到 `ShipAPI` 上,飞船销毁即回收,不构成泄漏 |
| F21 | P2 | `UNGP_VengefulSpirit.java:30` | `init(engine)` 里往引擎 listener 加 `LethalHitListener`,随引擎销毁 ✔;`:39-55` 用 `getCustomData` 做一次性标记 ✔ |
| F22 | P2 | `UNGP_HumanNatureTest.java:57-62` | 用"ruleData 槽是否为 null"当"首次应用"标志,而 `applyGlobalStats` 每次缓存刷新都会调(不是首次);配合 `:137-140 cleanUp` 清槽,规则重拾会重置 60 天宽限期。设计可接受,但语义应写进注释 |

### 6.5 框架/存档层(在途改动的配套部分)

| 编号 | 级别 | 位置 | 机制 |
| --- | --- | --- | --- |
| F23 | P2 | `UNGP_modPlugin.java:78` | `x.registerConverter(new MapConverter(x.getMapper(), WeakHashMap.class), XStream.PRIORITY_LOW)`:**注册基本无效**。XStream 默认已用 `MapConverter(mapper)`(PRIORITY_NORMAL)覆盖所有 `Map` 子类,低优先级永远不会被查;若意图是"让 WeakHashMap 正确序列化",默认已满足;若意图是自定义 key 处理,则没生效。需作者确认意图,否则删除 |
| F24 | P2 | `UNGP_modPlugin.java:106-110` | `registerLegacyBountyAlias` 先 `serializedClass()` 取当前名,再 `alias(legacy)`,再 `alias(current)`。效果依赖 `DefaultMapper.alias` 是否刷新 class→name 缓存(第二次 alias 把缓存改回全限定名→"只读旧名、写新名",正是想要的行为)。建议加注释说明顺序依赖,否则后人调整顺序会改变序列化名 |
| F25 | P2 | `UNGP_RulesManager.java:796-850` | `loadPickTimes` 在 `loadAllRules` 内启动 daemon 线程做 HTTPS 并遍历 `ALL_RULES` 写 `pickedTimes`(:811),无同步、只 catch `JSONException/IOException`(运行时异常会让 daemon 静默死亡,但若在写列表途中抛,主线程可能看到半更新状态)。加载路径不该带网络 IO |
| F26 | P3 | `RulesManager.java:63` | `isSpecialistMode` 静态字段已无使用点(死字段) |
| F27 | P2 | `UNGP_BaseRuleEffect.java:219-241` | 数据槽键 = `rule.getId() + slot`(int 直接拼接)。当前全项目无碰撞,但"id 以数字结尾 + slot=10"与"id 不以数字结尾 + slot 是两位数"存在潜在命名空间碰撞(如 `foo1`+slot 0 → `foo10` 与 `foo`+slot 10 → `foo10`)。建议 `rule.getId() + "_" + slot` |
| F28 | P3 | `data/config/settings.json:2-4` | 文件带尾逗号,非严格 JSON;但 Starsector 用宽容 `org.json`,Nexerelin/MagicLib 同样如此 → **不算缺陷**,仅记录以免后人误改 |

---

## 7. 汇总表

### P0
无(本轮无可确定性复现条目)。疑似项:S-1(`UNGP_BountyHunter.java:59-63`)、S-2(`UNGP_SlipperyModification.java:290-294`),需 Lead 复核。

### P1(12 条)

| # | 位置 | 机制 |
| --- | --- | --- |
| P1-1 | `UNGP_SpecialistCombatPlugin.java:43` | 战斗初始化路径用会抛 `RuntimeException` 的 `getDataInSave()`,异常即战斗崩溃;已有 `getDataInSaveOrNull()` 却未使用 |
| P1-2 | `UNGP_SpecialistCombatPlugin.java:58-59` | `tweakBeforeApply` 第二参可变,违反 `UNGP_TweakBeforeApplyCombatTag.java:14` 的 `Unmodifiable` 契约(生涯侧不可变) |
| P1-3 | `UNGP_RulesManager.java:122-143` + `UNGP_RepickRulesDialog.java:100-107` | 卸载是"全量 ALL_RULES unapply"补偿模型;改规则时先 `saveActivatedRules`(触发 cleanUp)后 `updateRulesCache`,cleanUp 若清掉 unapply 依赖的数据 → stat 永久残留 |
| P1-4 | `UNGP_FullOfCompany.java:23-29`、`UNGP_TachLabour.java:23-28` | 只改势力关系、无 `unapplyGlobalStats`;规则移除后 FRIENDLY/COOPERATIVE 永久保留 |
| P1-5 | `UNGP_InGameData.java:27` + `UNGP_NoWayBack.java:29-36` | 效果写到随存档序列化的 `changeTimeStat`(MutableStat);规则 id 改名/删除后 `unapply` 永不执行 → 每年重置次数永久放大 |
| P1-6 | `UNGP_BountyHunter.java:50-52, 58-70` | 每帧 4 次 intel 全量扫描 |
| P1-7 | `UNGP_BountyHunter.java:59-63, 78` | WeakHashMap 账本 + 对象恒等判重:GC 或读档引用失配后奖金无法撤回/二次叠加 |
| P1-8 | `UNGP_SlipperyModification.java:290-294` | 规则重拾后对同一艘船重复施加 D-mod(不可逆资产变更) |
| P1-9 | `UNGP_StillGrowingUp.java:71` | `Math.min(6 - \|cost\|, 1)` 应为 `Math.max`,当前权值恒为 1,"cost 越低越可能"未实现(正向规则 cost ∈ {-4..0},见 CSV 46 条正向规则) |
| P1-10 | `UNGP_RulesManager.java:178` | 规则归类互斥:同时实现 `CombatTag` 与 `CampaignTag` 的规则 `advanceInCampaign` 静默不执行,无任何告警 |
| P1-11 | `UNGP_RulesManager.java:129` + tag 接口 `getClassOfListener` 返回接口 | `removeListenerOfClass(接口)` 能否命中取决于 `ObjectRepository.getList` 语义,**待复核**(反编译源码缺该类) |
| P1-12 | `UNGP_KeepTheWayClear.java:24-34`、`UNGP_IHaveActivated.java:104-112` | 每帧全舰队遍历 + 写 MemoryAPI / 每帧对全舰做 listener 类扫描 |

### P2(17 条)
3.2 的 D1/D2/D4/D5/D6/D7/D8/D9;4.2 的三条 i18n 耦合;6.1 的 F2/F4/F5/F6;6.2 的 F11/F12;6.3 的 F14/F15;6.4 的 F17/F18/F21/F22;6.5 的 F23/F24/F25/F27 —— 详见对应章节。

### P3(6 条)
F7、F16、F19、F20、F26、F28;以及 `UNGP_RulesManager.java:356` 的 `@Deprecated getBuffID()` 与 `"ungp_"`/`"UNGP_"` 双前缀并存。

---

## 附录 A · 证据索引(本轮实际读取)

- 契约层:`src/ungp/api/rules/UNGP_RuleEffectAPI.java`、`UNGP_BaseRuleEffect.java`、`UNGP_MemberBuffRuleEffect.java`、`src/ungp/api/rules/tags/*.java`(12 个)、`src/ungp/api/backgrounds/UNGP_BackgroundPluginAPI.java`、`UNGP_BaseBackgroundPlugin.java`
- 管理器:`src/ungp/scripts/campaign/specialist/rules/UNGP_RulesManager.java`(952 行全文)、`UNGP_RuleInfoLoader.java`
- 存档/生命周期:`UNGP_InGameData.java`、`UNGP_SharedData.java`、`UNGP_CampaignPlugin.java`、`UNGP_modPlugin.java`、`UNGP_PlayerFleetMemberBuff.java`、`UNGP_SpecialistManager.java`、`UNGP_SpecialistSettings.java`、`UNGP_SpecialistSkills.java`
- 战斗:`src/ungp/scripts/plugins/UNGP_SpecialistCombatPlugin.java`、`src/ungp/api/UNGP_CombatRulesAPI.java`、`impl/rules/combat/UNGP_IHaveActivated.java`、`UNGP_VengefulSpirit.java`
- 在途新规则:`impl/rules/fleet/UNGP_HumanNatureTest.java`、`UNGP_KeepTheWayClear.java`、`impl/rules/member/UNGP_SlipperyModification.java`、`impl/rules/other/UNGP_BountyHunter.java`、`UNGP_FullOfCompany.java`、`UNGP_TachLabour.java`、`UNGP_NoWayBack.java`、`UNGP_StillGrowingUp.java`
- UI/其他:`UNGP_RepickRulesDialog.java`、`UNGP_InteractionDialog.java`、`UNGP_ChallengeInfo.java`、`UNGP_ChallengeManager.java`、`UNGP_ChallengeLoader.java`、`UNGP_BackgroundIntel.java`、`UNGP_BackgroundManager.java`、`UNGP_CodexManager.java`、`UNGP_EconomyListener.java`、`SimpleI18n.java`、`Constants.java`、`UNGPUtils.java`
- 数据:`data/campaign/UNGP_rules.csv`(102 行/46 条正向规则)、`data/campaign/UNGP_challenges.csv`(5 行)、`data/config/settings.json`、`data/config/LunaSettings.csv`
- 引擎语义(部分):`D:/StarsectorDevelopment/098/Starsector/apis/starsector-deobf/src/com/fs/starfarer/campaign/ListenerManager.java`(transient/persistent 双桶、`removeListenerOfClass` 实现)
