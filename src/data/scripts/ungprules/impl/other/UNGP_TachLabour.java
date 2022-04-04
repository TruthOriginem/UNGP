package data.scripts.ungprules.impl.other;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import data.scripts.campaign.specialist.UNGP_SpecialistSettings;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;

public class UNGP_TachLabour extends UNGP_BaseRuleEffect {

    @Override
    public void updateDifficultyCache(UNGP_SpecialistSettings.Difficulty difficulty) {

    }

    @Override
    public float getValueByDifficulty(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        return 0;
    }


    @Override
    public void applyGlobalStats() {
        if (!Global.getSector().getPersistentData().containsKey(buffID)) {
            Global.getSector().getPlayerFaction().setRelationship(Factions.REMNANTS, RepLevel.COOPERATIVE);
            Global.getSector().getPersistentData().put(buffID, true);
        }
    }

    @Override
    public String getDescriptionParams(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return RepLevel.COOPERATIVE.getDisplayName();
        return null;
    }
}
