package data.scripts.ungprules.impl.member;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.specialist.UNGP_SpecialistSettings;
import data.scripts.ungprules.impl.UNGP_MemberBuffRuleEffect;

import java.util.ArrayList;
import java.util.List;

public class UNGP_HssPhoenix extends UNGP_MemberBuffRuleEffect {
    private static final float NORMAL_BONUS = 20f;
    private static final float MANEUVER_BONUS = 50f;
    private static final float CR_BONUS = 5f;


    @Override
    public float getValueByDifficulty(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        return 0;
    }

    @Override
    public String getDescriptionParams(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return getPercentString(NORMAL_BONUS);
        if (index == 1) return getPercentString(MANEUVER_BONUS);
        if (index == 2) return getPercentString(CR_BONUS);
        return super.getDescriptionParams(index, difficulty);
    }

    @Override
    public void applyPlayerFleetMemberInCampaign(FleetMemberAPI member) {
        MutableShipStatsAPI stats = member.getStats();
        String id = buffID;
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
    public boolean addIntelTips(TooltipMakerAPI imageTooltip) {
        imageTooltip.addPara(rule.getExtra1(), 0f);
        List<String> names = new ArrayList<>();
        for (ShipHullSpecAPI hullSpec : Global.getSettings().getAllShipHullSpecs()) {
            if (isOnslaught(hullSpec) && !hullSpec.isDefaultDHull()) {
                names.add(hullSpec.getNameWithDesignationWithDashClass());
            }
        }
        imageTooltip.addPara(Misc.getAndJoined(names), Misc.getHighlightColor(), 5f);
        return true;
    }

    /**
     * For modders: if your ship has tag of "ungp_onslaught", your ship would be considered as onslaught-class too.
     *
     * @param member
     * @return
     */
    @Override
    public boolean canApply(FleetMemberAPI member) {
        return member.isFlagship() && isOnslaught(member.getHullSpec());
    }

    public static boolean isOnslaught(ShipHullSpecAPI hullSpec) {
        return hullSpec.getHullId().contains("onslaught") || hullSpec.getTags().contains("ungp_onslaught");
    }
}
