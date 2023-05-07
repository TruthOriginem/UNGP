package ungp.impl.rules.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import ungp.scripts.campaign.specialist.UNGP_SpecialistSettings;
import ungp.api.rules.UNGP_BaseRuleEffect;
import ungp.api.rules.tags.UNGP_CombatTag;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class UNGP_LuckySeven extends UNGP_BaseRuleEffect implements UNGP_CombatTag {
    private static final float CRITICAL_HIT_CHANCE = 0.07f;
	private static final float CRITICAL_HIT_CHANCE_DOUBLE = 0.07f;

    @Override
    public float getValueByDifficulty(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        return 0;
    }


    @Override
    public String getDescriptionParams(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return "7";
		if (index == 1) return "7%";
		if (index == 2) return "77%";
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
        if (engine.getPlayerShip() == ship) {
            if (!ship.hasListenerOfClass(LuckySevenListener.class)) {
                ship.addListener(new LuckySevenListener());
            }
        } else {
            ship.removeListenerOfClass(LuckySevenListener.class);
        }
    }

    private class LuckySevenListener implements DamageDealtModifier {

    	int damageCount = 0;

        @Override
        public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {

        	if (param instanceof DamagingProjectileAPI) {
				damageCount++;
				if (damageCount == 7) {
					damageCount = 0;

					if (roll(CRITICAL_HIT_CHANCE)) {
						if (roll(CRITICAL_HIT_CHANCE_DOUBLE)) {
							Global.getCombatEngine().addFloatingText(point, i18n.get("ct_777"), 40f, Color.red, target, 1f, 0f);
							damage.getModifier().modifyPercent(buffID, 777f);
						} else {
							if (damage.getDamage() >= 150f) Global.getCombatEngine().addFloatingText(point, i18n.get("ct_77"), 30f, Color.red, target, 1f, 0f);
							damage.getModifier().modifyPercent(buffID, 77f);
						}
					} else {
						if (damage.getDamage() >= 250f) Global.getCombatEngine().addFloatingText(point, i18n.get("ct_7"), 20f, Color.red, target, 1f, 0f);
						damage.getModifier().modifyPercent(buffID, 7f);
					}

					return buffID;
				}
			}

            return null;
        }
    }
}
