package data.scripts.ungprules.impl.member;

import com.fs.starfarer.api.fleet.FleetMemberAPI;
import data.scripts.ungprules.impl.UNGP_MemberBuffRuleEffect;

public class UNGP_LowRepairTech extends UNGP_MemberBuffRuleEffect {
    private float reduction;

    @Override
    public void updateDifficultyCache(int difficulty) {
        reduction = getValueByDifficulty(0, difficulty);
    }

    @Override
    public float getValueByDifficulty(int index, int difficulty) {
        if (index == 0) return getLinearValue(0.15f, 0.3f, difficulty);
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
