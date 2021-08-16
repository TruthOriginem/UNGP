package data.scripts.ungprules.impl.combat;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import data.scripts.ungprules.tags.UNGP_CombatTag;

public class UNGP_InfightingBook extends UNGP_BaseRuleEffect implements UNGP_CombatTag {
    private float multiplier;

    @Override
    public void updateDifficultyCache(int difficulty) {
        multiplier = getValueByDifficulty(1, difficulty);
    }

    //15%~25%
    @Override
    public float getValueByDifficulty(int index, int difficulty) {
        if (index == 1) return 0.13f + 0.02f * (float) Math.pow(difficulty, 0.598);
        return 1f;
    }

    @Override
    public void advanceInCombat(CombatEngineAPI engine, float amount) {

    }

    @Override
    public void applyEnemyShipInCombat(float amount, ShipAPI enemy) {

    }

    @Override
    public void applyPlayerShipInCombat(float amount, CombatEngineAPI engine, ShipAPI ship) {
        for (WeaponAPI weapon : ship.getAllWeapons()) {
            if (weapon.isDecorative() || weapon.isBeam() || weapon.isBurstBeam()) continue;
            if (weapon.getSpec().getMaxRange() < 500) {
                weapon.getDamage().getModifier().modifyMult(rule.getBuffID(), 1f + multiplier);
            }
        }
    }

    @Override
    public String getDescriptionParams(int index) {
        if (index == 0) return "500";
        if (index == 1) return getPercentString(multiplier * 100f);
        return null;
    }

    @Override
    public String getDescriptionParams(int index, int difficulty) {
        if (index == 0) return "500";
        if (index == 1) return getPercentString(getValueByDifficulty(index, difficulty) * 100f);
        return null;
    }

}
