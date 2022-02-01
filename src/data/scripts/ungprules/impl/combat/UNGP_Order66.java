package data.scripts.ungprules.impl.combat;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import data.scripts.campaign.specialist.UNGP_SpecialistSettings;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import data.scripts.ungprules.tags.UNGP_CombatTag;
import org.lwjgl.util.vector.Vector2f;

public class UNGP_Order66 extends UNGP_BaseRuleEffect implements UNGP_CombatTag {

    private float damageBonus;

    @Override
    public void updateDifficultyCache(UNGP_SpecialistSettings.Difficulty difficulty) {
        this.damageBonus = getValueByDifficulty(0, difficulty);
    }

    @Override
    public float getValueByDifficulty(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return difficulty.getLinearValue(50f, 50f);
        return 0f;
    }

    @Override
    public String getDescriptionParams(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return getPercentString(getValueByDifficulty(index, difficulty));
        return null;
    }

    @Override
    public void advanceInCombat(CombatEngineAPI engine, float amount) {
    }

    @Override
    public void applyEnemyShipInCombat(float amount, ShipAPI ship) {
    }

    @Override
    public void applyPlayerShipInCombat(float amount, CombatEngineAPI engine, ShipAPI ship) {
        if (!ship.hasListenerOfClass(Order66Listener.class)) {
            ship.addListener(new Order66Listener());
        }
    }

    private class Order66Listener implements DamageTakenModifier {

        private Order66Listener() {
        }

        @Override
        public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
            if (param instanceof DamagingProjectileAPI) {
                DamagingProjectileAPI proj = (DamagingProjectileAPI) param;
                if (proj.getOwner() == target.getOwner()) {
                    damage.getModifier().modifyPercent(buffID, damageBonus);
                    return buffID;
                }

            } else if (param instanceof BeamAPI) {
                BeamAPI beam = (BeamAPI) param;
                if (beam.getSource().getOwner() == target.getOwner()) {
                    damage.getModifier().modifyPercent(buffID, damageBonus);
                    return buffID;
                }

            } else if (param instanceof CombatEntityAPI) { // collision, yeah
                CombatEntityAPI crash = (CombatEntityAPI) param;
                if (crash.getOwner() == target.getOwner()) {
                    damage.getModifier().modifyPercent(buffID, damageBonus);
                    return buffID;
                }
            }

            return null;
        }
    }
}
