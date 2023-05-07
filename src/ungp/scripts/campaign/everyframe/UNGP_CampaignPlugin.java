package ungp.scripts.campaign.everyframe;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.Script;
import com.fs.starfarer.api.campaign.BuffManagerAPI;
import com.fs.starfarer.api.campaign.CampaignClockAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import ungp.scripts.campaign.UNGP_InGameData;
import ungp.scripts.campaign.UNGP_Settings;
import ungp.scripts.campaign.specialist.UNGP_PlayerFleetMemberBuff;
import ungp.scripts.campaign.ui.UNGP_InteractionDialog;
import ungp.api.rules.tags.UNGP_CampaignTag;
import ungp.api.rules.tags.UNGP_PlayerFleetMemberTag;
import ungp.api.rules.tags.UNGP_PlayerFleetTag;
import org.lwjgl.input.Keyboard;

import java.util.List;

import static com.fs.starfarer.api.campaign.BuffManagerAPI.Buff;
import static ungp.scripts.campaign.specialist.rules.UNGP_RulesManager.*;
import static ungp.scripts.utils.Constants.root_i18n;

/**
 * 主要战役逻辑所在地
 */
public class UNGP_CampaignPlugin implements EveryFrameScript {
    private static final String KEY = "UNGP_cam";
    private static final float BASE_BUFF_DURATION = 0.1f;

    /***
     * 源头
     */
    private UNGP_InGameData inGameData;

    private IntervalUtil inheritChecker = new IntervalUtil(1f, 1f);
    private int oneDayChecker = -1;
    private int oneYearChecker = -1;
    private int oneMonthChecker = -1;
    private float newGameCheckDays = 0.1f;
    private boolean newGameChecked = false;
    private boolean shouldShowDialog = false;

    public static UNGP_CampaignPlugin getInstance() {
        UNGP_CampaignPlugin plugin = (UNGP_CampaignPlugin) Global.getSector().getPersistentData().get(KEY);
        if (plugin != null) {
            return plugin;
        } else {
            throw new RuntimeException("Something wrong with UNGP, you may best reboot your game...");
        }
    }

    public UNGP_CampaignPlugin() {
        inGameData = new UNGP_InGameData();
        Global.getSector().getPersistentData().put(KEY, this);
        CampaignClockAPI clock = Global.getSector().getClock();
        oneDayChecker = clock.getDay();
        oneMonthChecker = clock.getMonth();
        oneYearChecker = clock.getCycle();
    }

    @Override
    public boolean isDone() {
        return false;
    }

    // 暂停时也会运行
    @Override
    public boolean runWhilePaused() {
        return true;
    }

