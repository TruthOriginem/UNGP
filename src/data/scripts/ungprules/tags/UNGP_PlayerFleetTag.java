package data.scripts.ungprules.tags;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;

/**
 * Called before {@link UNGP_PlayerFleetMemberTag}
 */
public interface UNGP_PlayerFleetTag {
    /**
     * 除了更新缓存时会更新，每帧都会更新
     */
    void applyPlayerFleetStats(CampaignFleetAPI fleet);

    void unapplyPlayerFleetStats(CampaignFleetAPI fleet);
}
