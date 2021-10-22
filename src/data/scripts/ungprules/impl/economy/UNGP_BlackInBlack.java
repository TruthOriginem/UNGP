package data.scripts.ungprules.impl.economy;

import com.fs.starfarer.api.campaign.SubmarketPlugin;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.shared.PlayerTradeDataForSubmarket;
import com.fs.starfarer.api.impl.campaign.shared.SharedData;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import data.scripts.ungprules.tags.UNGP_EconomyTag;

public class UNGP_BlackInBlack extends UNGP_BaseRuleEffect implements UNGP_EconomyTag {

    @Override
    public void updateDifficultyCache(int difficulty) {
    }

    @Override
    public float getValueByDifficulty(int index, int difficulty) {
        return 0f;
    }


    @Override
    public String getDescriptionParams(int index, int difficulty) {
        return null;
    }

    @Override
    public void applyPlayerMarket(MarketAPI market) {

    }

    @Override
    public void unapplyPlayerMarket(MarketAPI market) {

    }

    @Override
    public void applyAllMarket(MarketAPI market) {
        for (SubmarketAPI subMarket : market.getSubmarketsCopy()) {
            SubmarketPlugin plugin = subMarket.getPlugin();
            if (!plugin.isParticipatesInEconomy()) continue;

            PlayerTradeDataForSubmarket tradeData = SharedData.getData().getPlayerActivityTracker().getPlayerTradeData(subMarket);
            if (subMarket.getFaction().isHostileTo(market.getFaction()) || subMarket.getPlugin().isBlackMarket()) {
                tradeData.setTotalPlayerTradeValue(0f);
            }
        }

        market.getMemoryWithoutUpdate().set(MemFlags.MARKET_EXTRA_SUSPICION, 0f);
    }

    @Override
    public void unapplyAllMarket(MarketAPI market) {

    }
}
