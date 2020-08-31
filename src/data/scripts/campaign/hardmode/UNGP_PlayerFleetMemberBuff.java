package data.scripts.campaign.hardmode;

import com.fs.starfarer.api.campaign.BuffManagerAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import data.scripts.campaign.hardmode.UNGP_RulesManager.URule;

public class UNGP_PlayerFleetMemberBuff implements BuffManagerAPI.Buff {
    private String id;
    private float dur;

    public UNGP_PlayerFleetMemberBuff(String id, float dur) {
        this.id = id;
        this.dur = dur;
    }

    @Override
    public void advance(float days) {
        dur -= days;
    }

    @Override
    public void apply(FleetMemberAPI member) {
        for (URule rule : UNGP_RulesManager.ACTIVATED_RULES_IN_THIS_GAME) {
            rule.getRuleEffect().applyPlayerFleetMemberInCampaign(member);
        }
//        URule rule = URule.MAX_LOGISTICS_BONUS;
//        if (rule.isActivated()) {
//            member.getStats().getDynamic().getMod(Stats.MAX_LOGISTICS_HULLMODS_MOD).modifyFlat(rule.getBuffID(), rule.getValueByCache(0));
//        }
//        rule = URule.LOW_REPAIR_TECH;
//        if (rule.isActivated()) {
//            member.getStats().getBaseCRRecoveryRatePercentPerDay().modifyPercent(rule.getBuffID(), -rule.getValueByCache(0) * 100f);
//            member.getStats().getRepairRatePercentPerDay().modifyPercent(rule.getBuffID(), -rule.getValueByCache(0) * 100f);
//        }
    }

    public String getId() {
        return id;
    }

    @Override
    public boolean isExpired() {
        return dur <= 0;
    }

    public float getDur() {
        return dur;
    }

    public void setDur(float dur) {
        this.dur = dur;
    }

}
