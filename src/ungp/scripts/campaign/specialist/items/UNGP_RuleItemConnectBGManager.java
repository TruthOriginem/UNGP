package ungp.scripts.campaign.specialist.items;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import ungp.scripts.campaign.specialist.UNGP_SpecialistSettings;
import ungp.scripts.campaign.specialist.challenges.UNGP_ChallengeInfo;
import ungp.scripts.campaign.specialist.challenges.UNGP_ChallengeManager;
import ungp.scripts.campaign.specialist.rules.UNGP_RulesManager.URule;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Be used as temporary center
 */
public class UNGP_RuleItemConnectBGManager {
    public static final Set<URule> TEMP_HIGHLIGHT_RULE_LIST = new HashSet<>();
    private static boolean isDirty = true;

    /**
     * Rule item background highlighted.
     *
     * @param rule
     * @return
     */
    public static boolean canRuleHighlighted(URule rule) {
        return TEMP_HIGHLIGHT_RULE_LIST.contains(rule);
    }

    public static void refresh(CargoAPI combined, List<String> completedChallenges) {
        clear();
        Set<URule> pickedRules = new HashSet<>();
        for (CargoStackAPI stack : combined.getStacksCopy()) {
            if (stack.isSpecialStack()) {
                URule rule = URule.getByID(stack.getSpecialDataIfSpecial().getData());
                if (rule != null) {
                    pickedRules.add(rule);
                }
            }
        }
        //
        for (UNGP_ChallengeInfo challengeInfo : UNGP_ChallengeManager.getChallengeInfosCopy()) {
            // 已完成的不显示
            if (completedChallenges.contains(challengeInfo.getId())) {
                continue;
            }
            boolean highlightAll = false;
            List<URule> rulesRequired = challengeInfo.getRulesRequired();
            for (URule requiredRule : rulesRequired) {
                if (pickedRules.contains(requiredRule)) {
                    highlightAll = true;
                    break;
                }
            }
            if (highlightAll) {
                pickedRules.addAll(rulesRequired);
            }
        }
        List<UNGP_ChallengeInfo> runnableChallenges = UNGP_ChallengeManager.getRunnableChallenges(UNGP_SpecialistSettings.getChallengeMinLevelReq(),
                                                                                                  new ArrayList<>(pickedRules), completedChallenges);
        for (UNGP_ChallengeInfo runnableChallenge : runnableChallenges) {
            TEMP_HIGHLIGHT_RULE_LIST.addAll(runnableChallenge.getRulesRequired());
        }
    }

    public static void clear() {
        TEMP_HIGHLIGHT_RULE_LIST.clear();
    }
}
