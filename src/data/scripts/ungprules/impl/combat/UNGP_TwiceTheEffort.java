package data.scripts.ungprules.impl.combat;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.FighterWingAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import data.scripts.ungprules.tags.UNGP_CombatTag;

public class UNGP_TwiceTheEffort extends UNGP_BaseRuleEffect implements UNGP_CombatTag {
    private static final float REPLACEMENT_THRESHOLD = 0.5f;
    private float multiplier;

    @Override
    public void refreshDifficultyCache(int difficulty) {
        multiplier = getValueByDifficulty(0, difficulty);
    }

    //20%~40%
    @Override
    public float getValueByDifficulty(int index, int difficulty) {
        if (index == 0) return 0.16f + 0.04f * (float) Math.pow(difficulty, 0.5981);
        return 1f;
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
                    stats.getArmorDamageTakenMult().modifyMult(rule.getBuffID(), 1f + multiplier * level);
                    stats.getShieldDamageTakenMult().modifyMult(rule.getBuffID(), 1f + multiplier * level);
                    stats.getHullDamageTakenMult().modifyMult(rule.getBuffID(), 1f + multiplier * level);

                    stats.getBallisticWeaponDamageMult().modifyMult(rule.getBuffID(), 1f / (1f + multiplier * level));
                    stats.getEnergyWeaponDamageMult().modifyMult(rule.getBuffID(), 1f / (1f + multiplier * level));
                    stats.getMissileWeaponDamageMult().modifyMult(rule.getBuffID(), 1f / (1f + multiplier * level));
                }
            }
            if (ship == engine.getPlayerShip()) {
                engine.maintainStatusForPlayerShip(rule.getBuffID(),
                        rule.getSpritePath(),
                        rule.getName(),
                        rule.getRuleInfo().getExtra1() + (int) ((1f - multiplier * level) * 100f) + "%",
                        true);
            }
        }
    }

    @Override
    public String getDescriptionParams(int index) {
        if (index == 0) return (int) (REPLACEMENT_THRESHOLD * 100f) + "%";
        if (index == 1) return getPercentString(multiplier * 100f);
        return null;
    }

    @Override
    public String getDescriptionParams(int index, int difficulty) {
        if (index == 1) return getPercentString(getValueByDifficulty(0, difficulty) * 100f);
        return getDescriptionParams(index);
    }
}
