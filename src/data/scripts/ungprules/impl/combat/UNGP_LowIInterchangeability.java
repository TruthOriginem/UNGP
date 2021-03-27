package data.scripts.ungprules.impl.combat;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import data.scripts.ungprules.tags.UNGP_CombatTag;

public class UNGP_LowIInterchangeability extends UNGP_BaseRuleEffect implements UNGP_CombatTag {
    private float multiplier;

    @Override
    public void updateDifficultyCache(int difficulty) {
        multiplier = getValueByDifficulty(0, difficulty);
    }

    @Override
    public float getValueByDifficulty(int index, int difficulty) {
        if (index == 0) return getLinearValue(1.1f, 1.25f, difficulty);
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
        MutableShipStatsAPI stats = ship.getMutableStats();
        final String id = rule.getBuffID();
        stats.getCombatEngineRepairTimeMult().modifyMult(id, multiplier);
        stats.getCombatWeaponRepairTimeMult().modifyMult(id, multiplier);
        stats.getEngineDamageTakenMult().modifyMult(id, multiplier);
        stats.getWeaponDamageTakenMult().modifyMult(id, multiplier);
    }



    @Override
    public String getDescriptionParams(int index) {
        if (index == 0) return getFactorString(multiplier);
        return null;
    }

    @Override
    public String getDescriptionParams(int index, int difficulty) {
        if (index == 0) return getFactorString(getValueByDifficulty(index, difficulty));
        return null;
    }

}
