package ungp.scripts.campaign.specialist;

import com.fs.starfarer.api.Global;
import ungp.scripts.campaign.specialist.rules.UNGP_RulesManager.URule;

import java.awt.*;
import java.util.List;

public final class UNGP_SpecialistSettings {
    @Deprecated
    public static final int MAX_DIFFICULTY = Difficulty.ALPHA.legacyLevel;
    public static final String RULES_ENABLE_IN_SIMULATION = "UNGP_rulesEnableInSimulation";

    public enum Difficulty {
        GAMMA("Gamma", 1, 0f, "graphics/ungp/icons/UNGP_difficulty_gamma.png", new Color(232, 154, 65), 4, 8),
        BETA("Beta", 10, 0.5f, "graphics/ungp/icons/UNGP_difficulty_beta.png", new Color(179, 225, 41), 8, 20),
        ALPHA("Alpha", 20, 1f, "graphics/ungp/icons/UNGP_difficulty_alpha.png", new Color(119, 255, 230), 12, 42),
        OMEGA("Omega", 30, 1.5f, "graphics/ungp/icons/UNGP_difficulty_omega.png", new Color(145, 70, 255), 20, 64);

        public final String name;
        @Deprecated
        public final int legacyLevel;
        /**
         * Only used while called {@link Difficulty#getLinearValue(float, float)}
         */
        public final float extraValueMultiplier;
        public final String spritePath;
        public final Color color;
        public final int minRules;
        public final int maxRules;

        Difficulty(String name, int legacyLevel, float extraValueMultiplier, String spritePath, Color color, int minRules, int maxRules) {
            this.name = name;
            this.legacyLevel = legacyLevel;
            this.extraValueMultiplier = extraValueMultiplier;
            this.spritePath = spritePath;
            this.color = color;
            this.minRules = minRules;
            this.maxRules = maxRules;
        }

        /**
         * Generate linear value by difficulty.
         *
         * @param base           The basic value at Difficulty.Gamma
         * @param fullExtraValue The full interval of value. Return values will equal to base + fullExtraValue
         *                       while the extraValueMultiplier of difficulty reached 1f.
         * @return
         */
        public float getLinearValue(float base, float fullExtraValue) {
            return base + fullExtraValue * extraValueMultiplier;
        }

        /**
         * @param base
         * @param fullExtraValue
         * @param maxOrMin       A cap, a limitation
         * @return
         */
        public float getLinearValue(float base, float fullExtraValue, float maxOrMin) {
            if (fullExtraValue > 0) {
                return Math.min(getLinearValue(base, fullExtraValue), maxOrMin);
            } else {
                return Math.max(getLinearValue(base, fullExtraValue), maxOrMin);
            }
        }

        /**
         * @param legacyLevel
         * @return
         */
        public static Difficulty convertLegacyLevelToDifficulty(int legacyLevel) {
            for (Difficulty value : values()) {
                if (value.legacyLevel == legacyLevel) {
                    return value;
                }
            }
            return Difficulty.GAMMA;
        }

    }

    public static Difficulty getChallengeMinLevelReq() {
        return Difficulty.ALPHA;
    }

    /**
     * 这些规则是否满足条件
     *
     * @param rules
     * @param difficulty
     * @return
     */
    public static boolean rulesMeetCondition(List<URule> rules, Difficulty difficulty) {
        if (rules.isEmpty()) return false;
        if (difficulty == null) return false;
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
        return size <= difficulty.maxRules && size >= difficulty.minRules;
    }

    public static boolean isRulesEnabledInSimulation() {
        Boolean enable = (Boolean) Global.getSector().getPersistentData().get(RULES_ENABLE_IN_SIMULATION);
        if (enable == null) {
            enable = true;
            setRulesEnabledInSimulation(true);
        }
        return enable;
    }

    public static void setRulesEnabledInSimulation(boolean enable) {
        Global.getSector().getPersistentData().put(RULES_ENABLE_IN_SIMULATION, enable);
    }

    public static String getSpecialistModeIconPath() {
        return "graphics/ungp/icons/UNGP_specialist_mode_logo.png";
    }
}
