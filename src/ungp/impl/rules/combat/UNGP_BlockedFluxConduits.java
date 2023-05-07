package ungp.impl.rules.combat;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import ungp.api.rules.UNGP_BaseRuleEffect;
import ungp.api.rules.tags.UNGP_CombatTag;

import static ungp.scripts.campaign.specialist.UNGP_SpecialistSettings.Difficulty;

public class UNGP_BlockedFluxConduits extends UNGP_BaseRuleEffect implements UNGP_CombatTag {
    private float fluxReduction = 15f;

    @Override
    public void updateDifficultyCache(Difficulty difficulty) {
        fluxReduction = getValueByDifficulty(0, difficulty);
    }

    @Override
    public float getValueByDifficulty(int index, Difficulty difficulty) {
        if (index == 0) return difficulty.getLinearValue(15f, 10f);
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
            if (shield.isOn()) {
                float arcLevel = 1f - shield.getActiveArc() / shield.getArc();
                arcLevel = Math.min(Math.max(0, arcLevel), 1);
                arcLevel *= arcLevel;
                arcLevel = 1f - arcLevel;
                float finalReduction = fluxReduction * arcLevel;
                stats.getFluxDissipation().modifyPercent(buffID, -finalReduction);
                if (engine.getPlayerShip() == ship) {
                    engine.maintainStatusForPlayerShip(buffID, rule.getSpritePath(),
                                                       rule.getName(),
                                                       rule.getExtra1() + (int) finalReduction + "%", true);
                }
            } else {
                stats.getFluxDissipation().unmodify(buffID);
            }
        }
    }
}
