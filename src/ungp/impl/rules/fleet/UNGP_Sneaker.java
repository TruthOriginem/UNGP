package ungp.impl.rules.fleet;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import ungp.scripts.campaign.specialist.UNGP_SpecialistSettings;
import ungp.api.rules.UNGP_BaseRuleEffect;
import ungp.api.rules.tags.UNGP_PlayerFleetTag;

public class UNGP_Sneaker extends UNGP_BaseRuleEffect implements UNGP_PlayerFleetTag {

	public static final float BONUS = 1f;

    @Override
    public void updateDifficultyCache(UNGP_SpecialistSettings.Difficulty difficulty) {

    }

    @Override
    public float getValueByDifficulty(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        return 0;
    }

	@Override
	public void applyPlayerFleetStats(CampaignFleetAPI fleet) {
		fleet.getStats().getDynamic().getMod(Stats.MOVE_SLOW_SPEED_BONUS_MOD).modifyFlat(buffID, BONUS, rule.getName());
	}

	@Override
	public void unapplyPlayerFleetStats(CampaignFleetAPI fleet) {
		fleet.getStats().getDynamic().getMod(Stats.MOVE_SLOW_SPEED_BONUS_MOD).unmodifyFlat(buffID);
	}

    @Override
    public String getDescriptionParams(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return getFactorString(BONUS);
        return super.getDescriptionParams(index, difficulty);
    }
}