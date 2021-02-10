package data.scripts.ungprules.impl.combat;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import data.scripts.ungprules.tags.UNGP_CombatTag;
import org.lazywizard.lazylib.combat.CombatUtils;

import java.util.List;

public class UNGP_CloseFormation extends UNGP_BaseRuleEffect implements UNGP_CombatTag {
    private static final float RANGE = 600f;
    private static final float MULTIPLIER = 1.1f;

    @Override
    public float getValueByDifficulty(int index, int difficulty) {
        return 0f;
    }

    @Override
    public void advanceInCombat(CombatEngineAPI engine, float amount) {

    }

    @Override
    public void applyEnemyShipInCombat(float amount, ShipAPI enemy) {

    }

    @Override
    public void applyPlayerShipInCombat(float amount, CombatEngineAPI engine, ShipAPI ship) {
        boolean isEffective = false;
        List<ShipAPI> nearbyShips = CombatUtils.getShipsWithinRange(ship.getLocation(), RANGE);
        if (!nearbyShips.isEmpty()) {
            for (ShipAPI nearby : nearbyShips) {
                if (nearby.isFighter()) continue;
                if (nearby.getOwner() == ship.getOwner()) {
                    isEffective = true;
                    break;
                }
            }
        }
        final MutableShipStatsAPI stats = ship.getMutableStats();
        if (isEffective) {
            stats.getFluxDissipation().modifyMult(rule.getBuffID(), MULTIPLIER);
            stats.getShieldDamageTakenMult().modifyMult(rule.getBuffID(), 1f / MULTIPLIER);
            if (ship == engine.getPlayerShip()) {
                engine.maintainStatusForPlayerShip(rule.getBuffID(),
                        rule.getSpritePath(),
                        rule.getName(),
                        rule.getExtra1() + getFactorString(MULTIPLIER),
                        false);
            }
        } else {
            stats.getFluxDissipation().unmodify(rule.getBuffID());
            stats.getShieldDamageTakenMult().unmodify(rule.getBuffID());
        }
    }

    @Override
    public String getDescriptionParams(int index) {
        if (index == 0) return getFactorString(RANGE);
        if (index == 1) return getFactorString(MULTIPLIER);
        return null;
    }

    @Override
    public String getDescriptionParams(int index, int difficulty) {
        return getDescriptionParams(index);
    }

}

