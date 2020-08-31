package data.scripts.ungprules.impl.member;

import com.fs.starfarer.api.fleet.FleetMemberAPI;
import data.scripts.ungprules.UNGP_BaseRuleEffect;

public class UNGP_LowRepairTech extends UNGP_BaseRuleEffect {
    private float reduction;

    @Override
    public void refreshDifficultyCache(int difficulty) {
        reduction = getValueByDifficulty(0, difficulty);
    }

    @Override
    public float getValueByDifficulty(int index, int difficulty) {
        if (index == 0) return 0.05f + 0.05f * (float) Math.pow(difficulty, 0.5373);
        return 0;
    }

    @Override
    public void applyPlayerFleetMemberInCampaign(FleetMemberAPI member) {
        member.getStats().getBaseCRRecoveryRatePercentPerDay().modifyPercent(rule.getBuffID(), -reduction * 100f);
        member.getStats().getRepairRatePercentPerDay().modifyPercent(rule.getBuffID(), -reduction * 100f);
    }

    @Override
    public String getDescriptionParams(int index) {
        if (index == 0) return getPercentString(reduction * 100f);
        return null;
    }

    @Override
    public String getDescriptionParams(int index, int difficulty) {
        if (index == 0) return getPercentString(getValueByDifficulty(index, difficulty) * 100f);
        return null;
    }
}
