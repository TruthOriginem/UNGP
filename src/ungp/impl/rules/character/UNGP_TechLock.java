package ungp.impl.rules.character;

import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import ungp.scripts.campaign.specialist.UNGP_SpecialistSettings;
import ungp.api.rules.UNGP_BaseRuleEffect;
import ungp.api.rules.tags.UNGP_PlayerCharacterStatsSkillTag;

public class UNGP_TechLock extends UNGP_BaseRuleEffect implements UNGP_PlayerCharacterStatsSkillTag {
    private float reduction;

    @Override
    public void updateDifficultyCache(UNGP_SpecialistSettings.Difficulty difficulty) {
        reduction = getValueByDifficulty(0, difficulty);
    }

    @Override
    public void apply(MutableCharacterStatsAPI stats) {
        stats.getShipOrdnancePointBonus().modifyPercent(buffID, -reduction * 100f);
    }

    @Override
    public void unapply(MutableCharacterStatsAPI stats) {
        stats.getShipOrdnancePointBonus().unmodify(buffID);
    }

    @Override
    public float getValueByDifficulty(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return difficulty.getLinearValue(0.05f, 0.05f);
        return super.getValueByDifficulty(index, difficulty);
    }

    @Override
    public String getDescriptionParams(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return getPercentString(getValueByDifficulty(index, difficulty) * 100f);
        return super.getDescriptionParams(index, difficulty);
    }


}
