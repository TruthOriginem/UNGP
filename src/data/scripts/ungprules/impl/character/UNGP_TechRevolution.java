package data.scripts.ungprules.impl.character;

import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import data.scripts.ungprules.tags.UNGP_CharacterTag;

public class UNGP_TechRevolution extends UNGP_BaseRuleEffect implements UNGP_CharacterTag {
    private float bonus;

    @Override
    public void updateDifficultyCache(int difficulty) {
        bonus = getValueByDifficulty(0, difficulty);
    }

    @Override
    public float getValueByDifficulty(int index, int difficulty) {
        if (index == 0) return 0.05f;
        return 0;
    }

    @Override
    public void applyPlayerCharacterStats(MutableCharacterStatsAPI stats) {
        stats.getShipOrdnancePointBonus().modifyPercent(rule.getBuffID(), bonus * 100f);
    }

    @Override
    public void unapplyPlayerCharacterStats(MutableCharacterStatsAPI stats) {
        stats.getShipOrdnancePointBonus().unmodify(rule.getBuffID());
    }

    @Override
    public String getDescriptionParams(int index) {
        if (index == 0) return getPercentString(bonus * 100f);
        return null;
    }

    @Override
    public String getDescriptionParams(int index, int difficulty) {
        if (index == 0) return getPercentString(getValueByDifficulty(index, difficulty) * 100f);
        return null;
    }
}
