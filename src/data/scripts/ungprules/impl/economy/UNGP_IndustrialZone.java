package data.scripts.ungprules.impl.economy;

import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MutableCommodityQuantity;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.specialist.UNGP_SpecialistSettings;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import data.scripts.ungprules.tags.UNGP_EconomyTag;

public class UNGP_IndustrialZone extends UNGP_BaseRuleEffect implements UNGP_EconomyTag {
    private static final int SIZE_THRESHOLD = 5;

    @Override
    public void updateDifficultyCache(UNGP_SpecialistSettings.Difficulty difficulty) {

    }

    //1
    @Override
    public float getValueByDifficulty(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return 1;
        return 0;
    }


    @Override
    public void applyPlayerMarket(MarketAPI market) {
        market.getStats().getDynamic().getMod(Stats.MAX_INDUSTRIES).unmodify(buffID);
        int curIndustries = Misc.getNumIndustries(market);
        int maxIndustries = Misc.getMaxIndustries(market);
        int size = market.getSize();
        if (size <= 5 && curIndustries > maxIndustries) {
            for (Industry industry : market.getIndustries()) {
                if (industry.isIndustry()) {
                    for (MutableCommodityQuantity commodityQuantity : industry.getAllSupply()) {
                        commodityQuantity.getQuantity().modifyFlat(buffID, -1, rule.getName());
                    }
                }
            }
        } else {
            unapplyPlayerMarket(market);
        }
        market.getStats().getDynamic().getMod(Stats.MAX_INDUSTRIES).modifyFlat(buffID, 1);
    }

    @Override
    public void unapplyPlayerMarket(MarketAPI market) {
        market.getStats().getDynamic().getMod(Stats.MAX_INDUSTRIES).unmodify(buffID);
        for (Industry industry : market.getIndustries()) {
            if (industry.isIndustry()) {
                for (MutableCommodityQuantity commodityQuantity : industry.getAllSupply()) {
                    commodityQuantity.getQuantity().unmodify(buffID);
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
    public String getDescriptionParams(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return getFactorString(1);
        if (index == 1) return getFactorString(SIZE_THRESHOLD);
        if (index == 2) return getFactorString(1);
        return null;
    }
}
