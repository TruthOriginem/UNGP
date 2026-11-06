package ungp.impl.rules.fleet;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.util.Misc;
import ungp.api.rules.UNGP_BaseRuleEffect;
import ungp.api.rules.tags.UNGP_CampaignTag;
import ungp.scripts.campaign.everyframe.UNGP_CampaignPlugin.TempCampaignParams;

public class UNGP_KeepTheWayClear extends UNGP_BaseRuleEffect implements UNGP_CampaignTag {
    private static final String OWNERSHIP_KEY = "$ungp_keepTheWayClear";
    private static final float DURATION_DAYS = 0.05f;

    @Override
    public void advanceInCampaign(float amount, TempCampaignParams params) {
        CampaignFleetAPI player = Global.getSector().getPlayerFleet();
        if (player == null) return;
        LocationAPI location = player.getContainingLocation();
        if (location == null) return;

        for (CampaignFleetAPI fleet : location.getFleets()) {
            if (!Misc.isPatrol(fleet)) continue;
            MemoryAPI memory = fleet.getMemoryWithoutUpdate();
            String flag = MemFlags.MEMORY_KEY_PATROL_ALLOW_TOFF;
            if (!memory.getBoolean(flag) ||
                    (memory.getBoolean(OWNERSHIP_KEY) && memory.getExpire(flag) > 0f &&
                            memory.getExpire(flag) <= memory.getExpire(OWNERSHIP_KEY) + 0.0001f)) {
                memory.set(flag, true, DURATION_DAYS);
                memory.set(OWNERSHIP_KEY, true, DURATION_DAYS);
            }
        }
    }
}
