package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.thoughtworks.xstream.XStream;
import data.scripts.campaign.UNGP_InGameData;
import data.scripts.campaign.UNGP_SharedData;
import data.scripts.campaign.everyframe.UNGP_CampaignPlugin;
import data.scripts.campaign.everyframe.UNGP_UITimeScript;
import data.scripts.campaign.specialist.UNGP_PlayerFleetMemberBuff;
import data.scripts.campaign.specialist.challenges.UNGP_ChallengeManager;
import data.scripts.campaign.specialist.items.UNGP_RuleItem;
import data.scripts.campaign.specialist.rules.UNGP_RulesManager;
import data.scripts.utils.UNGPFont;
import data.scripts.utils.UNGP_LoadingChecker;

public class UNGP_modPlugin extends BaseModPlugin {

    @Override
    public void onApplicationLoad() {
        UNGP_LoadingChecker.checkLoad();


        UNGPFont.init();
        UNGP_RuleItem.loadSprite();
        // 规则与挑战
        UNGP_RulesManager.initOrReloadRules();
        UNGP_ChallengeManager.initOrReloadChallengeInfos();
        UNGP_RulesManager.tagAllChallengeProviders();
    }

    @Override
    public void onNewGameAfterTimePass() {
    }

    @Override
    public void onGameLoad(boolean newGame) {
        UNGP_SharedData.initialize();
        addScriptsIfNeeded();
        UNGP_RulesManager.updateRulesCache();
        UNGP_CampaignPlugin.loadUIEntity();
    }

    private void addScriptsIfNeeded() {
        if (!Global.getSector().hasScript(UNGP_CampaignPlugin.class)) {
            new UNGP_CampaignPlugin();
        }
//        UNGP_UITimeScript script = new UNGP_UITimeScript();
//        UNGP_UITimeScript.setInstance(script);
        Global.getSector().addTransientScript(new UNGP_UITimeScript());
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
        x.aliasAttribute(UNGP_InGameData.class, "difficultyLevel", "dL");
        x.aliasAttribute(UNGP_InGameData.class, "passedInheritTime", "pIT");
        x.aliasAttribute(UNGP_InGameData.class, "isRecorded", "iR");
        x.aliasAttribute(UNGP_InGameData.class, "inherited", "ied");
        x.aliasAttribute(UNGP_InGameData.class, "isHardMode", "iHM");
        x.aliasAttribute(UNGP_InGameData.class, "timesToChangeSpecialistMode", "tTCSM");
        x.aliasAttribute(UNGP_InGameData.class, "changeTimeStat", "cTS");
    }
}
