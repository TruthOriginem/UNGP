package data.scripts.ungprules.impl.fleet;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import data.scripts.campaign.specialist.UNGP_SpecialistSettings;
import data.scripts.ungprules.impl.UNGP_MemberBuffRuleEffect;
import data.scripts.ungprules.tags.UNGP_PlayerFleetTag;

public class UNGP_DisarmamentAgreement extends UNGP_MemberBuffRuleEffect implements UNGP_PlayerFleetTag {
    private static final float EXTRA_SUPPLIES_PENALTY = 20f;
    private int memberLimit;
    private int curMemberCount;
    private float curSuppliesPenalty;

    @Override
    public void updateDifficultyCache(UNGP_SpecialistSettings.Difficulty difficulty) {
        memberLimit = (int) getValueByDifficulty(0, difficulty);
    }

    @Override
    public float getValueByDifficulty(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return Math.round(difficulty.getLinearValue(20, -5));
        if (index == 1) return EXTRA_SUPPLIES_PENALTY;
        return 0;
    }

    @Override
    public String getDescriptionParams(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return getFactorString(getValueByDifficulty(index, difficulty));
        if (index == 1) return getPercentString(EXTRA_SUPPLIES_PENALTY);
        return super.getDescriptionParams(index, difficulty);
    }

    @Override
    public void applyPlayerFleetMemberInCampaign(FleetMemberAPI member) {
        member.getStats().getSuppliesPerMonth().modifyPercent(getBuffID(), curSuppliesPenalty);
    }

    @Override
    public boolean canApply(FleetMemberAPI member) {
        return curMemberCount > memberLimit;
    }

    @Override
    public void applyPlayerFleetStats(CampaignFleetAPI fleet) {
        curMemberCount = fleet.getNumMembersFast();
        int diff = curMemberCount - memberLimit;
        if (diff > 0) {
            curSuppliesPenalty = diff * EXTRA_SUPPLIES_PENALTY;
        } else {
            curSuppliesPenalty = 0;
        }
    }

    @Override
    public void unapplyPlayerFleetStats(CampaignFleetAPI fleet) {
        curMemberCount = 0;
        curSuppliesPenalty = 0;
    }
}
