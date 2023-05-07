package ungp.impl.rules.character;

import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import ungp.scripts.campaign.specialist.UNGP_SpecialistSettings;
import ungp.api.rules.UNGP_BaseRuleEffect;
import ungp.api.rules.tags.UNGP_PlayerCharacterStatsSkillTag;

public class UNGP_EwSmog extends UNGP_BaseRuleEffect implements UNGP_PlayerCharacterStatsSkillTag {

    private float ewBonus;

    @Override
    public void updateDifficultyCache(UNGP_SpecialistSettings.Difficulty difficulty) {
        ewBonus = getValueByDifficulty(0, difficulty);
    }

    @Override
    public float getValueByDifficulty(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return difficulty.getLinearValue(5f, 5f);
        return super.getValueByDifficulty(index, difficulty);
    }

    @Override
    public void apply(MutableCharacterStatsAPI stats) {
        stats.getDynamic().getMod(Stats.ELECTRONIC_WARFARE_MAX).modifyFlat(buffID, ewBonus);
    }

    @Override
    public void unapply(MutableCharacterStatsAPI stats) {
        stats.getDynamic().getMod(Stats.ELECTRONIC_WARFARE_MAX).unmodifyFlat(buffID);
    }

    @Override
    public String getDescriptionParams(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return getPercentString(getValueByDifficulty(index, difficulty));
        return super.getDescriptionParams(index, difficulty);
    }


}
