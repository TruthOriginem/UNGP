package data.scripts.ungprules.impl.fleet;

import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.BuffManagerAPI.Buff;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.intel.MessageIntel;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.everyframe.UNGP_CampaignPlugin.TempCampaignParams;
import data.scripts.campaign.specialist.UNGP_SpecialistSettings;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import data.scripts.ungprules.impl.fleet.UNGP_BloodLust.FleetBattleListener;
import data.scripts.ungprules.tags.UNGP_CampaignListenerTag;
import data.scripts.ungprules.tags.UNGP_CampaignTag;
import data.scripts.ungprules.tags.UNGP_PlayerFleetTag;
import data.scripts.utils.UNGP_BaseBuff;

import java.awt.*;

import static com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason;

public class UNGP_BloodLust extends UNGP_BaseRuleEffect implements UNGP_CampaignTag, UNGP_PlayerFleetTag, UNGP_CampaignListenerTag<FleetBattleListener> {
    private static final float MAX_CR_REDUCTION = 0.15f;
    private int lustThresholdDay = 15;
    private boolean shouldApply = false;

    @Override
    public FleetBattleListener getListener() {
        return new FleetBattleListener();
    }

    @Override
    public Class<FleetBattleListener> getClassOfListener() {
        return FleetBattleListener.class;
    }

    private class BloodLustBuff extends UNGP_BaseBuff {
        public BloodLustBuff(String id) {
            super(id);
        }

        @Override
        public void apply(FleetMemberAPI member) {
            decreaseMaxCR(member.getStats(), buffID, MAX_CR_REDUCTION, rule.getName());
        }
    }

    @Override
    public void updateDifficultyCache(UNGP_SpecialistSettings.Difficulty difficulty) {
        lustThresholdDay = (int) getValueByDifficulty(0, difficulty);
    }

    @Override
    public float getValueByDifficulty(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return Math.round(difficulty.getLinearValue(36, -9));
        return 0;
    }

    @Override
    public String getDescriptionParams(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return getFactorString(getValueByDifficulty(index, difficulty));
        if (index == 1) return getPercentString(MAX_CR_REDUCTION * 100f);
        return super.getDescriptionParams(index, difficulty);
    }

    @Override
    public void advanceInCampaign(float amount, TempCampaignParams params) {
        Integer dayPassed = getDataInCampaign(0);
        if (params.isOneDayPassed()) {
            if (dayPassed == null) {
                dayPassed = 1;
            } else {
                dayPassed++;
            }
            saveDataInCampaign(0, dayPassed);
        }
        if (dayPassed != null) {
            if (dayPassed >= lustThresholdDay) {
                if (!shouldApply) {
                    shouldApply = true;
                    MessageIntel message = createMessage();
                    message.addLine(rule.getExtra2(), Color.red);
                    showMessage(message);
                }
            } else {
                shouldApply = false;
            }
        }
    }

    @Override
    public void applyPlayerFleetStats(CampaignFleetAPI fleet) {
        if (shouldApply) {
            String buffId = buffID;
            boolean needsSync = false;
            for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
                Buff test = member.getBuffManager().getBuff(buffId);
                if (test instanceof BloodLustBuff) {
                    BloodLustBuff buff = (BloodLustBuff) test;
                    buff.refresh();
                } else {
                    member.getBuffManager().addBuff(new BloodLustBuff(buffId));
                    needsSync = true;
                }
            }
            if (needsSync) {
                fleet.forceSync();
            }
        }
    }

    @Override
    public void unapplyPlayerFleetStats(CampaignFleetAPI fleet) {

    }

    protected class FleetBattleListener implements FleetEventListener {

        @Override
        public void reportFleetDespawnedToListener(CampaignFleetAPI fleet, FleetDespawnReason reason, Object param) {
        }

        @Override
        public void reportBattleOccurred(CampaignFleetAPI fleet, CampaignFleetAPI primaryWinner, BattleAPI battle) {
            if (primaryWinner.isPlayerFleet()) {
                saveDataInCampaign(0, 0);
            }
        }
    }

//    @Override
//    public void applyGlobalStats() {
//        Global.getSector().getListenerManager().addListener(new FleetBattleListener(), true);
//    }
//
//    @Override
//    public void unapplyGlobalStats() {
//        Global.getSector().getListenerManager().removeListenerOfClass(FleetBattleListener.class);
//    }

    @Override
    public boolean addIntelTips(TooltipMakerAPI imageTooltip) {
        Integer dayPassed = getDataInCampaign(0);
        if (dayPassed == null) {
            dayPassed = 0;
        }
        if (shouldApply) {
            imageTooltip.addPara(rule.getExtra2(), Color.red, 0f);
        } else {
            imageTooltip.addPara(rule.getExtra1(), 0f, Misc.getHighlightColor(), "" + (lustThresholdDay - dayPassed));
        }
        return true;
    }

    @Override
    public void cleanUp() {
        clearDataInCampaign(0);
    }
}
