package data.scripts.ungprules.impl.combat;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import data.scripts.ungprules.UNGP_BaseRuleEffect;

import java.util.EnumSet;

public class UNGP_EnemyDamageIncrease extends UNGP_BaseRuleEffect {
    private float multiplier;

    @Override
    public void refreshDifficultyCache(int difficulty) {
        multiplier = getValueByDifficulty(0, difficulty);
    }

    @Override
    public float getValueByDifficulty(int index, int difficulty) {
        if (index == 0) return 1f + (0.05f + 0.05f * (float) Math.pow(difficulty, 0.4628));
        return 1f;
    }

    @Override
    public void applyEnemyShipInCombat(float amount, ShipAPI enemy) {
        MutableShipStatsAPI stats = enemy.getMutableStats();
        stats.getBallisticWeaponDamageMult().modifyMult(rule.getBuffID(), multiplier);
        stats.getEnergyWeaponDamageMult().modifyMult(rule.getBuffID(), multiplier);
        stats.getMissileWeaponDamageMult().modifyMult(rule.getBuffID(), multiplier);
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

    @Override
    public EnumSet<GameState> getEffectiveState() {
        return EnumSet.of(GameState.COMBAT);
    }
}
