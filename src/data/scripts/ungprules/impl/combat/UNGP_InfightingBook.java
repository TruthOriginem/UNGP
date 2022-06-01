package data.scripts.ungprules.impl.combat;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import data.scripts.campaign.specialist.UNGP_SpecialistSettings;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import data.scripts.ungprules.tags.UNGP_CombatTag;
import org.lwjgl.util.vector.Vector2f;

public class UNGP_InfightingBook extends UNGP_BaseRuleEffect implements UNGP_CombatTag {
    private float multiplier;

    @Override
    public void updateDifficultyCache(UNGP_SpecialistSettings.Difficulty difficulty) {
        multiplier = getValueByDifficulty(0, difficulty);
    }

    //15%~25%
    @Override
    public float getValueByDifficulty(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return difficulty.getLinearValue(0.15f, 0.1f);
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
//        for (WeaponAPI weapon : ship.getAllWeapons()) {
//            if (weapon.isDecorative() || weapon.isBeam() || weapon.isBurstBeam()) continue;
//            if (weapon.getSpec().getMaxRange() <= 500) {
//                weapon.getDamage().getModifier().modifyMult(buffID, 1f + multiplier);
//            }
//        }
        if (!ship.hasListenerOfClass(InfightingBookDamageModifier.class)) {
            ship.addListener(new InfightingBookDamageModifier());
        }
    }

    private class InfightingBookDamageModifier implements DamageDealtModifier {
        @Override
        public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
            WeaponAPI weapon;
            if (param instanceof DamagingProjectileAPI) {
                weapon = ((DamagingProjectileAPI) param).getWeapon();
                if (weapon != null)
                    if (weapon.getSpec().getMaxRange() <= 500) {
                        damage.getModifier().modifyMult(buffID, 1f + multiplier);
                        return buffID;
                    }
            } else {
                return null;
            }
            return null;
        }
    }

    @Override
    public String getDescriptionParams(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return "500";
        if (index == 1) return getPercentString(getValueByDifficulty(0, difficulty) * 100f);
        return null;
    }

}
