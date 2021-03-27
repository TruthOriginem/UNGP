package data.scripts.ungprules.impl.combat;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import data.scripts.ungprules.tags.UNGP_CombatTag;

public class UNGP_BlockedFluxConduits extends UNGP_BaseRuleEffect implements UNGP_CombatTag {
    private float fluxMultiplier = 1f;

    @Override
    public void updateDifficultyCache(int difficulty) {
        fluxMultiplier = getValueByDifficulty(0, difficulty);
    }

    @Override
    public float getValueByDifficulty(int index, int difficulty) {
        if (index == 0) return getLinearValue(20f, 30f, difficulty);
        return 0;
    }

    @Override
    public String getDescriptionParams(int index) {
        if (index == 0) return getPercentString(fluxMultiplier);
        return null;
    }

    @Override
    public String getDescriptionParams(int index, int difficulty) {
        if (index == 0) return getPercentString(getValueByDifficulty(index, difficulty));
        return super.getDescriptionParams(index, difficulty);
    }

    @Override
    public void advanceInCombat(CombatEngineAPI engine, float amount) {

    }

    @Override
    public void applyEnemyShipInCombat(float amount, ShipAPI enemy) {

    }

    @Override
    public void applyPlayerShipInCombat(float amount, CombatEngineAPI engine, ShipAPI ship) {
        if (ship.getShield() != null) {
            MutableShipStatsAPI stats = ship.getMutableStats();
            String id = rule.getBuffID();
            if (ship.getShield().isOn()) {
                stats.getFluxDissipation().modifyMult(id, 1f - fluxMultiplier*0.01f);
                if (engine.getPlayerShip() == ship) {
                    engine.maintainStatusForPlayerShip(id, rule.getSpritePath(),
                            rule.getName(),
                            String.format("%s%.1f", rule.getExtra1(), fluxMultiplier), true);
                }
            } else {
                stats.getFluxDissipation().unmodify(id);
            }
        }
    }
}
