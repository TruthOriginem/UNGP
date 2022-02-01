package data.scripts.ungprules.impl.economy;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.specialist.UNGP_SpecialistSettings;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import data.scripts.ungprules.tags.UNGP_EconomyTag;

import java.util.List;

public class UNGP_Spy extends UNGP_BaseRuleEffect implements UNGP_EconomyTag {
    @Override
    public float getValueByDifficulty(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        return 0;
    }

    @Override
    public String getDescriptionParams(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return "15";
        if (index == 1) return "1";
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
        market.getStability().modifyFlat(buffID, -hostileCount, rule.getName());
    }

    @Override
    public void unapplyPlayerMarket(MarketAPI market) {
        market.getStability().unmodify(buffID);
    }

    @Override
    public void applyAllMarket(MarketAPI market) {

    }

    @Override
    public void unapplyAllMarket(MarketAPI market) {

    }
}
