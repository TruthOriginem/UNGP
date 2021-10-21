package data.scripts.ungprules.impl.other;

import com.fs.starfarer.api.impl.campaign.intel.MessageIntel;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.campaign.specialist.rules.UNGP_RulesManager;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import data.scripts.ungprules.tags.UNGP_TweakBeforeApplyTag;

import java.util.List;

import static data.scripts.campaign.specialist.rules.UNGP_RulesManager.URule;
import static data.scripts.campaign.specialist.rules.UNGP_RulesManager.getAllRulesCopy;

public class UNGP_Ying extends UNGP_BaseRuleEffect implements UNGP_TweakBeforeApplyTag {
    @Override
    public float getValueByDifficulty(int index, int difficulty) {
        return 0;
    }

    @Override
    public String getDescriptionParams(int index,int difficulty) {
        if (index == 0) return UNGP_RulesManager.getBonusString(false);
        if (index == 1) return UNGP_RulesManager.getBonusString(true);
        return null;
    }

    @Override
    public void tweakBeforeApply(List<URule> activeRules, List<URule> originalActiveRules) {
        boolean containsYang = false;
        for (URule tmp : originalActiveRules) {
            if (tmp.getId().equals("yang")) {
                containsYang = true;
                break;
            }
        }
        WeightedRandomPicker<URule> picker = new WeightedRandomPicker<>(getRandom());
        List<URule> rulesToPick = getAllRulesCopy();
        rulesToPick.removeAll(originalActiveRules);
        //如果有另一个就全可以选
        for (URule tmp : rulesToPick) {
            if (tmp == rule) continue;
            if (!tmp.isRollable()) continue;
            if (containsYang) {
                picker.add(tmp);
            } else if (!tmp.isBonus()) {
                picker.add(tmp);
            }
        }
        if (!picker.isEmpty()) {
            URule rolled = picker.pick();
            activeRules.add(rolled);
            MessageIntel intel = new MessageIntel(rule.getName(), rolled.getCorrectColor());
            intel.setIcon(rolled.getSpritePath());
            intel.addLine(rule.getExtra1(), Misc.getTextColor(), new String[]{rolled.getName()}, rolled.getCorrectColor());
            showMessage(intel);
        }
        activeRules.remove(rule);
    }
}