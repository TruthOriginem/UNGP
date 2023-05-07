package ungp.impl.rules.character;

import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import ungp.scripts.campaign.specialist.UNGP_SpecialistSettings;
import ungp.api.rules.UNGP_BaseRuleEffect;
import ungp.api.rules.tags.UNGP_PlayerCharacterStatsSkillTag;

public class UNGP_GalatiaUnderachiever extends UNGP_BaseRuleEffect implements UNGP_PlayerCharacterStatsSkillTag {
    private int commandPointsReduction;
    private float commandRateReduction;


    @Override
    public void updateDifficultyCache(UNGP_SpecialistSettings.Difficulty difficulty) {
        commandPointsReduction = (int) getValueByDifficulty(0, difficulty);
        commandRateReduction = getValueByDifficulty(1, difficulty);
    }

    @Override
    public float getValueByDifficulty(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return difficulty.getLinearValue(1f, 1f);
        if (index == 1) return difficulty.getLinearValue(0.25f, 0.25f);
        return super.getValueByDifficulty(index, difficulty);
    }

    @Override
    public String getDescriptionParams(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return (int) getValueByDifficulty(index, difficulty) + "";
        if (index == 1) return getPercentString(getValueByDifficulty(index, difficulty) * 100);
        return super.getDescriptionParams(index, difficulty);
    }

    @Override
    public void apply(MutableCharacterStatsAPI stats) {
        stats.getCommandPoints().modifyFlat(buffID, -commandPointsReduction);
        stats.getDynamic().getStat(Stats.COMMAND_POINT_RATE_COMMANDER).modifyFlat(buffID, -commandRateReduction);
    }

    @Override
    public void unapply(MutableCharacterStatsAPI stats) {
        stats.getCommandPoints().unmodify(buffID);
        stats.getDynamic().getStat(Stats.COMMAND_POINT_RATE_COMMANDER).unmodify(buffID);
    }
}
