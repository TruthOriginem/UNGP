package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.thoughtworks.xstream.XStream;
import data.scripts.campaign.UNGP_CampaignPlugin;
import data.scripts.campaign.hardmode.UNGP_RulesManager;

import static com.fs.starfarer.api.Global.getSettings;

public class UNGP_modPlugin extends BaseModPlugin {
    @Override
    public void onApplicationLoad() {
        boolean hasLazyLib = getSettings().getModManager().isModEnabled("lw_lazylib");
        if (!hasLazyLib) {
            throw new RuntimeException("Unofficial New Game Plus requires LazyLib!");
        }
        UNGP_RulesManager.initOrReloadRules();
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
