package data.scripts.ungprules.impl.fleet;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import data.scripts.ungprules.tags.UNGP_PlayerFleetTag;

public class UNGP_EfficientFuel extends UNGP_BaseRuleEffect implements UNGP_PlayerFleetTag {
    private float bonus;

    @Override
    public void updateDifficultyCache(int difficulty) {
        bonus = getValueByDifficulty(0, difficulty);
    }

    @Override
    public float getValueByDifficulty(int index, int difficulty) {
        if (index == 0) return 1f - (0.05f + 0.05f * (float) Math.pow(difficulty, 0.5373));
        return 0;
    }


    @Override
    public void applyPlayerFleetStats(CampaignFleetAPI fleet) {
        fleet.getStats().getFuelUseHyperMult().modifyMult(rule.getBuffID(), bonus);
        fleet.getStats().getFuelUseNormalMult().modifyMult(rule.getBuffID(), bonus);
    }

    @Override
    public void unapplyPlayerFleetStats(CampaignFleetAPI fleet) {
        fleet.getStats().getFuelUseHyperMult().unmodify(rule.getBuffID());
        fleet.getStats().getFuelUseNormalMult().unmodify(rule.getBuffID());
    }

    @Override
    public String getDescriptionParams(int index) {
        if (index == 0) return getFactorString(bonus);
        return null;
    }

    @Override
    public String getDescriptionParams(int index, int difficulty) {
        if (index == 0) return getFactorString(getValueByDifficulty(index, difficulty));
        return null;
    }
}
