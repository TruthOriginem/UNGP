package data.scripts.ungprules.impl.economy;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import data.scripts.ungprules.tags.UNGP_EconomyTag;

public class UNGP_Mercantilism extends UNGP_BaseRuleEffect implements UNGP_EconomyTag {
    private static final float ACCESS_THRESHOLD = 1.5f;
    private float bonus;

    @Override
    public void updateDifficultyCache(int difficulty) {
        bonus = getValueByDifficulty(0, difficulty);
    }

    //20~30%
    @Override
    public float getValueByDifficulty(int index, int difficulty) {
        if (index == 0) return getLinearValue(0.2f, 0.3f, difficulty);
        return 0;
    }


    @Override
    public void applyPlayerMarket(MarketAPI market) {
        float couldBonus = bonus;
        if (market.getAccessibilityMod().computeEffective(0f) > ACCESS_THRESHOLD) {
            couldBonus *= 0.5f;
        }
        market.getAccessibilityMod().modifyFlat(rule.getBuffID(), couldBonus, rule.getName());
    }

    @Override
    public void unapplyPlayerMarket(MarketAPI market) {
        market.getAccessibilityMod().unmodify(rule.getBuffID());
    }

    @Override
    public String getDescriptionParams(int index) {
        if (index == 0) return getPercentString(bonus * 100f);
        if (index == 1) return getPercentString(ACCESS_THRESHOLD * 100f);
        if (index == 2) return i18n.get("halved");
        return null;
    }

    @Override
    public String getDescriptionParams(int index, int difficulty) {
        if (index == 0) return getPercentString(getValueByDifficulty(index, difficulty) * 100f);
        return getDescriptionParams(index);
    }
}
