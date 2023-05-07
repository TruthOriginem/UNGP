package ungp.scripts.campaign.specialist.economy;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.EconomyAPI.EconomyUpdateListener;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import ungp.api.rules.tags.UNGP_EconomyTag;

import java.util.List;

import static ungp.scripts.campaign.specialist.rules.UNGP_RulesManager.ECONOMY_TAGS_ITG;

public class UNGP_EconomyListener implements EconomyUpdateListener {

    public static void addListener() {
        for (EconomyUpdateListener listener : Global.getSector().getEconomy().getUpdateListeners()) {
            if (listener instanceof UNGP_EconomyListener) {
                return;
            }
        }
        Global.getSector().getEconomy().addUpdateListener(new UNGP_EconomyListener());
    }


    public static void unapplyMarkets(UNGP_EconomyTag tag) {
        for (MarketAPI market : Global.getSector().getEconomy().getMarketsCopy()) {
            tag.unapplyAllMarket(market);
            tag.unapplyPlayerMarket(market);
        }
    }

    public static void applyMarkets() {
        List<MarketAPI> markets = Global.getSector().getEconomy().getMarketsCopy();
        for (MarketAPI market : markets) {
            for (UNGP_EconomyTag tag : ECONOMY_TAGS_ITG) {
                tag.unapplyAllMarket(market);
                tag.unapplyPlayerMarket(market);
                tag.applyAllMarket(market);
            }
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
