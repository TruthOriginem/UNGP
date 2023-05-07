package ungp.impl.rules.other;

import com.fs.starfarer.api.impl.campaign.intel.MessageIntel;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import ungp.scripts.campaign.specialist.UNGP_SpecialistSettings;
import ungp.scripts.campaign.specialist.rules.UNGP_RulesManager;
import ungp.api.rules.UNGP_BaseRuleEffect;
import ungp.api.rules.tags.UNGP_TweakBeforeApplyTag;

import java.util.List;

import static ungp.scripts.campaign.specialist.rules.UNGP_RulesManager.URule;
import static ungp.scripts.campaign.specialist.rules.UNGP_RulesManager.getAllRulesCopy;

public class UNGP_RainPour extends UNGP_BaseRuleEffect implements UNGP_TweakBeforeApplyTag {
    @Override
    public float getValueByDifficulty(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        return 0;
    }

    @Override
    public String getDescriptionParams(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return "2";
        if (index == 1) return UNGP_RulesManager.getPNRuleString(false);
        return null;
    }

    @Override
    public void tweakBeforeApply(List<URule> activeRules, List<URule> originalActiveRules) {
        WeightedRandomPicker<URule> picker = new WeightedRandomPicker<>(getRandom());
        List<URule> rulesToPick = getAllRulesCopy();
        rulesToPick.removeAll(originalActiveRules);
        for (URule tmp : rulesToPick) {
            if (tmp == rule) continue;
            if (tmp.hasTag(URule.Tags.NO_ROLL)) continue;
            if (!tmp.isBonus()) {
                picker.add(tmp);
            }
        }
        if (!picker.isEmpty()) {
            for (int i = 0; i < 2; i++) {
                URule rolled = picker.pickAndRemove();
                activeRules.add(rolled);
                MessageIntel intel = createMessage();
                intel.setIcon(rolled.getSpritePath());
                intel.addLine(rule.getExtra1(), Misc.getTextColor(), new String[]{rolled.getName()}, rolled.getCorrectColor());
                showMessage(intel);
            }
        }
        activeRules.remove(rule);
    }
}