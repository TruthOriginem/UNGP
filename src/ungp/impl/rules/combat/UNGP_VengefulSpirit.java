package ungp.impl.rules.combat;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import ungp.api.rules.UNGP_BaseRuleEffect;
import ungp.api.rules.tags.UNGP_CombatInitTag;
import ungp.api.rules.tags.UNGP_CombatTag;

import java.util.HashMap;
import java.util.Map;

public class UNGP_VengefulSpirit extends UNGP_BaseRuleEffect implements UNGP_CombatTag, UNGP_CombatInitTag {
    private static final float DIRECTION_JITTER_DEGREES = 15f;
    private static final float MIN_DIRECTION_LENGTH_SQUARED = 1f;

    private String getLethalHitKey() {
        return buffID + "_lethal_hit_location";
    }

    @Override
    public void init(CombatEngineAPI engine) {
        Map<ShipAPI, Vector2f> lethalHits = new HashMap<>();
        putDataInEngine(engine, getLethalHitKey(), lethalHits);
        engine.getListenerManager().addListener(new LethalHitListener(lethalHits, buffID));
    }

    @Override
    public void advanceInCombat(CombatEngineAPI engine, float amount) {
        Map<ShipAPI, Vector2f> lethalHits = getDataInEngine(engine, getLethalHitKey());
        if (lethalHits == null) return;

        for (ShipAPI ship : engine.getShips()) {
            if (ship.isAlive() || !ship.isHulk()) continue;
            if (ship.getCustomData().containsKey(buffID)) continue;

            Vector2f killerLocation = lethalHits.remove(ship);
            if (killerLocation == null) continue;

            Vector2f direction = Vector2f.sub(killerLocation, ship.getLocation(), null);
            if (direction.lengthSquared() < MIN_DIRECTION_LENGTH_SQUARED) continue;
            direction.normalise();
            // 加一点随机偏移，避免多具残骸汇聚到同一条线上互相碰撞
            direction = Misc.rotateAroundOrigin(direction,
                    MathUtils.getRandomNumberInRange(-DIRECTION_JITTER_DEGREES, DIRECTION_JITTER_DEGREES));

            float speed = ship.getVelocity().length();
            ship.getVelocity().set(direction.x * speed, direction.y * speed);
            ship.setCustomData(buffID, true);
        }
    }

    @Override
    public void applyEnemyShipInCombat(float amount, ShipAPI enemy) {
    }

    @Override
    public void applyPlayerShipInCombat(float amount, CombatEngineAPI engine, ShipAPI ship) {
    }

    private static final class LethalHitListener implements DamageListener {
        private final Map<ShipAPI, Vector2f> lethalHits;
        private final String buffID;

        private LethalHitListener(Map<ShipAPI, Vector2f> lethalHits, String buffID) {
            this.lethalHits = lethalHits;
            this.buffID = buffID;
        }

        @Override
        public void reportDamageApplied(Object source, CombatEntityAPI target, ApplyDamageResultAPI result) {
            // 只处理舰船来源、对舰船结构的伤害
            if (!(source instanceof ShipAPI) || !(target instanceof ShipAPI)) return;
            if (source == target) return;
            if (result.getDamageToHull() <= 0f) return;

            ShipAPI ship = (ShipAPI) target;
            // 结构未归零就不是致命一击；已改向的残骸、已有记录的残骸不再覆盖
            if (ship.getHitpoints() > 0f) return;
            if (ship.getCustomData().containsKey(buffID)) return;
            if (lethalHits.containsKey(ship)) return;

            // 只记录击杀瞬间的位置，来源舰船之后如何移动都不影响
            lethalHits.put(ship, new Vector2f(((ShipAPI) source).getLocation()));
        }
    }
}
