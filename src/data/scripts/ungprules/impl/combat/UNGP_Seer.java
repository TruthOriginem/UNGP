package data.scripts.ungprules.impl.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.FogOfWarAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import data.scripts.campaign.specialist.UNGP_SpecialistSettings;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import data.scripts.ungprules.tags.UNGP_CombatTag;
import data.scripts.utils.UNGPUtils;
import org.lwjgl.util.vector.Vector2f;

public class UNGP_Seer extends UNGP_BaseRuleEffect implements UNGP_CombatTag {

    private static final Vector2f ZERO = new Vector2f();
    private static final float REVEAL_COOLDOWN = 20f;
    private float revealDuration;

    @Override
    public void updateDifficultyCache(UNGP_SpecialistSettings.Difficulty difficulty) {
        this.revealDuration = getValueByDifficulty(0, difficulty);
    }

    @Override
    public float getValueByDifficulty(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        return difficulty.getLinearValue(2f, 1f);
    }


    @Override
    public String getDescriptionParams(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return "20";
        if (index == 1) return getFactorString(getValueByDifficulty(index, difficulty));
        return null;
    }

    @Override
    public void advanceInCombat(CombatEngineAPI engine, float amount) {
        RevealData data = getDataInEngine(engine, buffID);
        if (data == null) {
            data = new RevealData();
            putDataInEngine(engine, buffID, data);
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
            FogOfWarAPI fogOfWar = engine.getFogOfWar(UNGPUtils.PLAYER);
            if (fogOfWar != null) {
                fogOfWar.revealAroundPoint(this, 0f, 0f, radiusEx);
            }
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
