package data.scripts.ungprules.impl.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import data.scripts.ungprules.tags.UNGP_CombatInitTag;
import data.scripts.utils.UNGPUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;

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
        engine.getListenerManager().addListener(new CollectorListener(engine));
    }

    public static class CollectorListener implements DamageListener {
        public static final String DEBUFF_ID = "UNGP_collector_debuff";
        private float hiddenCoolDown = 0f; // 隐藏CD，每0.1秒才能触发一次

        public CollectorListener(CombatEngineAPI engine) {
            engine.addPlugin(new BaseEveryFrameCombatPlugin() {
                @Override
                public void advance(float amount, List<InputEventAPI> events) {
                    CollectorListener.this.advance(amount);
                }
            });
        }

        @Override
        public void reportDamageApplied(Object source, CombatEntityAPI target, ApplyDamageResultAPI result) {
            if (hiddenCoolDown > 0f) return;
            if (source instanceof ShipAPI && target instanceof ShipAPI && source != target) {
                ShipAPI dmgSourceShip = (ShipAPI) source;
                // 不是玩家方舰船就不能触发
                if (!UNGPUtils.isPlayerShip(dmgSourceShip)) return;
                ShipAPI dmgTargetShip = (ShipAPI) target;
                // 目标需要存活，不能是战机，不能是相同所属，并且本次对结构伤害大于0
                if (dmgTargetShip.isAlive() && !dmgTargetShip.isFighter()
                        && dmgSourceShip.getOwner() != dmgTargetShip.getOwner()
                        && result.getDamageToHull() > 0) {
                    CombatEngineAPI engine = Global.getCombatEngine();
                    ShipAPI playerShip = engine.getPlayerShip();
                    // 如果目标是我方旗舰，直接跳过
                    if (dmgTargetShip == playerShip) return;
                    // 如果源头来自战机，看战机源头是否是玩家正在操控的船
                    if (dmgSourceShip.isFighter()) {
                        if (dmgSourceShip.getWing() != null) {
                            if (dmgSourceShip.getWing().getSourceShip() != playerShip) return;
                        } else {
                            return;
                        }
                    } else if (dmgSourceShip != playerShip) {
                        // 如果源头不是玩家操控战舰，直接返回
                        return;
                    }
                    // 目标结构低于结构值时
                    if (target.getHullLevel() < COLLECT_HP_THRESHOLD) {
                        final ArmorGridAPI armorGrid = dmgTargetShip.getArmorGrid();
                        Vector2f damagePoint;
                        int check = 20;
                        final Vector2f targetLocation = target.getLocation();
                        while (check > 0) {
                            check--;
                            damagePoint = MathUtils.getRandomPointInCircle(targetLocation, target.getCollisionRadius());
                            final int[] cellAtLocation = armorGrid.getCellAtLocation(damagePoint);
                            if (cellAtLocation != null) {
                                final MutableShipStatsAPI stats = dmgTargetShip.getMutableStats();
                                // 取消装甲最小减伤
                                stats.getMinArmorFraction().modifyMult(DEBUFF_ID, 0f);
                                // 设置中心区域装甲为0
                                clearArmor(cellAtLocation[0], cellAtLocation[1], armorGrid);
                                final float damage = target.getMaxHitpoints() * COLLECT_HP_THRESHOLD + 999f;
                                dmgTargetShip.setHitpoints(1f);
                                engine.applyDamage(target,
                                                   targetLocation,
                                                   damage, result.getType(),
                                                   0f, true, false, dmgSourceShip, true);
                                stats.getMinArmorFraction().unmodify(DEBUFF_ID);

                                hiddenCoolDown = 0.15f;
                                // 增加星币
                                Global.getSoundPlayer().playSound("UNGP_collector_activate", 1f, 1f, targetLocation, Misc.ZERO);
                                if (!engine.isInCampaignSim()) {
                                    final CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
                                    if (playerFleet != null) {
                                        playerFleet.getCargo().getCredits().add(25f);
                                        engine.addFloatingText(targetLocation,
                                                               "+" + Misc.getDGSCredits(25f),
                                                               50f, Color.yellow,
                                                               null, 0f, 0f);
                                    }
                                }
                                check = 0;
                                break;
                            }
                        }
                    }
                }
            }
        }

        public void advance(float amount) {
            if (hiddenCoolDown > 0f) {
                hiddenCoolDown -= amount;
            }
//            CombatEngineAPI engine = Global.getCombatEngine();
//            ShipAPI playerShip = engine.getPlayerShip();
//            if (playerShip.isAlive() && !engine.getCustomData().containsKey("test")) {
//                playerShip.setCurrentCR(0.01f);
//                playerShip.setHitpoints(300f);
//                engine.getCustomData().put("test", true);
//            }
        }

        private void clearArmor(int x, int y, ArmorGridAPI grid) {
            int gridWidth = grid.getGrid().length;
            int gridHeight = grid.getGrid()[0].length;
            for (int i = -2; i <= 2; i++) {
                for (int j = -2; j <= 2; j++) {
                    if ((i == 2 || i == -2) && (j == 2 || j == -2)) continue; // skip corners
                    int cx = x + i;
                    int cy = y + j;
                    if (cx < 0 || cx >= gridWidth || cy < 0 || cy >= gridHeight) continue;
                    grid.setArmorValue(cx, cy, 0);
                }
            }
        }
    }
}
