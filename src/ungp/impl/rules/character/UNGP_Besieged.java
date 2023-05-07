package ungp.impl.rules.character;

import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import ungp.scripts.campaign.specialist.UNGP_SpecialistSettings;
import ungp.api.rules.UNGP_BaseRuleEffect;
import ungp.api.rules.tags.UNGP_PlayerCharacterStatsSkillTag;

public class UNGP_Besieged extends UNGP_BaseRuleEffect implements UNGP_PlayerCharacterStatsSkillTag {

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
    public String getDescriptionParams(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return getPercentString(getValueByDifficulty(index, difficulty) * 100f);
        return super.getDescriptionParams(index, difficulty);
    }

    @Override
    public void apply(MutableCharacterStatsAPI stats) {
        stats.getDynamic().getMod(Stats.DEPLOYMENT_POINTS_FRACTION_OF_BATTLE_SIZE_BONUS_MOD).modifyFlat(buffID, -dp);
        stats.getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MIN_FRACTION_OF_BATTLE_SIZE_BONUS_MOD).modifyFlat(buffID, -dp);
    }

    @Override
    public void unapply(MutableCharacterStatsAPI stats) {
        stats.getDynamic().getMod(Stats.DEPLOYMENT_POINTS_FRACTION_OF_BATTLE_SIZE_BONUS_MOD).unmodifyFlat(buffID);
        stats.getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MIN_FRACTION_OF_BATTLE_SIZE_BONUS_MOD).unmodifyFlat(buffID);
    }
}
