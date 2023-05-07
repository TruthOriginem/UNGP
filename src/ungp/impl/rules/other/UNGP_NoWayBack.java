package ungp.impl.rules.other;

import ungp.scripts.campaign.UNGP_InGameData;
import ungp.scripts.campaign.specialist.UNGP_SpecialistSettings;
import ungp.api.rules.UNGP_BaseRuleEffect;

public class UNGP_NoWayBack extends UNGP_BaseRuleEffect {
    private float timeMultiplier;

    @Override
    public void updateDifficultyCache(UNGP_SpecialistSettings.Difficulty difficulty) {
        timeMultiplier = getValueByDifficulty(0, difficulty);
    }

    @Override
    public float getValueByDifficulty(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return difficulty.getLinearValue(0.25f, -0.15f);
        return 0;
    }

    @Override
    public String getDescriptionParams(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return getFactorString(getValueByDifficulty(0, difficulty));
        if (index == 1) return getFactorString((float) Math.ceil(1f / getValueByDifficulty(0, difficulty)));
        return super.getDescriptionParams(index, difficulty);
    }

    @Override
    public void applyGlobalStats() {
        UNGP_InGameData.getDataInSave().getChangeTimeStat().modifyMult(buffID, timeMultiplier);
    }

    @Override
    public void unapplyGlobalStats() {
        UNGP_InGameData.getDataInSave().getChangeTimeStat().unmodify(buffID);
    }
}
