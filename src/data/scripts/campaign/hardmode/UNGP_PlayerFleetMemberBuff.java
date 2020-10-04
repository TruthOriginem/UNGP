package data.scripts.campaign.hardmode;

import com.fs.starfarer.api.campaign.BuffManagerAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import data.scripts.ungprules.tags.UNGP_PlayerFleetMemberTag;

public class UNGP_PlayerFleetMemberBuff implements BuffManagerAPI.Buff {
    private String id;
    private float dur;

    public UNGP_PlayerFleetMemberBuff(String id, float dur) {
        this.id = id;
        this.dur = dur;
    }

    @Override
    public void advance(float days) {
        dur -= days;
    }

    @Override
    public void apply(FleetMemberAPI member) {
        for (UNGP_PlayerFleetMemberTag tag : UNGP_RulesManager.PLAYER_FLEET_MEMBER_TAGS_ITG) {
            tag.applyPlayerFleetMemberInCampaign(member);
        }
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

}
