package data.scripts.ungprules.impl.economy;

import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MutableCommodityQuantity;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import data.scripts.ungprules.tags.UNGP_EconomyTag;

public class UNGP_GLaDos extends UNGP_BaseRuleEffect implements UNGP_EconomyTag {

    @Override
    public void updateDifficultyCache(int difficulty) {

    }

    //1
    @Override
    public float getValueByDifficulty(int index, int difficulty) {
        if (index == 0) return 1;
        return 0;
    }


    @Override
    public void applyPlayerMarket(MarketAPI market) {
        for (Industry industry : market.getIndustries()) {
            if (industry.getAICoreId() != null && industry.getAICoreId().equals(Commodities.ALPHA_CORE)) {
                for (MutableCommodityQuantity mutableCommodityQuantity : industry.getAllSupply()) {
                    mutableCommodityQuantity.getQuantity().modifyFlat(rule.getBuffID(), 1, rule.getName());
                }
            } else {
                for (MutableCommodityQuantity mutableCommodityQuantity : industry.getAllSupply()) {
                    mutableCommodityQuantity.getQuantity().unmodify(rule.getBuffID());
                }
            }
        }
    }

    @Override
    public void unapplyPlayerMarket(MarketAPI market) {
        for (Industry industry : market.getIndustries()) {
            for (MutableCommodityQuantity mutableCommodityQuantity : industry.getAllSupply())
                mutableCommodityQuantity.getQuantity().unmodify(rule.getBuffID());
        }
    }

    @Override
    public String getDescriptionParams(int index) {
        if (index == 0) return getFactorString(1);
        return null;
    }

    @Override
    public String getDescriptionParams(int index, int difficulty) {
        return getDescriptionParams(index);
    }
}
