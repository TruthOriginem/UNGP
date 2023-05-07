package ungp.impl.rules.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import ungp.scripts.campaign.specialist.UNGP_SpecialistSettings;
import ungp.api.rules.UNGP_BaseRuleEffect;
import ungp.api.rules.tags.UNGP_CombatTag;
import ungp.scripts.utils.UNGPUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;

public class UNGP_PhasingGhost extends UNGP_BaseRuleEffect implements UNGP_CombatTag {
    private float chance = 0.1f;

    @Override
    public void advanceInCombat(CombatEngineAPI engine, float amount) {
        GhostManager data = getDataInEngine(engine, buffID);
        if (data == null) {
            data = new GhostManager();
            putDataInEngine(engine, buffID, data);
        }
        data.advance(engine, amount);
    }

    @Override
    public void applyEnemyShipInCombat(float amount, ShipAPI enemy) {

    }

    @Override
    public void applyPlayerShipInCombat(float amount, CombatEngineAPI engine, ShipAPI ship) {

    }

    @Override
    public void updateDifficultyCache(UNGP_SpecialistSettings.Difficulty difficulty) {
        chance = getValueByDifficulty(0, difficulty);
    }

    @Override
    public float getValueByDifficulty(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return difficulty.getLinearValue(0.15f, 0.1f);
        return super.getValueByDifficulty(index, difficulty);
    }

    @Override
    public String getDescriptionParams(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return "60";
        if (index == 1) return getPercentString(getValueByDifficulty(0, difficulty) * 100f);
        if (index == 2) return "0.5";
        if (index == 3) return "2";
        return super.getDescriptionParams(index, difficulty);
    }

    private class GhostManager {
        private IntervalUtil checkInterval = new IntervalUtil(55, 65f);
        private float rollChance = chance;
        private String lastGeneratedHullId;

        public void advance(CombatEngineAPI engine, float amount) {
            checkInterval.advance(amount);
            ShipAPI.HullSize biggestHullSize = ShipAPI.HullSize.DEFAULT;
            if (checkInterval.intervalElapsed()) {
                if (roll(rollChance)) {
                    Vector2f toSpawn = null;
                    WeightedRandomPicker<ShipAPI> spawnLocks = new WeightedRandomPicker<>();
                    for (ShipAPI ship : engine.getShips()) {
                        if (!ship.isAlive() || ship.isFighter()) continue;
                        ShipAPI.HullSize hullSize = ship.getHullSize();
                        if (biggestHullSize.ordinal() < hullSize.ordinal()) {
                            biggestHullSize = hullSize;
                        }
                        if (UNGPUtils.isEnemyShip(ship)) {
                            spawnLocks.add(ship);
                        }
                    }
                    if (!spawnLocks.isEmpty()) {
                        ShipAPI lockedPlaceShip = spawnLocks.pick();
                        float distance = MathUtils.getRandomNumberInRange(lockedPlaceShip.getCollisionRadius(), lockedPlaceShip.getCollisionRadius() * 3f);
                        toSpawn = MathUtils.getPointOnCircumference(lockedPlaceShip.getLocation(), distance, RANDOM.nextFloat() * 360f);
                    }
                    if (toSpawn != null && biggestHullSize != ShipAPI.HullSize.DEFAULT) {
                        WeightedRandomPicker<String> hullPicker = new WeightedRandomPicker<>();
                        switch (biggestHullSize) {
                            case FIGHTER:
                                break;
                            case FRIGATE:
                                hullPicker.add("afflictor", 10f);
                                hullPicker.add("shade", 10f);
                                break;
                            case DESTROYER:
                                hullPicker.add("afflictor", 8);
                                hullPicker.add("shade", 8);
                                hullPicker.add("harbinger", 10);
                                break;
                            case CRUISER:
                                hullPicker.add("afflictor", 8);
                                hullPicker.add("shade", 8);
                                hullPicker.add("harbinger", 8);
                                hullPicker.add("doom", 4);
                                break;
                            case CAPITAL_SHIP:
                                hullPicker.add("afflictor", 10f);
                                hullPicker.add("shade", 10f);
                                hullPicker.add("harbinger", 10f);
                                hullPicker.add("doom", 10f);
                                break;
                        }
                        String hull = hullPicker.pick();
                        if (hull != null) {
                            // 隐藏机制 重复的船概率减半
                            if (lastGeneratedHullId != null) {
                                int index = hullPicker.getItems().indexOf(lastGeneratedHullId);
                                if (index != -1) {
                                    hullPicker.setWeight(index, hullPicker.getWeight(index) * 0.5f);
                                }
                            }
                            String variant = getVariant(hull);
                            if (variant != null) {
                                CombatFleetManagerAPI fleetManager = engine.getFleetManager(FleetSide.ENEMY);
                                boolean wasSuppressed = fleetManager.isSuppressDeploymentMessages();
                                fleetManager.setSuppressDeploymentMessages(true);
                                ShipAPI spawnedGhost = fleetManager.spawnShipOrWing(variant, toSpawn, RANDOM.nextFloat() * 360f);
                                fleetManager.setSuppressDeploymentMessages(wasSuppressed);
                                engine.getCombatUI().addMessage(0, rule.getCorrectColor(), rule.getExtra1());
                                Global.getSoundPlayer().playSound("UNGP_phasingghost_activate", 1f, 1f, toSpawn, new Vector2f());
                                Global.getSoundPlayer().playUISound("UNGP_phasingghost_warning", 1f, 0.4f);
                                engine.addPlugin(new PhasingFadeInPlugin(spawnedGhost));
                                // 成功生成的话
                                // 每生成一次，概率下降
                                rollChance *= 0.9f;
                                rollChance /= 2f;
                                lastGeneratedHullId = hull;
                            }
                        }
                    }
                } else {
                    rollChance *= 2f;
                }
            }
        }
    }

