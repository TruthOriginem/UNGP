package ungp.impl.rules.combat;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import org.lwjgl.util.vector.Vector2f;
import ungp.api.rules.UNGP_BaseRuleEffect;
import ungp.api.rules.tags.UNGP_CombatInitTag;
import ungp.api.rules.tags.UNGP_CombatTag;
import ungp.scripts.campaign.specialist.UNGP_SpecialistSettings;

public class UNGP_IHaveActivated extends UNGP_BaseRuleEffect implements UNGP_CombatTag, UNGP_CombatInitTag {
    private static final String DATA_KEY = "UNGP_i_have_activated_data";
    private static final float PENALTY_MULT = 0.75f;
    private static final float BUFF_MULT = 1.10f;
    private static final float CHARGE_TIME = 4f;
    private static final float BUFF_DURATION = 30f;
    private static final float DECAY_PER_SECOND = 0.1f;

    private static class ActivationData {
        private boolean activated = false;
        private float progress = 0f;
        private float buffRemaining = 0f;
    }

    @Override
    public void init(CombatEngineAPI engine) {
        putDataInEngine(engine, DATA_KEY, new ActivationData());
    }

    @Override
    public void advanceInCombat(CombatEngineAPI engine, float amount) {
        ShipAPI playerShip = engine.getPlayerShip();
        if (playerShip == null || playerShip.isShuttlePod()) return;
        ActivationData data = getDataInEngine(engine, DATA_KEY);
        if (data == null) return;
        if (amount > 0f) {
            advanceState(playerShip, data, amount);
        }
        if (data.activated) {
            engine.maintainStatusForPlayerShip(buffID, rule.getSpritePath(), rule.getName(),
                                               String.format(rule.getExtra2(), String.format("%.1f", data.buffRemaining)), false);
        } else {
            engine.maintainStatusForPlayerShip(buffID, rule.getSpritePath(), rule.getName(),
                                               String.format(rule.getExtra1(), (int) (data.progress * 100f) + "%"), true);
        }
    }

    private void advanceState(ShipAPI playerShip, ActivationData data, float amount) {
        if (data.activated) {
            data.buffRemaining -= amount;
            if (data.buffRemaining <= 0f) {
                data.activated = false;
                data.buffRemaining = 0f;
                data.progress = 0f;
            }
            return;
        }
        int totalWeapons = 0;
        int firingWeapons = 0;
        for (WeaponAPI weapon : playerShip.getAllWeapons()) {
            if (weapon.isDecorative() || isPurePD(weapon)) continue;
            totalWeapons++;
            if (!weapon.isDisabled() && weapon.isFiring()) {
                firingWeapons++;
            }
        }
        float halfWeapons = totalWeapons * 0.5f;
        float efficiency = halfWeapons <= 0f ? 0f : Math.min(1f, firingWeapons / halfWeapons);
        if (firingWeapons > 0) {
            data.progress = Math.min(1f, data.progress + amount * efficiency / CHARGE_TIME);
            if (data.progress >= 1f) {
                data.activated = true;
                data.buffRemaining = BUFF_DURATION;
            }
        } else if (data.progress > 0f) {
            data.progress = Math.max(0f, data.progress - amount * DECAY_PER_SECOND);
        }
    }

    private static boolean isPurePD(WeaponAPI weapon) {
        if (weapon.hasAIHint(WeaponAPI.AIHints.PD_ONLY)) return true;
        return weapon.hasAIHint(WeaponAPI.AIHints.PD) && !weapon.hasAIHint(WeaponAPI.AIHints.PD_ALSO);
    }

    @Override
    public String getDescriptionParams(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return getPercentString(25f);
        if (index == 1) return getFactorString(CHARGE_TIME);
        if (index == 2) return getPercentString(10f);
        if (index == 3) return getFactorString(BUFF_DURATION);
        return null;
    }

    @Override
    public void applyEnemyShipInCombat(float amount, ShipAPI enemy) {
    }

    @Override
    public void applyPlayerShipInCombat(float amount, CombatEngineAPI engine, ShipAPI ship) {
        if (ship == engine.getPlayerShip()) {
            if (!ship.hasListenerOfClass(ActivationListener.class)) {
                ship.addListener(new ActivationListener(engine, ship));
            }
        } else {
            ship.removeListenerOfClass(ActivationListener.class);
        }
    }

    private class ActivationListener implements DamageDealtModifier, AdvanceableListener {
        private final CombatEngineAPI engine;
        private final ShipAPI ship;

        private ActivationListener(CombatEngineAPI engine, ShipAPI ship) {
            this.engine = engine;
            this.ship = ship;
        }

        @Override
        public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
            ActivationData data = getDataInEngine(engine, DATA_KEY);
            if (data == null) return null;
            damage.getModifier().modifyMult(buffID, data.activated ? BUFF_MULT : PENALTY_MULT);
            if (data.activated && target instanceof ShipAPI) {
                ShipAPI targetShip = (ShipAPI) target;
                if (targetShip.getOwner() != ship.getOwner() && !targetShip.isHulk()) {
                    data.buffRemaining = BUFF_DURATION;
                }
            }
            return buffID;
        }

        @Override
        public void advance(float amount) {
            if (ship != engine.getPlayerShip()) {
                ship.removeListenerOfClass(ActivationListener.class);
            }
        }
    }
}
