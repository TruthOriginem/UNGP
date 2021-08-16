package data.scripts.ungprules.impl.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import data.scripts.ungprules.tags.UNGP_CombatTag;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class UNGP_CriticalHit extends UNGP_BaseRuleEffect implements UNGP_CombatTag {
    private static final float CRITICAL_HIT_CHANCE = 0.05f;

    @Override
    public float getValueByDifficulty(int index, int difficulty) {
        return 0;
    }

    @Override
    public String getDescriptionParams(int index) {
        if (index == 0) return getPercentString(CRITICAL_HIT_CHANCE * 100f);
        return null;
    }

    @Override
    public void advanceInCombat(CombatEngineAPI engine, float amount) {

    }

    @Override
    public void applyEnemyShipInCombat(float amount, ShipAPI enemy) {
    }

    @Override
    public void applyPlayerShipInCombat(float amount, CombatEngineAPI engine, ShipAPI ship) {
        if (engine.getPlayerShip() == ship) {
            if (!ship.hasListenerOfClass(CriticalHitListener.class)) {
                ship.addListener(new CriticalHitListener());
            }
        } else {
            ship.removeListenerOfClass(CriticalHitListener.class);
        }
    }

    private class CriticalHitListener implements DamageDealtModifier {
        @Override
        public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
            if (roll(CRITICAL_HIT_CHANCE)) {
                final String id = rule.getBuffID();
                if (damage.getDamage() >= 100) {
                    Global.getCombatEngine().addFloatingText(point, i18n.get("ct"), 30f, Color.red, target, 1f, 0f);
                }
                damage.getModifier().modifyMult(id, 2f);
                return id;
            }
            return null;
        }
    }
}
