package ungp.impl.rules.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import org.lazywizard.lazylib.MathUtils;
import ungp.api.rules.UNGP_BaseRuleEffect;
import ungp.api.rules.tags.UNGP_CombatInitTag;
import ungp.scripts.campaign.specialist.UNGP_SpecialistSettings;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class UNGP_VForVent extends UNGP_BaseRuleEffect implements UNGP_CombatInitTag {
    private static final float VENT_BONUS = 15f;
    private static final float BONUS_TIME = 0.33f;

    @Override
    public void init(CombatEngineAPI engine) {
        engine.addPlugin(new VTriggerPlugin());
    }

    private class VTriggerPlugin extends BaseEveryFrameCombatPlugin {
        private class TimeRemaining {
            float activeRemaining;

            public TimeRemaining(float activeRemaining) {
                this.activeRemaining = activeRemaining;
            }
        }

        private LinkedList<TimeRemaining> timeoutQueue = new LinkedList<>();
        private ShipAPI flagShip;
        private CombatEngineAPI engine;

        @Override
        public void init(CombatEngineAPI engine) {
            this.engine = engine;
        }

        @Override
        public void advance(float amount, List<InputEventAPI> events) {
            if (engine.isPaused()) return;
            ShipAPI playerShip = engine.getPlayerShip();
            if (flagShip != playerShip) {
                if (flagShip != null)
                    flagShip.getMutableStats().getVentRateMult().unmodify(buffID);
                flagShip = playerShip;
                timeoutQueue.clear();
            }
            if (playerShip != null && playerShip.getFluxTracker().isVenting()) {
                for (InputEventAPI event : events) {
                    if (event.isConsumed()) continue;
                    // 按键弹起
                    if (event.isControlUpEvent("SHIP_VENT_FLUX")) {
                        timeoutQueue.addLast(new TimeRemaining(BONUS_TIME));
                        float angle = MathUtils.getRandomNumberInRange(0, 360);
                        float radius = playerShip.getCollisionRadius() * MathUtils.getRandomNumberInRange(0.2f, 0.4f);
                        engine.spawnEmpArcVisual(MathUtils.getPointOnCircumference(playerShip.getLocation(), radius, angle),
                                                 playerShip,
                                                 MathUtils.getPointOnCircumference(playerShip.getLocation(), radius * 2f, angle), playerShip,
                                                 10f, playerShip.getVentFringeColor(), playerShip.getVentCoreColor());
                        break;
                    }
                }
                int timeoutSize = timeoutQueue.size();
                if (timeoutSize > 2) timeoutSize = 2;
                if (timeoutSize > 0) {
                    Iterator<TimeRemaining> iterator = timeoutQueue.iterator();
                    float maxRemaining = 0f;
                    while (iterator.hasNext()) {
                        TimeRemaining next = iterator.next();
                        next.activeRemaining -= amount;
                        if (next.activeRemaining > maxRemaining) {
                            maxRemaining = next.activeRemaining;
                        }
                        if (next.activeRemaining < 0f) {
                            iterator.remove();
                        }
                    }
                    playerShip.getMutableStats().getVentRateMult().modifyPercent(buffID, VENT_BONUS * timeoutSize);
                    engine.maintainStatusForPlayerShip(buffID,
                                                       "graphics/icons/tactical/venting_flux2.png",
                                                       rule.getName(),
                                                       String.format(rule.getExtra1(), ((int) (maxRemaining * 100f)) / 100f + ""),
                                                       false);
                } else {
                    playerShip.getMutableStats().getVentRateMult().unmodify(buffID);
                }
            }
        }
    }

    @Override
    public String getDescriptionParams(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return Global.getSettings().getControlStringForEnumName("SHIP_VENT_FLUX");
        if (index == 1) return BONUS_TIME + "";
        if (index == 2) return (int) VENT_BONUS + "%";
        if (index == 3) return "2";
        return super.getDescriptionParams(index, difficulty);
    }
}
