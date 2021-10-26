package data.scripts.ungprules.impl.combat;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import data.scripts.ungprules.tags.UNGP_CombatTag;

public class UNGP_FragmentGraveyard extends UNGP_BaseRuleEffect implements UNGP_CombatTag {

    private float velBonus;
    private float massBonus;

    @Override
    public void updateDifficultyCache(int difficulty) {
        this.velBonus = getValueByDifficulty(0, difficulty);
        this.massBonus = getValueByDifficulty(1, difficulty);
    }

    @Override
    public float getValueByDifficulty(int index, int difficulty) {
        if (index == 0) return getLinearValue(3f, 5f, difficulty);
        if (index == 1) return getLinearValue(1.5f, 2f, difficulty);
        return 1f;
    }

    @Override
    public String getDescriptionParams(int index, int difficulty) {
        if (index == 0) return getPercentString(getValueByDifficulty(index, difficulty) * 100f);
        if (index == 1) return getPercentString(getValueByDifficulty(index, difficulty) * 100f);
        return null;
    }

    @Override
    public void advanceInCombat(CombatEngineAPI engine, float amount) {
        for (ShipAPI ship : engine.getShips()) {
            if (!ship.isHulk()) continue;
            if (ship.getCustomData().containsKey(rule.getBuffID())) continue;

            ship.setCustomData(rule.getBuffID(), true);
            ship.getVelocity().scale(velBonus);
            ship.setMass(ship.getMass() * massBonus);
        }
    }

    @Override
    public void applyEnemyShipInCombat(float amount, ShipAPI ship) {
    }

    @Override
    public void applyPlayerShipInCombat(float amount, CombatEngineAPI engine, ShipAPI ship) {
    }
}
