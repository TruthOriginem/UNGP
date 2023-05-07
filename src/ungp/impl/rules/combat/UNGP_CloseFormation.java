package ungp.impl.rules.combat;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import ungp.scripts.campaign.specialist.UNGP_SpecialistSettings;
import ungp.api.rules.UNGP_BaseRuleEffect;
import ungp.api.rules.tags.UNGP_CombatTag;
import org.lazywizard.lazylib.combat.CombatUtils;

import java.util.List;

public class UNGP_CloseFormation extends UNGP_BaseRuleEffect implements UNGP_CombatTag {
    private static final float RANGE = 600f;
    private static final float MULTIPLIER = 1.1f;

    @Override
    public float getValueByDifficulty(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
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
            stats.getFluxDissipation().modifyMult(buffID, MULTIPLIER);
            stats.getShieldDamageTakenMult().modifyMult(buffID, 1f / MULTIPLIER);
            if (ship == engine.getPlayerShip()) {
                engine.maintainStatusForPlayerShip(buffID,
                                                   rule.getSpritePath(),
                                                   rule.getName(),
                                                   rule.getExtra1() + getFactorString(MULTIPLIER),
                                                   false);
            }
        } else {
            stats.getFluxDissipation().unmodify(buffID);
            stats.getShieldDamageTakenMult().unmodify(buffID);
        }
    }


    @Override
    public String getDescriptionParams(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return getFactorString(RANGE);
        if (index == 1) return getFactorString(MULTIPLIER);
        return null;
    }

}

