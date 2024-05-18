package ungp.impl.rules.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import org.lwjgl.util.vector.Vector2f;
import ungp.api.rules.UNGP_BaseRuleEffect;
import ungp.api.rules.tags.UNGP_CombatTag;
import ungp.scripts.campaign.specialist.UNGP_SpecialistSettings;

public class UNGP_LetThereBeLight extends UNGP_BaseRuleEffect implements UNGP_CombatTag {
    private float beamDmgBonus = 0f;

    @Override
    public void updateDifficultyCache(UNGP_SpecialistSettings.Difficulty difficulty) {
        beamDmgBonus = getValueByDifficulty(0, difficulty);
    }

    @Override
    public float getValueByDifficulty(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        return difficulty.getLinearValue(10f, 5f, 15f);
    }

    @Override
    public String getDescriptionParams(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
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
        if (ship == engine.getPlayerShip() && !ship.hasListenerOfClass(BeamDamageModifier.class)) {
            ship.addListener(new BeamDamageModifier(ship));
        }
    }

    private class BeamDamageModifier implements DamageDealtModifier, AdvanceableListener {

        private ShipAPI ship;

        public BeamDamageModifier(ShipAPI ship) {
            this.ship = ship;
        }

        @Override
        public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
            if (shieldHit && param instanceof BeamAPI) {
                damage.getModifier().modifyPercent(buffID, beamDmgBonus);
                return buffID;
            }
            return null;
        }

        @Override
        public void advance(float amount) {
            if (ship != Global.getCombatEngine().getPlayerShip()) {
                ship.removeListenerOfClass(BeamDamageModifier.class);
            }
        }
    }
}
