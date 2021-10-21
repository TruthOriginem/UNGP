package data.scripts.ungprules.impl.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import com.fs.starfarer.api.util.Misc;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import data.scripts.ungprules.tags.UNGP_CombatInitTag;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class UNGP_Collector extends UNGP_BaseRuleEffect implements UNGP_CombatInitTag {

    public static final float COLLECT_HP_THRESHOLD = 0.05f;

    @Override
    public float getValueByDifficulty(int index, int difficulty) {
        return 0;
    }

    @Override
    public String getDescriptionParams(int index, int difficulty) {
        if (index == 0) return "5%";
        if (index == 1) return Misc.getDGSCredits(25f);
        return super.getDescriptionParams(index, difficulty);
    }

    @Override
    public void init(CombatEngineAPI engine) {
        engine.getListenerManager().addListener(new CollectorListener());
    }

    public static class CollectorListener implements DamageListener, AdvanceableListener {
        public static final String DEBUFF_ID = "UNGP_collector_debuff";
        private float hiddenCoolDown = 0f; // 隐藏CD，每0.1秒才能触发一次

        @Override
        public void reportDamageApplied(Object source, CombatEntityAPI target, ApplyDamageResultAPI result) {
            if (hiddenCoolDown > 0f) return;
            if (source instanceof ShipAPI && target instanceof ShipAPI) {
                ShipAPI ship = (ShipAPI) source;
                ShipAPI targetShip = (ShipAPI) target;
                final CombatEngineAPI engine = Global.getCombatEngine();
                if (targetShip.isAlive() && !targetShip.isFighter() && result.getDamageToHull() > 0) {
                    if (ship.isFighter()) {
                        if (ship.getWing() != null) {
                            if (ship.getWing().getSourceShip() != engine.getPlayerShip()) {
                                return;
                            }
                        } else {
                            return;
                        }
                    } else {
                        if (ship != engine.getPlayerShip()) {
                            return;
                        }
                    }
                    if (target.getHullLevel() < COLLECT_HP_THRESHOLD) {
                        final ArmorGridAPI armorGrid = targetShip.getArmorGrid();
                        Vector2f damagePoint;
                        int check = 20;
                        final Vector2f targetLocation = target.getLocation();
                        while (check > 0) {
                            check--;
                            damagePoint = MathUtils.getRandomPointInCircle(targetLocation, target.getCollisionRadius());
                            final int[] cellAtLocation = armorGrid.getCellAtLocation(damagePoint);
                            if (cellAtLocation != null) {
                                final MutableShipStatsAPI stats = targetShip.getMutableStats();
                                // 取消装甲减伤
                                stats.getMaxArmorDamageReduction().modifyMult(DEBUFF_ID, 0f);
                                stats.getMinArmorFraction().modifyMult(DEBUFF_ID, 0f);
                                // 设置中心装甲为0
                                armorGrid.setArmorValue(cellAtLocation[0], cellAtLocation[1], 0f);
                                final float damage = target.getHitpoints() * COLLECT_HP_THRESHOLD + 999f;
                                targetShip.setHitpoints(1f);
                                engine.applyDamage(target,
                                                   targetLocation,
                                                   damage, DamageType.OTHER,
                                                   0f, true, false, ship, true);
                                stats.getMaxArmorDamageReduction().unmodify(DEBUFF_ID);
                                stats.getMinArmorFraction().unmodify(DEBUFF_ID);

                                engine.addFloatingText(targetLocation,
                                                       "+" + Misc.getDGSCredits(25f),
                                                       50f, Color.yellow,
                                                       null, 0f, 0f);
                                Global.getSoundPlayer().playSound("UNGP_collector_activate", 1f, 1f, targetLocation, Misc.ZERO);
                                hiddenCoolDown = 0.1f;
                                // 增加星币
                                if (!engine.isInCampaignSim()) {
                                    final CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
                                    if (playerFleet != null) {
                                        playerFleet.getCargo().getCredits().add(25f);
                                    }
                                }
                                break;
                            }
                        }
                    }
                }
            }
        }

        @Override
        public void advance(float amount) {
            if (hiddenCoolDown > 0f) {
                hiddenCoolDown -= amount;
            }
        }
    }
}
