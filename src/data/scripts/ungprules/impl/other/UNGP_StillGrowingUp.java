package data.scripts.ungprules.impl.other;

import com.fs.starfarer.api.impl.campaign.intel.MessageIntel;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.campaign.UNGP_InGameData;
import data.scripts.campaign.everyframe.UNGP_CampaignPlugin;
import data.scripts.campaign.specialist.rules.UNGP_RulesManager;
import data.scripts.campaign.specialist.rules.UNGP_RulesManager.URule;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import data.scripts.ungprules.tags.UNGP_CampaignTag;

import java.util.List;

import static data.scripts.campaign.specialist.rules.UNGP_RulesManager.getAllRulesCopy;

public class UNGP_StillGrowingUp extends UNGP_BaseRuleEffect implements UNGP_CampaignTag {
    @Override
    public float getValueByDifficulty(int index, int difficulty) {
        return 0;
    }

    @Override
    public String getDescriptionParams(int index, int difficulty) {
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
                // 不能roll到黄金规则
                for (URule uRule : allRules) {
                    if (!uRule.isBonus() || activatedRules.contains(uRule)) continue;
                    if (uRule.isGolden()) continue;
                    if (!uRule.isRollable()) continue;
                    // 权重：cost点数越低越有可能
                    picker.add(uRule, Math.min(6 - Math.abs(uRule.getCost()), 1));
                }
                URule toAdd = picker.pick();
                if (toAdd != null) {
                    activatedRules.add(toAdd);
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
            MessageIntel intel = new MessageIntel(rule.getName(), rule.getCorrectColor());
            intel.setIcon(rule.getSpritePath());
            intel.addLine(rule.getExtra2(), Misc.getTextColor(), new String[]{"" + (monthData[1] - monthData[0])}, Misc.getHighlightColor());
            showMessage(intel);
            saveDataInCampaign(0, monthData);
        }
    }
}
