package ungp.impl.rules.other;

import com.fs.starfarer.api.impl.campaign.intel.MessageIntel;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import ungp.scripts.campaign.specialist.UNGP_SpecialistSettings;
import ungp.scripts.campaign.specialist.rules.UNGP_RulesManager;
import ungp.scripts.campaign.specialist.rules.UNGP_RulesManager.URule;
import ungp.api.rules.UNGP_BaseRuleEffect;
import ungp.api.rules.tags.UNGP_TweakBeforeApplyTag;

import java.util.List;

public class UNGP_Amnesia extends UNGP_BaseRuleEffect implements UNGP_TweakBeforeApplyTag {
    @Override
    public void tweakBeforeApply(List<URule> activeRules, List<URule> originalActiveRules) {
        WeightedRandomPicker<URule> picker = new WeightedRandomPicker<>(getRandom());
        for (URule activeRule : activeRules) {
            if (activeRule.isBonus()) {
                picker.add(activeRule);
            }
        }
        if (!picker.isEmpty()) {
            URule rolled = picker.pick();
            activeRules.remove(rolled);
            MessageIntel intel = createMessage();
            intel.setIcon(rolled.getSpritePath());
            intel.addLine(rule.getExtra1(), Misc.getTextColor(), new String[]{rolled.getName()}, rolled.getCorrectColor());
            showMessage(intel);
        }
        activeRules.remove(rule);
    }

    @Override
    public String getDescriptionParams(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return UNGP_RulesManager.getPNRuleString(true);
        return super.getDescriptionParams(index, difficulty);
    }
}
