package data.scripts.ungprules.impl.other;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import data.scripts.campaign.specialist.UNGP_SpecialistSettings;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import data.scripts.ungprules.tags.UNGP_TweakBeforeApplyTag;

import java.util.List;

import static data.scripts.campaign.specialist.rules.UNGP_RulesManager.URule;

public class UNGP_FullOfCompany extends UNGP_BaseRuleEffect implements UNGP_TweakBeforeApplyTag {

    @Override
    public void updateDifficultyCache(UNGP_SpecialistSettings.Difficulty difficulty) {

    }

    @Override
    public float getValueByDifficulty(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        return 0;
    }

    @Override
    public void tweakBeforeApply(List<URule> activeRules, List<URule> originalActiveRules) {
        if (!Global.getSector().getPersistentData().containsKey(buffID)) {
            Global.getSector().getPlayerFaction().setRelationship(Factions.PIRATES, RepLevel.FRIENDLY);
            Global.getSector().getPlayerFaction().setRelationship(Factions.INDEPENDENT, RepLevel.FRIENDLY);
            Global.getSector().getPersistentData().put(buffID, true);
        }
    }

    @Override
    public String getDescriptionParams(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return RepLevel.FRIENDLY.getDisplayName();
        return null;
    }
}
