package data.scripts.ungprules.impl.member;

import com.fs.starfarer.api.fleet.FleetMemberAPI;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import data.scripts.ungprules.tags.UNGP_PlayerFleetMemberTag;

public class UNGP_WideRadar extends UNGP_BaseRuleEffect implements UNGP_PlayerFleetMemberTag {
    private float bonus;

    @Override
    public void updateDifficultyCache(int difficulty) {
        bonus = getValueByDifficulty(0, difficulty);
    }

    //15~30
    @Override
    public float getValueByDifficulty(int index, int difficulty) {
        if (index == 0) return 0.15f + ((0.15f / 19f) * (difficulty - 1));
        return 0;
    }

    @Override
    public void applyPlayerFleetMemberInCampaign(FleetMemberAPI member) {
        member.getStats().getSensorStrength().modifyMult(rule.getBuffID(), 1f + bonus);
    }

    @Override
    public String getDescriptionParams(int index) {
        if (index == 0) return getPercentString(bonus * 100f);
        return null;
    }

    @Override
    public String getDescriptionParams(int index, int difficulty) {
        if (index == 0) return getPercentString(getValueByDifficulty(index, difficulty) * 100f);
        return null;
    }
}
