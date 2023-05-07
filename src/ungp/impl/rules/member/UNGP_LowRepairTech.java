package ungp.impl.rules.member;

import com.fs.starfarer.api.fleet.FleetMemberAPI;
import ungp.scripts.campaign.specialist.UNGP_SpecialistSettings;
import ungp.api.rules.UNGP_MemberBuffRuleEffect;

public class UNGP_LowRepairTech extends UNGP_MemberBuffRuleEffect {
    private float reduction;

    @Override
    public void updateDifficultyCache(UNGP_SpecialistSettings.Difficulty difficulty) {
        reduction = getValueByDifficulty(0, difficulty);
    }

    //0.15~0.25
    @Override
    public float getValueByDifficulty(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return difficulty.getLinearValue(0.15f, 0.1f);
        return 0;
    }

    @Override
    public void applyPlayerFleetMemberInCampaign(FleetMemberAPI member) {
        member.getStats().getBaseCRRecoveryRatePercentPerDay().modifyPercent(buffID, -reduction * 100f);
        member.getStats().getRepairRatePercentPerDay().modifyPercent(buffID, -reduction * 100f);
    }

    @Override
    public String getDescriptionParams(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return getPercentString(getValueByDifficulty(index, difficulty) * 100f);
        return super.getDescriptionParams(index, difficulty);
    }
}
