package data.scripts.ungprules;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import data.scripts.campaign.UNGP_CampaignPlugin;

import java.math.BigDecimal;
import java.util.EnumSet;
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
    public abstract void refreshDifficultyCache(int difficulty);

    @Override
    public abstract float getValueByDifficulty(int index, int difficulty);

    @Override
    public abstract String getDescriptionParams(int index);

    @Override
    public String getDescriptionParams(int index, int difficulty) {
        return getDescriptionParams(index);
    }

    @Override
    public void advanceInCombat(CombatEngineAPI engine, float amount) {

    }

    @Override
    public void advanceInCampaign(float amount, UNGP_CampaignPlugin.TempCampaignParams params) {

    }

    @Override
    public void applyEnemyShipInCombat(float amount, ShipAPI enemy) {

    }

    @Override
    public void applyPlayerShipInCombat(float amount, CombatEngineAPI engine, ShipAPI ship) {
    }

    @Override
    public void applyPlayerFleetMemberInCampaign(FleetMemberAPI member) {

    }

    @Override
    public void applyPlayerCharacterStats(MutableCharacterStatsAPI stats) {

    }

    @Override
    public void unapplyPlayerCharacterStats(MutableCharacterStatsAPI stats) {

    }

    @Override
    public void applyPlayerFleetStats(CampaignFleetAPI fleet) {

    }

    @Override
    public void unapplyPlayerFleetStats(CampaignFleetAPI fleet) {

    }

    @Override
    public void applyGlobalStats() {

    }

    @Override
    public void unapplyGlobalStats() {

    }

    @Override
    public EnumSet<GameState> getEffectiveState() {
        return EnumSet.of(GameState.CAMPAIGN);
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
