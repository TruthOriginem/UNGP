# 04 构建、格式、仓库卫生与版本一致性审计

- 审计对象: UnofficialNewGamePlus(Starsector 0.98a, gameVersion 0.98a-RC8),目录 D:\StarsectorDevelopment\098\Starsector\mods\UnofficialNewGamePlus
- 审计范围: build.gradle / settings.gradle / gradle.properties / .gitignore / UnofficialNewGamePlus.iml / mod_info*.json / UNGP.version / changelog.txt / README.md / AGENTS.md / tools/** / 根级 *.py / data/** 的实际格式状态 / git 追踪与工作区状态
- 审计方式: 只读审计 + 命令执行(全队唯一允许跑 gradle 的成员)。未修改 src/、data/ 等任何源文件;仅允许的写副作用是 `gradle jar` 重新生成 `jars/ungp.jar`(构建产物)与 `gradle clean` 清除 `build/`(gitignore 内的构建目录)。
- 环境: Windows 11,Gradle 9.6.1(本机 C:\gradle-9.6.1\bin\gradle,无 wrapper),JDK 17.0.20(Azul, C:/Users/55381/.jdks/azul-17.0.20),Python 3.14.6。注意 PATH 里没有 java/JAVA_HOME,必须显式设置才能跑 Gradle。

## 0. 结论速览

| 级别 | 数量 | 条目 |
| --- | --- | --- |
| P0 | 0 | 未发现会导致存档损坏/崩溃/数据错误的构建层问题。唯一可能的 P0 面(jar 与源码不一致)实测为一致,见 1.3 |
| P1 | 4 | ① 格式门禁 `gradle verifyFormat` / `gradle build` 在当前环境必然失败;② mod_info_ENG.json 版本 2.5.0 与其余 2.6.0 不一致,且依赖声明与主文件不同;③ `jars/ungp.jar` 入库并随功能提交改动,直接违反 AGENTS.md:31;④ 仓库内 distZip 打包内容错误(打包未入库脚本/源素材,漏掉 changelog 承诺的语言切换脚本) |
| P2 | 8 | 格式化工具链覆盖缺口、无 wrapper/无 toolchain、依赖目录硬编码且大小写敏感、MagicLib 为无用依赖、mod_info 的 LAX 解析双份实现、.gitignore 缺口(dist/ 等)、索引里 2 个"已删未撤"的 png、formatProject 无 dry-run 且 Exec 任务无输入输出声明 |
| P3 | 10+ | 见第 4、5 节 |

一句话总结: 编译与打包是本仓库最健康的一环(clean 编译通过、jar 可复现、class 版本与游戏 JRE 匹配),问题集中在 **格式门禁在本机永远失败**、**发布元数据(ENG 版本/依赖)与发布打包内容失真**、以及 **构建产物入库** 三处。

## 1. 必答问题 1: 构建可复现性与打包正确性

### 1.1 主命令的实际结果(命令 -> 退出码 -> 关键输出)

由于本会话 PATH 中没有 java、JAVA_HOME 也未设置,第一次执行 gradle 直接失败:

    $ gradle -v
    ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
    Please set the JAVA_HOME variable in your environment to match the location of your Java installation.
    -> exit 1

设置 JAVA_HOME=C:/Users/55381/.jdks/azul-17.0.20 后(Gradle 自报 "Launcher JVM: 17.0.20 (Azul Systems, Inc.)"),各命令结果:

| # | 命令 | 退出码 | 关键输出 |
| --- | --- | --- | --- |
| 1 | gradle -v | 0 | Gradle 9.6.1,Launcher JVM 17.0.20,OS Windows 11 amd64 |
| 2 | gradle compileJava | 0 | :compileJava UP-TO-DATE,BUILD SUCCESSFUL in 10s |
| 3 | gradle clean compileJava | 0 | BUILD SUCCESSFUL in 6s,**4 条警告**(见下) |
| 4 | gradle verifyFormat | **1** | :checkUtf8Bom 通过(BOM found: 0),:checkLf FAILED -> LF needed: 183 |
| 5 | gradle build | **1** | :checkLf FAILED -> Process 'command 'python'' finished with non-zero exit value 1,BUILD FAILED |
| 6 | gradle jar | 0 | :jar,BUILD SUCCESSFUL in 900ms |
| 7 | gradle jar --rerun-tasks | 0 | 同 #6,输出 sha256 完全一致 -> jar 内容可复现 |
| 8 | gradle compileJava --offline | 0 | BUILD SUCCESSFUL -> 无网络依赖,可离线构建 |
| 9 | gradle distZip --dry-run | 0 | 任务图 :compileJava -> :jar -> :distZip(未真正出包,避免在仓库产生 dist/) |

clean 编译的 4 条警告原文(Windows 控制台是 GBK,已转码):

    警告: 未知的枚举常量 DeprecationLevel.HIDDEN
      原因: 找不到kotlin.DeprecationLevel的类文件          (同类警告共 4 条)
    注: 某些输入文件使用或覆盖了已过时的 API。
    注: 某些输入文件使用了未经检查或不安全的操作。

警告来源已定位:编译类路径里只有 mods/MagicLib/jars/MagicLib-Kotlin.jar 含 kotlin/ 条目(36 个),而 kotlin-stdlib 不在 classpath 上。根因是 build.gradle:28 把 MagicLib 目录整体加入 compileOnly,而本仓库源码完全没用到 MagicLib(见 6.4)。

### 1.2 verifyFormat 失败的根因: 工作区 CRLF + 缺 .gitattributes(不是源码内容问题)

- :checkLf 报告 183 个文件需要 LF,从 changelog.txt、mod_info.json 一直到 src/**/*.java、data/**/*.csv。
- git ls-files --eol 显示:**入库内容全部是 LF**(184 个 i/lf w/crlf + 31 个 i/lf w/lf,0 个 i/crlf),即仓库 blob 没问题,是**工作区检出被转成了 CRLF**。
- 直接原因:本机 git 配置 core.autocrlf=true(git config --show-origin -> file:C:/Users/55381/.dsh/.../etc/gitconfig);仓库内**没有 .gitattributes**(ls .gitattributes -> 不存在),仓库 config 与用户全局 config 也都没有 autocrlf 设置(所以这个 autocrlf 来自当前 shell 的 git 环境)。
- 后果:只要在 Windows 上用 core.autocrlf=true(Git for Windows 安装默认值之一)检出,gradle verifyFormat / gradle build / gradle check 就 100% 失败,与代码质量无关。这是**门禁可用性问题,不是代码风格问题**。
- 逐项分项实测(按 tools/formatter 的算法,在"先把 CRLF 归一为 LF"的内容上重算,以剥离 autocrlf 噪声):BOM 0 个;LF 归一后 Java 缩进违规 7 个文件;Java 文件末尾违规 10 个文件;非 Java 文本文件末尾违规 15 个文件。清单见第 4 节。
- 单个脚本直跑(只读 --check)的原始计数:utf8_no_bom.py 0;to_lf.py 183;java_spaces.py 160;java_single_final_blank_line.py 160;remove_pycache.py 0。其中 160 是把 CRLF 也算成差异导致的(见 tools/formatter/java_spaces.py:19-21 先归一 CRLF 再比字节),不能当作真实缩进违规数——真实数是 7。

