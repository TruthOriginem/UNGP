package data.scripts.ungprules.impl.member;

import com.fs.starfarer.api.fleet.FleetMemberAPI;
import data.scripts.ungprules.UNGP_BaseRuleEffect;

public class UNGP_WideRadar extends UNGP_BaseRuleEffect {
    private float bonus;

    @Override
    public void refreshDifficultyCache(int difficulty) {
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
        member.getStats().getSensorStrength().modifyPercent(rule.getBuffID(), bonus * 100f);
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
