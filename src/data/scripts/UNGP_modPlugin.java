package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.thoughtworks.xstream.XStream;
import data.scripts.campaign.UNGP_InGameData;
import data.scripts.campaign.everyframe.UNGP_CampaignPlugin;
import data.scripts.campaign.everyframe.UNGP_UITimeScript;
import data.scripts.campaign.specialist.UNGP_PlayerFleetMemberBuff;
import data.scripts.campaign.specialist.items.UNGP_RuleItem;
import data.scripts.campaign.specialist.rules.UNGP_RulesManager;
import data.scripts.utils.UNGP_LoadingChecker;

import static com.fs.starfarer.api.Global.getSettings;

public class UNGP_modPlugin extends BaseModPlugin {

    @Override
    public void onApplicationLoad() {
        UNGP_LoadingChecker.checkLoad();
        boolean hasLazyLib = getSettings().getModManager().isModEnabled("lw_lazylib");
        if (!hasLazyLib) {
            throw new RuntimeException("Unofficial New Game Plus requires LazyLib!");
        }
        UNGP_RulesManager.initOrReloadRules();
        UNGP_RuleItem.loadSprite();
    }

    @Override
    public void onNewGameAfterTimePass() {
    }

    @Override
    public void onGameLoad(boolean newGame) {
        addScriptsIfNeeded();
        UNGP_RulesManager.updateRulesCache();
        UNGP_CampaignPlugin.loadUIEntity();
    }

    private void addScriptsIfNeeded() {
        if (!Global.getSector().hasScript(UNGP_CampaignPlugin.class)) {
            new UNGP_CampaignPlugin();
        }
        UNGP_UITimeScript script = new UNGP_UITimeScript();
        UNGP_UITimeScript.setInstance(script);
        Global.getSector().addTransientScript(script);
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
        x.aliasAttribute(UNGP_InGameData.class, "timesToChangeSpecialistMode", "tTCSM");
        x.aliasAttribute(UNGP_InGameData.class, "changeTimeStat", "cTS");
//        x.aliasAttribute(UNGP_InGameData.class, "curCycle", "cC");
    }

    //    @Override
//    public void afterGameSave() {
//        UNGP_InGameData inGameData = UNGP_InGameData.getInstance();
//        if (inGameData.shouldDeleteRecordNextSave) {
//            inGameData.shouldDeleteRecordNextSave = false;
//            UNGP_InheritData.Delete();
//        }
//    }
}
