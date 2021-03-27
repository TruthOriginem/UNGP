package data.scripts.ungprules.impl.member;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import data.scripts.ungprules.impl.UNGP_MemberBuffRuleEffect;

public class UNGP_HssPhoenix extends UNGP_MemberBuffRuleEffect {
    private static final float NORMAL_BONUS = 20f;
    private static final float MANEUVER_BONUS = 50f;
    private static final float CR_BONUS = 5f;


    @Override
    public float getValueByDifficulty(int index, int difficulty) {
        return 0;
    }

    @Override
    public String getDescriptionParams(int index) {
        if (index == 0) return getPercentString(NORMAL_BONUS);
        if (index == 1) return getPercentString(MANEUVER_BONUS);
        if (index == 2) return getPercentString(CR_BONUS);
        return null;
    }

    @Override
    public void applyPlayerFleetMemberInCampaign(FleetMemberAPI member) {
        MutableShipStatsAPI stats = member.getStats();
        String id = rule.getBuffID();
        stats.getHullBonus().modifyPercent(id, NORMAL_BONUS);
        stats.getArmorBonus().modifyPercent(id, NORMAL_BONUS);
        stats.getFluxCapacity().modifyPercent(id, NORMAL_BONUS);
        stats.getFluxDissipation().modifyPercent(id, NORMAL_BONUS);
        stats.getMaxSpeed().modifyPercent(id, MANEUVER_BONUS);
        stats.getAcceleration().modifyPercent(id, MANEUVER_BONUS);
        stats.getDeceleration().modifyPercent(id, MANEUVER_BONUS);
        stats.getMaxTurnRate().modifyPercent(id, MANEUVER_BONUS);
        stats.getTurnAcceleration().modifyPercent(id, MANEUVER_BONUS);

        stats.getMaxCombatReadiness().modifyFlat(id, CR_BONUS * 0.01f, rule.getName());
    }


    @Override
    public boolean canApply(FleetMemberAPI member) {
        return member.isFlagship() && member.getHullId().contains("onslaught");
    }

    @Override
    public String getBuffID() {
        return rule.getBuffID();
    }
}
