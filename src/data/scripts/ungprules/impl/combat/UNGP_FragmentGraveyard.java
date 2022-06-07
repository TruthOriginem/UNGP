package data.scripts.ungprules.impl.combat;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import data.scripts.campaign.specialist.UNGP_SpecialistSettings;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import data.scripts.ungprules.tags.UNGP_CombatTag;

public class UNGP_FragmentGraveyard extends UNGP_BaseRuleEffect implements UNGP_CombatTag {

    private float velBonus;
    private float massBonus;
    private float breakProbBonus;

    @Override
    public void updateDifficultyCache(UNGP_SpecialistSettings.Difficulty difficulty) {
        this.velBonus = getValueByDifficulty(0, difficulty);
        this.massBonus = getValueByDifficulty(1, difficulty);
		this.breakProbBonus = getValueByDifficulty(2, difficulty);
    }

    @Override
    public float getValueByDifficulty(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return difficulty.getLinearValue(4f, 2f);
        if (index == 1) return difficulty.getLinearValue(3f, 2f);
		if (index == 1) return difficulty.getLinearValue(50f, 50f);
        return 1f;
    }

    @Override
    public String getDescriptionParams(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return getPercentString(getValueByDifficulty(index, difficulty) * 100f);
        if (index == 1) return getPercentString(getValueByDifficulty(index, difficulty) * 100f);
        return null;
    }

    @Override
    public void advanceInCombat(CombatEngineAPI engine, float amount) {
        for (ShipAPI ship : engine.getShips()) {
            if (ship.isAlive()) continue;
            if (!ship.isHulk()) continue;
            if (ship.getCustomData().containsKey(buffID)) continue;

            ship.setCustomData(buffID, true);
            ship.getVelocity().scale(velBonus);
            ship.setMass(ship.getMass() * massBonus);
        }
    }

    @Override
    public void applyEnemyShipInCombat(float amount, ShipAPI ship) {
    	ship.getMutableStats().getBreakProb().modifyPercent(buffID, breakProbBonus);
    }

    @Override
    public void applyPlayerShipInCombat(float amount, CombatEngineAPI engine, ShipAPI ship) {
		ship.getMutableStats().getBreakProb().modifyPercent(buffID, breakProbBonus);
    }
}
