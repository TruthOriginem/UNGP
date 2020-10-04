package data.scripts.ungprules.tags;

import data.scripts.campaign.UNGP_CampaignPlugin.TempCampaignParams;

public interface UNGP_CampaignTag {
    void advanceInCampaign(float amount, TempCampaignParams params);
}
