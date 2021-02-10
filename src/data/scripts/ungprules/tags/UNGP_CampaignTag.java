package data.scripts.ungprules.tags;

import data.scripts.campaign.everyframe.UNGP_CampaignPlugin.TempCampaignParams;

public interface UNGP_CampaignTag {
    void advanceInCampaign(float amount, TempCampaignParams params);
}
