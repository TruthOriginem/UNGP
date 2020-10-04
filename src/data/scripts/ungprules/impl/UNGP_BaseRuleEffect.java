package data.scripts.ungprules.impl;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import data.scripts.campaign.hardmode.UNGP_RulesManager;
import data.scripts.campaign.hardmode.UNGP_SpecialistSettings;
import data.scripts.ungprules.UNGP_RuleEffectAPI;
import data.scripts.utils.SimpleI18n.I18nSection;

import java.math.BigDecimal;
import java.util.Random;

import static data.scripts.campaign.hardmode.UNGP_RulesManager.URule;

public abstract class UNGP_BaseRuleEffect implements UNGP_RuleEffectAPI {
    protected static final Random RANDOM = new Random();
    protected URule rule;

    public UNGP_BaseRuleEffect() {

    }

    public void setRule(URule rule) {
        this.rule = rule;
    }

    @Override
    public URule getRule() {
        return rule;
    }

    @Override
    public void updateDifficultyCache(int difficulty) {

    }

    @Override
    public abstract float getValueByDifficulty(int index, int difficulty);

    @Override
    public abstract String getDescriptionParams(int index);

    @Override
    public String getDescriptionParams(int index, int difficulty) {
        return getDescriptionParams(index);
    }

    @Override
    public void applyGlobalStats() {

    }

    @Override
    public void unapplyGlobalStats() {

    }

    /**
     * Get linear value
     *
     * @param min
     * @param max
     * @param difficulty
     * @param maxDifficulty
     * @return
     */
    protected float getLinearValue(float min, float max, int difficulty, int maxDifficulty) {
        return min + (max - min) / (maxDifficulty - 1) * (difficulty - 1);
    }

    protected float getLinearValue(float min, float max, int difficulty) {
        return getLinearValue(min, max, difficulty, UNGP_SpecialistSettings.MAX_DIFFICULTY);
    }

    protected I18nSection i18n() {
        return UNGP_RulesManager.rules_i18n;
    }

    /**
     * value should be 0~100
     *
     * @param value
     * @return
     */
    protected String getPercentString(float value) {
        BigDecimal number = new BigDecimal(String.format("%.1f", value));
        return number.stripTrailingZeros().toPlainString() + "%";
    }

    /**
     * value should be 0~1
     *
     * @param value
     * @return
     */
    protected String getFactorString(float value) {
        BigDecimal number = new BigDecimal(String.format("%.2f", value));
        return number.stripTrailingZeros().toPlainString();
    }

    /**
     * @param engine
     * @param key
     * @param <T>
     * @return could be null.
     */
    protected static <T> T getDataInEngine(CombatEngineAPI engine, String key) {
        Object record = engine.getCustomData().get(key);
        if (record != null) {
            return (T) (record);
        }
        return null;
    }

    protected static <T> void putDataInEngine(CombatEngineAPI engine, String key, T t) {
        engine.getCustomData().put(key, t);
    }

    protected static void decreaseMaxCR(MutableShipStatsAPI memberStats, String id, float flat, String desc) {
        float curMaxCR = memberStats.getMaxCombatReadiness().getModifiedValue();
        float multiplier = memberStats.getMaxCombatReadiness().computeMultMod();
        float toDecrease = Math.min(flat, curMaxCR / multiplier);
        memberStats.getMaxCombatReadiness().modifyFlat(id, -toDecrease, desc);
    }
}
