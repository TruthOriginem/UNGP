package data.scripts.campaign.hardmode.economy;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.EconomyAPI.EconomyUpdateListener;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import data.scripts.ungprules.tags.UNGP_EconomyTag;

import static data.scripts.campaign.hardmode.UNGP_RulesManager.ECONOMY_TAGS_ITG;

public class UNGP_EconomyListener implements EconomyUpdateListener {

    public static void addListener() {
        for (EconomyUpdateListener listener : Global.getSector().getEconomy().getUpdateListeners()) {
            if (listener instanceof UNGP_EconomyListener) {
                return;
            }
        }
        Global.getSector().getEconomy().addUpdateListener(new UNGP_EconomyListener());
    }


    public static void unapplyMarkets(UNGP_EconomyTag effect) {
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            if (!market.isPlayerOwned()) continue;
            effect.unapplyPlayerMarket(market);
        }
    }

    public static void applyMarkets() {
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            if (!market.isPlayerOwned()) continue;
            for (UNGP_EconomyTag tag : ECONOMY_TAGS_ITG) {
                tag.applyPlayerMarket(market);
            }
        }
    }

    @Override
    public void commodityUpdated(String commodityId) {

    }

    @Override
    public void economyUpdated() {
        applyMarkets();
    }

    @Override
    public boolean isEconomyListenerExpired() {
        return ECONOMY_TAGS_ITG.isEmpty();
    }
}
