package data.scripts.ungprules.impl.combat;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import data.scripts.ungprules.tags.UNGP_CombatTag;

public class UNGP_GravityDisruptor extends UNGP_BaseRuleEffect implements UNGP_CombatTag {
    private float playerMultiplier;
    private float enemyMultiplier;

    @Override
    public void updateDifficultyCache(int difficulty) {
        playerMultiplier = getValueByDifficulty(0, difficulty);
        enemyMultiplier = getValueByDifficulty(1, difficulty);
    }

    @Override
    public float getValueByDifficulty(int index, int difficulty) {
        if (index == 0) return getLinearValue(0.02f, 0.03f, difficulty);
        if (index == 1) return getLinearValue(0.01f, 0.02f, difficulty);
        return 1;
    }

    @Override
    public String getDescriptionParams(int index) {
        if (index == 0) return getPercentString(playerMultiplier * 100f);
        if (index == 1) return getPercentString(enemyMultiplier * 100f);
        return null;
    }

    @Override
    public String getDescriptionParams(int index, int difficulty) {
        if (index == 0 || index == 1) return getPercentString(getValueByDifficulty(index, difficulty) * 100f);
        return super.getDescriptionParams(index, difficulty);
    }

    @Override
    public void advanceInCombat(CombatEngineAPI engine, float amount) {

    }

    @Override
    public void applyEnemyShipInCombat(float amount, ShipAPI enemy) {
        enemy.getMutableStats().getTimeMult().modifyMult(rule.getBuffID(), 1f + enemyMultiplier);
    }

    @Override
    public void applyPlayerShipInCombat(float amount, CombatEngineAPI engine, ShipAPI ship) {
        ship.getMutableStats().getTimeMult().modifyMult(rule.getBuffID(), 1f + playerMultiplier);
        if (engine.getPlayerShip() == ship) {
            engine.maintainStatusForPlayerShip(rule.getBuffID(), rule.getSpritePath(),
                                               rule.getName(),
                                               String.format(rule.getExtra1(), getFactorString(1f + playerMultiplier)), false);
        }
    }
}
