package ungp.impl.rules.character;

import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import ungp.api.rules.UNGP_BaseRuleEffect;
import ungp.api.rules.tags.UNGP_PlayerCharacterStatsSkillTag;
import ungp.scripts.campaign.specialist.UNGP_SpecialistSettings;

import static com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription.*;

public class UNGP_TechRevolution extends UNGP_BaseRuleEffect implements UNGP_PlayerCharacterStatsSkillTag {
    private float bonus;

    @Override
    public void updateDifficultyCache(UNGP_SpecialistSettings.Difficulty difficulty) {
        bonus = getValueByDifficulty(0, difficulty);
    }

    @Override
    public float getValueByDifficulty(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return difficulty.getLinearValue(0.08f, 0.02f);
        return super.getValueByDifficulty(index, difficulty);
    }

    @Override
    public void apply(MutableCharacterStatsAPI stats) {
        float opMult = 1f;
        FleetDataAPI playerFleetData = getFleetData(null);
        if (playerFleetData != null) {
            float totalFleetDP = getTotalCombatOP(playerFleetData, stats);
            if (totalFleetDP > OP_ALL_THRESHOLD) {
                opMult = Math.max(0.5f, 1f - 0.5f * ((totalFleetDP - OP_ALL_THRESHOLD) / (OP_ALL_THRESHOLD)));
            }
        }
        stats.getShipOrdnancePointBonus().modifyPercent(buffID, bonus * 100f * opMult);
    }

    @Override
    public void unapply(MutableCharacterStatsAPI stats) {
        stats.getShipOrdnancePointBonus().unmodify(buffID);
    }

    @Override
    public String getDescriptionParams(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return getPercentString(getValueByDifficulty(index, difficulty) * 100f);
        if (index == 1) return String.valueOf(OP_ALL_THRESHOLD);
        if (index == 2) return getPercentString(50f);
        return super.getDescriptionParams(index, difficulty);
    }
}
