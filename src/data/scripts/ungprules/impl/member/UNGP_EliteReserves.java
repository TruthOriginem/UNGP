package data.scripts.ungprules.impl.member;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import data.scripts.ungprules.impl.UNGP_MemberBuffRuleEffect;
import data.scripts.ungprules.tags.UNGP_PlayerFleetTag;

public class UNGP_EliteReserves extends UNGP_MemberBuffRuleEffect implements UNGP_PlayerFleetTag {
    private float maxBonus;
    private float curBonus;

    @Override
    public void updateDifficultyCache(int difficulty) {
        maxBonus = getValueByDifficulty(0, difficulty);
        curBonus = 0f;
    }

    //15~30
    @Override
    public float getValueByDifficulty(int index, int difficulty) {
        if (index == 0) return 0.15f;
        return 0;
    }

    @Override
    public void applyPlayerFleetStats(CampaignFleetAPI fleet) {
        float crew = fleet.getCargo().getCrew();
        float minCrew = fleet.getFleetData().getMinCrew();
        float level = (crew - minCrew) / minCrew;
        level = Math.min(1, Math.max(0, level));
        curBonus = level * maxBonus;
    }

    @Override
    public void unapplyPlayerFleetStats(CampaignFleetAPI fleet) {

    }

    @Override
    public void applyPlayerFleetMemberInCampaign(FleetMemberAPI member) {
        member.getStats().getMaxCombatReadiness().modifyFlat(rule.getBuffID(), curBonus, rule.getName());
    }

    @Override
    public String getDescriptionParams(int index) {
        if (index == 0) return getPercentString(maxBonus * 100f);
        return null;
    }

    @Override
    public String getDescriptionParams(int index, int difficulty) {
        if (index == 0) return getPercentString(getValueByDifficulty(index, difficulty) * 100f);
        return null;
    }
}
