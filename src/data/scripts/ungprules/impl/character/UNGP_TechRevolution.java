package data.scripts.ungprules.impl.character;

import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import data.scripts.campaign.specialist.UNGP_SpecialistSettings;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import data.scripts.ungprules.tags.UNGP_PlayerCharacterStatsSkillTag;

public class UNGP_TechRevolution extends UNGP_BaseRuleEffect implements UNGP_PlayerCharacterStatsSkillTag {
    private float bonus;

    @Override
    public void updateDifficultyCache(UNGP_SpecialistSettings.Difficulty difficulty) {
        bonus = getValueByDifficulty(0, difficulty);
    }

    @Override
    public float getValueByDifficulty(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return difficulty.getLinearValue(0.07f, 0.02f);
        return super.getValueByDifficulty(index, difficulty);
    }

    @Override
    public void apply(MutableCharacterStatsAPI stats) {
        stats.getShipOrdnancePointBonus().modifyPercent(buffID, bonus * 100f);
    }

    @Override
    public void unapply(MutableCharacterStatsAPI stats) {
        stats.getShipOrdnancePointBonus().unmodify(buffID);
    }

    @Override
    public String getDescriptionParams(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return getPercentString(getValueByDifficulty(index, difficulty) * 100f);
        return super.getDescriptionParams(index, difficulty);
    }


}
