package data.scripts.ungprules.impl.character;

import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import data.scripts.ungprules.UNGP_BaseRuleEffect;

public class UNGP_TechLock extends UNGP_BaseRuleEffect {
    private float reduction;

    @Override
    public void refreshDifficultyCache(int difficulty) {
        reduction = getValueByDifficulty(0, difficulty);
    }

    @Override
    public float getValueByDifficulty(int index, int difficulty) {
        if (index == 0) return 0.04f + 0.01f * (float) Math.pow(difficulty, 0.598);
        return 0;
    }

    @Override
    public void applyPlayerCharacterStats(MutableCharacterStatsAPI stats) {
        stats.getShipOrdnancePointBonus().modifyPercent(rule.getBuffID(), -reduction * 100f);
    }

    @Override
    public void unapplyPlayerCharacterStats(MutableCharacterStatsAPI stats) {
        stats.getShipOrdnancePointBonus().unmodify(rule.getBuffID());

    }

    @Override
    public String getDescriptionParams(int index) {
        if (index == 0) return getPercentString(reduction * 100f);
        return null;
    }

    @Override
    public String getDescriptionParams(int index, int difficulty) {
        if (index == 0) return getPercentString(getValueByDifficulty(index, difficulty) * 100f);
        return null;
    }
}
