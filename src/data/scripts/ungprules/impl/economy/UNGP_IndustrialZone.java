package data.scripts.ungprules.impl.economy;

import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MutableCommodityQuantity;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.util.Misc;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import data.scripts.ungprules.tags.UNGP_EconomyTag;

public class UNGP_IndustrialZone extends UNGP_BaseRuleEffect implements UNGP_EconomyTag {
    private static final int SIZE_THRESHOLD = 5;

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
        market.getStats().getDynamic().getMod(Stats.MAX_INDUSTRIES).unmodify(rule.getBuffID());
        int curIndustries = Misc.getNumIndustries(market);
        int maxIndustries = Misc.getMaxIndustries(market);
        int size = market.getSize();
        if (size <= 5 && curIndustries > maxIndustries) {
            for (Industry industry : market.getIndustries()) {
                if (industry.isIndustry()) {
                    for (MutableCommodityQuantity commodityQuantity : industry.getAllSupply()) {
                        commodityQuantity.getQuantity().modifyFlat(rule.getBuffID(), -1, rule.getName());
                    }
                }
            }
        } else {
            unapplyPlayerMarket(market);
        }
        market.getStats().getDynamic().getMod(Stats.MAX_INDUSTRIES).modifyFlat(rule.getBuffID(), 1);
    }

    @Override
    public void unapplyPlayerMarket(MarketAPI market) {
        market.getStats().getDynamic().getMod(Stats.MAX_INDUSTRIES).unmodify(rule.getBuffID());
        for (Industry industry : market.getIndustries()) {
            if (industry.isIndustry()) {
                for (MutableCommodityQuantity commodityQuantity : industry.getAllSupply()) {
                    commodityQuantity.getQuantity().unmodify(rule.getBuffID());
                }
            }
        }
    }

    @Override
    public void applyAllMarket(MarketAPI market) {

    }

    @Override
    public void unapplyAllMarket(MarketAPI market) {

    }

    @Override
    public String getDescriptionParams(int index) {
        if (index == 0) return getFactorString(1);
        if (index == 1) return getFactorString(SIZE_THRESHOLD);
        if (index == 2) return getFactorString(1);
        return null;
    }

    @Override
    public String getDescriptionParams(int index, int difficulty) {
        return getDescriptionParams(index);
    }
}
