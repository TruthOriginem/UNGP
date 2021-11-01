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
                ShipAPI sourceShip = (ShipAPI) source;
                ShipAPI targetShip = (ShipAPI) target;
                CombatEngineAPI engine = Global.getCombatEngine();
                // 目标需要存活，不能是战机，不能是相同所属，并且本次对结构伤害大于0
                if (targetShip.isAlive() && !targetShip.isFighter() && sourceShip.getOwner() != targetShip.getOwner() &&
                        result.getDamageToHull() > 0) {
                    ShipAPI playerShip = engine.getPlayerShip();
                    // 如果源头来自战机，看战机源头是否是玩家正在操控的船
                    if (sourceShip.isFighter()) {
                        if (sourceShip.getWing() != null) {
                            if (sourceShip.getWing().getSourceShip() != playerShip) return;
                        } else {
                            return;
                        }
                    } else if (sourceShip != playerShip) {
                        // 如果源头不是玩家操控战舰，直接返回
                        return;
                    }
                    // 目标结构低于结构值时
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
                                // 取消装甲最小减伤
                                stats.getMinArmorFraction().modifyMult(DEBUFF_ID, 0f);
                                // 设置中心区域装甲为0
                                clearArmor(cellAtLocation[0], cellAtLocation[1], armorGrid);
                                final float damage = target.getMaxHitpoints() * COLLECT_HP_THRESHOLD + 999f;
                                targetShip.setHitpoints(1f);
                                engine.applyDamage(target,
                                                   targetLocation,
                                                   damage, result.getType(),
                                                   0f, true, false, sourceShip, true);
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
