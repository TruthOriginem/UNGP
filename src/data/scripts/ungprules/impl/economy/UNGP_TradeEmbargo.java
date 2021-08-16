package data.scripts.ungprules.impl.economy;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import data.scripts.ungprules.tags.UNGP_EconomyTag;

import java.util.List;

public class UNGP_TradeEmbargo extends UNGP_BaseRuleEffect implements UNGP_EconomyTag {
    @Override
    public float getValueByDifficulty(int index, int difficulty) {
        return 0;
    }

    @Override
    public String getDescriptionParams(int index) {
        if (index == 0) return "15";
        if (index == 1) return "10%";
        return null;
    }

    @Override
    public void applyPlayerMarket(MarketAPI market) {
        List<MarketAPI> nearbyMarkets = Misc.getNearbyMarkets(market.getLocationInHyperspace(), 15);
        int hostileCount = 0;
        for (MarketAPI nearbyMarket : nearbyMarkets) {
            if (nearbyMarket.getFaction().isHostileTo(market.getFaction())) {
                hostileCount++;
            }
        }
        market.getAccessibilityMod().modifyFlat(rule.getBuffID(), -hostileCount * 0.1f, rule.getName());
    }

    @Override
    public void unapplyPlayerMarket(MarketAPI market) {
        market.getAccessibilityMod().unmodify(rule.getBuffID());
    }
}
