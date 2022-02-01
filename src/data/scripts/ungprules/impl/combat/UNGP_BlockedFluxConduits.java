package data.scripts.ungprules.impl.combat;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import data.scripts.ungprules.tags.UNGP_CombatTag;

import static data.scripts.campaign.specialist.UNGP_SpecialistSettings.Difficulty;

public class UNGP_BlockedFluxConduits extends UNGP_BaseRuleEffect implements UNGP_CombatTag {
    private float fluxReduction = 20f;

    @Override
    public void updateDifficultyCache(Difficulty difficulty) {
        fluxReduction = getValueByDifficulty(0, difficulty);
    }

    @Override
    public float getValueByDifficulty(int index, Difficulty difficulty) {
        if (index == 0) return difficulty.getLinearValue(20f, 10f);
        return 0;
    }

    @Override
    public String getDescriptionParams(int index, Difficulty difficulty) {
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
        ShieldAPI shield = ship.getShield();
        if (shield != null) {
            MutableShipStatsAPI stats = ship.getMutableStats();
            String id = rule.getBuffID();
            if (shield.isOn()) {
                float arcLevel = 1f - shield.getActiveArc() / shield.getArc();
                arcLevel = Math.min(Math.max(0, arcLevel), 1);
                arcLevel *= arcLevel;
                arcLevel = 1f - arcLevel;
                float finalReduction = fluxReduction * arcLevel;
                stats.getFluxDissipation().modifyPercent(id, -finalReduction);
                if (engine.getPlayerShip() == ship) {
                    engine.maintainStatusForPlayerShip(id, rule.getSpritePath(),
                                                       rule.getName(),
                                                       rule.getExtra1() + (int) finalReduction + "%", true);
                }
            } else {
                stats.getFluxDissipation().unmodify(id);
            }
        }
    }
}
