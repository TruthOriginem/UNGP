package data.scripts.ungprules.impl.combat;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import data.scripts.ungprules.tags.UNGP_CombatTag;

public class UNGP_ExperimentalShunt extends UNGP_BaseRuleEffect implements UNGP_CombatTag {
    private float hardFluxFlat;


    @Override
    public void updateDifficultyCache(int difficulty) {
        hardFluxFlat = getValueByDifficulty(0, difficulty);
    }

    @Override
    public float getValueByDifficulty(int index, int difficulty) {
        if (index == 0) return getLinearValue(0.04f, 0.08f, difficulty);
        return 0;
    }

    @Override
    public String getDescriptionParams(int index) {
        if (index == 0) return getPercentString(hardFluxFlat * 100f);
        return null;
    }

    @Override
    public String getDescriptionParams(int index, int difficulty) {
        if (index == 0) return getPercentString(getValueByDifficulty(index, difficulty) * 100f);
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
        MutableShipStatsAPI stats = ship.getMutableStats();
        stats.getHardFluxDissipationFraction().modifyFlat(rule.getBuffID(), hardFluxFlat);
    }
}
