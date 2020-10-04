package data.scripts.ungprules.tags;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;

/**
 * Implement it if you are a combat rule.
 */
public interface UNGP_CombatTag {
    void advanceInCombat(CombatEngineAPI engine, float amount);

    /**
     * 每帧执行一次
     *
     * @param amount
     * @param enemy
     */
    void applyEnemyShipInCombat(float amount, ShipAPI enemy);

    void applyPlayerShipInCombat(float amount, CombatEngineAPI engine, ShipAPI ship);
}
