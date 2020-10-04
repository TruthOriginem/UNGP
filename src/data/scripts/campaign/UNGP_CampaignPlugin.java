package data.scripts.campaign;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.Script;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.AbilityPlugin;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.campaign.hardmode.UNGP_PlayerFleetMemberBuff;
import data.scripts.ungprules.tags.UNGP_CampaignTag;
import data.scripts.ungprules.tags.UNGP_PlayerFleetTag;
import data.scripts.utils.SimpleI18n;
import org.lwjgl.input.Keyboard;

import static com.fs.starfarer.api.campaign.BuffManagerAPI.Buff;
import static data.scripts.campaign.hardmode.UNGP_RulesManager.*;

public class UNGP_CampaignPlugin implements EveryFrameScript, CampaignEventListener {
    private static final String KEY = "UNGP_cam";
    private static final String BUFF_ID = "UNGP_buffs";
    private static final String ENTITY_ID = "ungp_ui_entity";
    private static final SimpleI18n.I18nSection i18n = new SimpleI18n.I18nSection("UNGP", "c", true);

    /***
     * 源头
     */
    private UNGP_InGameData inGameData;

    private IntervalUtil inheritChecker = new IntervalUtil(1f, 1f);
    private int oneDayChecker = -1;
    private int oneYearChecker = 207;
    private float newGameCheckDays = 0.1f;
    private boolean newGameChecked = false;
    private boolean shouldShowDialog = false;
    private SectorEntityToken ui_entity;


    public static UNGP_CampaignPlugin getInstance() {
        Object plugin = Global.getSector().getPersistentData().get(KEY);
        if (plugin != null) {
            return (UNGP_CampaignPlugin) Global.getSector().getPersistentData().get(KEY);
        } else {
            return new UNGP_CampaignPlugin();
        }
    }

    /**
     * The entity loaded has a custom plugin {@link data.scripts.utils.UNGP_UIEntityPlugin}
     */
    public static void loadUIEntity() {
        UNGP_CampaignPlugin plugin = UNGP_CampaignPlugin.getInstance();
        plugin.ui_entity = Global.getSector().getEntityById(ENTITY_ID);
        if (plugin.ui_entity == null) {
            plugin.ui_entity = Global.getSector().getCurrentLocation().addCustomEntity(ENTITY_ID, null, "ungp_ui", null);
        }
    }

    public static UNGP_InGameData getInGameData() {
        return getInstance().inGameData;
    }

    public UNGP_CampaignPlugin() {
        inGameData = new UNGP_InGameData();
        init();
    }

    public void init() {
        //clear the listener
        Global.getSector().getListenerManager().removeListenerOfClass(UNGP_CampaignPlugin.class);
        Global.getSector().removeScriptsOfClass(UNGP_CampaignPlugin.class);

        //add listener
        Global.getSector().getListenerManager().addListener(this);
        Global.getSector().addScript(this);
        Global.getSector().getPersistentData().put(KEY, this);
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return true;
    }

