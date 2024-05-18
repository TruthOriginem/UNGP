package ungp.impl.rules.economy;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PlayerMarketTransaction;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.ColonyInteractionListener;
import com.fs.starfarer.api.impl.campaign.intel.MessageIntel;
import com.fs.starfarer.api.impl.campaign.submarkets.BlackMarketPlugin;
import com.fs.starfarer.api.util.Misc;
import ungp.api.rules.UNGP_BaseRuleEffect;
import ungp.api.rules.tags.UNGP_CampaignListenerTag;
import ungp.impl.rules.economy.UNGP_Fraudulent.MarketTransactionListener;
import ungp.scripts.campaign.specialist.UNGP_SpecialistSettings;

public class UNGP_Fraudulent extends UNGP_BaseRuleEffect implements UNGP_CampaignListenerTag<MarketTransactionListener> {
    private static final float DROP_FACTOR = 0.2f;

    public class MarketTransactionListener implements ColonyInteractionListener {

        @Override
        public void reportPlayerOpenedMarket(MarketAPI market) {

        }

        @Override
        public void reportPlayerClosedMarket(MarketAPI market) {

        }

        @Override
        public void reportPlayerOpenedMarketAndCargoUpdated(MarketAPI market) {

        }

        @Override
        public void reportPlayerMarketTransaction(PlayerMarketTransaction transaction) {
            float rollChance = chance;
            if (transaction.getSubmarket() != null) {
                if (transaction.getSubmarket().getPlugin() instanceof BlackMarketPlugin) {
                    rollChance *= 2f;
                }
            }
            if (roll(rollChance)) {
                float creditValue = transaction.getCreditValue();
                if (creditValue > 0) {
                    int dropCredit = (int) (creditValue * DROP_FACTOR);
                    MessageIntel message = createMessage();
                    message.addLine(rule.getExtra1(), Misc.getTextColor(), new String[]{
                            Misc.getDGSCredits(dropCredit)
                    }, Misc.getHighlightColor());
                    message.setSound("UNGP_fraudulent_activate");
                    showMessage(message);
                    Global.getSector().getPlayerFleet().getCargo().getCredits().subtract(dropCredit);
                }
            }
        }
    }

    private float chance;

    @Override
    public void updateDifficultyCache(UNGP_SpecialistSettings.Difficulty difficulty) {
        chance = getValueByDifficulty(0, difficulty);
    }

    @Override
    public float getValueByDifficulty(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return difficulty.getLinearValue(0.15f, 0.15f);
        return 0;
    }

    @Override
    public String getDescriptionParams(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return getPercentString(getValueByDifficulty(index, difficulty) * 100f);
        if (index == 1) return getPercentString(DROP_FACTOR * 100f);
        return super.getDescriptionParams(index, difficulty);
    }

    @Override
    public MarketTransactionListener getListener() {
        return new MarketTransactionListener();
    }

    @Override
    public Class<MarketTransactionListener> getClassOfListener() {
        return MarketTransactionListener.class;
    }
}
