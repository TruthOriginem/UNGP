package ungp.impl.rules.other;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import ungp.scripts.campaign.specialist.UNGP_SpecialistSettings;
import ungp.api.rules.UNGP_BaseRuleEffect;

public class UNGP_FullOfCompany extends UNGP_BaseRuleEffect {

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
