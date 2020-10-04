package data.scripts.ungprules.tags;

import com.fs.starfarer.api.campaign.econ.MarketAPI;

public interface UNGP_EconomyTag {
    void applyPlayerMarket(MarketAPI market);

    void unapplyPlayerMarket(MarketAPI market);
}
