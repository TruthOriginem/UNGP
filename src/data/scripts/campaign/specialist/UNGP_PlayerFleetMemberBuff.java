package data.scripts.campaign.specialist;

import com.fs.starfarer.api.campaign.BuffManagerAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import data.scripts.campaign.everyframe.UNGP_CampaignPlugin;
import data.scripts.ungprules.tags.UNGP_PlayerFleetMemberTag;

public class UNGP_PlayerFleetMemberBuff implements BuffManagerAPI.Buff {
    private transient static boolean shouldForceSyncNextStep = false;
    private transient UNGP_PlayerFleetMemberTag tag;
    private String id;
    private float dur;

    public UNGP_PlayerFleetMemberBuff(UNGP_PlayerFleetMemberTag tag, float dur) {
        this.id = tag.getBuffID();
        this.dur = dur;
        this.tag = tag;
    }


    @Override
    public void advance(float days) {
        dur -= days;
    }

    @Override
    public void apply(FleetMemberAPI member) {
        if (tag != null)
            tag.applyPlayerFleetMemberInCampaign(member);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean isExpired() {
        return dur <= 0;
    }

    public float getDur() {
        return dur;
    }

    public void setDur(float dur) {
        this.dur = dur;
    }

    public void setTag(UNGP_PlayerFleetMemberTag tag) {
        if (this.tag == null) {
            this.id = tag.getBuffID();
            this.tag = tag;
        }
    }

    public static boolean isShouldForceSyncNextStep() {
        return shouldForceSyncNextStep;
    }

    /**
     * Call this to force sync the buff in {@link UNGP_CampaignPlugin} in next step;
     */
    public static void forceSyncNextStep() {
        shouldForceSyncNextStep = true;
    }

    public static void completeForceSync() {
        shouldForceSyncNextStep = false;
    }
}
