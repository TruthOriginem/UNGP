package ungp.impl.rules.economy;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.util.Misc;
import ungp.scripts.campaign.specialist.UNGP_SpecialistSettings;
import ungp.api.rules.UNGP_BaseRuleEffect;
import ungp.api.rules.tags.UNGP_EconomyTag;

import java.util.List;

public class UNGP_TradeEmbargo extends UNGP_BaseRuleEffect implements UNGP_EconomyTag {
    @Override
    public float getValueByDifficulty(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        return 0;
    }


    @Override
    public String getDescriptionParams(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return "15";
        if (index == 1) return "10%";
        return super.getDescriptionParams(index, difficulty);
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
        market.getAccessibilityMod().modifyFlat(buffID, -hostileCount * 0.1f, rule.getName());
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
}
