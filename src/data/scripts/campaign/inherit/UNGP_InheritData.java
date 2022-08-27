package data.scripts.campaign.inherit;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.UNGP_InGameData;
import data.scripts.campaign.specialist.UNGP_SpecialistSettings.Difficulty;
import data.scripts.campaign.specialist.challenges.UNGP_ChallengeInfo;
import data.scripts.campaign.specialist.challenges.UNGP_ChallengeManager;
import data.scripts.campaign.specialist.rules.UNGP_RulesManager;
import data.scripts.ungpsaves.UNGP_DataSaverAPI;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static data.scripts.utils.Constants.root_i18n;

public class UNGP_InheritData {
    private static final List<UNGP_DataSaverAPI> SAVER_INSTANCES = new ArrayList<>();
    public static final String DEFAULT_NAME = "[Anonymous]";
    public static final String BULLETED_PREFIX = "       ";

    public String ungp_id;
    public String lastPlayerName;
    public int cycle;
    //    public boolean isHardMode;
    public List<String> completedChallenges;
    public List<UNGP_DataSaverAPI> dataSavers;

    /**
     * 创建一个可被记录的重生点
     *
     * @param inGameData
     * @return
     */
    public static UNGP_InheritData createInheritData(UNGP_InGameData inGameData) {
        UNGP_InheritData inheritData = new UNGP_InheritData();
        inheritData.ungp_id = UUID.randomUUID().toString();
        inheritData.lastPlayerName = Global.getSector().getPlayerPerson().getNameString();
        inheritData.cycle = inGameData.getCurCycle() + 1;
        inheritData.completedChallenges = new ArrayList<>(inGameData.getCompletedChallenges());
        inheritData.dataSavers = new ArrayList<>();
        for (UNGP_DataSaverAPI saverInstance : SAVER_INSTANCES) {
            inheritData.dataSavers.add(saverInstance.createSaverBasedOnCurrentGame(inGameData));
        }
        return inheritData;
    }

    /**
     * An empty data that for new players who want to use Specialist mode directly.
     *
     * @return
     */
    public static UNGP_InheritData createEmptyData() {
        UNGP_InheritData inheritData = new UNGP_InheritData();
        inheritData.ungp_id = UUID.randomUUID().toString();
        inheritData.lastPlayerName = root_i18n.get("defaultSlotName");
        inheritData.cycle = 1;
//        inheritData.isHardMode = false;
        inheritData.completedChallenges = new ArrayList<>();
        inheritData.dataSavers = new ArrayList<>();
        for (UNGP_DataSaverAPI saverInstance : SAVER_INSTANCES) {
            inheritData.dataSavers.add(saverInstance.createEmptySaver());
        }
        return inheritData;
    }

    public void addInheritTooltip(TooltipMakerAPI root) {
        addDescriptionTooltip(root, "inheritData");
    }

    public void addRecordTooltip(TooltipMakerAPI root, Difficulty difficulty) {
        addDescriptionTooltip(root, "inGameData");
//        if (isHardMode) {
//            TooltipMakerAPI section = root.beginImageWithText(difficulty.spritePath, 24f);
//            section.addPara(d_i18n.get("specialistMode"), Misc.getNegativeHighlightColor(), 0f);
//            section.addPara(d_i18n.get("hardmodeLevel") + ": %s", 0f, Misc.getGrayColor(),
//                            Misc.getHighlightColor(), difficulty.name);
//            root.addImageWithText(10f);
//        }
    }

    public void addDescriptionTooltip(TooltipMakerAPI root, String descKey) {
        TooltipMakerAPI section;
        Color hl = Misc.getHighlightColor();
        // Cycle Name
        if (!lastPlayerName.contentEquals(UNGP_InheritData.DEFAULT_NAME)) {
            section = root.beginImageWithText("graphics/icons/reports/officers24.png", 24f);
            section.addPara(root_i18n.get(descKey + "_0") + "%s", 0f, hl, "" + (cycle));
            section.addPara(root_i18n.get(descKey + "_1") + "%s", 3f, hl, lastPlayerName);
            root.addImageWithText(5f);
        }
//        // Credits
//        section = root.beginImageWithText("graphics/icons/reports/generic_income.png", 24f);
//        section.addPara(d_i18n.get(descKey + "_2"), 3f);
//        root.addImageWithText(5f);
//        root.addPara(BULLETED_PREFIX + Misc.getDGSCredits(inheritCredits), hl, 5f);
        // Savers
        for (UNGP_DataSaverAPI dataSaver : dataSavers) {
            dataSaver.addSaverInfo(root, descKey);
        }
        // Completed challenges
        section = root.beginImageWithText("graphics/icons/reports/hazard_pay2.png", 24f);
        section.addPara(root_i18n.get(descKey + "_4"), 3f);
        root.addImageWithText(5f);
        root.setBulletedListMode(BULLETED_PREFIX);
        if (completedChallenges.isEmpty()) {
            root.addPara(root_i18n.get("challenge_empty"), Misc.getGrayColor(), 5f);
        } else {
            for (String challengeId : completedChallenges) {
                UNGP_ChallengeInfo challengeInfo = UNGP_ChallengeManager.getChallengeInfo(challengeId);
                if (challengeInfo != null) {
                    root.addPara(challengeInfo.getName(), UNGP_RulesManager.getMilestoneColor(), 5f);
                }
            }
        }
        root.setBulletedListMode(null);
    }

    public Color getColorByCycle() {
        if (cycle < 5) {
            return Misc.getButtonTextColor();
        } else if (cycle < 10) {
            return Difficulty.GAMMA.color;
        } else if (cycle < 25) {
            return Difficulty.BETA.color;
        } else if (cycle < 50) {
            return Difficulty.ALPHA.color;
        } else if (cycle < 100) {
            return Difficulty.OMEGA.color;
        } else {
            return UNGP_RulesManager.getGoldenColor();
        }
    }

    public String getPrefixByCycle() {
        if (cycle < 5) {
            return "";
        } else if (cycle < 10) {
            return "γ - ";
        } else if (cycle < 25) {
            return "β - ";
        } else if (cycle < 50) {
            return "α - ";
        } else if (cycle < 100) {
            return "Ω - ";
        } else {
            return "Ψ - ";
        }
    }

    public <T extends UNGP_DataSaverAPI> T getFirstSaverOfClass(Class<T> saverClass) {
        for (UNGP_DataSaverAPI dataSaver : dataSavers) {
            if (saverClass.isInstance(dataSaver)) {
                return (T) dataSaver;
            }
        }
        return null;
    }

    public static List<UNGP_DataSaverAPI> getSaverInstancesCopy() {
        return new ArrayList<>(SAVER_INSTANCES);
    }

    private static final String SAVER_CLASS_PATH = "data/campaign/UNGP_dataSavers.csv";

    /**
     * Haven't completed yet.
     */
    public static void loadAllSavers() {
        try {
            ClassLoader classLoader = Global.getSettings().getScriptClassLoader();
            JSONArray ruleInfos = Global.getSettings().getMergedSpreadsheetDataForMod("id", SAVER_CLASS_PATH, "ungp");
            for (int i = 0, len = ruleInfos.length(); i < len; i++) {
                JSONObject row = ruleInfos.getJSONObject(i);
                String saverClassName = row.getString("saverClass");
                Class<?> saverClass = classLoader.loadClass(saverClassName);
                SAVER_INSTANCES.add((UNGP_DataSaverAPI) saverClass.newInstance());
            }
        } catch (Exception e) {
            Global.getLogger(UNGP_InheritData.class).error(e);
            throw new RuntimeException("Failed to load UNGP data savers:", e);
        }
    }
}
