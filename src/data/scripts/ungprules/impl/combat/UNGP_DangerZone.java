package data.scripts.ungprules.impl.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.campaign.specialist.UNGP_SpecialistSettings;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import data.scripts.ungprules.tags.UNGP_CombatTag;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UNGP_DangerZone extends UNGP_BaseRuleEffect implements UNGP_CombatTag {

    private float spawnRateMult;
    private float poolMaxSize;
    private float cooldownRateMult;
    private static final Map<ShipAPI.HullSize, Float> BASE_SPAWN_TIME = new HashMap<>();

    static {
        BASE_SPAWN_TIME.put(ShipAPI.HullSize.CAPITAL_SHIP, 10f);
        BASE_SPAWN_TIME.put(ShipAPI.HullSize.CRUISER, 14f);
        BASE_SPAWN_TIME.put(ShipAPI.HullSize.DESTROYER, 20f);
        BASE_SPAWN_TIME.put(ShipAPI.HullSize.FRIGATE, 30f);
    }

    @Override
    public void updateDifficultyCache(UNGP_SpecialistSettings.Difficulty difficulty) {
        spawnRateMult = getValueByDifficulty(0, difficulty);
        poolMaxSize = getValueByDifficulty(1, difficulty);
		cooldownRateMult = getValueByDifficulty(2, difficulty);
    }

    @Override
    public float getValueByDifficulty(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return difficulty.getLinearValue(1f, 1f);
        if (index == 1) return difficulty.getLinearValue(10f, 8f);
		if (index == 2) return difficulty.getLinearValue(1f, 1f);
        return 0f;
    }

    @Override
    public String getDescriptionParams(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        float rate = getValueByDifficulty(0, difficulty);
        if (index == 0) return getFactorString(BASE_SPAWN_TIME.get(ShipAPI.HullSize.FRIGATE) / rate);
        if (index == 1) return getFactorString(BASE_SPAWN_TIME.get(ShipAPI.HullSize.DESTROYER) / rate);
        if (index == 2) return getFactorString(BASE_SPAWN_TIME.get(ShipAPI.HullSize.CRUISER) / rate);
        if (index == 3) return getFactorString(BASE_SPAWN_TIME.get(ShipAPI.HullSize.CAPITAL_SHIP) / rate);
        if (index == 4) return "2000";
        return null;
    }

    @Override
    public void advanceInCombat(CombatEngineAPI engine, float amount) {
    }

    private final WeightedRandomPicker<ShipAPI> cache = new WeightedRandomPicker<>();

    @Override
    public void applyEnemyShipInCombat(float amount, ShipAPI ship) {
        if (!ship.isAlive()) return;
        if (ship.isDrone() || ship.isFighter()) return;
        if (ship.isStation() || ship.isStationModule()) return;

        CombatEngineAPI engine = Global.getCombatEngine();
		Float cooldownPool = getDataInEngine(engine, buffID);
		if (cooldownPool == null) {
			cooldownPool = 0f;
			putDataInEngine(engine, buffID, cooldownPool);
		}

		cooldownPool = Math.max(cooldownPool - amount * cooldownRateMult, 0f);
		if (cooldownPool < poolMaxSize) {
			if (ship.getCustomData().get(buffID) == null) {
				float rate = BASE_SPAWN_TIME.get(ship.getHullSize()) / spawnRateMult;
				ship.setCustomData(buffID, new IntervalUtil(rate, rate));
			}

			IntervalUtil timer = (IntervalUtil) ship.getCustomData().get(buffID);
			timer.advance(amount);

			if (timer.intervalElapsed()) {
				for (ShipAPI victim : AIUtils.getEnemiesOnMap(ship)) {
					if (victim.isFighter() || victim.isDrone()) continue;
					if (victim.isStation() || victim.isStationModule()) continue;
					if (victim.getTravelDrive() != null && victim.getTravelDrive().isActive()) continue;

					cache.add(victim, victim.getHullSize().ordinal());
				}

				ShipAPI victim = cache.pick();
				if (victim != null) {
					cache.clear();

					Vector2f target = findClearLocation(victim);
					if (target != null) {
						spawnMine(ship, target);
						cooldownPool += 1f;
					}
				}
			}
		}

		putDataInEngine(engine, buffID, cooldownPool);
    }

    @Override
    public void applyPlayerShipInCombat(float amount, CombatEngineAPI engine, ShipAPI ship) {
    }

    public static void spawnMine(ShipAPI source, Vector2f mineLoc) {
        CombatEngineAPI engine = Global.getCombatEngine();
        Vector2f currLoc = Misc.getPointAtRadius(mineLoc, 60f + (float) Math.random() * 80f);
        MissileAPI mine = (MissileAPI) engine.spawnProjectile(source, null,
                                                              "minelayer2", currLoc, (float) Math.random() * 360f, null);

        if (source != null) {
            Global.getCombatEngine().applyDamageModifiersToSpawnedProjectileWithNullWeapon(source, WeaponAPI.WeaponType.MISSILE, false, mine.getDamage());
        }

        mine.setFlightTime((float) Math.random());
        mine.fadeOutThenIn(1f);

        Global.getSoundPlayer().playSound("mine_spawn", 1f, 1f, mine.getLocation(), mine.getVelocity());
    }

    public static Vector2f findClearLocation(ShipAPI victim) {

        List<Vector2f> tested = new ArrayList<>();
        for (float angle = 0; angle <= 360f; angle += 10f) {

            Vector2f mineLoc = MathUtils.getRandomPointOnCircumference(victim.getLocation(), victim.getCollisionRadius() + 400f + 200f * (float) Math.random());
            float minOk = 400f + victim.getCollisionRadius();
            if (!isAreaClear(mineLoc, minOk)) continue;

            tested.add(mineLoc);
        }

        if (tested.isEmpty()) return null; // shouldn't happen
        return tested.get((int) (Math.random() * tested.size()));
    }

    public static boolean isAreaClear(Vector2f loc, float range) {
        CombatEngineAPI engine = Global.getCombatEngine();
        for (ShipAPI other : engine.getShips()) {
            if (other.isFighter()) continue;
            if (other.isDrone()) continue;

            float dist = Misc.getDistance(loc, other.getLocation());
            if (dist < range) {
                return false;
            }
        }

        for (CombatEntityAPI other : Global.getCombatEngine().getAsteroids()) {
            float dist = Misc.getDistance(loc, other.getLocation());
            if (dist < other.getCollisionRadius() + 100f) {
                return false;
            }
        }

        return true;
    }
}
