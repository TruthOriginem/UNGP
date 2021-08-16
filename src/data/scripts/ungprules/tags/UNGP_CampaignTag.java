package data.scripts.ungprules.tags;

import data.scripts.campaign.everyframe.UNGP_CampaignPlugin.TempCampaignParams;

public interface UNGP_CampaignTag {

    /**
     * Called while paused.
     *
     * @param amount amount == 0 while paused
     * @param params
     */
    void advanceInCampaign(float amount, TempCampaignParams params);
}