    @Override
    public void advance(float amount) {
        if (isCacheNeedUpdate()) {
            updateRulesCache();
        }
        // 初始变量设定
        final boolean isPaused = Global.getSector().isPaused();
        final SectorAPI sector = Global.getSector();
        final CampaignClockAPI clock = sector.getClock();

        if (!isPaused) {
            // 如果没有错过继承时间
            if (!inGameData.isPassedInheritTime()) {
                float days = clock.convertToDays(amount);
                // 在0.1天内弹出继承隔窗
                if (!newGameChecked) {
                    if (newGameCheckDays > 0f) {
                        newGameCheckDays -= days;
                    } else {
                        newGameChecked = true;
                        sector.getCampaignUI().showConfirmDialog(root_i18n.get("gameStartMessage"), root_i18n.get("confirm"), root_i18n.get("cancel"), new Script() {
                            @Override
                            public void run() {
                                shouldShowDialog = true;
                            }
                        }, null);
                    }
                }
                // 一天后就算错过时间了
                inheritChecker.advance(days);
                if (inheritChecker.intervalElapsed()) {
                    inGameData.setPassedInheritTime(true);
                }
            }
        }
        if (!sector.getCampaignUI().isShowingDialog()) {
            // 打开界面
            if (Keyboard.isKeyDown(UNGP_Settings.getShowMenuKey1())
                    && Keyboard.isKeyDown(UNGP_Settings.getShowMenuKey2())) {
                shouldShowDialog = true;
            }
            if (shouldShowDialog) {
                if (showUNGPDialog()) {
                    shouldShowDialog = false;
                }
            }
        }
        // 以下专家模式才可触发
        if (!inGameData.isHardMode()) return;

        int currentDay = clock.getDay();
        int currentYear = clock.getCycle();
        int currentMonth = clock.getMonth();
        TempCampaignParams params = new TempCampaignParams();
        // 每日一触发
        if (currentDay != oneDayChecker) {
            oneDayChecker = currentDay;
            params.oneDayPassed = true;
            // 如果是开发模式
            if (Global.getSettings().isDevMode()) {
                for (int i = 0; i < 10; i++) {
                    inGameData.addTimesToChangeSpecialistMode();
                }
            }
        }
        // 每月一触发
        if (currentMonth != oneMonthChecker) {
            oneMonthChecker = currentMonth;
            params.oneMonthPassed = true;
        }
        // 每年一触发
        if (currentYear != oneYearChecker) {
            oneYearChecker = currentYear;
            params.oneYearPassed = true;
            inGameData.addTimesToChangeSpecialistMode();
        }

        // 如果暂停，时间参数改为0
        if (isPaused) {
            amount = 0;
        }

        // 生涯通用效果
        for (UNGP_CampaignTag tag : CAMPAIGN_TAGS_ITG) {
            tag.advanceInCampaign(amount, params);
        }

        // 玩家舰队
        CampaignFleetAPI playerFleet = sector.getPlayerFleet();
        if (playerFleet == null) return;

        // 对玩家舰队的效果
        for (UNGP_PlayerFleetTag tag : PLAYER_FLEET_TAGS_ITG) {
            tag.applyPlayerFleetStats(playerFleet);
        }

        // 玩家舰队成员效果
        if (!PLAYER_FLEET_MEMBER_TAGS_ITG.isEmpty()) {
            boolean needsSync = false;
            List<FleetMemberAPI> members = playerFleet.getFleetData().getMembersListCopy();
            for (FleetMemberAPI member : members) {
                BuffManagerAPI buffManager = member.getBuffManager();
                for (UNGP_PlayerFleetMemberTag tag : PLAYER_FLEET_MEMBER_TAGS_ITG) {
                    // 如果能apply
                    if (tag.canApply(member)) {
                        Buff test = buffManager.getBuff(tag.getBuffID());
                        if (test instanceof UNGP_PlayerFleetMemberBuff) {
                            UNGP_PlayerFleetMemberBuff buff = (UNGP_PlayerFleetMemberBuff) test;
                            buff.setDur(BASE_BUFF_DURATION);
                            buff.setTag(tag);
                        } else {
                            buffManager.addBuff(new UNGP_PlayerFleetMemberBuff(tag, BASE_BUFF_DURATION));
                            needsSync = true;
                        }
                    }
                }
            }
            if (needsSync || UNGP_PlayerFleetMemberBuff.isShouldForceSyncNextStep()) {
                playerFleet.forceSync();
                UNGP_PlayerFleetMemberBuff.completeForceSync();
            }
        }
    }


    public boolean showUNGPDialog() {
        newGameChecked = true;
        return Global.getSector().getCampaignUI().showInteractionDialog(new UNGP_InteractionDialog(inGameData), Global.getSector().getPlayerFleet());
    }

    public UNGP_InGameData getInGameData() {
        return inGameData;
    }


    /**
     * Just some parameters that rules may used many times
     */
    public static class TempCampaignParams {
        private boolean oneDayPassed = false;
        private boolean oneMonthPassed = false;
        private boolean oneYearPassed = false;

        public boolean isOneYearPassed() {
            return oneYearPassed;
        }

        public boolean isOneMonthPassed() {
            return oneMonthPassed;
        }

        public boolean isOneDayPassed() {
            return oneDayPassed;
        }
    }
}
