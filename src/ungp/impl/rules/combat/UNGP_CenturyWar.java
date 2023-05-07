package ungp.impl.rules.combat;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import ungp.scripts.campaign.specialist.UNGP_SpecialistSettings;
import ungp.api.rules.UNGP_BaseRuleEffect;
import ungp.api.rules.tags.UNGP_CombatTag;

public class UNGP_CenturyWar extends UNGP_BaseRuleEffect implements UNGP_CombatTag {
    private float damageMultiplier;
    private float rangeBonus;

    @Override
    public void updateDifficultyCache(UNGP_SpecialistSettings.Difficulty difficulty) {
        damageMultiplier = 1f - getValueByDifficulty(0, difficulty) * 0.01f;
        rangeBonus = getValueByDifficulty(1, difficulty);
    }

    @Override
    public float getValueByDifficulty(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return difficulty.getLinearValue(20f, 10f);
        if (index == 1) return difficulty.getLinearValue(30f, 20f);
        return 0;
    }

    @Override
    public String getDescriptionParams(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return getPercentString(getValueByDifficulty(index, difficulty));
        if (index == 1) return getPercentString(getValueByDifficulty(index, difficulty));
        return super.getDescriptionParams(index, difficulty);
    }

    @Override
    public void advanceInCombat(CombatEngineAPI engine, float amount) {

    }

    @Override
    public void applyEnemyShipInCombat(float amount, ShipAPI enemy) {
        boolean shouldApply = false;
        if (enemy.isStation()) {
            shouldApply = true;
        } else if (enemy.isStationModule() && enemy.getParentStation().isStation()) {
            shouldApply = true;
        }
        if (shouldApply) {
            final MutableShipStatsAPI stats = enemy.getMutableStats();
            stats.getHullDamageTakenMult().modifyMult(buffID, damageMultiplier);
            stats.getArmorDamageTakenMult().modifyMult(buffID, damageMultiplier);
            stats.getShieldDamageTakenMult().modifyMult(buffID, damageMultiplier);
            stats.getEnergyWeaponRangeBonus().modifyPercent(buffID, rangeBonus);
            stats.getBallisticWeaponRangeBonus().modifyPercent(buffID, rangeBonus);
        }
    }

    @Override
    public void applyPlayerShipInCombat(float amount, CombatEngineAPI engine, ShipAPI ship) {

    }
}
