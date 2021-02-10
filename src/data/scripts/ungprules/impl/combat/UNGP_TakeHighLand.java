package data.scripts.ungprules.impl.combat;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BattleObjectiveAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import data.scripts.ungprules.tags.UNGP_CombatTag;
import data.scripts.utils.UNGPUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

public class UNGP_TakeHighLand extends UNGP_BaseRuleEffect implements UNGP_CombatTag {
    private static final float RANGE_BONUS = 20f;
    private float range;

    @Override
    public void updateDifficultyCache(int difficulty) {
        range = getValueByDifficulty(0, difficulty);
    }

    @Override
    public float getValueByDifficulty(int index, int difficulty) {
        if (index == 0) return (int) (1500f + 500f * (float) Math.pow(difficulty, 0.3669));
        return 1f;
    }

    @Override
    public void advanceInCombat(CombatEngineAPI engine, float amount) {

    }

    @Override
    public void applyEnemyShipInCombat(float amount, ShipAPI enemy) {

    }

    @Override
    public void applyPlayerShipInCombat(float amount, CombatEngineAPI engine, ShipAPI ship) {
        if (UNGPUtils.isPlayerShip(ship)) {
            Vector2f shipLoc = ship.getLocation();
            boolean takeEffect = false;
            for (BattleObjectiveAPI tmp : Global.getCombatEngine().getObjectives()) {
                if (MathUtils.isWithinRange(tmp.getLocation(), shipLoc, range)) {
                    takeEffect = true;
                    break;
                }
            }
            MutableShipStatsAPI stats = ship.getMutableStats();
            if (takeEffect) {
                stats.getBallisticWeaponRangeBonus().modifyPercent(rule.getBuffID(), RANGE_BONUS);
                stats.getEnergyWeaponRangeBonus().modifyPercent(rule.getBuffID(), RANGE_BONUS);
                if (ship == engine.getPlayerShip()) {
                    engine.maintainStatusForPlayerShip(rule,
                            "graphics/icons/sensor_array.png",
                            rule.getName(),
                            rule.getExtra1(),
                            false);
                }
            } else {
                stats.getBallisticWeaponRangeBonus().unmodify(rule.getBuffID());
                stats.getEnergyWeaponRangeBonus().unmodify(rule.getBuffID());
            }
        }
    }

    @Override
    public String getDescriptionParams(int index) {
        if (index == 0) return getFactorString(range);
        if (index == 1) return "20%";
        return null;
    }

    @Override
    public String getDescriptionParams(int index, int difficulty) {
        if (index == 0) return getFactorString(getValueByDifficulty(index, difficulty));
        return getDescriptionParams(index);
    }

}
