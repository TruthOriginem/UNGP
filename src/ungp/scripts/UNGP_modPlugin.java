package ungp.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.campaign.missions.cb.MilitaryCustomBounty;
import com.fs.starfarer.api.impl.campaign.missions.cb.UnderworldCustomBounty;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.collections.MapConverter;
import ungp.scripts.campaign.UNGP_InGameData;
import ungp.scripts.campaign.UNGP_Settings;
import ungp.scripts.campaign.UNGP_SharedData;
import ungp.scripts.campaign.background.UNGP_BackgroundManager;
import ungp.scripts.campaign.everyframe.UNGP_CampaignPlugin;
import ungp.scripts.campaign.everyframe.UNGP_SpecialistWidgetPlugin;
import ungp.scripts.campaign.everyframe.UNGP_UITimeScript;
import ungp.scripts.campaign.inherit.UNGP_InheritData;
import ungp.scripts.campaign.specialist.UNGP_PlayerFleetMemberBuff;
import ungp.scripts.campaign.specialist.UNGP_SpecialistManager;
import ungp.scripts.campaign.specialist.challenges.UNGP_ChallengeManager;
import ungp.scripts.campaign.specialist.listeners.UNGP_CampaignEventListener;
import ungp.scripts.campaign.specialist.rules.UNGP_RulesManager;
import ungp.scripts.codex.UNGP_CodexManager;
import ungp.scripts.ui.UNGPFont;

import java.util.WeakHashMap;

public class UNGP_modPlugin extends BaseModPlugin {

    @Override
    public void onApplicationLoad() {
        UNGP_Settings.loadSettings();

        UNGPFont.init();
        UNGP_InheritData.loadAllSavers();
        UNGP_BackgroundManager.loadAllBackgrounds();
        // 规则与挑战
        UNGP_RulesManager.initOrReloadRules();
        UNGP_ChallengeManager.initOrReloadChallengeInfos();
        UNGP_RulesManager.tagAllChallengeProviders();
    }

    @Override
    public void onGameLoad(boolean newGame) {
        UNGP_SpecialistManager.resetCache();
        UNGP_SharedData.initialize();
        addScriptsIfNeeded();
        UNGP_RulesManager.updateRulesCache();
    }

    private void addScriptsIfNeeded() {
        if (!Global.getSector().hasScript(UNGP_CampaignPlugin.class)) {
            Global.getSector().addScript(new UNGP_CampaignPlugin());
        }
        Global.getSector().addTransientScript(new UNGP_UITimeScript());
        Global.getSector().addTransientListener(new UNGP_CampaignEventListener());

        if (UNGP_Settings.isLeftTopSpecialistWidgetShown()) {
            Global.getSector().addTransientScript(new UNGP_SpecialistWidgetPlugin());
        }

        if (!Global.getSector().getCharacterData().getAbilities().contains("UNGP_checkSavepoint")) {
            Global.getSector().getCharacterData().addAbility("UNGP_checkSavepoint");
        }
    }

    @Override
    public void onAboutToStartGeneratingCodex() {
        UNGP_CodexManager.init();
    }

    @Override
    public void onAboutToLinkCodexEntries() {
        UNGP_CodexManager.setRelatedEntries();
    }

    @Override
    public void configureXStream(XStream x) {
        x.registerConverter(new MapConverter(x.getMapper(), WeakHashMap.class), XStream.PRIORITY_LOW);
        registerLegacyBountyAlias(x, "ungp_militaryBounty", MilitaryCustomBounty.class);
        registerLegacyBountyAlias(x, "ungp_underworldBounty", UnderworldCustomBounty.class);
        x.alias("ungp_camplugin", UNGP_CampaignPlugin.class);
        x.aliasAttribute(UNGP_CampaignPlugin.class, "inheritChecker", "ic");
        x.aliasAttribute(UNGP_CampaignPlugin.class, "oneDayChecker", "odc");
        x.aliasAttribute(UNGP_CampaignPlugin.class, "oneYearChecker", "oyc");
        x.aliasAttribute(UNGP_CampaignPlugin.class, "newGameCheckDays", "ngcd");
        x.aliasAttribute(UNGP_CampaignPlugin.class, "newGameChecked", "ngc");
        x.aliasAttribute(UNGP_CampaignPlugin.class, "shouldShowDialog", "ssd");

        // UNGP_SharedData 会被写进 persistentData；没有别名时存档里以完整类名落盘，
        // 一旦改包名/类名就会静默丢失全部规则运行时数据
        x.alias("ungp_sharedData", UNGP_SharedData.class);
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
        x.aliasAttribute(UNGP_InGameData.class, "blockCombatRulesInNextBattle", "bCRNB");
    }

    private void registerLegacyBountyAlias(XStream stream, String legacyAlias, Class<?> bountyClass) {
        String currentAlias = stream.getMapper().serializedClass(bountyClass);
        stream.alias(legacyAlias, bountyClass);
        stream.alias(currentAlias, bountyClass);
    }
}
