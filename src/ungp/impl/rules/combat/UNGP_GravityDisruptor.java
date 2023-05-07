package ungp.impl.rules.combat;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import ungp.scripts.campaign.specialist.UNGP_SpecialistSettings;
import ungp.api.rules.UNGP_BaseRuleEffect;
import ungp.api.rules.tags.UNGP_CombatTag;

public class UNGP_GravityDisruptor extends UNGP_BaseRuleEffect implements UNGP_CombatTag {
    private float playerMultiplier;
    private float enemyMultiplier;

    @Override
    public void updateDifficultyCache(UNGP_SpecialistSettings.Difficulty difficulty) {
        playerMultiplier = getValueByDifficulty(0, difficulty);
        enemyMultiplier = getValueByDifficulty(1, difficulty);
    }

    @Override
    public float getValueByDifficulty(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return difficulty.getLinearValue(0.02f, 0.01f);
        if (index == 1) return difficulty.getLinearValue(0.01f, 0.01f);
        return 1;
    }


    @Override
    public String getDescriptionParams(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0 || index == 1) return getPercentString(getValueByDifficulty(index, difficulty) * 100f);
        return super.getDescriptionParams(index, difficulty);
    }

    @Override
    public void advanceInCombat(CombatEngineAPI engine, float amount) {

    }

    @Override
    public void applyEnemyShipInCombat(float amount, ShipAPI enemy) {
        enemy.getMutableStats().getTimeMult().modifyMult(buffID, 1f + enemyMultiplier);
    }

    @Override
    public void applyPlayerShipInCombat(float amount, CombatEngineAPI engine, ShipAPI ship) {
        ship.getMutableStats().getTimeMult().modifyMult(buffID, 1f + playerMultiplier);
        if (engine.getPlayerShip() == ship) {
            engine.maintainStatusForPlayerShip(buffID, rule.getSpritePath(),
                                               rule.getName(),
                                               String.format(rule.getExtra1(), getFactorString(1f + playerMultiplier)), false);
        }
    }
}
