package data.scripts.ungprules.impl;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignClockAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.comm.CommMessageAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.intel.MessageIntel;
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import data.scripts.campaign.UNGP_SharedData;
import data.scripts.campaign.specialist.UNGP_PlayerFleetMemberBuff;
import data.scripts.campaign.specialist.UNGP_SpecialistSettings.Difficulty;
import data.scripts.campaign.specialist.intel.UNGP_SpecialistIntel;
import data.scripts.ungprules.UNGP_RuleEffectAPI;
import data.scripts.utils.SimpleI18n.I18nSection;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.Random;

import static data.scripts.campaign.specialist.rules.UNGP_RulesManager.URule;

public abstract class UNGP_BaseRuleEffect implements UNGP_RuleEffectAPI {
    protected static final Random RANDOM = new Random();
    protected static final I18nSection i18n = new I18nSection("UNGP_rule_impl", "", false);
    protected URule rule;
    protected String buffID;

    public UNGP_BaseRuleEffect() {
    }

    public boolean roll(float chance) {
        return roll(RANDOM, chance);
    }

    public boolean roll(Random random, float chance) {
        return random.nextFloat() < chance;
    }

    public boolean roll2() {
        return RANDOM.nextBoolean();
    }

    public void setRule(URule rule) {
        this.rule = rule;
        this.buffID = "UNGP_" + rule.getId();
    }

    @Override
    public URule getRule() {
        return rule;
    }

    @Deprecated
    public void updateDifficultyCache(int legacyLevel) {

    }

    @Override
    public void updateDifficultyCache(Difficulty difficulty) {
        updateDifficultyCache(difficulty.legacyLevel);
    }

    @Deprecated
    public float getValueByDifficulty(int index, int legacyLevel) {
        return 0f;
    }

    @Override
    public float getValueByDifficulty(int index, Difficulty difficulty) {
        return getValueByDifficulty(index, difficulty.legacyLevel);
    }

    @Deprecated
    public String getDescriptionParams(int index) {
        return null;
    }

    @Deprecated
    public String getDescriptionParams(int index, int legacyLevel) {
        return null;
    }

    @Override
    public String getDescriptionParams(int index, Difficulty difficulty) {
        return getDescriptionParams(index, difficulty.legacyLevel);
    }

    @Override
    public void applyGlobalStats() {

    }

    @Override
    public void unapplyGlobalStats() {

    }

    @Override
    public void cleanUp() {

    }

    @Override
    public boolean addIntelTips(TooltipMakerAPI imageTooltip) {
        return false;
    }


    @Deprecated
    protected float getLinearValue(float min, float max, int legacyLevel) {
        Difficulty difficulty = Difficulty.convertLegacyLevelToDifficulty(legacyLevel);
        if (difficulty != null) {
            return difficulty.getLinearValue(min, max - min);
        } else {
            return 0f;
        }
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
     * Used for create MessageIntel, should call {@link UNGP_BaseRuleEffect#showMessage(MessageIntel)} after.
     *
     * @return
     */
    protected MessageIntel createMessage() {
        MessageIntel message = new MessageIntel(rule.getName(), rule.getCorrectColor());
        message.setIcon(rule.getSpritePath());
        return message;
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
        campaignUI.addMessage(intel, CommMessageAPI.MessageClickAction.INTEL_TAB, UNGP_SpecialistIntel.getInstance());
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


    protected static <T> void saveDataInCampaign(String key, T t) {
        UNGP_SharedData.saveRuleData(key, t);
    }

    /**
     * Called to save data.
     *
     * @param slot To distinguish the different data, it's not the *saving slot*.
     * @param t
     * @param <T>
     */
    protected <T> void saveDataInCampaign(int slot, T t) {
        saveDataInCampaign(rule.getId() + slot, t);
    }

    protected static <T> T getDataInCampaign(String key) {
        return UNGP_SharedData.loadRuleData(key);
    }

    /**
     * Called to get the data, might be null
     *
     * @param slot
     * @param <T>
     * @return
     */
    @Nullable
    protected <T> T getDataInCampaign(int slot) {
        return getDataInCampaign(rule.getId() + slot);
    }

    protected void clearDataInCampaign(int slot) {
        UNGP_SharedData.clearRuleData(rule.getId() + slot);
    }

    /**
     * 基于游戏种子，规则id，当前年，月，获取随机对象
     * Get random object based on game seed, rule id, current cycle and month
     *
     * @return
     */
    protected Random getRandom() {
        final CampaignClockAPI clock = Global.getSector().getClock();
        String sb = Global.getSector().getSeedString() + rule.getId() + clock.getCycle() + clock.getMonth();
        return new Random(sb.hashCode());
    }

    /**
     * @return
     */
    protected Random getRandomByDay() {
        final CampaignClockAPI clock = Global.getSector().getClock();
        String sb = Global.getSector().getSeedString() + rule.getId() + clock.getCycle() + clock.getMonth() + clock.getDay();
        return new Random(sb.hashCode());
    }

    /**
     * The same check as {@link com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription#isCivilian(FleetMemberAPI)}
     *
     * @param member
     * @return
     */
    public static boolean isCivilian(FleetMemberAPI member) {
        return BaseSkillEffectDescription.isCivilian(member);
//        return member.getVariant() != null &&
//                ((member.getVariant().hasHullMod(HullMods.CIVGRADE) && !member.getVariant().hasHullMod(HullMods.MILITARIZED_SUBSYSTEMS))
//                        || (!member.getVariant().hasHullMod(HullMods.CIVGRADE) && member.getHullSpec().getHints().contains(ShipHullSpecAPI.ShipTypeHints.CIVILIAN)));
    }


    /**
     * Should be called to reapply the buff by syncing the player fleet.
     */
    protected void forceSyncPlayerMemberBuff() {
        UNGP_PlayerFleetMemberBuff.forceSyncNextStep();
    }
}
