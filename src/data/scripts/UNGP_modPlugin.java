package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.thoughtworks.xstream.XStream;
import data.scripts.campaign.UNGP_InGameData;
import data.scripts.campaign.UNGP_Settings;
import data.scripts.campaign.UNGP_SharedData;
import data.scripts.campaign.background.UNGP_BackgroundManager;
import data.scripts.campaign.everyframe.UNGP_CampaignPlugin;
import data.scripts.campaign.everyframe.UNGP_SpecialistWidgetPlugin;
import data.scripts.campaign.everyframe.UNGP_UITimeScript;
import data.scripts.campaign.inherit.UNGP_InheritData;
import data.scripts.campaign.specialist.UNGP_PlayerFleetMemberBuff;
import data.scripts.campaign.specialist.challenges.UNGP_ChallengeManager;
import data.scripts.campaign.specialist.items.UNGP_RuleItem;
import data.scripts.campaign.specialist.rules.UNGP_RulesManager;
import data.scripts.utils.UNGP_LoadingChecker;
import ungp.ui.UNGPFont;

public class UNGP_modPlugin extends BaseModPlugin {

    @Override
    public void onApplicationLoad() {
        UNGP_LoadingChecker.runCheckProcess();
        UNGP_Settings.loadSettings();

        UNGPFont.init();
        UNGP_RuleItem.loadSprite();
        UNGP_InheritData.loadAllSavers();
        UNGP_BackgroundManager.loadAllBackgrounds();
        // 规则与挑战
        UNGP_RulesManager.initOrReloadRules();
        UNGP_ChallengeManager.initOrReloadChallengeInfos();
        UNGP_RulesManager.tagAllChallengeProviders();
    }

    @Override
    public void onGameLoad(boolean newGame) {
        UNGP_SharedData.initialize();
        addScriptsIfNeeded();
        UNGP_RulesManager.updateRulesCache();
    }

    private void addScriptsIfNeeded() {
        if (!Global.getSector().hasScript(UNGP_CampaignPlugin.class)) {
            Global.getSector().addScript(new UNGP_CampaignPlugin());
        }
        Global.getSector().addTransientScript(new UNGP_UITimeScript());
        Global.getSector().addTransientScript(new UNGP_SpecialistWidgetPlugin());
        if (!Global.getSector().getCharacterData().getAbilities().contains("UNGP_checkSavepoint")) {
            Global.getSector().getCharacterData().addAbility("UNGP_checkSavepoint");
        }
    }

    @Override
    public void configureXStream(XStream x) {
        x.alias("ungp_camplugin", UNGP_CampaignPlugin.class);
        x.aliasAttribute(UNGP_CampaignPlugin.class, "inheritChecker", "ic");
        x.aliasAttribute(UNGP_CampaignPlugin.class, "oneDayChecker", "odc");
        x.aliasAttribute(UNGP_CampaignPlugin.class, "oneYearChecker", "oyc");
        x.aliasAttribute(UNGP_CampaignPlugin.class, "newGameCheckDays", "ngcd");
        x.aliasAttribute(UNGP_CampaignPlugin.class, "newGameChecked", "ngc");
        x.aliasAttribute(UNGP_CampaignPlugin.class, "shouldShowDialog", "ssd");

        x.alias("ungp_pfmb", UNGP_PlayerFleetMemberBuff.class);
        x.alias("ungp_ut", UNGP_UITimeScript.class);

        x.alias("ungp_igd", UNGP_InGameData.class);
        x.aliasAttribute(UNGP_InGameData.class, "curCycle", "cC");
        x.aliasAttribute(UNGP_InGameData.class, "difficulty", "d");
        x.aliasAttribute(UNGP_InGameData.class, "passedInheritTime", "pIT");
        x.aliasAttribute(UNGP_InGameData.class, "isRecorded", "iR");
        x.aliasAttribute(UNGP_InGameData.class, "inherited", "ied");
        x.aliasAttribute(UNGP_InGameData.class, "isHardMode", "iHM");
        x.aliasAttribute(UNGP_InGameData.class, "timesToChangeSpecialistMode", "tTCSM");
        x.aliasAttribute(UNGP_InGameData.class, "changeTimeStat", "cTS");
        x.aliasAttribute(UNGP_InGameData.class, "activatedRuleIDs", "aRIDs");
        x.aliasAttribute(UNGP_InGameData.class, "completedChallenges", "cChs");
    }
}
