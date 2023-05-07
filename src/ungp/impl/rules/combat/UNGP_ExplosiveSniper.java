package ungp.impl.rules.combat;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import ungp.scripts.campaign.specialist.UNGP_SpecialistSettings;
import ungp.api.rules.UNGP_BaseRuleEffect;
import ungp.api.rules.tags.UNGP_CombatTag;

public class UNGP_ExplosiveSniper extends UNGP_BaseRuleEffect implements UNGP_CombatTag {

	private float speedIncrease;
	private float turnIncrease;

    @Override
    public void updateDifficultyCache(UNGP_SpecialistSettings.Difficulty difficulty) {
        speedIncrease = getValueByDifficulty(0, difficulty);
		turnIncrease = getValueByDifficulty(1, difficulty);
    }

    @Override
    public float getValueByDifficulty(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return difficulty.getLinearValue(20f, 30f);
		if (index == 1) return difficulty.getLinearValue(10f, 10f);
        return 1f;
    }

    @Override
    public void advanceInCombat(CombatEngineAPI engine, float amount) {

    }

    @Override
    public void applyEnemyShipInCombat(float amount, ShipAPI enemy) {
        MutableShipStatsAPI stats = enemy.getMutableStats();
        stats.getMissileMaxSpeedBonus().modifyPercent(buffID, speedIncrease);
        stats.getMissileAccelerationBonus().modifyPercent(buffID, speedIncrease);
        stats.getMissileTurnAccelerationBonus().modifyPercent(buffID, turnIncrease);
        stats.getMissileMaxTurnRateBonus().modifyPercent(buffID, turnIncrease);
    }

    @Override
    public void applyPlayerShipInCombat(float amount, CombatEngineAPI engine, ShipAPI ship) {

    }

    @Override
    public String getDescriptionParams(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return getPercentString(getValueByDifficulty(index, difficulty));
		if (index == 1) return getPercentString(getValueByDifficulty(index, difficulty));
        return null;
    }
}
