package ungp.api.rules.tags;

import com.fs.starfarer.api.campaign.econ.MarketAPI;

public interface UNGP_EconomyTag {

    /**
     * Would be called after applyAllMarket.
     *
     * @param market
     */
    void applyPlayerMarket(MarketAPI market);

    /**
     * Would be called for all markets too.
     *
     * @param market
     */
    void unapplyPlayerMarket(MarketAPI market);

    void applyAllMarket(MarketAPI market);

    void unapplyAllMarket(MarketAPI market);
}
