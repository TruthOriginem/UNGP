package data.scripts.ungprules.impl;

import com.fs.starfarer.api.fleet.FleetMemberAPI;
import data.scripts.ungprules.tags.UNGP_PlayerFleetMemberTag;

/**
 * 如果是带有FleetMemberTag的可以继承此类
 */
public abstract class UNGP_MemberBuffRuleEffect extends UNGP_BaseRuleEffect implements UNGP_PlayerFleetMemberTag {
    @Override
    public boolean canApply(FleetMemberAPI member) {
        return true;
    }

    @Override
    public String getBuffID() {
        return buffID;
    }
}
