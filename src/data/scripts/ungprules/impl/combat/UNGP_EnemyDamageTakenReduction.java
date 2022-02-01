package data.scripts.ungprules.impl.combat;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import data.scripts.campaign.specialist.UNGP_SpecialistSettings;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import data.scripts.ungprules.tags.UNGP_CombatTag;

public class UNGP_EnemyDamageTakenReduction extends UNGP_BaseRuleEffect implements UNGP_CombatTag {
    private float multiplier;

    @Override
    public void updateDifficultyCache(UNGP_SpecialistSettings.Difficulty difficulty) {
        multiplier = getValueByDifficulty(0, difficulty);
    }

    //10~20
    @Override
    public float getValueByDifficulty(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return 1f - difficulty.getLinearValue(0.1f, 0.1f);
        return 1f;
    }

    @Override
    public void advanceInCombat(CombatEngineAPI engine, float amount) {

    }

    @Override
    public void applyEnemyShipInCombat(float amount, ShipAPI enemy) {
        MutableShipStatsAPI stats = enemy.getMutableStats();
        stats.getHullDamageTakenMult().modifyMult(buffID, multiplier);
        stats.getShieldDamageTakenMult().modifyMult(buffID, multiplier);
        stats.getArmorDamageTakenMult().modifyMult(buffID, multiplier);
    }

    @Override
    public void applyPlayerShipInCombat(float amount, CombatEngineAPI engine, ShipAPI ship) {

    }

    @Override
    public String getDescriptionParams(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return getFactorString(getValueByDifficulty(index, difficulty));
        return null;
    }
}
