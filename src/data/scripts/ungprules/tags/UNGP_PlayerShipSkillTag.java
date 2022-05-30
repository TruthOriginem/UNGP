package data.scripts.ungprules.tags;

import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

/**
 * Apply to all ships in player's fleet like the vanilla skills.
 */
public interface UNGP_PlayerShipSkillTag extends UNGP_SkillTag {
    void apply(FleetDataAPI fleetData, FleetMemberAPI member, MutableShipStatsAPI stats, ShipAPI.HullSize hullSize);

    void unapply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize);
}
