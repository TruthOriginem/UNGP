package data.scripts.ungprules.impl.combat;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import data.scripts.campaign.specialist.UNGP_SpecialistSettings;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import data.scripts.ungprules.tags.UNGP_CombatTag;

public class UNGP_StrikerSchool extends UNGP_BaseRuleEffect implements UNGP_CombatTag {
    private float rateMultiplier;
    private float replacementMultiplier;

    @Override
    public void updateDifficultyCache(UNGP_SpecialistSettings.Difficulty difficulty) {
        rateMultiplier = getValueByDifficulty(0, difficulty);
        replacementMultiplier = getValueByDifficulty(1, difficulty);
    }

    @Override
    public float getValueByDifficulty(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return difficulty.getLinearValue(25f, 25f);
        if (index == 1) return difficulty.getLinearValue(20f, 20f);
        return 0;
    }

    @Override
    public String getDescriptionParams(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return getPercentString(getValueByDifficulty(index, difficulty));
		if (index == 1) return getPercentString(getValueByDifficulty(0, difficulty));
        if (index == 2) return getPercentString(getValueByDifficulty(1, difficulty));
        return super.getDescriptionParams(index, difficulty);
    }

    @Override
    public void advanceInCombat(CombatEngineAPI engine, float amount) {

    }

    @Override
    public void applyEnemyShipInCombat(float amount, ShipAPI enemy) {
		MutableShipStatsAPI stats = enemy.getMutableStats();
		stats.getDynamic().getMod(Stats.REPLACEMENT_RATE_INCREASE_MULT).modifyPercent(buffID, rateMultiplier);
		stats.getDynamic().getMod(Stats.REPLACEMENT_RATE_DECREASE_MULT).modifyMult(buffID, 1f - rateMultiplier * 0.01f);
		stats.getFighterRefitTimeMult().modifyMult(buffID, 1f - replacementMultiplier * 0.01f);
    }

    @Override
    public void applyPlayerShipInCombat(float amount, CombatEngineAPI engine, ShipAPI ship) {

    }
}
