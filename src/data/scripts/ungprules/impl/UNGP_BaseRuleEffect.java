package data.scripts.ungprules.impl;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignClockAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.impl.campaign.intel.MessageIntel;
import data.scripts.campaign.specialist.UNGP_SpecialistSettings;
import data.scripts.ungprules.UNGP_RuleEffectAPI;
import data.scripts.utils.SimpleI18n.I18nSection;

import java.math.BigDecimal;
import java.util.Random;

import static data.scripts.campaign.specialist.rules.UNGP_RulesManager.URule;

public abstract class UNGP_BaseRuleEffect implements UNGP_RuleEffectAPI {
    protected static final Random RANDOM = new Random();
    protected static final I18nSection i18n = new I18nSection("UNGP_rule_impl", "", false);
    protected URule rule;

    public UNGP_BaseRuleEffect() {

    }

    public boolean roll(float chance) {
        return RANDOM.nextFloat() < chance;
    }

    public boolean roll2() {
        return RANDOM.nextBoolean();
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

    @Deprecated
    public String getDescriptionParams(int index) {
        return null;
    }

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


/*
    public static float smooth(float x) {
        return 0.5f - ((float) (FastTrig.cos(Math.min(1, Math.max(0, x)) * MathUtils.FPI) / 2));
    }*/

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
     * 在文本框和生涯界面都显示信息
     *
     * @param intel
     */
    protected void showMessage(MessageIntel intel) {
        final CampaignUIAPI campaignUI = Global.getSector().getCampaignUI();
        if (campaignUI == null) return;
        if (campaignUI.isShowingDialog()) {
            InteractionDialogAPI dialog = campaignUI.getCurrentInteractionDialog();
            if (dialog != null && dialog.getTextPanel() != null)
                Global.getSector().getIntelManager().addIntelToTextPanel(intel,
                                                                         dialog.getTextPanel());
        }
        campaignUI.addMessage(intel);
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

    protected Random getRandom() {
        final CampaignClockAPI clock = Global.getSector().getClock();
        String sb = Global.getSector().getSeedString() + rule.getId() +
                String.format("%d%d", clock.getCycle(), clock.getMonth());
        return new Random(sb.hashCode());
    }

    protected Random getRandomByDay() {
        final CampaignClockAPI clock = Global.getSector().getClock();
        String sb = Global.getSector().getSeedString() + rule.getId() +
                String.format("%d%d%d", clock.getCycle(), clock.getMonth(), clock.getDay());
        return new Random(sb.hashCode());
    }
}
