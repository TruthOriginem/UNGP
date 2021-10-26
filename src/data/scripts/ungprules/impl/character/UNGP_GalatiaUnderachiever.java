package data.scripts.ungprules.impl.character;

import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import data.scripts.ungprules.tags.UNGP_CharacterTag;

public class UNGP_GalatiaUnderachiever extends UNGP_BaseRuleEffect implements UNGP_CharacterTag {
    private int commandPointsReduction;
    private float commandRateReduction;

    @Override
    public void updateDifficultyCache(int difficulty) {
        commandPointsReduction = (int) getValueByDifficulty(0, difficulty);
        commandRateReduction = getValueByDifficulty(1, difficulty);
    }

    @Override
    public float getValueByDifficulty(int index, int difficulty) {
        if (index == 0) return getLinearValue(1, 2, difficulty);
        if (index == 1) return getLinearValue(0.25f, 0.5f, difficulty);
        return 0;
    }

    @Override
    public String getDescriptionParams(int index, int difficulty) {
        if (index == 0) return (int) getValueByDifficulty(index, difficulty) + "";
        if (index == 1) return getPercentString(getValueByDifficulty(index, difficulty) * 100);
        return super.getDescriptionParams(index, difficulty);
    }

    @Override
    public void applyPlayerCharacterStats(MutableCharacterStatsAPI stats) {
        stats.getCommandPoints().modifyFlat(rule.getBuffID(), -commandPointsReduction);
        stats.getDynamic().getStat(Stats.COMMAND_POINT_RATE_COMMANDER).modifyFlat(rule.getBuffID(), commandRateReduction);
    }

    @Override
    public void unapplyPlayerCharacterStats(MutableCharacterStatsAPI stats) {
        stats.getCommandPoints().unmodify(rule.getBuffID());
        stats.getDynamic().getStat(Stats.COMMAND_POINT_RATE_COMMANDER).unmodify(rule.getBuffID());
    }
}
