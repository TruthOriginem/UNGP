package ungp.scripts.campaign.specialist.rules;

import ungp.scripts.campaign.specialist.UNGP_SpecialistSettings.Difficulty;

import java.util.ArrayList;
import java.util.List;

import static ungp.scripts.campaign.specialist.rules.UNGP_RulesManager.URule;

public class UNGP_RulePickHelper {
    public enum UnmetReasonType {
        WRONG_COST("unmet_wrong_cost"),
        GOLDEN_MORE_THAN_ONE("unmet_golden_more_than_one"),
        TOO_FEW_RULES("unmet_too_few_rules"),
        TOO_MUCH_RULES("unmet_too_much_rules"),
        EMPTY_RULES("unmet_empty_rules"),
        GENERAL("unmet_general");

        final String i18n_key;

        public String getI18nKey() {
            return i18n_key;
        }

        UnmetReasonType(String i18n_key) {
            this.i18n_key = i18n_key;
        }
    }

    public static class UnmetReason {
        private UnmetReasonType type;
        private Object[] params;

        public UnmetReason(UnmetReasonType type, Object... params) {
            this.type = type;
            this.params = params;
        }

        public UnmetReason(UnmetReasonType type) {
            this.type = type;
        }

        public UnmetReasonType getType() {
            return type;
        }
    }

    public static List<UnmetReason> generateUnmetReasons(List<URule> rules, Difficulty difficulty) {
        List<UnmetReason> reasons = new ArrayList<>();

        if (rules.isEmpty()) {
            reasons.add(new UnmetReason(UnmetReasonType.EMPTY_RULES));
            return reasons;
        }
        if (difficulty == null) {
            reasons.add(new UnmetReason(UnmetReasonType.GENERAL));
            return reasons;
        }
        int cost = 0;
        for (URule rule : rules) {
            cost += rule.getCost();
        }
        if (cost < 0) {
            reasons.add(new UnmetReason(UnmetReasonType.WRONG_COST));
        }
        int size = rules.size();
        int goldenRuleSize = 0;
        for (URule rule : rules) {
            if (rule.isGolden()) {
                goldenRuleSize++;
            }
        }
        if (goldenRuleSize > 1) {
            reasons.add(new UnmetReason(UnmetReasonType.GOLDEN_MORE_THAN_ONE));
        }
        if (size > difficulty.maxRules) {
            reasons.add(new UnmetReason(UnmetReasonType.TOO_MUCH_RULES));
        }
        if (size < difficulty.minRules) {
            reasons.add(new UnmetReason(UnmetReasonType.TOO_FEW_RULES));
        }
        return reasons;
    }

    public static boolean hasUnmetReasons(List<URule> rules, Difficulty difficulty) {
        return !generateUnmetReasons(rules, difficulty).isEmpty();
    }
}