    @Override
    public void advance(float amount) {
        if (!Global.getSector().isPaused()) {
            //如果没有错过继承时间
            if (!inGameData.passedInheritTime) {
                float days = Global.getSector().getClock().convertToDays(amount);
                //在0.1天内弹出继承隔窗
                if (!newGameChecked) {
                    if (newGameCheckDays > 0f) {
                        newGameCheckDays -= days;
                    } else {
                        newGameChecked = true;
                        if (UNGP_InheritManager.SavePointsExist()) {
                            Global.getSector().getCampaignUI().showConfirmDialog(i18n.get("message"), i18n.get("yes"), i18n.get("no"), new Script() {
                                @Override
                                public void run() {
                                    shouldShowDialog = true;
                                }
                            }, null);
                        }
                    }
                }
                //一天后就算错过时间了
                inheritChecker.advance(days);
                if (inheritChecker.intervalElapsed()) {
                    inGameData.passedInheritTime = true;
                }
            }
        }
        if (!Global.getSector().getCampaignUI().isShowingDialog()) {
            //打开界面
            if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) && Keyboard.isKeyDown(Keyboard.KEY_P)) {
                shouldShowDialog = true;
            }
            if (shouldShowDialog) {
                if (showUNGPDialog()) {
                    shouldShowDialog = false;
                }
            }
        }
        //以下专家模式才可触发
        if (!inGameData.isHardMode) return;

        //调整专家模式UI的位置
        if (!ui_entity.isInCurrentLocation()) {
            LocationAPI loc = Global.getSector().getCurrentLocation();
            ui_entity.getContainingLocation().removeEntity(ui_entity);
            loc.addEntity(ui_entity);
            ui_entity.setContainingLocation(loc);
        }

        int currentDay = Global.getSector().getClock().getDay();
        int currentYear = Global.getSector().getClock().getCycle();
        TempCampaignParams params = new TempCampaignParams();
        //每日一触发
        if (currentDay != oneDayChecker) {
            oneDayChecker = currentDay;
            params.oneDayPassed = true;
        }
        //每年一触发
        if (currentYear != oneYearChecker) {
            oneYearChecker = currentYear;
            params.oneYearPassed = true;
            inGameData.addTimesToChangeSpecialistMode();
        }
        if (Global.getSector().isPaused()) {
            amount = 0;
        }

        //生涯通用效果
        for (UNGP_CampaignTag tag : CAMPAIGN_TAGS_ITG) {
            tag.advanceInCampaign(amount, params);
        }

        //玩家舰队
        CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
        if (playerFleet == null) return;

        //对玩家舰队的效果
        for (UNGP_PlayerFleetTag tag : PLAYER_FLEET_TAGS_ITG) {
            tag.applyPlayerFleetStats(playerFleet);
        }

        //玩家舰队成员效果
        if (!PLAYER_FLEET_MEMBER_TAGS_ITG.isEmpty()) {
            float buffDur = 0.1f;
            boolean needsSync = false;
            for (FleetMemberAPI member : playerFleet.getFleetData().getMembersListCopy()) {
                Buff test = member.getBuffManager().getBuff(BUFF_ID);
                if (test instanceof UNGP_PlayerFleetMemberBuff) {
                    UNGP_PlayerFleetMemberBuff buff = (UNGP_PlayerFleetMemberBuff) test;
                    buff.setDur(buffDur);
                } else {
                    member.getBuffManager().addBuff(new UNGP_PlayerFleetMemberBuff(BUFF_ID, buffDur));
                    needsSync = true;
                }
            }
            if (needsSync) {
                playerFleet.forceSync();
            }
        }
    }


    private boolean showUNGPDialog() {
        return Global.getSector().getCampaignUI().showInteractionDialog(new UNGP_InteractionDialog(inGameData), null);
    }


    public static class TempCampaignParams {
        private boolean oneDayPassed = false;
        private boolean oneYearPassed = false;

        public boolean isOneYearPassed() {
            return oneYearPassed;
        }

        public boolean isOneDayPassed() {
            return oneDayPassed;
        }
    }

    @Override
    public void reportPlayerOpenedMarket(MarketAPI market) {

    }

    @Override
    public void reportPlayerClosedMarket(MarketAPI market) {

    }

    @Override
    public void reportPlayerOpenedMarketAndCargoUpdated(MarketAPI market) {

    }

    @Override
    public void reportEncounterLootGenerated(FleetEncounterContextPlugin plugin, CargoAPI loot) {

    }

    @Override
    public void reportPlayerMarketTransaction(PlayerMarketTransaction transaction) {

    }

    @Override
    public void reportBattleOccurred(CampaignFleetAPI primaryWinner, BattleAPI battle) {

    }

    @Override
    public void reportBattleFinished(CampaignFleetAPI primaryWinner, BattleAPI battle) {

    }

    @Override
    public void reportPlayerEngagement(EngagementResultAPI result) {

    }

    @Override
    public void reportFleetDespawned(CampaignFleetAPI fleet, FleetDespawnReason reason, Object param) {

    }

    @Override
    public void reportFleetSpawned(CampaignFleetAPI fleet) {

    }

    @Override
    public void reportFleetReachedEntity(CampaignFleetAPI fleet, SectorEntityToken entity) {

    }

    @Override
    public void reportFleetJumped(CampaignFleetAPI fleet, SectorEntityToken from, JumpPointAPI.JumpDestination to) {

    }

    @Override
    public void reportShownInteractionDialog(InteractionDialogAPI dialog) {

    }

    @Override
    public void reportPlayerReputationChange(String faction, float delta) {

    }

    @Override
    public void reportPlayerReputationChange(PersonAPI person, float delta) {

    }

    @Override
    public void reportPlayerActivatedAbility(AbilityPlugin ability, Object param) {

    }

    @Override
    public void reportPlayerDeactivatedAbility(AbilityPlugin ability, Object param) {

    }

    @Override
    public void reportPlayerDumpedCargo(CargoAPI cargo) {

    }

    @Override
    public void reportPlayerDidNotTakeCargo(CargoAPI cargo) {

    }

    @Override
    public void reportEconomyTick(int iterIndex) {

    }

    @Override
    public void reportEconomyMonthEnd() {

    }
}
