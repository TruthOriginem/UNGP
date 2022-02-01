package data.scripts.ungprules.impl.member;

import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import data.scripts.campaign.specialist.UNGP_SpecialistSettings;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import data.scripts.ungprules.tags.UNGP_PlayerFleetMemberTag;

public class UNGP_CruisingHunting extends UNGP_BaseRuleEffect implements UNGP_PlayerFleetMemberTag {
    private static final int DP_REDUCTION_FLAT = 2;
    private float dpReductionFactor;

    @Override
    public void updateDifficultyCache(UNGP_SpecialistSettings.Difficulty difficulty) {
        dpReductionFactor = getValueByDifficulty(0, difficulty);
    }

    @Override
    public float getValueByDifficulty(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return difficulty.getLinearValue(0.1f, 0.05f);
        return super.getValueByDifficulty(index, difficulty);
    }

    @Override
    public void applyPlayerFleetMemberInCampaign(FleetMemberAPI member) {
        float dp = member.getDeploymentPointsCost();
        int reduction = (int) Math.ceil(Math.max(DP_REDUCTION_FLAT, dp * dpReductionFactor));
        member.getStats().getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD).modifyFlat(buffID, -reduction);
    }

    @Override
    public boolean canApply(FleetMemberAPI member) {
        return member.isCruiser();
    }

    @Override
    public String getBuffID() {
        return buffID;
    }

    @Override
    public String getDescriptionParams(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return getPercentString(getValueByDifficulty(index, difficulty) * 100f);
        if (index == 1) return DP_REDUCTION_FLAT + "";
        return super.getDescriptionParams(index, difficulty);
    }
}
