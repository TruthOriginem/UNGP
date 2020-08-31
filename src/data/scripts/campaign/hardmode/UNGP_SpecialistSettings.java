package data.scripts.campaign.hardmode;

import com.fs.starfarer.api.Global;
import data.scripts.campaign.hardmode.UNGP_RulesManager.URule;

import java.util.List;

public class UNGP_SpecialistSettings {
    public static int getMaxDifficultyLevel(int cycle) {
        if (Global.getSettings().getBoolean("noDifficultyLimit")) {
            return 20;
        }
        return Math.min(20, (cycle - 1) * 5);
    }

    public static int getMaxRulesAmount(int difficutlyLevel) {
        return (int) (3 + Math.pow(difficutlyLevel, 0.65));
    }

    public static boolean rulesMeetCondition(List<URule> rules, int difficultyLevel) {
        if (rules.isEmpty()) return false;
        int cost = 0;
        for (URule rule : rules) {
            cost += rule.getCost();
        }
        if (cost < 0) return false;
        return rules.size() <= getMaxRulesAmount(difficultyLevel);
    }

    public static String getSpecialistModeIconPath() {
        return "graphics/icons/UNGP_hmode_logo.png";
    }
}
