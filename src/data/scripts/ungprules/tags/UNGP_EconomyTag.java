package data.scripts.ungprules.tags;

import com.fs.starfarer.api.campaign.econ.MarketAPI;

public interface UNGP_EconomyTag {

    /**
     * Would be called after applyAllMarket.
     *
     * @param market
     */
    void applyPlayerMarket(MarketAPI market);

    void unapplyPlayerMarket(MarketAPI market);

    void applyAllMarket(MarketAPI market);

    void unapplyAllMarket(MarketAPI market);
}
