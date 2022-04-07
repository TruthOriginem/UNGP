package data.scripts.ungprules.impl.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.HullDamageAboutToBeTakenListener;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.campaign.specialist.UNGP_SpecialistSettings;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import data.scripts.ungprules.tags.UNGP_CombatInitTag;
import data.scripts.utils.UNGPUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.LinkedList;
import java.util.List;

public class UNGP_SymbolPerseus extends UNGP_BaseRuleEffect implements UNGP_CombatInitTag {

    public static final float MAX_TIME_PERIOD = 8f;
    public static final float TIME_TICK = 0.1f;

    public static final int MIN_KILL_COUNT = 2;
    public static final float FLUX_DECREASE_FACTOR = 0.15f;

    @Override
    public float getValueByDifficulty(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        return 0;
    }

    @Override
    public String getDescriptionParams(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return getFactorString(MAX_TIME_PERIOD);
        if (index == 1) return getFactorString(MIN_KILL_COUNT);
        if (index == 2) return getPercentString(FLUX_DECREASE_FACTOR * 100f);
        return super.getDescriptionParams(index, difficulty);
    }

    @Override
    public void init(CombatEngineAPI engine) {
        engine.getListenerManager().addListener(new SymbolPerseusListener(engine));
    }

    public static class SymbolPerseusListener implements HullDamageAboutToBeTakenListener, AdvanceableListener {

        private final IntervalUtil interval = new IntervalUtil(TIME_TICK, TIME_TICK);
        private final LinkedList<Integer> killCount = new LinkedList<>();
        private ShipAPI checkThisShipIfKilled = null;
        private int killCountInThisCheck = 0;

        public SymbolPerseusListener(final CombatEngineAPI engine) {
            for (int i = 0; i < MAX_TIME_PERIOD / TIME_TICK; i++) {
                killCount.add(0);
            }

            engine.addPlugin(new BaseEveryFrameCombatPlugin() {
                @Override
                public void advance(float amount, List<InputEventAPI> events) {
                    if (engine.isPaused()) return;
                    SymbolPerseusListener.this.advance(amount);
                }
            });
        }

        @Override
        public void advance(float amount) {

            if (checkThisShipIfKilled != null) {
                if (!checkThisShipIfKilled.isAlive()) { // alex, why do that
                    killCountInThisCheck++;
                }

                checkThisShipIfKilled = null;
            }

            interval.advance(amount);
            if (interval.intervalElapsed()) {
                killCount.removeFirst();
                killCount.addLast(killCountInThisCheck);

                int sum = 0;
                for (int i : killCount) {
                    sum += i;
                }

                boolean allowBonus = sum > MIN_KILL_COUNT;
                if (allowBonus && killCountInThisCheck > 0) {
                    ShipAPI player = Global.getCombatEngine().getPlayerShip();
                    player.getFluxTracker().decreaseFlux(player.getMaxFlux() * FLUX_DECREASE_FACTOR);
                    player.setJitterUnder(this, Color.CYAN, 2f, 5, 10f);
                    Global.getSoundPlayer().playSound("system_emp_emitter_impact", 1f, 1f, player.getLocation(), new Vector2f());
                    // maybe add some visual effect here will be better
                }

                killCountInThisCheck = 0;
            }
        }

        @Override
        public boolean notifyAboutToTakeHullDamage(Object param, ShipAPI ship, Vector2f point, float damageAmount) {

            if (UNGPUtils.isPlayerShip(ship)) return false;
            if (!ship.isAlive()) return false;
            if (ship.isFighter() || ship.isDrone()) return false;
            if (ship.isStation() || ship.isStationModule()) return false;

            if (param instanceof DamagingProjectileAPI) {
                DamagingProjectileAPI proj = (DamagingProjectileAPI) param;
                if (proj.getSource() != Global.getCombatEngine().getPlayerShip()) return false;
            } else if (param instanceof BeamAPI) {
                BeamAPI beam = (BeamAPI) param;
                if (beam.getSource() != Global.getCombatEngine().getPlayerShip()) return false;
            } else if (param instanceof CombatEntityAPI) {
                CombatEntityAPI entity = (CombatEntityAPI) param;
                if (entity != Global.getCombatEngine().getPlayerShip()) return false;
            } else return false;

            float hull = ship.getHitpoints();
            if (damageAmount >= hull) {
                checkThisShipIfKilled = ship; // i dont think someone can kill multi in single frame
            }

            return false;
        }
    }
}
