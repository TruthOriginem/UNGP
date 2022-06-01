package data.scripts.ungprules.impl.character;

import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import data.scripts.campaign.specialist.UNGP_SpecialistSettings;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import data.scripts.ungprules.tags.UNGP_CharacterTag;

public class UNGP_Besieged extends UNGP_BaseRuleEffect implements UNGP_CharacterTag {

    private float dp;

    @Override
    public void updateDifficultyCache(UNGP_SpecialistSettings.Difficulty difficulty) {
        dp = getValueByDifficulty(0, difficulty);
    }

    @Override
    public float getValueByDifficulty(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return difficulty.getLinearValue(0.04f, 0.04f);
        return super.getValueByDifficulty(index, difficulty);
    }

    @Override
    public void applyPlayerCharacterStats(MutableCharacterStatsAPI stats) {
        stats.getDynamic().getMod(Stats.DEPLOYMENT_POINTS_FRACTION_OF_BATTLE_SIZE_BONUS_MOD).modifyFlat(buffID, -dp);
        stats.getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MIN_FRACTION_OF_BATTLE_SIZE_BONUS_MOD).modifyFlat(buffID, -dp);
    }

    @Override
    public void unapplyPlayerCharacterStats(MutableCharacterStatsAPI stats) {
        stats.getDynamic().getMod(Stats.DEPLOYMENT_POINTS_FRACTION_OF_BATTLE_SIZE_BONUS_MOD).unmodifyFlat(buffID);
        stats.getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MIN_FRACTION_OF_BATTLE_SIZE_BONUS_MOD).unmodifyFlat(buffID);
    }

    @Override
    public String getDescriptionParams(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return getPercentString(getValueByDifficulty(index, difficulty) * 100f);
        return super.getDescriptionParams(index, difficulty);
    }
}
