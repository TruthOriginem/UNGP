package data.scripts.campaign.specialist.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.characters.CharacterStatsSkillEffect;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.ShipSkillEffect;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import data.scripts.campaign.specialist.rules.UNGP_RulesManager;
import data.scripts.ungprules.tags.UNGP_PlayerCharacterStatsSkillTag;
import data.scripts.ungprules.tags.UNGP_PlayerShipSkillTag;

import static com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription.isInCampaign;
import static data.scripts.campaign.specialist.rules.UNGP_RulesManager.URule;
import static data.scripts.campaign.specialist.rules.UNGP_RulesManager.getAllRulesCopy;

/**
 * Skill for rules, would be automatically added to player.
 */
public class UNGP_SpecialistSkills {
    public static class UNGP_SpecialistShipSkill implements ShipSkillEffect {
        @Override
        public void apply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id, float level) {
            FleetDataAPI fleetData = null;
            FleetMemberAPI member = stats.getFleetMember();
            if (isInCampaign()) {
                fleetData = Global.getSector().getPlayerFleet().getFleetData();
            } else if (member != null) {
                fleetData = member.getFleetDataForStats();
                if (fleetData == null) fleetData = member.getFleetData();
            }
            for (UNGP_PlayerShipSkillTag tag : UNGP_RulesManager.PLAYER_SHIP_SKILL_TAGS_ITG) {
                tag.apply(fleetData, member, stats, hullSize);
            }
        }

        @Override
        public void unapply(MutableShipStatsAPI stats, ShipAPI.HullSize hullSize, String id) {
            for (URule rule : getAllRulesCopy()) {
                if (rule.getRuleEffect() instanceof UNGP_PlayerShipSkillTag) {
                    ((UNGP_PlayerShipSkillTag) rule.getRuleEffect()).unapply(stats, hullSize);
                }
            }
        }

        @Override
        public String getEffectDescription(float level) {
            return null;
        }

        @Override
        public String getEffectPerLevelDescription() {
            return null;
        }

        @Override
        public ScopeDescription getScopeDescription() {
            return null;
        }
    }

    public static class UNGP_SpecialistCharacterSkill implements CharacterStatsSkillEffect {

        @Override
        public void apply(MutableCharacterStatsAPI stats, String id, float level) {
            for (UNGP_PlayerCharacterStatsSkillTag tag : UNGP_RulesManager.PLAYER_CHARACTER_SKILL_TAGS_ITG) {
                tag.apply(stats);
            }
        }

        @Override
        public void unapply(MutableCharacterStatsAPI stats, String id) {
            for (URule rule : getAllRulesCopy()) {
                if (rule.getRuleEffect() instanceof UNGP_PlayerCharacterStatsSkillTag) {
                    ((UNGP_PlayerCharacterStatsSkillTag) rule.getRuleEffect()).unapply(stats);
                }
            }
        }

        @Override
        public String getEffectDescription(float level) {
            return null;
        }

        @Override
        public String getEffectPerLevelDescription() {
            return null;
        }

        @Override
        public ScopeDescription getScopeDescription() {
            return null;
        }
    }
}
