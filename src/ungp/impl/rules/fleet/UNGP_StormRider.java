package ungp.impl.rules.fleet;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import ungp.scripts.campaign.specialist.UNGP_SpecialistSettings;
import ungp.api.rules.UNGP_BaseRuleEffect;
import ungp.api.rules.tags.UNGP_PlayerFleetTag;

public class UNGP_StormRider extends UNGP_BaseRuleEffect implements UNGP_PlayerFleetTag {

    @Override
    public void updateDifficultyCache(UNGP_SpecialistSettings.Difficulty difficulty) {

    }

    @Override
    public float getValueByDifficulty(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        return 0;
    }

    @Override
    public void applyPlayerFleetStats(CampaignFleetAPI fleet) {
        fleet.getMemoryWithoutUpdate().set("$stormStrikeTimeout", true, 0.1f);
    }

    @Override
    public void unapplyPlayerFleetStats(CampaignFleetAPI fleet) {
        fleet.getMemoryWithoutUpdate().unset("$stormStrikeTimeout");
    }

    @Override
    public String getDescriptionParams(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        return super.getDescriptionParams(index, difficulty);
    }
}
