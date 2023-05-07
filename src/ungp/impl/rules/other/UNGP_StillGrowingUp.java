package ungp.impl.rules.other;

import com.fs.starfarer.api.impl.campaign.intel.MessageIntel;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import ungp.scripts.campaign.UNGP_InGameData;
import ungp.scripts.campaign.everyframe.UNGP_CampaignPlugin;
import ungp.scripts.campaign.specialist.UNGP_SpecialistSettings;
import ungp.scripts.campaign.specialist.rules.UNGP_RulesManager;
import ungp.scripts.campaign.specialist.rules.UNGP_RulesManager.URule;
import ungp.api.rules.UNGP_BaseRuleEffect;
import ungp.api.rules.tags.UNGP_CampaignTag;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static ungp.scripts.campaign.specialist.rules.UNGP_RulesManager.getAllRulesCopy;

public class UNGP_StillGrowingUp extends UNGP_BaseRuleEffect implements UNGP_CampaignTag {
    @Override
    public void updateDifficultyCache(UNGP_SpecialistSettings.Difficulty difficulty) {
        int[] monthData = getDataInCampaign(0);
        if (monthData == null) {
            monthData = new int[]{0, 3};
            saveDataInCampaign(0, monthData);
        }
    }

    @Override
    public float getValueByDifficulty(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        return 0;
    }

    @Override
    public String getDescriptionParams(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0 || index == 1) return "3";
        return super.getDescriptionParams(index, difficulty);
    }

    @Override
    public void advanceInCampaign(float amount, UNGP_CampaignPlugin.TempCampaignParams params) {
        if (params.isOneMonthPassed()) {
            int[] monthData = getDataInCampaign(0);
            if (monthData == null) {
                monthData = new int[]{0, 3};
            }
            int elapsedMonth = monthData[0] + 1;
            int maxMonth = monthData[1];
            if (elapsedMonth >= maxMonth) {
                UNGP_InGameData inGameData = UNGP_InGameData.getDataInSave();
                List<URule> activatedRules = inGameData.getActivatedRules();
                List<URule> allRules = getAllRulesCopy();
                WeightedRandomPicker<URule> picker = new WeightedRandomPicker<>(getRandom());
                Set<String> addedRuleIDs = getDataInCampaign(1);
                if (addedRuleIDs == null) {
                    addedRuleIDs = new HashSet<>();
                    saveDataInCampaign(1, addedRuleIDs);
                }
                // 不能roll到成就及黄金规则
                for (URule uRule : allRules) {
                    if (!uRule.isBonus() || activatedRules.contains(uRule)) continue;
                    if (uRule.isGolden()) continue;
                    if (uRule.isMilestone()) continue;
                    if (!uRule.isRollable()) continue;
                    int weight;
                    if (addedRuleIDs.contains(uRule.getId())) {
                        weight = 1;
                    } else {
                        weight = Math.min(6 - Math.abs(uRule.getCost()), 1);
                    }
                    // 权重：cost点数越低越有可能
                    picker.add(uRule, weight);
                }
                URule toAdd = picker.pick();
                if (toAdd != null) {
                    activatedRules.add(toAdd);
                    addedRuleIDs.add(toAdd.getId());
                    MessageIntel intel = new MessageIntel(rule.getName(), toAdd.getCorrectColor());
                    intel.setIcon(toAdd.getSpritePath());
                    intel.addLine(rule.getExtra1(), Misc.getTextColor(), new String[]{toAdd.getName()}, toAdd.getCorrectColor());
                    intel.setSound("UNGP_still_growing_up_activate");
                    showMessage(intel);
                }
                inGameData.saveActivatedRules(activatedRules);
                UNGP_RulesManager.updateCacheNextFrame();

                monthData[0] = 0;
                monthData[1] = monthData[1] + 3;
            } else {
                monthData[0] = elapsedMonth;
            }
            MessageIntel intel = createMessage();
            intel.setIcon(rule.getSpritePath());
            intel.addLine(rule.getExtra2(), Misc.getTextColor(), new String[]{"" + (monthData[1] - monthData[0])}, Misc.getHighlightColor());
            showMessage(intel);
            saveDataInCampaign(0, monthData);
        }
    }

    @Override
    public boolean addIntelTips(TooltipMakerAPI imageTooltip) {
        int[] monthData = getDataInCampaign(0);
        if (monthData != null) {
            imageTooltip.addPara(rule.getExtra2(), 0f, Misc.getHighlightColor(), "" + (monthData[1] - monthData[0]));
            return true;
        }
        return false;
    }
}
