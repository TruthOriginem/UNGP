package data.scripts.ungprules.impl.other;

import com.fs.starfarer.api.impl.campaign.intel.MessageIntel;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.campaign.UNGP_InGameData;
import data.scripts.campaign.everyframe.UNGP_CampaignPlugin.TempCampaignParams;
import data.scripts.campaign.specialist.UNGP_SpecialistSettings;
import data.scripts.campaign.specialist.rules.UNGP_RulesManager;
import data.scripts.campaign.specialist.rules.UNGP_RulesManager.URule;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import data.scripts.ungprules.tags.UNGP_CampaignTag;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class UNGP_EntropiusMind extends UNGP_BaseRuleEffect implements UNGP_CampaignTag {

    @Override
    public void updateDifficultyCache(UNGP_SpecialistSettings.Difficulty difficulty) {
        Integer daysPassed = getDataInCampaign(0);
        if (daysPassed == null) {
            saveDataInCampaign(0, 0);
        }
    }

    @Override
    public float getValueByDifficulty(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        return 0f;
    }

    @Override
    public void advanceInCampaign(float amount, TempCampaignParams params) {
        if (params.isOneDayPassed()) {
            Integer daysPassed = getDataInCampaign(0);
            if (daysPassed == null) {
                daysPassed = 0;
            }
            daysPassed++;

            if (daysPassed == 7) {

                UNGP_InGameData inGameData = UNGP_InGameData.getDataInSave();
                List<URule> allRules = UNGP_RulesManager.getAllRulesCopy();
                List<URule> activatedRules = new ArrayList<>();
                activatedRules.add(rule);

                Random random = getRandomByDay();
                WeightedRandomPicker<URule> picker = new WeightedRandomPicker<>(random);
                for (URule otherRule : allRules) {
                    if (otherRule != rule && otherRule.isRollable()) {
                        picker.add(otherRule);
                    }
                }

                int rulesToAdd = random.nextInt((int) picker.getTotal());
                for (int i = 0; i < rulesToAdd; i++) {
                    URule toAdd = picker.pickAndRemove();
                    activatedRules.add(toAdd);
                }

                MessageIntel message = createMessage();
                message.addLine(rule.getExtra1(), Misc.getHighlightColor());
                showMessage(message);

                inGameData.saveActivatedRules(activatedRules);
                UNGP_RulesManager.updateCacheNextFrame();

                daysPassed = 0;
            }

            saveDataInCampaign(0, daysPassed);
        }
    }

    @Override
    public String getDescriptionParams(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        return null;
    }

    @Override
    public boolean addIntelTips(TooltipMakerAPI imageTooltip) {
        Integer daysPassed = getDataInCampaign(0);
        if (daysPassed != null) {
            imageTooltip.addPara(rule.getExtra2(), 0f, Misc.getHighlightColor(), 7 - daysPassed + "");
            return true;
        }
        return false;
    }
}
