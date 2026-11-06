package ungp.impl.rules.member;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.RepairTrackerAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import ungp.api.rules.UNGP_MemberBuffRuleEffect;
import ungp.scripts.campaign.specialist.UNGP_SpecialistSettings.Difficulty;

public class UNGP_LegendaryMechanic extends UNGP_MemberBuffRuleEffect {
    private static final float MAINTENANCE_MULT = 0.5f;
    private static final float DMOD_EFFECT_MULT = 0.5f;
    private static final float CR_PER_DOCK = 0.5f;
    private static final float CR_HARD_CAP = 1f;
    private static final float DOCK_COOLDOWN_DAYS = 15f;
    private static final String MEM_DOCK_COOLDOWN = "$ungp_legendaryMechanicDockCooldown";

    private static final int LEGACY_LAST_TRIGGER_TIMESTAMP_SLOT = 0;
    private static final int LEGACY_WAS_AT_PORT_SLOT = 1;

    @Override
    public void applyPlayerFleetMemberInCampaign(FleetMemberAPI member) {
        if (member == null || member.getStats() == null) return;

        MutableShipStatsAPI stats = member.getStats();
        stats.getSuppliesPerMonth().modifyMult(getBuffID(), MAINTENANCE_MULT, rule.getName());
        stats.getDynamic().getMod(Stats.DMOD_EFFECT_MULT).modifyMult(getBuffID(), DMOD_EFFECT_MULT, rule.getName());
    }

    public void reportShownInteractionDialog(InteractionDialogAPI dialog) {
        if (!isAtPort(dialog)) return;

        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        if (playerFleet == null) return;
        if (playerFleet.getMemoryWithoutUpdate().contains(MEM_DOCK_COOLDOWN)) return;

        playerFleet.getMemoryWithoutUpdate().set(MEM_DOCK_COOLDOWN, true, DOCK_COOLDOWN_DAYS);
        for (FleetMemberAPI member : playerFleet.getFleetData().getMembersListCopy()) {
            RepairTrackerAPI repairTracker = member.getRepairTracker();
            float currentBaseCR = repairTracker.getBaseCR();
            float crIncrease = Math.min(CR_PER_DOCK, Math.max(0f, CR_HARD_CAP - currentBaseCR));
            if (crIncrease > 0f) {
                repairTracker.applyCREvent(crIncrease, getBuffID(), rule.getName());
            }
        }
    }

    private boolean isAtPort(InteractionDialogAPI dialog) {
        if (dialog == null) return false;

        SectorEntityToken target = dialog.getInteractionTarget();
        if (target == null || target instanceof CampaignFleetAPI) return false;

        MarketAPI market = target.getMarket();
        return market != null && !market.isPlanetConditionMarketOnly() && market.getSize() > 0;
    }

    @Override
    public float getValueByDifficulty(int index, Difficulty difficulty) {
        if (index == 0) return MAINTENANCE_MULT;
        if (index == 1) return CR_PER_DOCK * 100f;
        if (index == 2) return DOCK_COOLDOWN_DAYS;
        return super.getValueByDifficulty(index, difficulty);
    }

    @Override
    public String getDescriptionParams(int index, Difficulty difficulty) {
        if (index == 0) return getFactorString(MAINTENANCE_MULT);
        if (index == 1) return getPercentString(CR_PER_DOCK * 100f);
        if (index == 2) return String.valueOf((int) DOCK_COOLDOWN_DAYS);
        return super.getDescriptionParams(index, difficulty);
    }

    @Override
    public void cleanUp() {
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        if (playerFleet != null) {
            playerFleet.getMemoryWithoutUpdate().unset(MEM_DOCK_COOLDOWN);
        }
        clearDataInCampaign(LEGACY_LAST_TRIGGER_TIMESTAMP_SLOT);
        clearDataInCampaign(LEGACY_WAS_AT_PORT_SLOT);
    }
}
