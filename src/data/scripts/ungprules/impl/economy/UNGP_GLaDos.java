package data.scripts.ungprules.impl.economy;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MutableCommodityQuantity;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.intel.MessageIntel;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.specialist.UNGP_SpecialistSettings;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import data.scripts.ungprules.tags.UNGP_EconomyTag;

public class UNGP_GLaDos extends UNGP_BaseRuleEffect implements UNGP_EconomyTag {

    @Override
    public void updateDifficultyCache(UNGP_SpecialistSettings.Difficulty difficulty) {
        Boolean sent = getDataInCampaign(0);
        if (sent == null) {
            Global.getSector().addTransientScript(new EveryFrameScript() {
                float duration = 2f + RANDOM.nextFloat();
                boolean done = false;

                @Override
                public boolean isDone() {
                    return done;
                }

                @Override
                public boolean runWhilePaused() {
                    return false;
                }

                @Override
                public void advance(float amount) {
                    duration -= amount;
                    if (duration <= 0f) {
                        MessageIntel message = createMessage();
                        message.addLine(rule.getExtra1(), Misc.getBasePlayerColor());
                        message.setSound("UNGP_glados_chosen");
                        showMessage(message);
                        done = true;
                        Global.getSector().removeTransientScript(this);
                    }
                }
            });
            saveDataInCampaign(0, true);
        }
    }

    @Override
    public float getValueByDifficulty(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return 1;
        return super.getValueByDifficulty(index, difficulty);
    }

    @Override
    public void applyPlayerMarket(MarketAPI market) {
        for (Industry industry : market.getIndustries()) {
            if (industry.getAICoreId() != null && industry.getAICoreId().contentEquals(Commodities.ALPHA_CORE)) {
                for (MutableCommodityQuantity mutableCommodityQuantity : industry.getAllSupply()) {
                    mutableCommodityQuantity.getQuantity().modifyFlat(buffID, 1, rule.getName());
                }
            } else {
                for (MutableCommodityQuantity mutableCommodityQuantity : industry.getAllSupply()) {
                    mutableCommodityQuantity.getQuantity().unmodify(buffID);
                }
            }
        }
    }

    @Override
    public void unapplyPlayerMarket(MarketAPI market) {
        for (Industry industry : market.getIndustries()) {
            for (MutableCommodityQuantity mutableCommodityQuantity : industry.getAllSupply())
                mutableCommodityQuantity.getQuantity().unmodify(buffID);
        }
    }

    @Override
    public void applyAllMarket(MarketAPI market) {

    }

    @Override
    public void unapplyAllMarket(MarketAPI market) {

    }


    @Override
    public String getDescriptionParams(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return getFactorString(1);
        return null;
    }
}
