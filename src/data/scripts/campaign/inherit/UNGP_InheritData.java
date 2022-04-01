package data.scripts.campaign.inherit;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.UNGP_InGameData;
import data.scripts.campaign.specialist.UNGP_SpecialistSettings.Difficulty;
import data.scripts.campaign.specialist.challenges.UNGP_ChallengeInfo;
import data.scripts.campaign.specialist.challenges.UNGP_ChallengeManager;
import data.scripts.campaign.specialist.rules.UNGP_RulesManager;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static data.scripts.campaign.UNGP_Settings.d_i18n;

public class UNGP_InheritData {
    public static final String DEFAULT_NAME = "[Anonymous]";

    public String ungp_id;
    public String lastPlayerName;
    public int cycle;
    public boolean isHardMode;
    public int inheritCredits;
    public List<String> fighters;
    public List<String> ships;
    public List<String> weapons;
    public List<String> hullmods;
    public List<String> completedChallenges;

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
        inheritData.isHardMode = inGameData.isHardMode();
        inheritData.inheritCredits = (int) Global.getSector().getPlayerFleet().getCargo().getCredits().get();
        FactionAPI playerFaction = Global.getSector().getPlayerFaction();
        inheritData.ships = new ArrayList<>(playerFaction.getKnownShips());
        inheritData.fighters = new ArrayList<>(playerFaction.getKnownFighters());
        inheritData.weapons = new ArrayList<>(playerFaction.getKnownWeapons());
        inheritData.hullmods = new ArrayList<>(playerFaction.getKnownHullMods());
        inheritData.completedChallenges = new ArrayList<>(inGameData.getCompletedChallenges());

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
        inheritData.lastPlayerName = "UNGP-NewFound";
        inheritData.cycle = 0;
        inheritData.isHardMode = false;
        inheritData.inheritCredits = 0;
        inheritData.ships = new ArrayList<>();
        inheritData.fighters = new ArrayList<>();
        inheritData.weapons = new ArrayList<>();
        inheritData.hullmods = new ArrayList<>();
        inheritData.completedChallenges = new ArrayList<>();
        return inheritData;
    }

    public void addInheritTooltip(TooltipMakerAPI root) {
        addDescriptionTooltip(root, "inheritData");
    }

    public void addRecordTooltip(TooltipMakerAPI root, Difficulty difficulty) {
        addDescriptionTooltip(root, "inGameData");
        if (isHardMode) {
            TooltipMakerAPI section = root.beginImageWithText(difficulty.spritePath, 24f);
            section.addPara(d_i18n.get("specialistMode"), Misc.getNegativeHighlightColor(), 0f);
            section.addPara(d_i18n.get("hardmodeLevel") + ": %s", 0f, Misc.getGrayColor(),
                            Misc.getHighlightColor(), difficulty.name);
            root.addImageWithText(10f);
        }
    }

    public void addDescriptionTooltip(TooltipMakerAPI root, String descKey) {
        TooltipMakerAPI section;
        Color hl = Misc.getHighlightColor();
        String bulletedPrefix = "       ";
        // Cycle Name
        if (!lastPlayerName.equals(UNGP_InheritData.DEFAULT_NAME)) {
            section = root.beginImageWithText("graphics/icons/reports/officers24.png", 24f);
            section.addPara(d_i18n.get(descKey + "_0") + "%s", 0f, hl, "" + (cycle - 1));
            section.addPara(d_i18n.get(descKey + "_1") + "%s", 3f, hl, lastPlayerName);
            root.addImageWithText(5f);
        }
        // Credits
        section = root.beginImageWithText("graphics/icons/reports/generic_income.png", 24f);
        section.addPara(d_i18n.get(descKey + "_2"), 3f);
        root.addImageWithText(5f);
        root.addPara(bulletedPrefix + Misc.getDGSCredits(inheritCredits), hl, 5f);
        // BPs
        section = root.beginImageWithText("graphics/icons/reports/exports24.png", 24f);
        section.addPara(d_i18n.get(descKey + "_3"), 3f);
        root.addImageWithText(5f);
        root.addPara(d_i18n.get("data_bps"), 5f, hl,
                     bulletedPrefix + ships.size(),
                     bulletedPrefix + fighters.size(),
                     bulletedPrefix + weapons.size(),
                     bulletedPrefix + hullmods.size());
        // Completed challenges
        section = root.beginImageWithText("graphics/icons/reports/hazard_pay2.png", 24f);
        section.addPara(d_i18n.get(descKey + "_4"), 3f);
        root.addImageWithText(5f);
        root.setBulletedListMode(bulletedPrefix);
        if (completedChallenges.isEmpty()) {
            root.addPara(d_i18n.get("challenge_empty"), Misc.getGrayColor(), 5f);
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
}
