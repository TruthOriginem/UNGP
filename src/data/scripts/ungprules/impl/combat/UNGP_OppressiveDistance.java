package data.scripts.ungprules.impl.combat;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import data.scripts.ungprules.tags.UNGP_CombatTag;

public class UNGP_OppressiveDistance extends UNGP_BaseRuleEffect implements UNGP_CombatTag {
    private float factor;

    @Override
    public void updateDifficultyCache(int difficulty) {
        factor = getValueByDifficulty(0, difficulty);
    }

    //10~20
    @Override
    public float getValueByDifficulty(int index, int difficulty) {
        if (index == 0) return 0.05f + 0.05f * (float) Math.pow(difficulty, 0.3668);
        return 1f;
    }

    @Override
    public void advanceInCombat(CombatEngineAPI engine, float amount) {

    }

    @Override
    public void applyEnemyShipInCombat(float amount, ShipAPI enemy) {
        MutableShipStatsAPI stats = enemy.getMutableStats();
        float bonus = factor * 100f;
        stats.getBallisticWeaponRangeBonus().modifyPercent(rule.getBuffID(), bonus);
        stats.getEnergyWeaponRangeBonus().modifyPercent(rule.getBuffID(), bonus);
        stats.getSensorStrength().modifyMult(rule.getBuffID(), bonus);
    }

    @Override
    public void applyPlayerShipInCombat(float amount, CombatEngineAPI engine, ShipAPI ship) {

    }

    @Override
    public String getDescriptionParams(int index) {
        if (index == 0) getPercentString(factor * 100f);
        return null;
    }

    @Override
    public String getDescriptionParams(int index, int difficulty) {
        if (index == 0) return getPercentString(getValueByDifficulty(index, difficulty) * 100f);
        return null;
    }
}
