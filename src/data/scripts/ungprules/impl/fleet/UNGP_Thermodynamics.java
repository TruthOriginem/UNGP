package data.scripts.ungprules.impl.fleet;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.impl.campaign.ids.Abilities;
import data.scripts.campaign.everyframe.UNGP_CampaignPlugin;
import data.scripts.campaign.specialist.UNGP_SpecialistSettings;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import data.scripts.ungprules.tags.UNGP_CampaignTag;

public class UNGP_Thermodynamics extends UNGP_BaseRuleEffect implements UNGP_CampaignTag {

    @Override
    public void updateDifficultyCache(UNGP_SpecialistSettings.Difficulty difficulty) {

    }

    @Override
    public float getValueByDifficulty(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        return 0;
    }

    @Override
    public void advanceInCampaign(float amount, UNGP_CampaignPlugin.TempCampaignParams params) {
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        if (playerFleet != null) {
        	boolean burn = playerFleet.getAbility(Abilities.EMERGENCY_BURN) != null && playerFleet.getAbility(Abilities.EMERGENCY_BURN).isInProgress();
            if (burn) {
            	playerFleet.getStats().getFleetwideMaxBurnMod().modifyFlat(buffID, 100f, rule.getName());
			} else {
				playerFleet.getStats().getFleetwideMaxBurnMod().unmodifyFlat(buffID);
			}
        }
    }

    @Override
    public String getDescriptionParams(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        return super.getDescriptionParams(index, difficulty);
    }
}