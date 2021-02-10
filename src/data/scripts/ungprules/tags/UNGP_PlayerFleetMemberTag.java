package data.scripts.ungprules.tags;

import com.fs.starfarer.api.campaign.BuffManagerAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

public interface UNGP_PlayerFleetMemberTag {
    /**
     * 通过{@link BuffManagerAPI.Buff} 实现，这个buff存续时间是0.1秒，每帧刷新一次
     *
     * @param member
     */
    void applyPlayerFleetMemberInCampaign(FleetMemberAPI member);

    /**
     * 判断能不能上buff
     *
     * @param member
     * @return
     */
    boolean canApply(FleetMemberAPI member);

    String getBuffID();
}
