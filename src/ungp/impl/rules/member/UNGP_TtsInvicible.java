package ungp.impl.rules.member;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import ungp.scripts.campaign.specialist.UNGP_SpecialistSettings;
import ungp.api.rules.UNGP_MemberBuffRuleEffect;

import java.util.ArrayList;
import java.util.List;

public class UNGP_TtsInvicible extends UNGP_MemberBuffRuleEffect {

    private static final float NORMAL_BONUS = 50f;
    private static final float DAMAGE_BONUS = 15f;
    private static final float CR_BONUS = 10f;

    @Override
    public float getValueByDifficulty(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        return 0;
    }

    @Override
    public String getDescriptionParams(int index, UNGP_SpecialistSettings.Difficulty difficulty) {
        if (index == 0) return getPercentString(NORMAL_BONUS);
        if (index == 1) return getPercentString(DAMAGE_BONUS);
        if (index == 2) return getPercentString(CR_BONUS);
        return super.getDescriptionParams(index, difficulty);
    }

    @Override
    public void applyPlayerFleetMemberInCampaign(FleetMemberAPI member) {
        MutableShipStatsAPI stats = member.getStats();
        String id = buffID;
        stats.getShieldUpkeepMult().modifyMult(id, 1f - NORMAL_BONUS * 0.01f);
        stats.getShieldUnfoldRateMult().modifyPercent(id, NORMAL_BONUS);
        stats.getShieldTurnRateMult().modifyPercent(id, NORMAL_BONUS);
        stats.getEnergyWeaponDamageMult().modifyPercent(id, DAMAGE_BONUS);
        stats.getBallisticWeaponDamageMult().modifyPercent(id, DAMAGE_BONUS);
        stats.getMissileWeaponDamageMult().modifyPercent(id, DAMAGE_BONUS);
        stats.getEnergyRoFMult().modifyPercent(id, DAMAGE_BONUS);
        stats.getBallisticRoFMult().modifyPercent(id, DAMAGE_BONUS);
        stats.getMissileRoFMult().modifyPercent(id, DAMAGE_BONUS);

        stats.getMaxCombatReadiness().modifyFlat(id, CR_BONUS * 0.01f, rule.getName());
    }

    @Override
    public boolean addIntelTips(TooltipMakerAPI imageTooltip) {
        imageTooltip.addPara(rule.getExtra1(), 0f);
        List<String> names = new ArrayList<>();
        for (ShipHullSpecAPI hullSpec : Global.getSettings().getAllShipHullSpecs()) {
            if (isParagon(hullSpec) && !hullSpec.isDefaultDHull()) {
                names.add(hullSpec.getNameWithDesignationWithDashClass());
            }
        }
        imageTooltip.addPara(Misc.getAndJoined(names), Misc.getHighlightColor(), 5f);
        return true;
    }

    /**
     * For modders: if your ship has tag of "ungp_paragon", your ship would be considered as paragon-class too.
     *
     * @param member
     * @return
     */
    @Override
    public boolean canApply(FleetMemberAPI member) {
        return member.isFlagship() && isParagon(member.getHullSpec());
    }

    public static boolean isParagon(ShipHullSpecAPI hullSpec) {
        return hullSpec.getHullId().contains("paragon") || hullSpec.getTags().contains("ungp_paragon");
    }
}