    protected String getVariant(String hullId) {
        List<String> variants = Global.getSettings().getHullIdToVariantListMap().get(hullId);
        WeightedRandomPicker<String> variantPicker = new WeightedRandomPicker<>();
        if (!variants.isEmpty()) {
            variantPicker.addAll(variants);
        }
        return variantPicker.pick();
    }

    private static class PhasingFadeInPlugin extends BaseEveryFrameCombatPlugin {
        public static Color JITTER_COLOR = new Color(61, 5, 122, 50);
        private static final float FADE_IN_TIME = 1f;
        private float elapsed;
        private ShipAPI ship;
        private CollisionClass collisionClass;

        public PhasingFadeInPlugin(ShipAPI ship) {
            this.ship = ship;
            this.collisionClass = ship.getCollisionClass();
            ship.setAlphaMult(0f);
        }

        @Override
        public void advance(float amount, List<InputEventAPI> events) {
            if (Global.getCombatEngine().isPaused()) return;
            CombatEngineAPI engine = Global.getCombatEngine();
            elapsed += amount;
            float progress = Math.min(1f, elapsed / FADE_IN_TIME);

            ship.setAlphaMult(progress);
            if (progress < 0.5f) {
                ship.blockCommandForOneFrame(ShipCommand.ACCELERATE);
                ship.blockCommandForOneFrame(ShipCommand.TURN_LEFT);
                ship.blockCommandForOneFrame(ShipCommand.TURN_RIGHT);
                ship.blockCommandForOneFrame(ShipCommand.STRAFE_LEFT);
                ship.blockCommandForOneFrame(ShipCommand.STRAFE_RIGHT);
            }

            ship.blockCommandForOneFrame(ShipCommand.USE_SYSTEM);
            ship.blockCommandForOneFrame(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK);
            ship.blockCommandForOneFrame(ShipCommand.FIRE);
            ship.blockCommandForOneFrame(ShipCommand.PULL_BACK_FIGHTERS);
            ship.blockCommandForOneFrame(ShipCommand.VENT_FLUX);
            ship.setHoldFireOneFrame(true);
            ship.setHoldFire(true);
            ship.setCollisionClass(CollisionClass.NONE);
            ship.getMutableStats().getHullDamageTakenMult().modifyMult("UNGP_PGFadeIn", 0f);
            if (progress > 0.75f) {
                ship.setCollisionClass(collisionClass);
                ship.getMutableStats().getHullDamageTakenMult().unmodifyMult("UNGP_PGFadeIn");
            }

            float jitterLevel = progress;
            if (jitterLevel < 0.5f) {
                jitterLevel *= 2f;
            } else {
                jitterLevel = (1f - jitterLevel) * 2f;
            }

            float jitterRange = 1f - progress;
            float maxRangeBonus = 50f;
            float jitterRangeBonus = jitterRange * maxRangeBonus;
            Color c = JITTER_COLOR;

            ship.setJitter(this, c, jitterLevel, 25, 0f, jitterRangeBonus);

            if (elapsed > FADE_IN_TIME) {
                ship.setAlphaMult(1f);
                ship.setHoldFire(false);
                ship.setCollisionClass(collisionClass);
                ship.getMutableStats().getHullDamageTakenMult().unmodifyMult("UNGP_PGFadeIn");
                engine.removePlugin(this);
            }
        }
    }
}
