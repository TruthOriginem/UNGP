package data.scripts.ungprules.impl.character;

import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import data.scripts.ungprules.tags.UNGP_CharacterTag;

public class UNGP_TechLock extends UNGP_BaseRuleEffect implements UNGP_CharacterTag {
    private float reduction;

    @Override
    public void updateDifficultyCache(int difficulty) {
        reduction = getValueByDifficulty(0, difficulty);
    }

    @Override
    public float getValueByDifficulty(int index, int difficulty) {
        if (index == 0) return getLinearValue(0.05f, 0.1f, difficulty);
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
    public String getDescriptionParams(int index, int difficulty) {
        if (index == 0) return getPercentString(getValueByDifficulty(index, difficulty) * 100f);
        return null;
    }
}
