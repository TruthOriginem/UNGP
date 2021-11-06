package data.scripts.ungprules.impl.fleet;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import data.scripts.ungprules.tags.UNGP_PlayerFleetMemberTag;
import data.scripts.ungprules.tags.UNGP_PlayerFleetTag;

public class UNGP_DeepSpaceFear extends UNGP_BaseRuleEffect implements UNGP_PlayerFleetTag, UNGP_PlayerFleetMemberTag {
    private static final float SENSOR_STRENGTH_MULTIPLIER = 0.5f;
    private float profileMultiplier;
    private boolean isInHyperspace = false;

    @Override
    public void updateDifficultyCache(int difficulty) {
        profileMultiplier = getValueByDifficulty(1, difficulty);
    }

    @Override
    public float getValueByDifficulty(int index, int difficulty) {
        if (index == 1) return getLinearValue(1.5f, 3f, difficulty);
        return 0;
    }


    @Override
    public String getDescriptionParams(int index, int difficulty) {
        if (index == 0) return getFactorString(SENSOR_STRENGTH_MULTIPLIER);
        if (index == 1) return getFactorString(getValueByDifficulty(index, difficulty));
        return super.getDescriptionParams(index, difficulty);
    }

    @Override
    public void applyPlayerFleetStats(CampaignFleetAPI fleet) {
        isInHyperspace = fleet.isInHyperspace();
    }

    @Override
    public void unapplyPlayerFleetStats(CampaignFleetAPI fleet) {
    }

    @Override
    public void applyPlayerFleetMemberInCampaign(FleetMemberAPI member) {
        String id = rule.getBuffID();
        member.getStats().getSensorProfile().modifyMult(id, profileMultiplier);
        member.getStats().getSensorStrength().modifyMult(id, SENSOR_STRENGTH_MULTIPLIER);
    }

    @Override
    public boolean canApply(FleetMemberAPI member) {
        return isInHyperspace;
    }

    @Override
    public String getBuffID() {
        return rule.getBuffID();
    }
}
