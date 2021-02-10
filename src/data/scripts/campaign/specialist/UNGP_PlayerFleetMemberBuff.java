package data.scripts.campaign.specialist;

import com.fs.starfarer.api.campaign.BuffManagerAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import data.scripts.ungprules.tags.UNGP_PlayerFleetMemberTag;

public class UNGP_PlayerFleetMemberBuff implements BuffManagerAPI.Buff {
    private String id;
    private float dur;
    private transient UNGP_PlayerFleetMemberTag tag;

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
}
