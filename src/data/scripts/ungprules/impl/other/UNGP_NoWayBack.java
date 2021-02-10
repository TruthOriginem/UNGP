package data.scripts.ungprules.impl.other;

import data.scripts.campaign.UNGP_InGameData;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;

public class UNGP_NoWayBack extends UNGP_BaseRuleEffect {
    private float timeMultiplier;

    @Override
    public void updateDifficultyCache(int difficulty) {
        timeMultiplier = getValueByDifficulty(0, difficulty);
    }

    @Override
    public float getValueByDifficulty(int index, int difficulty) {
        if (index == 0) return getLinearValue(0.25f, 0.1f, difficulty);
        return 0;
    }

    @Override
    public String getDescriptionParams(int index) {
        if (index == 0) return getFactorString(timeMultiplier);
        if (index == 1) return getFactorString((int) (1f / timeMultiplier));
        return null;
    }

    @Override
    public String getDescriptionParams(int index, int difficulty) {
        if (index == 0) return getFactorString(getValueByDifficulty(0, difficulty));
        if (index == 1) return getFactorString((float) Math.ceil(1f / getValueByDifficulty(0, difficulty)));
        return super.getDescriptionParams(index, difficulty);
    }

    @Override
    public void applyGlobalStats() {
        UNGP_InGameData.getDataInSave().getChangeTimeStat().modifyMult(rule.getBuffID(), timeMultiplier);
    }

    @Override
    public void unapplyGlobalStats() {
        UNGP_InGameData.getDataInSave().getChangeTimeStat().unmodify(rule.getBuffID());
    }
}
