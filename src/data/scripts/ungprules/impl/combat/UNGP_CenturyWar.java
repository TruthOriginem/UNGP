package data.scripts.ungprules.impl.combat;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import data.scripts.ungprules.tags.UNGP_CombatTag;

public class UNGP_CenturyWar extends UNGP_BaseRuleEffect implements UNGP_CombatTag {
    private float damageMultiplier;
    private float rangeBonus;


    @Override
    public void updateDifficultyCache(int difficulty) {
        damageMultiplier = 1f - getValueByDifficulty(0, difficulty) * 0.01f;
        rangeBonus = getValueByDifficulty(1, difficulty);
    }

    @Override
    public float getValueByDifficulty(int index, int difficulty) {
        if (index == 0) return getLinearValue(15f, 30f, difficulty);
        if (index == 1) return getLinearValue(25f, 50f, difficulty);
        return 0;
    }

    @Override
    public String getDescriptionParams(int index, int difficulty) {
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
            final String buffID = rule.getBuffID();
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
