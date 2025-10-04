package ungp.scripts.codex;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.codex.CodexDataV2;
import com.fs.starfarer.api.impl.codex.CodexEntryV2;
import com.fs.starfarer.api.ui.TagDisplayAPI;
import ungp.scripts.campaign.specialist.challenges.UNGP_ChallengeInfo;
import ungp.scripts.campaign.specialist.challenges.UNGP_ChallengeManager;
import ungp.scripts.campaign.specialist.rules.UNGP_RulesManager;
import ungp.scripts.utils.Constants;

import java.util.List;

public class UNGP_CodexManager {

    public static final String CODEX_ROOT = "UNGP_codex_root";
    public static final String SPECIALIST_ENTRY = "UNGP_specialist_entry";
    public static final String RULE_ENTRY = "UNGP_rule_entry";

    public static void init() {
        CodexEntryV2 rootEntry = new CodexEntryV2(CODEX_ROOT, Constants.codex_i18n.get("root_title"), Global.getSettings().getSpriteName("icons", "UNGP_intel_icon"));
        CodexEntryV2 specialistEntry = new CodexEntryV2(SPECIALIST_ENTRY, Constants.codex_i18n.get("specialist_title"), Global.getSettings().getSpriteName("icons", "UNGP_specialist_mode_logo"));
        rootEntry.addChild(specialistEntry);
        List<UNGP_RulesManager.URule> allRulesCopy = UNGP_RulesManager.getAllRulesCopy();
        allRulesCopy.sort(new UNGP_RulesManager.UNGP_RuleSorter());
        CodexEntryV2 allRulesEntry = new CodexEntryV2(RULE_ENTRY, Constants.codex_i18n.get("rule_title"), Global.getSettings().getSpriteName("codex", "game_mechanics")) {
            @Override
            public boolean hasTagDisplay() {
                return true;
            }

            @Override
            public void configureTagDisplay(TagDisplayAPI tags) {
                int total = 0;
                int positive = 0;
                int negative = 0;
                int golden = 0;
                int milestone = 0;
                for (UNGP_RulesManager.URule uRule : allRulesCopy) {
                    total++;
                    if (uRule.isPositive()) {
                        positive++;
                    } else {
                        negative++;
                    }
                    if (uRule.isGolden()) {
                        golden++;
                    }
                    if (uRule.isMilestone()) {
                        milestone++;
                    }
                }
                tags.beginGroup(false, Constants.codex_i18n.get("all_tag"));
                tags.addTag("positive", UNGP_RulesManager.getRuleTypeName(false, false, true), positive);
                tags.addTag("negative", UNGP_RulesManager.getRuleTypeName(false, false, false), negative);
                tags.addTag("golden", UNGP_RulesManager.getRuleTypeName(false, true, false), golden);
                tags.addTag("milestone", UNGP_RulesManager.getRuleTypeName(true, false, false), milestone);
                tags.setTotalOverrideForCurrentGroup(total);
                tags.addGroup(0f);
                tags.setGroupChecked(0, true);
            }
        };
        for (UNGP_RulesManager.URule uRule : allRulesCopy) {
            UNGP_RuleCodexEntryPlugin ruleEntry = new UNGP_RuleCodexEntryPlugin(uRule);
            ruleEntry.setRetainOrderOfRelatedEntries(true);
            allRulesEntry.addChild(ruleEntry);
        }
        //        SharedUnlockData.get().reportPlayerAwareOfFighter()
        specialistEntry.addChild(allRulesEntry);
        allRulesEntry.setRetainOrderOfChildren(true);

        CodexDataV2.ROOT.addChild(rootEntry);
    }

    /**
     * 建立规则条目间的关联关系
     * 1. 将每个挑战需求规则与其对应的里程碑规则双向关联
     * 2. 将同一个挑战中的所有需求规则互相连接
     */
    public static void setRelatedEntries() {
        // 获取所有挑战配置信息
        List<UNGP_ChallengeInfo> challengeInfosCopy = UNGP_ChallengeManager.getChallengeInfosCopy();

        // 遍历每个挑战
        for (UNGP_ChallengeInfo challengeInfo : challengeInfosCopy) {
            // 第一层处理：连接需求规则与里程碑规则
            challengeInfo.getRulesRequired().forEach(rule -> {
                // 获取该挑战对应的里程碑规则
                UNGP_RulesManager.URule milestoneToUnlock = challengeInfo.getMilestoneToUnlock();
                // 双向建立关联：需求规则 <-> 里程碑规则
                CodexDataV2.makeRelated(rule.getCodexEntryId(), milestoneToUnlock.getCodexEntryId());
            });

            // 第二层处理：连接同一挑战中的所有需求规则
            challengeInfo.getRulesRequired().forEach(rule -> {
                challengeInfo.getRulesRequired().forEach(anotherRule -> {
                    // 避免规则自连接和重复连接
                    if (rule != anotherRule) {
                        CodexDataV2.makeRelated(rule.getCodexEntryId(), anotherRule.getCodexEntryId());
                    }
                });
            });
        }
    }

}
