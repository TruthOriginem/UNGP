package data.scripts.ungprules.impl.combat;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import data.scripts.ungprules.UNGP_BaseRuleEffect;

import java.util.EnumSet;

public class UNGP_LowIInterchangeability extends UNGP_BaseRuleEffect {
    private float multiplier;

    @Override
    public void refreshDifficultyCache(int difficulty) {
        multiplier = getValueByDifficulty(0, difficulty);
    }

    @Override
    public float getValueByDifficulty(int index, int difficulty) {
        if (index == 0) return 0.5f + (float) Math.pow(difficulty, 0.23138);
        return 1f;
    }

    @Override
    public void applyEnemyShipInCombat(float amount, ShipAPI enemy) {
        MutableShipStatsAPI stats = enemy.getMutableStats();
        stats.getCombatEngineRepairTimeMult().modifyMult(rule.getBuffID(), multiplier);
        stats.getCombatWeaponRepairTimeMult().modifyMult(rule.getBuffID(), multiplier);
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

    @Override
    public EnumSet<GameState> getEffectiveState() {
        return EnumSet.of(GameState.COMBAT);
    }
}
