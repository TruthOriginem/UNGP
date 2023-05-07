package ungp.impl.rules.economy;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import ungp.scripts.campaign.specialist.UNGP_SpecialistSettings;
import ungp.api.rules.UNGP_BaseRuleEffect;
import ungp.api.rules.tags.UNGP_EconomyTag;

public class UNGP_Mercantilism extends UNGP_BaseRuleEffect implements UNGP_EconomyTag {
    private static final float ACCESS_THRESHOLD = 1.5f;
    private float bonus;

    @Override
    public void updateDifficultyCache(UNGP_SpecialistSettings.Difficulty difficulty) {
        bonus = getValueByDifficulty(0, difficulty);
    }

    //20~30%
    @Override
    public float getValueByDifficulty(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return difficulty.getLinearValue(0.2f, 0.1f);
        return 0;
    }


    @Override
    public void applyPlayerMarket(MarketAPI market) {
        float couldBonus = bonus;
        if (market.getAccessibilityMod().computeEffective(0f) > ACCESS_THRESHOLD) {
            couldBonus *= 0.5f;
        }
        market.getAccessibilityMod().modifyFlat(buffID, couldBonus, rule.getName());
    }

    @Override
    public void unapplyPlayerMarket(MarketAPI market) {
        market.getAccessibilityMod().unmodify(buffID);
    }

    @Override
    public void applyAllMarket(MarketAPI market) {

    }

    @Override
    public void unapplyAllMarket(MarketAPI market) {

    }

    @Override
    public String getDescriptionParams(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return getPercentString(getValueByDifficulty(index, difficulty) * 100f);
        if (index == 1) return getPercentString(ACCESS_THRESHOLD * 100f);
        if (index == 2) return i18n.get("halved");
        return null;
    }
}
