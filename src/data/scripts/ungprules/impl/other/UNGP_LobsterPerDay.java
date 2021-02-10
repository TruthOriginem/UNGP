package data.scripts.ungprules.impl.other;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import data.scripts.campaign.everyframe.UNGP_CampaignPlugin;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import data.scripts.ungprules.tags.UNGP_CampaignTag;

public class UNGP_LobsterPerDay extends UNGP_BaseRuleEffect implements UNGP_CampaignTag {

    @Override
    public void updateDifficultyCache(int difficulty) {

    }

    @Override
    public float getValueByDifficulty(int index, int difficulty) {
        return 0;
    }

    @Override
    public void advanceInCampaign(float amount, UNGP_CampaignPlugin.TempCampaignParams params) {
        if (params.isOneDayPassed()) {
            CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
            if (fleet != null) {
                CargoAPI cargo = fleet.getCargo();
                if (cargo.getSpaceLeft() > 0.9) {
                    cargo.addCommodity(Commodities.LOBSTER, 1f);
                }
            }
        }
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
