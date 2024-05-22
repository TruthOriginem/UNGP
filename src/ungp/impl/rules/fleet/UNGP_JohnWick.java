package ungp.impl.rules.fleet;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.ai.FleetAssignmentDataAPI;
import org.lazywizard.lazylib.MathUtils;
import ungp.api.rules.UNGP_BaseRuleEffect;
import ungp.api.rules.tags.UNGP_CampaignTag;
import ungp.scripts.campaign.everyframe.UNGP_CampaignPlugin;
import ungp.scripts.campaign.specialist.UNGP_SpecialistSettings;

public class UNGP_JohnWick extends UNGP_BaseRuleEffect implements UNGP_CampaignTag {
    @Override
    public void advanceInCampaign(float amount, UNGP_CampaignPlugin.TempCampaignParams params) {
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        LocationAPI containingLocation = playerFleet.getContainingLocation();
        for (CampaignFleetAPI otherFleet : containingLocation.getFleets()) {
            if (otherFleet.isDespawning()) continue;
            if (otherFleet.isHidden()) continue;
            if (!otherFleet.isHostileTo(playerFleet)) continue;
            if (MathUtils.getDistance(playerFleet, otherFleet) > 3000) continue;
            if (otherFleet.getAI() != null) {
                FleetAssignmentDataAPI assignment = otherFleet.getAI().getCurrentAssignment();
                if (assignment != null && assignment.getTarget() == playerFleet) {
                    otherFleet.getStats().addTemporaryModFlat(3f, buffID, 3, otherFleet.getStats().getFleetwideMaxBurnMod());
                }
            }
        }
    }

    @Override
    public String getDescriptionParams(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return "3";
        return super.getDescriptionParams(index, difficulty);
    }
}
