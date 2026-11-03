package ungp.impl.rules.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;
import ungp.scripts.campaign.specialist.UNGP_SpecialistSettings;
import ungp.api.rules.UNGP_BaseRuleEffect;
import ungp.api.rules.tags.UNGP_CombatInitTag;
import ungp.scripts.utils.UNGPUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;

public class UNGP_Collector extends UNGP_BaseRuleEffect implements UNGP_CombatInitTag {

    public static final float COLLECT_HP_THRESHOLD = 0.05f;
    public static final float COLLECT_CREDITS = 25f;
    public static final float COLLECT_COOLDOWN = 0.15f;

    @Override
    public float getValueByDifficulty(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        return 0;
    }

    @Override
    public String getDescriptionParams(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return "5%";
        if (index == 1) return Misc.getDGSCredits(COLLECT_CREDITS);
        return super.getDescriptionParams(index, difficulty);
    }

    @Override
    public void init(CombatEngineAPI engine) {
        engine.getListenerManager().addListener(new CollectorListener(engine));
    }

    public static class CollectorListener implements DamageListener {
        public static final String DEBUFF_ID = "UNGP_collector_debuff";

        private final CombatEngineAPI engine;
        private float hiddenCoolDown = 0f; // 隐藏CD，每0.15秒才能触发一次
        // 待处决的目标。不能在这里直接调用 applyDamage：那会同步再次触发 reportDamageApplied 造成自循环，
        // 因此只登记目标，统一在每帧的 advance 中结算。
        private ShipAPI pendingTarget;
        private ShipAPI pendingSource;
        private DamageType pendingDamageType;
        // 结算期间的再入保护：结算自身造成的伤害不应再次进入本监听器
        private boolean collecting = false;

        public CollectorListener(final CombatEngineAPI engine) {
            this.engine = engine;
            engine.addPlugin(new BaseEveryFrameCombatPlugin() {
                @Override
                public void advance(float amount, List<InputEventAPI> events) {
                    if (engine.isPaused()) return;
                    CollectorListener.this.advance(amount);
                }
            });
        }

        @Override
        public void reportDamageApplied(Object source, CombatEntityAPI target, ApplyDamageResultAPI result) {
            if (collecting) return;
            if (hiddenCoolDown > 0f) return;
            if (!(source instanceof ShipAPI) || !(target instanceof ShipAPI) || source == target) return;

            ShipAPI dmgSourceShip = (ShipAPI) source;
            // 不是玩家方舰船就不能触发
            if (!UNGPUtils.isPlayerShip(dmgSourceShip)) return;

            ShipAPI dmgTargetShip = (ShipAPI) target;
            // 目标需要存活，不能是战机，不能是相同所属，并且本次对结构伤害大于0
            if (!dmgTargetShip.isAlive() || dmgTargetShip.isFighter()) return;
            if (dmgSourceShip.getOwner() == dmgTargetShip.getOwner()) return;
            if (result.getDamageToHull() <= 0f) return;

            ShipAPI playerShip = engine.getPlayerShip();
            // 如果目标是我方旗舰，直接跳过
            if (dmgTargetShip == playerShip) return;
            // 如果源头来自战机，看战机源头是否是玩家正在操控的船
            if (dmgSourceShip.isFighter()) {
                if (dmgSourceShip.getWing() == null) return;
                if (dmgSourceShip.getWing().getSourceShip() != playerShip) return;
            } else if (dmgSourceShip != playerShip) {
                // 如果源头不是玩家操控战舰，直接返回
                return;
            }

            // 目标结构低于结构值时，登记为待处决目标
            if (dmgTargetShip.getHullLevel() < COLLECT_HP_THRESHOLD) {
                pendingTarget = dmgTargetShip;
                pendingSource = dmgSourceShip;
                pendingDamageType = result.getType();
                hiddenCoolDown = COLLECT_COOLDOWN;
            }
        }

        public void advance(float amount) {
            if (hiddenCoolDown > 0f) {
                hiddenCoolDown -= amount;
            }
            if (pendingTarget == null) return;

            final ShipAPI target = pendingTarget;
            final ShipAPI source = pendingSource;
            final DamageType damageType = pendingDamageType;
            pendingTarget = null;
            pendingSource = null;
            pendingDamageType = null;
            collect(target, source, damageType);
        }

        private void collect(ShipAPI target, ShipAPI source, DamageType damageType) {
            // 目标可能已经被其他伤害击毁，或者结构不再满足条件
            if (!target.isAlive() || target.getHullLevel() >= COLLECT_HP_THRESHOLD) return;

            final ArmorGridAPI armorGrid = target.getArmorGrid();
            if (armorGrid == null) return;
            final Vector2f damagePoint = getRandomArmorPoint(target, armorGrid);
            if (damagePoint == null) return;
            final int[] cellAtLocation = armorGrid.getCellAtLocation(damagePoint);
            if (cellAtLocation == null) return;

            collecting = true;
            try {
                final MutableShipStatsAPI stats = target.getMutableStats();
                // 取消装甲最小减伤
                stats.getMinArmorFraction().modifyMult(DEBUFF_ID, 0f);
                try {
                    // 设置中心区域装甲为0
                    clearArmor(cellAtLocation[0], cellAtLocation[1], armorGrid);
                    final float damage = target.getMaxHitpoints() * COLLECT_HP_THRESHOLD + 999f;
                    target.setHitpoints(1f);
                    engine.applyDamage(target, damagePoint, damage, damageType, 0f, true, false, source, true);
                } finally {
                    stats.getMinArmorFraction().unmodify(DEBUFF_ID);
                }
                // 只有真正击毁目标才算处决成功
                if (target.isAlive()) return;

                // 增加星币
                Global.getSoundPlayer().playSound("UNGP_collector_activate", 1f, 1f, damagePoint, Misc.ZERO);
                if (!engine.isInCampaignSim()) {
                    final CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
                    if (playerFleet != null) {
                        playerFleet.getCargo().getCredits().add(COLLECT_CREDITS);
                        engine.addFloatingText(damagePoint,
                                               "+" + Misc.getDGSCredits(COLLECT_CREDITS),
                                               50f, Color.yellow,
                                               null, 0f, 0f);
                    }
                }
            } finally {
                collecting = false;
            }
        }

        private Vector2f getRandomArmorPoint(ShipAPI ship, ArmorGridAPI armorGrid) {
            final Vector2f location = ship.getLocation();
            for (int check = 20; check > 0; check--) {
                final Vector2f damagePoint = MathUtils.getRandomPointInCircle(location, ship.getCollisionRadius());
                if (armorGrid.getCellAtLocation(damagePoint) != null) return damagePoint;
            }
            return null;
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
