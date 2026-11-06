package ungp.impl.rules.fleet;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignClockAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.intel.MessageIntel;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import ungp.api.rules.UNGP_BaseRuleEffect;
import ungp.api.rules.tags.UNGP_CampaignTag;
import ungp.scripts.campaign.everyframe.UNGP_CampaignPlugin.TempCampaignParams;
import ungp.scripts.campaign.specialist.UNGP_SpecialistSettings.Difficulty;

public class UNGP_HumanNatureTest extends UNGP_BaseRuleEffect implements UNGP_CampaignTag {
    private static final int CREW_LOSS_PERCENT = 1;
    private static final int DRUG_CONSUMPTION_PERCENT = 10;
    private static final int REMINDER_START_DAYS = 9;
    private static final int REMINDER_INTERVAL_DAYS = 3;

    private int gracePeriod = 60;

    @Override
    public void updateDifficultyCache(Difficulty difficulty) {
        gracePeriod = (int) getValueByDifficulty(0, difficulty);
    }

    @Override
    public float getValueByDifficulty(int index, Difficulty difficulty) {
        if (index == 0) {
            return switch (difficulty) {
                case GAMMA -> 60;
                case BETA -> 40;
                case ALPHA -> 30;
                case OMEGA -> 20;
            };
        }
        if (index == 1) return CREW_LOSS_PERCENT;
        if (index == 2) return DRUG_CONSUMPTION_PERCENT;
        return 0;
    }

    @Override
    public String getDescriptionParams(int index, Difficulty difficulty) {
        if (index == 0) return getFactorString(getValueByDifficulty(index, difficulty));
        if (index == 1 || index == 2) return getPercentString(getValueByDifficulty(index, difficulty));
        return super.getDescriptionParams(index, difficulty);
    }

    @Override
    public void applyGlobalStats() {
        if (getDataInCampaign(0) == null) {
            saveDataInCampaign(0, 0);
        }
        if (getDataInCampaign(1) == null) {
            saveDataInCampaign(1, getCurrentDay());
        }
    }

    @Override
    public void advanceInCampaign(float amount, TempCampaignParams params) {
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        if (playerFleet == null) return;

        int currentDay = getCurrentDay();
        if (isAtPort()) {
            saveDataInCampaign(0, 0);
            saveDataInCampaign(1, currentDay);
            return;
        }
        if (Global.getSector().isPaused()) return;

        Integer lastProcessedDay = getDataInCampaign(1);
        if (lastProcessedDay == null || lastProcessedDay == currentDay) {
            if (lastProcessedDay == null) saveDataInCampaign(1, currentDay);
            return;
        }
        saveDataInCampaign(1, currentDay);
        Integer daysPassed = getDataInCampaign(0);
        daysPassed = daysPassed == null ? 1 : daysPassed + 1;
        saveDataInCampaign(0, daysPassed);
        int remaining = gracePeriod - daysPassed;
        if (remaining > 0) {
            if (remaining <= REMINDER_START_DAYS && remaining % REMINDER_INTERVAL_DAYS == 0) {
                MessageIntel message = createMessage();
                message.addLine(rule.getExtra1(), Misc.getTextColor(), new String[]{String.valueOf(remaining)}, Misc.getHighlightColor());
                showMessage(message);
            }
            return;
        }

        CargoAPI cargo = playerFleet.getCargo();
        float drugs = cargo.getCommodityQuantity(Commodities.DRUGS);
        if (drugs > 0f) {
            float consumed = Math.min(drugs, (float) Math.ceil((double) drugs * DRUG_CONSUMPTION_PERCENT / 100d));
            cargo.removeCommodity(Commodities.DRUGS, consumed);
        } else {
            int crew = cargo.getCrew();
            int lost = Math.min(crew, (int) Math.ceil(crew * CREW_LOSS_PERCENT / 100d));
            if (lost > 0) cargo.removeCrew(lost);
        }
    }

    private boolean isAtPort() {
        CampaignUIAPI campaignUI = Global.getSector().getCampaignUI();
        InteractionDialogAPI dialog = campaignUI == null ? null : campaignUI.getCurrentInteractionDialog();
        if (dialog == null) return false;
        SectorEntityToken target = dialog.getInteractionTarget();
        if (target == null || target instanceof CampaignFleetAPI) return false;
        MarketAPI market = target.getMarket();
        return market != null && !market.isPlanetConditionMarketOnly() && market.getSize() > 0;
    }

    private int getCurrentDay() {
        CampaignClockAPI clock = Global.getSector().getClock();
        return clock.getCycle() * 10000 + clock.getMonth() * 100 + clock.getDay();
    }

    @Override
    public boolean addIntelTips(TooltipMakerAPI imageTooltip) {
        Integer daysPassed = getDataInCampaign(0);
        int remaining = gracePeriod - (daysPassed == null ? 0 : daysPassed);
        if (remaining > 0) {
            imageTooltip.addPara(rule.getExtra1(), 0f, Misc.getHighlightColor(), String.valueOf(remaining));
        } else {
            imageTooltip.addPara(rule.getExtra2(), Misc.getNegativeHighlightColor(), 0f);
        }
        return true;
    }

    @Override
    public void cleanUp() {
        clearDataInCampaign(0);
        clearDataInCampaign(1);
    }
}
