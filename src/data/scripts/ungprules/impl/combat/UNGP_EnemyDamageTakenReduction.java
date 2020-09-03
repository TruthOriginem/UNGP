package data.scripts.ungprules.impl.combat;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import data.scripts.ungprules.tags.UNGP_CombatTag;

public class UNGP_EnemyDamageTakenReduction extends UNGP_BaseRuleEffect implements UNGP_CombatTag {
    private float multiplier;

    @Override
    public void refreshDifficultyCache(int difficulty) {
        multiplier = getValueByDifficulty(0, difficulty);
    }

    //10~20
    @Override
    public float getValueByDifficulty(int index, int difficulty) {
        if (index == 0) return 1f - (0.05f + 0.05f * (float) Math.pow(difficulty, 0.3668));
        return 1f;
    }

    @Override
    public void applyEnemyShipInCombat(float amount, ShipAPI enemy) {
        MutableShipStatsAPI stats = enemy.getMutableStats();
        stats.getHullDamageTakenMult().modifyMult(rule.getBuffID(), multiplier);
        stats.getShieldDamageTakenMult().modifyMult(rule.getBuffID(), multiplier);
        stats.getArmorDamageTakenMult().modifyMult(rule.getBuffID(), multiplier);
    }

    @Override
    public String getDescriptionParams(int index) {
        if (index == 0) getFactorString(multiplier);
        return null;
    }

    @Override
    public String getDescriptionParams(int index, int difficulty) {
        if (index == 0) return getFactorString(getValueByDifficulty(index, difficulty));
        return null;
    }
}
