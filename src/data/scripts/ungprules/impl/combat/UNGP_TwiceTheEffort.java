package data.scripts.ungprules.impl.combat;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.FighterWingAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import data.scripts.campaign.specialist.UNGP_SpecialistSettings;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import data.scripts.ungprules.tags.UNGP_CombatTag;

public class UNGP_TwiceTheEffort extends UNGP_BaseRuleEffect implements UNGP_CombatTag {
    private static final float REPLACEMENT_THRESHOLD = 0.5f;
    private float multiplier;

    @Override
    public void updateDifficultyCache(UNGP_SpecialistSettings.Difficulty difficulty) {
        multiplier = getValueByDifficulty(0, difficulty);
    }

    //20%~40%
    @Override
    public float getValueByDifficulty(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return difficulty.getLinearValue(0.2f, 0.2f);
        return 1f;
    }

    @Override
    public void advanceInCombat(CombatEngineAPI engine, float amount) {


    }

    @Override
    public void applyEnemyShipInCombat(float amount, ShipAPI enemy) {

    }

    @Override
    public void applyPlayerShipInCombat(float amount, CombatEngineAPI engine, ShipAPI ship) {
        if (ship.getNumFighterBays() > 0) {
            float replacementRate = ship.getSharedFighterReplacementRate();
            float level = (replacementRate - REPLACEMENT_THRESHOLD) / REPLACEMENT_THRESHOLD;
            level = 1f - Math.max(0, level);
            for (FighterWingAPI wing : ship.getAllWings()) {
                for (ShipAPI fighter : wing.getWingMembers()) {
                    MutableShipStatsAPI stats = fighter.getMutableStats();
                    stats.getArmorDamageTakenMult().modifyMult(buffID, 1f + multiplier * level);
                    stats.getShieldDamageTakenMult().modifyMult(buffID, 1f + multiplier * level);
                    stats.getHullDamageTakenMult().modifyMult(buffID, 1f + multiplier * level);

                    stats.getBallisticWeaponDamageMult().modifyMult(buffID, 1f / (1f + multiplier * level));
                    stats.getEnergyWeaponDamageMult().modifyMult(buffID, 1f / (1f + multiplier * level));
                    stats.getMissileWeaponDamageMult().modifyMult(buffID, 1f / (1f + multiplier * level));
                }
            }
            if (ship == engine.getPlayerShip()) {
                engine.maintainStatusForPlayerShip(buffID,
                                                   rule.getSpritePath(),
                                                   rule.getName(),
                                                   rule.getExtra1() + (int) ((1f - multiplier * level) * 100f) + "%",
                                                   true);
            }
        }
    }

    @Override
    public String getDescriptionParams(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return (int) (REPLACEMENT_THRESHOLD * 100f) + "%";
        if (index == 1) return getPercentString(getValueByDifficulty(0, difficulty) * 100f);
        return null;
    }
}
