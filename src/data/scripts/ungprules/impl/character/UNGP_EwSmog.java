package data.scripts.ungprules.impl.character;

import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import data.scripts.campaign.specialist.UNGP_SpecialistSettings;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import data.scripts.ungprules.tags.UNGP_CharacterTag;

public class UNGP_EwSmog extends UNGP_BaseRuleEffect implements UNGP_CharacterTag {

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
    public void applyPlayerCharacterStats(MutableCharacterStatsAPI stats) {
        stats.getDynamic().getMod(Stats.ELECTRONIC_WARFARE_MAX).modifyFlat(buffID, ewBonus);
    }

    @Override
    public void unapplyPlayerCharacterStats(MutableCharacterStatsAPI stats) {
        stats.getDynamic().getMod(Stats.ELECTRONIC_WARFARE_MAX).unmodifyFlat(buffID);
    }

    @Override
    public String getDescriptionParams(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return getPercentString(getValueByDifficulty(index, difficulty));
        return super.getDescriptionParams(index, difficulty);
    }
}