### 1.3 jars/ungp.jar 入库是否合理

事实:

- git ls-files 中 .jar 只有 jars/ungp.jar,且本次状态为 M(jars/ungp.jar):工作区 451034 B vs HEAD 423465 B(+27569 B)。
- HEAD 的 jar 与其提交源码**严格一致**:从 HEAD:jars/ungp.jar 解出 165 个顶层类,与 git ls-tree -r HEAD 的 165 个 .java 一一对应(无多、无缺)。
- 工作区 jar 与源码也基本一致:我重建前的旧 jar 与重建结果只差 ungp/api/UNGP_CombatRulesAPI.class(Defl:N 499 -> 501 B),说明维护者提交前确实会重建 jar。
- 当前 jar:290 个 zip 条目 / 172 个顶层类,与 src 下 172 个 .java 一一对应,无残留 class;class 文件 major version 61(Java 17),与游戏自带 jre/bin/java.exe(OpenJDK 17.0.10)匹配,不存在运行期 class 版本不兼容。

结论:**不建议继续入库**,理由是政策与流程而非当前一致性:

1. AGENTS.md:31 明确写"不要提交编译生成的 jar、分发包或 Gradle 缓存",而 jars/ungp.jar 正是被追踪的编译产物,本次又被修改 -> 仓库规则与实际做法自相矛盾(P1)。
2. 二进制 blob 无法 review,每次功能提交都在 git 里新写一份约 450 KB blob(该仓库目前 4731 个松散对象 / 29.85 MiB 且从未打包,git count-objects -vH 显示 size-pack: 0),长期推高仓库体积并拖慢 git 操作。
3. 正确做法:jar 作为 CI/本地构建产物进发布 zip(distZip 或 AutoPack),在 .gitignore 增加 /jars/*.jar(或把 jar 任务输出改到 build/),由发布流程生成。

补充:**重建可复现性良好**。#6 与 #7 两次构建 sha256 都是 de8d1e53eef74f246ecbf4c5feb2c72f7cac3b109776fc745eb726ec652b330d,zip 条目时间戳统一为 1980-02-01 00:00(Gradle 9 默认 reproducible archive),说明"同一份源码 -> 同一份 jar"成立;因此"入库 jar 是否陈旧"完全可以用构建校验来保证,没必要用入库来保证。

### 1.4 formatProject / verifyFormat 的改动面与误伤风险

- verifyFormat = checkUtf8Bom -> checkLf -> checkJavaSpaces -> checkJavaEof -> checkPycache 的依赖链(build.gradle:123-172),只读,遇到第一个失败即停(build.gradle:174-176 让 check 依赖它,所以 gradle build 会被格式门禁挡住)。
- formatProject 是同一条链的**写盘版**(build.gradle:162-166):**没有 dry-run/预览/文件清单确认**,一次调用会改写整个仓库的文本文件。当前仓库若执行,实际改动面 = 183 个文件的行尾 + 7 个 Java 的 tab 缩进 + 10 个 Java 的文件尾 + 可能的 __pycache__ 清理;其中仅 15 个 Java 文件存在"真实内容变化"(7 个 tab 与 10 个文件尾有 2 个文件重叠:UNGP_Sneaker.java、UNGP_Thermodynamics.java),其余是纯行尾噪声。因此**误伤风险主要是"把 183 个文件的 CRLF 噪声混进业务提交"**,而不是改坏代码。
- 潜在(当前未触发)误改风险:java_spaces.py:6-8 把每行行首 tab 一律换成 4 空格,不区分"代码行/文本块内容行"。当前 src 中 0 个 Java 文本块(检索三引号为 0 命中),所以还没有实际影响;一旦以后使用文本块,行首 tab 会被静默改写成空格,直接改变字符串字面量内容——属于工具设计隐患(P3)。
- 另一侧:这些 Exec 任务没有任何 inputs/outputs 声明(build.gradle:84-160 全都只有 commandLine),所以每次调用都无条件执行,无法 UP-TO-DATE、无法进 build cache(P3)。

### 1.5 distZip 的打包内容与 jar 目录风险

distZip(build.gradle:56-74)的规则是:from(rootDir) + include data/**、graphics/**、jars/**、sounds/**、mod_info.json、mod_info_ENG.json、根级 *.txt/*.version/*.ini,仅排除 **/*.7z。按此规则静态模拟,会打包 **173 个文件 / 3,329,061 B**,其中存在三个问题:

