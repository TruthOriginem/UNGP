package ungp.impl.rules.other;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignClockAPI;
import com.fs.starfarer.api.impl.campaign.intel.MessageIntel;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import ungp.scripts.campaign.UNGP_InGameData;
import ungp.scripts.campaign.everyframe.UNGP_CampaignPlugin;
import ungp.scripts.campaign.specialist.UNGP_SpecialistSettings;
import ungp.scripts.campaign.specialist.rules.UNGP_RulesManager;
import ungp.api.rules.UNGP_BaseRuleEffect;
import ungp.api.rules.tags.UNGP_CampaignTag;

import java.util.List;

import static ungp.scripts.campaign.specialist.rules.UNGP_RulesManager.URule;
import static ungp.scripts.campaign.specialist.rules.UNGP_RulesManager.getAllRulesCopy;

public class UNGP_BirthdayPresent extends UNGP_BaseRuleEffect implements UNGP_CampaignTag {

    @Override
    public void updateDifficultyCache(UNGP_SpecialistSettings.Difficulty difficulty) {
        int[] giftTime = getDataInCampaign(0);
        if (giftTime == null) {
            CampaignClockAPI clock = Global.getSector().getClock();
            giftTime = new int[]{clock.getCycle() + 1, clock.getMonth(), clock.getDay()};
            saveDataInCampaign(0, giftTime);
        }
    }

    @Override
    public float getValueByDifficulty(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        return 0;
    }

    @Override
    public String getDescriptionParams(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return "1";
        return super.getDescriptionParams(index, difficulty);
    }

    @Override
    public void advanceInCampaign(float amount, UNGP_CampaignPlugin.TempCampaignParams params) {
        if (params.isOneDayPassed()) {
            int[] giftTime = getDataInCampaign(0);
            CampaignClockAPI clock = Global.getSector().getClock();
            if (giftTime != null) {
                if (clock.getCycle() == giftTime[0] && clock.getMonth() == giftTime[1] && clock.getDay() == giftTime[2]) {
                    UNGP_InGameData inGameData = UNGP_InGameData.getDataInSave();
                    List<URule> activatedRules = inGameData.getActivatedRules();
                    List<URule> allRules = getAllRulesCopy();
                    WeightedRandomPicker<URule> picker = new WeightedRandomPicker<>(Misc.getRandom(clock.getCycle() + clock.getMonth() + clock.getDay(), inGameData.getCurCycle()));
                    for (URule uRule : allRules) {
                        if (!uRule.isBonus() || activatedRules.contains(uRule)) continue;
                        if (!uRule.isRollable()) continue;
                        picker.add(uRule, Math.abs(uRule.getCost()));
                    }
                    URule toAdd = picker.pick();
                    activatedRules.remove(rule);
                    if (toAdd != null) {
                        activatedRules.add(toAdd);
                        MessageIntel intel = createMessage();
                        intel.setIcon(toAdd.getSpritePath());
                        intel.addLine(rule.getExtra1(), Misc.getTextColor(), new String[]{toAdd.getName()}, toAdd.getCorrectColor());
                        showMessage(intel);
                        Global.getSoundPlayer().playUISound("UNGP_birthdaypresent_activate", 1f, 1f);
                    }
                    inGameData.saveActivatedRules(activatedRules);
                    UNGP_RulesManager.updateCacheNextFrame();
                }
            } else {
                giftTime = new int[]{clock.getCycle() + 1, clock.getMonth(), clock.getDay()};
                saveDataInCampaign(0, giftTime);
            }
        }
    }

    @Override
    public void cleanUp() {
        clearDataInCampaign(0);
    }

    @Override
    public boolean addIntelTips(TooltipMakerAPI imageTooltip) {
        int[] giftTime = getDataInCampaign(0);
        if (giftTime != null) {
            imageTooltip.addPara(rule.getExtra2(), 0f, Misc.getHighlightColor(), giftTime[0] + "", giftTime[1] + "", giftTime[2] + "");
            return true;
        }
        return false;
    }
}
