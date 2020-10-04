package data.scripts.ungprules.impl.fleet;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import data.scripts.ungprules.tags.UNGP_PlayerFleetTag;

public class UNGP_StormRider extends UNGP_BaseRuleEffect implements UNGP_PlayerFleetTag {

    @Override
    public void updateDifficultyCache(int difficulty) {

    }

    @Override
    public float getValueByDifficulty(int index, int difficulty) {
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
    public String getDescriptionParams(int index) {
        return null;
    }

    @Override
    public String getDescriptionParams(int index, int difficulty) {
        return null;
    }
}