1. **把未入库的脚本打进发布包**:data/campaign/CSV_id_order.py(455 B)被 git check-ignore -v 证实由 .gitignore:1 的 *.py 忽略、未入库,但 data/** 会把它打进 zip -> 发布产物里有 VCS 里不存在的文件。
2. **把源素材打进发布包**:graphics/ungp/icons/UNGP PS Blend.zip(2,719 B,2022 年的 PS 源文件)会随 graphics/** 发布。
3. **漏掉 changelog 承诺的用户可用脚本**:changelog.txt:386 与 changelog.txt:392 明确写"仍然可以通过 mod 目录下的 swapLangFile 进行语言切换",但 swapLangFile-切换语言.py 在仓库根、被 *.py 忽略,且不在 distZip 的任何 include 规则内 -> 用 distZip 出的包**没有这个脚本**,文档与产物不一致。

另外两条打包层面的脆弱点:

- include jars/** 会打包 jars/ 目录下的**所有**文件,而 jar 任务(build.gradle:51-54)的输出目录就是 jars/。今天 jars/ 只有 ungp.jar 所以结果正确;但一旦出现改名/换版本的残留 jar(例如手工备份的 ungp-old.jar),会被静默打进发布包。这是"产物目录同时是源目录"的结构性风险,不是严格意义上的任务循环依赖(distZip 已 dependsOn "jar",见 build.gradle:59)。
- distZip 同时打包 mod_info.json 与 mod_info_ENG.json,而 ENG 文件的版本是 2.5.0(见第 2 节),等于把一份过期的发布元数据一起发出去。
- dist/(build.gradle:61 的输出目录)**没有被 .gitignore 覆盖**,一旦执行 distZip,生成的 zip 会成为未追踪文件、极易被误提交。

## 2. 必答问题 2: 版本一致性

| 位置 | 值 | 结论 |
| --- | --- | --- |
| mod_info.json:5 | {"major": 2, "minor": 6, "patch": 0} | 通过,2.6.0(权威,AGENTS.md:10) |
| UNGP.version:6-10 | major 2, minor 6, patch "0" | 版本一致(2.6.0),但 **patch 是字符串 "0"**,与 major/minor 的数字类型不一致 |
| changelog.txt:1 | 2.6.0 2026/11/3 | 通过,与 HEAD 提交日期 2026-11-03 一致(git log -1 -> 08482f7 2026-11-03 14:12:34 +0800 2.6.0 update) |
| data/config/version/version_files.csv:1-2 | "version file" / "UNGP.version" | 指向 UNGP.version(文件 26 B,有结尾换行);但**消费者无法确认**(见下) |
| mod_info_ENG.json:5 | {"major": 2, "minor": 5, "patch": 0} | **不一致:2.5.0,落后一个版本** |
| mod_info.json:11 / UNGP.version:11 | "0.98a-RC8" / "0.98a-RC8" | 两处一致;游戏侧版本常量未能从本机文件读出,此项只做了"两文件互相一致"的核对 |

**P1: mod_info_ENG.json 版本与依赖双失真。**

- 版本 2.5.0 落在 mod_info_ENG.json:5。这个文件不是给人看的注释文件,而是 swapLangFile-切换语言.py 的互换对象:该脚本用 os.path.splitext 找同名 + _ENG 的配对(swapLangFile-切换语言.py:17-19),mod_info.json 与 mod_info_ENG.json 正好构成一对。也就是说**切到英文发行时,游戏读到的版本会变成 2.5.0**,与 changelog/UNGP.version 宣称的 2.6.0 冲突。
- 依赖也不同:mod_info.json:6-9 声明 LazyLib + MagicLib;mod_info_ENG.json:6-8 只有 LazyLib。两者必须人工同步,没有自动校验(P2:应当用构建期校验任务保证两份 mod_info 的 id/version/dependencies 一致)。

**LAX 解析与尾逗号脆弱点(按要求记录)。**

- mod_info.json:14 是 "modPlugin": "ungp.scripts.UNGP_modPlugin",——右花括号前多一个逗号;mod_info_ENG.json:12 同样有一个尾逗号。严格 JSON 解析器会直接报错。
- 构建脚本对此的应对是显式开启宽松解析:build.gradle:12-18 用 JsonSlurper().setType(JsonParserType.LAX) 解析 mod_info.json;settings.gradle:17-23 又原样重复了一遍同样的 8 行。
- 实测该写法在生态里常见,不是孤例:对 mods/ 下 24 个 mod_info.json 做严格 JSON 校验,15 个通过、**9 个失败**(含 Nexerelin、MagicLib、LazyLib、GraphicsLib、Mirfak Parcel Service、Polaris_Prime、PFT 等,失败原因多为尾逗号或未加引号的键)。据此**不能断言**游戏会因此拒绝加载(相反,同级大 mod 普遍如此,说明游戏侧解析是宽松的),但它确实会打断所有标准 JSON 工具链(IDE 校验、CI、mod 索引站、GitHub 语法检查),并把"文件是否可解析"的风险绑在 build/settings 这两处 LAX 开关上 -> P2,建议直接去掉尾逗号(一行改动,零收益损失)。
- settings.gradle:17-23 在**配置期**解析 mod_info.json(为了拿 rootProject.name)。任何需要配置项目模型的 gradle 调用都要先过这一步;mod_info.json 一旦损坏,除 gradle -v 外的命令会一起失效,且报错是 Groovy JSON 异常而非"mod_info.json 第 X 行有问题"。同一逻辑在 build.gradle:12-18 重复存在,属重复实现(P2)。

**version_files.csv 的可疑性(P3)。** data/config/version/version_files.csv 内容只有两行(version file / UNGP.version),但这个路径在本安装中找不到消费者:游戏根与 starsector-core/ 下都**没有** data/config/version/version_files.csv(深度 6 内 find 无命中);对 starsector-core/*.jar 与游戏根 *.jar 共 **19 个 jar** 的全部 class 做字符串检索,version_files / masterVersionFile / "version file" **0 命中**;mods/ 下 24 个 mod 中没有任何一个提供同路径文件。结论:该文件很可能是模板遗留的死配置,或者依赖某个本机未安装的读取方;建议人工确认后删除或在文档中写清用途(不做"一定无效"的断言)。

另:UNGP.version:2 的 masterVersionFile 指向 GitHub master 分支 raw 地址,本次审计**无法联网核对**(web_fetch 因域名解析到非公网地址被拒绝),远端与本地 2.6.0 是否一致未验证。

## 3. 必答问题 3: 仓库卫生

### 3.1 .gitignore 覆盖情况

现状(.gitignore:1-12):*.py、*.iml、.idea、.build、.gradle、build/,然后用 6 条 ! 反忽略 tools/、tools/formatter/、tools/formatter/*.py、tools/icon_tint/、tools/icon_tint/*.py、UnofficialNewGamePlus.iml。

| 检查项 | 结果 |
| --- | --- |
| build/、.gradle、.build | 已忽略(实测 git status --ignored 显示 !! build/、!! .gradle/) |
| dist/(distZip 输出,build.gradle:61) | **未忽略**;当前没有 dist/ 目录,一旦出包即成为"可被误提交的产物" |
| jars/*.jar | 未忽略,jars/ungp.jar 正在被追踪(P1,见 1.3) |
| *.class、*.zip | 未忽略(当前无 .class 散落;zip 只有 graphics 下那个源素材) |
| 已追踪但命中忽略规则的文件 | 无(git ls-files -ci --exclude-standard 输出为空) |
| 未追踪且未被忽略的文件 | 无(git status --porcelain 中无 ?? 条目) |

**"该入库却被忽略"的实锤(4 个,由 git check-ignore -v 逐条确认):**

- AutoPack-CHS.py、AutoPack-ENG.py、swapLangFile-切换语言.py(均命中 .gitignore:1 的 *.py)
- data/campaign/CSV_id_order.py(命中 .gitignore:1 的 *.py)

其中 swapLangFile-切换语言.py 是 changelog 明文承诺给用户的语言切换手段(changelog.txt:386、392),却既不入库、也不在 distZip 的 include 规则里 -> P1,见 1.5。CSV_id_order.py 则是"不入库却会被发布包携带"的反向问题。

**"不该入库却被追踪"的实锤(2 个):**

- jars/ungp.jar(451 KB 二进制构建产物,本次被修改;违反 AGENTS.md:31)
- graphics/ungp/icons/UNGP PS Blend.zip(2.7 KB,图标源素材,会随 graphics/** 进发布包)

说明:UnofficialNewGamePlus.iml 虽然被 *.iml 命中又被 ! 反忽略,但这是**有意为之**——AGENTS.md:18 明确要求保留它给 IDEA 用;实测已被追踪(git ls-tree HEAD 可查到),因此不算违规。另注意 .gitignore 里 !tools/、!tools/formatter/ 这类目录级反忽略实际冗余(目录本身从未被忽略),真正起作用的是 *.py 反忽略规则(P3:配置可读性)。

### 3.2 工作区在途改动规模与构成

git status --short 共 **30 条**;分成 4 类(staged/未 staged 混合是这里的主要卫生问题):

| 状态 | 数量 | 内容 |
| --- | --- | --- |
| 仅工作区修改(未 staging) | 11 | .gitignore、README.md、data/campaign/UNGP_rules.csv、UNGP_rules_ENG.csv、data/strings/strings.json、strings_ENG.json、jars/ungp.jar、src/ungp/scripts/UNGP_modPlugin.java、campaign/UNGP_InGameData.java、campaign/everyframe/UNGP_CampaignPlugin.java、plugins/UNGP_SpecialistCombatPlugin.java |
| AM(已暂存后又改) | 10 | 5 个 graphics/ungp/icons/ungp_rules/*.png + src/ungp/api/UNGP_CombatRulesAPI.java、impl/rules/combat/UNGP_IHaveActivated.java、fleet/UNGP_HumanNatureTest.java、fleet/UNGP_KeepTheWayClear.java、member/UNGP_SlipperyModification.java |
| A(已暂存未再改) | 7 | graphics/.../i_have_activated.png、src/ungp/impl/rules/combat/UNGP_VengefulSpirit.java、other/UNGP_BountyHunter.java、tools/icon_tint/{README.md,example.png,icon_tint.py,图标着色.bat} |
| AD(**索引中已 add、工作区已删**) | 2 | graphics/ungp/icons/ungp_rules/keep_the_way_clear_green.png、tools/icon_tint/i_have_activated.png |

diff 规模:未暂存 git diff --stat = **23 files changed, 296 insertions(+), 67 deletions(-)**;已暂存 git diff --cached --stat = **19 files changed, 1868 insertions(+)**(其中 tools/icon_tint/icon_tint.py 946 行、README.md 152 行是主要增量)。

**P2: 2 个 AD 条目意味着现在直接 git commit 会提交两个在工作区已不存在的 png**(索引里有 blob、磁盘上没有)。必须先 git rm --cached 或 git add -u 清理,否则版本库会凭空多出两个死文件。

另外:本地 master 相对 origin/master **ahead 1**(08482f7 2.6.0 update 未推送),而最后一次 tag 停在 2.3.0-hotfix——2.4/2.5/2.6 三个版本都没有 tag(P3:发布可追溯性)。仓库有 4731 个松散对象、29.85 MiB、size-pack: 0(从未 gc),git 操作会偏慢(P3,git gc 可解)。

### 3.3 其它卫生项

- 绝对路径/本地配置:已追踪文本中未发现 D:\ 或 C:/\ 这类机器相关路径(build.gradle:8-9 用 rootDir.parentFile 推导游戏根;.iml 用 $MODULE_DIR$)。唯一硬编码绝对路径在未入库的 tools/icon_tint/icon_tint.py:362-367(C:/Windows/Fonts/*,有 fallback,见 5.3)。
- .build/ 是仓库根的空目录,全仓库无任何引用(无脚本/构建读取它),属遗留空壳;已被 .gitignore 忽略,不影响 git,但可作为清理项(P3)。
- build/ 下本次审计前存在 build/bounty-hunter-validation/、build/verification/ 两个开发期验证产物目录;它们都被 gitignore 覆盖。需要说明:我执行的 gradle clean 会删除整个 build/(Gradle 默认行为,build.gradle:76-78 只额外追加删除 dist),因此这两个**未入库的构建产物目录已被清除**,无已追踪文件受影响。

## 4. 必答问题 4: 代码风格与规范符合度(AGENTS.md:22-23)

扫描口径:全量遍历 src/**、data/** 与根级文本文件,排除 graphics/、sounds/、jars/、build/、dist/、docs/;二进制扩展名按 tools/formatter/common.py:6-13 的白名单剔除;行尾判定前先把 CRLF 归一为 LF,以剥离本机 autocrlf 噪声。共扫描 **207 个文本文件**(其中 172 个 .java)。

| 规范项 | 违规文件数 | 判定 |
| --- | --- | --- |
| UTF-8 BOM | **0** | 完全合规 |
| LF 行尾(入库内容) | **0**(全部 i/lf) | 入库内容合规;工作区 184 个文件是 CRLF,属 autocrlf 检出噪声,见 1.2 |
| Java 4 空格缩进(行首 Tab) | **7 个文件** | 不合规 |
| Java 文件末尾单个空行 | **10 个文件** | 不合规 |
| 非 Java 文本文件末尾单个空行 | **15 个文件** | 不合规(工具链完全不检查此项,见 5.1) |
| 文件末尾多个空行 | 3 个文件 | 不合规 |

**Java 行首 Tab(7 个,全部为真实违规;行号为首个命中行):**

    src/ungp/impl/rules/combat/UNGP_DangerZone.java:22
    src/ungp/impl/rules/combat/UNGP_ExplosiveSniper.java:12
    src/ungp/impl/rules/combat/UNGP_FragmentGraveyard.java:19
    src/ungp/impl/rules/combat/UNGP_LuckySeven.java:15
    src/ungp/impl/rules/combat/UNGP_StrikerSchool.java:31
    src/ungp/impl/rules/fleet/UNGP_Sneaker.java:11
    src/ungp/impl/rules/fleet/UNGP_Thermodynamics.java:30

(已逐行核对样例:UNGP_DangerZone.java:22 为 tab 开头的 "private float spawnRateMult;",UNGP_Sneaker.java:11 为 tab 开头的 "public static final float BONUS = 1f;"。另 UNGP_Yang.java:31、UNGP_Ying.java:31 的 tab 出现在 // 注释掉的旧代码里,不是缩进,归入 P3。)

**Java 文件末尾不合规(10 个):**

    末尾缺少换行(9 个):
    src/ungp/impl/rules/fleet/UNGP_BustlingFungus.java
    src/ungp/impl/rules/fleet/UNGP_NavalTreaty.java
    src/ungp/impl/rules/fleet/UNGP_Sneaker.java
    src/ungp/impl/rules/fleet/UNGP_Thermodynamics.java
    src/ungp/impl/rules/other/UNGP_RainPour.java
    src/ungp/impl/rules/other/UNGP_Yang.java
    src/ungp/impl/rules/other/UNGP_Ying.java
    src/ungp/impl/saves/UNGP_BlueprintsDataSaver.java
    src/ungp/scripts/campaign/specialist/intel/UNGP_SpecialistBackgroundUI.java
    末尾 2 个换行(1 个):
    src/ungp/impl/rules/combat/UNGP_CloseFormation.java

**非 Java 文本文件末尾不合规(15 个,列出前 10 + 总数):**

    data/campaign/CSV_id_order.py            data/campaign/UNGP_challenges.csv
    data/campaign/UNGP_challenges_ENG.csv    data/campaign/UNGP_dataSavers.csv
    data/characters/skills/skill_data.csv    data/config/LunaSettings.csv
    data/config/LunaSettings_ENG.csv         data/config/LunaSettingsConfig.json
    data/config/settings.json                data/config/sounds.json
    ... 共 15 个(其余: data/strings/strings.json、data/strings/strings_ENG.json、
    license.txt、README.md、UNGP_OPTIONS.ini)

**文件末尾多个空行(3 个):** data/config/tag_data.json(4 个换行)、data/config/tag_data_ENG.json(4 个换行)、src/ungp/impl/rules/combat/UNGP_CloseFormation.java(2 个换行)。

结论:AGENTS.md:22-23 的规范被部分执行——BOM 与入库 LF 是干净的,但"4 空格缩进"和"文件末尾单个空行"两条有 34 处真实违规(涉及 32 个不同文件),且现有工具链**永远不会发现其中一半**(非 Java 尾行,以及"2 空格缩进"这类非 tab 情形)。

## 5. 必答问题 5: tools/ 脚本健壮性、重复与死代码

### 5.1 tools/formatter/(gradle 的格式化与校验后端)

- **覆盖范围与 AGENTS.md 不一致**:common.py:5 的 SKIP_DIRS 含 "tools",因此 tools/ 下的 .py、根级 .py 都**不被格式化/校验**;而 AGENTS.md:22 说的是"文本文件统一……"。
- **只修不查**:java_spaces.py:6-8 只把行首 tab 换成 4 空格,从不判断"是否 4 的倍数";java_single_final_blank_line.py:6-11 只处理 .java。也就是说 **2 空格缩进、非 Java 文件缺尾换行都不会被发现**——第 4 节的 15 个非 Java 尾行违规与现状直接相关。
- **重复实现**:normalize_eol()(common.py:68-70)只有 java_single_final_blank_line.py:11 一处调用,且传入的就是换行符常量,等价于直接调 to_lf();可以删掉这层抽象(P3)。
- **自实现 .gitignore 匹配存在语义偏差**:common.py:16-52 自己解析 .gitignore,但 load_gitignore_patterns() 直接跳过所有 ! 开头的行(common.py:24)-> 反忽略规则(!UnofficialNewGamePlus.iml、!tools/...)在这套逻辑里完全失效;* 分支用 Path.match 也与 git 语义不完全等价。当前结果碰巧无害,但属于"两套 ignore 语义并存"(P3)。
- **注释引用不存在的文件**:common.py:10-13 写"对齐 .gitattributes 的 binary 列表",而仓库**没有 .gitattributes**(见 1.2)——说明这份脚本从别的项目搬来、配套文件漏了(P3,同时是"加 .gitattributes"这条建议的旁证)。
- **严格 UTF-8 解码会中断检查**:java_spaces.py:19 用严格模式 decode("utf-8"),遇到任意非 UTF-8 的 .java 会抛 UnicodeDecodeError 让整个任务崩掉,而不是报出文件名(P3)。
- **检查顺序问题**:verifyFormat 链上 checkPycache 在最后(build.gradle:154-160),前面任一项失败就看不到 __pycache__ 报告;而如果开发者不通过 gradle、直接跑这些脚本,Python 会生成 tools/formatter/__pycache__,反过来让 checkPycache 报假失败(构建里用 PYTHONDONTWRITEBYTECODE=1 规避,但脚本自身没有防护)。本次审计直跑脚本时显式设置了该变量,实测仓库中 __pycache__ 数量为 0。

### 5.2 根级打包/语言脚本

- AutoPack-CHS.py:4 / AutoPack-ENG.py:4 都 import auto_pack,而 auto_pack 模块在全盘 D:/StarsectorDevelopment(深度 4)内**不存在** -> 两个脚本按现状必然 ModuleNotFoundError,是死脚本。况且它们被 .gitignore:1 忽略、不在仓库里,属于"作者本机残留"(P3)。同时它们是**唯一已知的实际发布打包手段**(AGENTS.md:17 提到 distZip,但 distZip 内容有缺陷,见 1.5),发布链路在仓库内是缺失的(P2)。
- swapLangFile-切换语言.py 的健壮性问题:
  - os.walk(dirPath)(第 11 行)从仓库根开始递归**全部目录**,包括 .git/、build/、jars/,只按"同名 + _ENG"配对就改名(第 17-19 行),没有任何 include/exclude 清单或 dry-run(第 21-22 行只打印)。
  - 改名是三次 os.rename 的临时文件中转(第 23-27 行),中途异常虽被 catch,但会留下 "xxx temp.ext" 残留文件,且脚本继续往下跑(P3)。
  - 结尾 input("Press Any key To Exit.")(第 31 行)使其无法在 CI/批处理中非交互运行(P3)。
  - 最重要的不是代码而是**位置**:它是 changelog 承诺给用户的功能(changelog.txt:386、392),却未入库、未进发布包(P1,见 1.5)。
- data/campaign/CSV_id_order.py(14 行,未入库、但会进发布包):
  - 第 1 行 from numpy.core.numeric import NaN:导入了 NaN 却从头到尾没用;且 numpy.core 是 NumPy 的私有/弃用命名空间(NumPy 2.x 已迁到 numpy._core,该导入在部分版本会告警甚至失效)(P3)。
  - 第 4-5 行用裸文件名 "UNGP_rules.csv"/"UNGP_rules_ENG.csv",依赖"必须 cd 到 data/campaign 再运行";无 argparse、无 main guard、无异常处理(第 10-14 行)。
  - 行为是**破坏性覆盖** _ENG.csv(第 14 行 origin_df.to_csv(compare_csv_file)),且会把索引列写回,对换行/编码不设防 -> 这类脚本不该躺在会被发布的 data/ 目录里(P2)。

### 5.3 tools/icon_tint/(新增,946 行,已暂存)

质量明显高于其余脚本(argparse、--dry-run、--overwrite、Pillow 缺失时给中文提示并退出,见 icon_tint.py:37-41、643-686)。仅两点小问题:

- icon_tint.py:362-367 硬编码 C:/Windows/Fonts/msyh.ttc 等 4 个 Windows 字体路径;有 _load_font() 回退(374-379),不会崩,但非 Windows 上对照预览图的中文标签会变方块(P3)。
- 图标着色.bat:5-15 的 Python 探测(py -3 -> python)与 %~dp0 用法都正确,无绝对路径,合规。
- 与仓库约定的冲突:它挂在 tools/ 下会被 common.py:5 的 SKIP_DIRS 跳过格式化,但 .gitignore 又专门为它开了反忽略(.gitignore:10-11),形成"入库但不受格式约束"的口子(P3)。

## 6. 必答问题 6: 构建脚本层面的优化点

按收益排序:

1. **加 .gitattributes**(最高优先):* text=auto eol=lf + 二进制白名单(*.jar binary、*.png binary、*.ogg binary、*.zip binary)。这一条同时解决 1.2 的 verifyFormat 必失败、common.py:10-13 的悬空注释、以及"Windows 检出即脏"的长期摩擦。没有它,格式门禁在任何 autocrlf=true 的机器上都不可用。
2. **修掉 distZip 的打包内容**(见 1.5):加 exclude "**/*.py"、"**/*.zip"、排除 mod_info_ENG.json;把 include "jars/**" 收窄为 include "jars/ungp.jar"(或用 from(tasks.jar) 精确取产物);加 duplicatesStrategy = DuplicatesStrategy.FAIL;并把 dist/ 写进 .gitignore。
3. **让构建不依赖机器状态**:加 java { toolchain { languageVersion = JavaLanguageVersion.of(17) } }(build.gradle 现在只靠 options.release = 17 兜底,build.gradle:46-49;一旦有人用更低 JDK 跑 Gradle 就编译失败,而 AGENTS.md:14-15 只写了"用本机 Gradle 9.6.1 + 完整 JDK 17",没给 JAVA_HOME 设置方法);同时补一个 gradle wrapper --gradle-version 9.6.1,否则"Gradle 9.6.1"这个前提只能靠口头约定。
4. **依赖声明收窄**:删掉 build.gradle:28 的 MagicLib(未使用,还是 4 条 kotlin 警告的唯一来源),fileTree 加 exclude "*-sources.jar"(会把 jaxb-api-...-sources.jar 也放进编译类路径);mod_info.json:6-9 与 AGENTS.md:16 同步删 MagicLib。另外 build.gradle:25-29 的 LazyLib/Lunalib/MagicLib 目录名硬编码且大小写敏感(Windows 不敏感、Linux 敏感),建议改为按 mod id 探测或提供 -P 覆盖参数。
5. **去重 mod_info.json 解析**:settings.gradle:17-23 与 build.gradle:12-18 是同一段逻辑的两份拷贝;settings 阶段其实只需要 rootProject.name,可以由 gradle.properties 提供固定值,或用一个极简正则(避免把整份 JSON 的解析风险压在配置期)。
6. **给 Exec 任务声明 inputs/outputs**:例如 inputs.dir("src")、inputs.dir("data")、inputs.dir("tools/formatter")、outputs.upToDateWhen { false }(格式化任务本身不可缓存,但校验任务可以)。再打开 org.gradle.caching=true 与 org.gradle.configuration-cache=true(Gradle 9.6.1 每次构建都会提示,现 gradle.properties:1-2 只有 warning mode 和 jvmargs)。
7. **补一个真正有意义的校验任务**:目前 gradle check = 格式检查 + test(仓库没有 src/test,test 任务没有可执行内容;本次 check --dry-run 显示 :test SKIPPED)。本项目的核心风险是"CSV 行 <-> 反射类名/tag 混入"的联动,建议加一个 Gradle 任务校验 data/campaign/UNGP_rules*.csv 里每个 id/class 都能在 src/ 或 jar 中找到,并校验两份 mod_info 的 id/version/dependencies 一致——这两项都是本次审计已经暴露缺陷的地方(P2)。
8. 小项:jar 任务的 archiveFileName 由 mod_info.jars 第一项取 name(build.gradle:19),若将来写成子目录路径会静默丢路径,建议加断言;clean 只额外删 dist(build.gradle:76-78),不会删 jars/,建议在注释里写明"jar 是有意保留的产物";jars/ 同时是 jar 的输出目录和 distZip 的源目录,更干净的做法是把 jar 输出放 build/,distZip 再从 build/ 取。

## 7. 建议的修复顺序(可直接排期)

1. 加 .gitattributes(* text=auto eol=lf + 二进制白名单)-> 立刻让 gradle verifyFormat / gradle build 在 Windows 上恢复可用。
2. 修 mod_info_ENG.json 版本(2.5.0 -> 2.6.0)与依赖(与主文件对齐);顺手删掉两份 mod_info 的尾逗号。
3. 处理 2 个 AD 索引条目(git rm --cached),把 dist/ 和 /jars/*.jar 加进 .gitignore,并决定 jars/ungp.jar 的去留(建议移出 VCS + 发布时构建)。
4. 修 distZip 的 include/exclude;确认 swapLangFile-切换语言.py 与 CSV_id_order.py 的最终归属(前者应入库+进包,后者应移出 data/)。
5. 批量修正 7 个 tab 缩进、10 个 Java 尾行、15 个非 Java 尾行、2 个 json 多余尾空行(注意:先做第 1 步,否则 formatProject 会把 183 个 CRLF 文件一起卷进 diff)。
6. 按第 6 节的 2、3、4 项精简构建脚本(去 MagicLib、加 toolchain/wrapper)。

## 8. 本次审计的已知局限

- 未执行 gradle distZip(避免在仓库产生 dist/ 与新的构建产物),打包结论来自 --dry-run + 对 include 规则的静态模拟(173 files / 3,329,061 B 为模拟值)。
- 未能联网核对 UNGP.version:2 指向的远端 master 版本文件(web_fetch 被拒绝:域名解析到非公网地址)。
- data/config/version/version_files.csv 的读取方未能定位(19 个游戏 jar 的 class 字符串检索 0 命中;游戏数据目录内不存在同名文件),因此只记录为"疑似死配置",未下断言。
- 游戏内 mod_info.json 尾逗号的实际解析行为未做运行验证;仅以同级 9 个 mod 同样非严格 JSON 的现象作为"游戏侧宽松解析"的旁证。
- 未经游戏实机运行,本次不覆盖任何运行期/存档层结论。
