package data.scripts.ungprules.impl.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import data.scripts.ungprules.tags.UNGP_CombatTag;
import org.lwjgl.util.vector.Vector2f;

public class UNGP_Seer extends UNGP_BaseRuleEffect implements UNGP_CombatTag {

    private static final Vector2f ZERO = new Vector2f();
    private static final float REVEAL_COOLDOWN = 20f;
    private float revealDuration;

    @Override
    public void updateDifficultyCache(int difficulty) {
        this.revealDuration = getValueByDifficulty(0, difficulty);
    }

    @Override
    public float getValueByDifficulty(int index, int difficulty) {
        return getLinearValue(2f, 3f, difficulty);
    }


    @Override
    public String getDescriptionParams(int index, int difficulty) {
        if (index == 0) return "20";
        if (index == 1) return getFactorString(getValueByDifficulty(index, difficulty));
        return null;
    }

    @Override
    public void advanceInCombat(CombatEngineAPI engine, float amount) {

        final String buffID = rule.getBuffID();
        RevealData data = getDataInEngine(engine, buffID);
        if (data == null) {
            data = new RevealData();
            putDataInEngine(engine, buffID, data);
        }

        if (engine.isPaused()) {
            amount = 0f;
        }

        data.timeLeftForThisStage -= amount;
        if (data.timeLeftForThisStage <= 0f) {
            data.isEffecting = !data.isEffecting;
            data.timeLeftForThisStage = data.isEffecting ? revealDuration : REVEAL_COOLDOWN;

            if (data.isEffecting) {
                Global.getSoundPlayer().playSound("world_sensor_burst_on", 1f, 1f, engine.getPlayerShip().getLocation(), ZERO);
            }
        }

        if (data.isEffecting) {
            float radiusEx = engine.getMapHeight() * 2f + engine.getMapWidth() * 2f;
            engine.getFogOfWar(engine.getPlayerShip().getOwner()).revealAroundPoint(this, 0f, 0f, radiusEx);
        }
    }

    @Override
    public void applyEnemyShipInCombat(float amount, ShipAPI ship) {
    }

    @Override
    public void applyPlayerShipInCombat(float amount, CombatEngineAPI engine, ShipAPI ship) {
    }

    public static final class RevealData {

        float timeLeftForThisStage;
        boolean isEffecting;

        public RevealData() {
            timeLeftForThisStage = REVEAL_COOLDOWN;
            isEffecting = false;
        }
    }
}