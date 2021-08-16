package data.scripts.campaign.specialist;

import com.fs.starfarer.api.Global;
import data.scripts.campaign.specialist.rules.UNGP_RulesManager.URule;

import java.util.List;

public final class UNGP_SpecialistSettings {
    public static final int MAX_DIFFICULTY = 20;

    public static int getMaxDifficultyLevel(int cycle) {
        if (Global.getSettings().getBoolean("noDifficultyLimit")) {
            return MAX_DIFFICULTY;
        }
        return Math.min(20, (cycle - 1) * 5);
    }

    //3~10
    public static int getMaxRulesAmount(int difficultly) {
        return (int) (4.42f + difficultly * 1.58f);
    }

    //1~5
    public static int getMinRulesAmount(int difficultly) {
        return (int) (3f + difficultly * 0.3f);
    }

    /**
     * 这些规则是否满足条件
     *
     * @param rules
     * @param difficultyLevel
     * @return
     */
    public static boolean rulesMeetCondition(List<URule> rules, int difficultyLevel) {
        if (rules.isEmpty()) return false;
        int cost = 0;
        for (URule rule : rules) {
            cost += rule.getCost();
        }
        if (cost < 0) return false;
        int size = rules.size();
        int goldenRuleSize = 0;
        for (URule rule : rules) {
            if (rule.isGolden()) {
                goldenRuleSize++;
            }
            if (goldenRuleSize > 1) {
                return false;
            }
        }
        return size <= getMaxRulesAmount(difficultyLevel) &&
                size >= getMinRulesAmount(difficultyLevel);
    }

    public static String getSpecialistModeIconPath() {
        return "graphics/icons/UNGP_hmode_logo.png";
    }
}
