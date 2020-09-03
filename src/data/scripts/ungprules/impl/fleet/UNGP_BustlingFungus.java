package data.scripts.ungprules.impl.fleet;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BuffManagerAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.CampaignPingSpec;
import data.scripts.campaign.UNGP_CampaignPlugin;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import data.scripts.utils.UNGP_BaseBuff;

import java.awt.*;

public class UNGP_BustlingFungus extends UNGP_BaseRuleEffect {
    private static final String STAY_MEM = "$ungp_fungus_elapsed";
    private static final Color NOTICE_COLOR = new Color(0, 213, 140, 255);
    private static final float WAIT_DAY = 1;
    private static final float REPAIR_RATE = 50f;
    private static final float SUPPLY_FACTOR = 0.5f;

    private class FungusBuff extends UNGP_BaseBuff {
        public FungusBuff(String id, float dur) {
            super(id, dur);
        }

        @Override
        public void apply(FleetMemberAPI member) {
            member.getStats().getRepairRatePercentPerDay().modifyPercent(rule.getBuffID(), REPAIR_RATE);
            member.getStats().getSuppliesPerMonth().modifyMult(rule.getBuffID(), SUPPLY_FACTOR);
        }

    }


    @Override
    public void refreshDifficultyCache(int difficulty) {

    }

    @Override
    public float getValueByDifficulty(int index, int difficulty) {
        return 0;
    }

    @Override
    public void advanceInCampaign(float amount, UNGP_CampaignPlugin.TempCampaignParams params) {
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        if (playerFleet != null) {
            float vel = playerFleet.getCurrBurnLevel();
            if (vel < 1e-2) {
                Object mem = playerFleet.getMemoryWithoutUpdate().get(STAY_MEM);
                if (mem == null) {
                    playerFleet.getMemoryWithoutUpdate().set(STAY_MEM, 0f);
                } else {
                    float elapsed = (float) mem;
                    if (elapsed >= WAIT_DAY) {
                        String buffId = rule.getBuffID();
                        float buffDur = 0.1f;
                        boolean needsSync = false;
                        for (FleetMemberAPI member : playerFleet.getFleetData().getMembersListCopy()) {
                            BuffManagerAPI.Buff test = member.getBuffManager().getBuff(buffId);
                            if (test instanceof FungusBuff) {
                                FungusBuff buff = (FungusBuff) test;
                                buff.setDur(buffDur);
                            } else {
                                member.getBuffManager().addBuff(new FungusBuff(buffId, buffDur));
                                needsSync = true;
                            }
                        }
                        if (needsSync) {
                            playerFleet.forceSync();
                        }
                    } else {
                        float days = Global.getSector().getClock().convertToDays(amount);
                        elapsed += days;
                        playerFleet.getMemoryWithoutUpdate().set(STAY_MEM, elapsed);
                        //只会运行一次
                        if (elapsed >= WAIT_DAY) {
                            playerFleet.addFloatingText(rule.getRuleInfo().getExtra1(), NOTICE_COLOR, 1f);
                            CampaignPingSpec custom = new CampaignPingSpec();
                            custom.setColor(NOTICE_COLOR);
                            custom.setWidth(10);
                            custom.setRange(200);
                            custom.setDuration(3f);
                            custom.setAlphaMult(0.25f);
                            custom.setInFraction(0.1f);
                            custom.setDelay(0.3f);
                            custom.setNum(3);
                            Global.getSector().addPing(playerFleet, custom);
                            Global.getSoundPlayer().playUISound("UNGP_fungus_activate", 1f, 1f);
                        }
                    }
                }
            } else {
                playerFleet.getMemoryWithoutUpdate().unset(STAY_MEM);
            }
        }
    }

    @Override
    public String getDescriptionParams(int index) {
        if (index == 0) return (int) WAIT_DAY + "";
        if (index == 1) return (int) (REPAIR_RATE) + "%";
        if (index == 2) return (int) ((1 - SUPPLY_FACTOR) * 100f) + "%";
        return null;
    }
}