package data.scripts.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.ui.ValueDisplayMode;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.utils.SimpleI18n;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UNGP_InteractionDialog implements InteractionDialogPlugin {
    private enum OptionID {
        CHECK_INHERIT_DATA,
        CHECK_RECORD,
        HELP,

        SWITCH_MODE,
        INHERIT,

        START_RECORD,

        BACK_TO_MENU,
        LEAVE
    }

    private static final SimpleI18n.I18nSection i18n = new SimpleI18n.I18nSection("UNGP", "d", true);
    private InteractionDialogAPI dialog;
    private TextPanelAPI textPanel;
    private OptionPanelAPI options;
    private VisualPanelAPI visual;
    private SectorAPI sector;
    private UNGP_InGameData inGameData;
    private UNGP_InheritData lastInheritData;
    private UNGP_InheritData inheritData;
    private boolean isHardMode = false;

    private String inheritCreditsSelector = Misc.genUID();
    private String inheritBPSelector = Misc.genUID();
    private float creditsSelecterValue;
    private float bpSelecterValue;

    public UNGP_InteractionDialog(UNGP_InGameData inGameData) {
        this.inGameData = inGameData;
    }

    @Override
    public void init(InteractionDialogAPI dialog) {
        this.dialog = dialog;
        textPanel = dialog.getTextPanel();
        options = dialog.getOptionPanel();
        visual = dialog.getVisualPanel();
        sector = Global.getSector();
        //lastInheritData = UNGP_InheritData.Load();

        inheritData = UNGP_InheritData.GetInheritData(inGameData);
        initMenu();
        dialog.setOptionOnEscape(null, OptionID.LEAVE);
        dialog.setBackgroundDimAmount(0.4f);
        visual.showImagePortion("illustrations", "UNGP_logo", 400f, 300f, 0f, 0f, 400f, 300f);
    }

    private void initMenu() {
        textPanel.addPara(i18n.get("menu"));
        options.addOption(i18n.get("checkInherit"), OptionID.CHECK_INHERIT_DATA);
        options.addOption(i18n.get("checkRecord"), OptionID.CHECK_RECORD);
        if (inGameData.passedInheritTime) {
            textPanel.addPara(i18n.get("hasPassedTime"), Misc.getNegativeHighlightColor());
        }
        if (!inGameData.couldBeRecorded()) {
            options.setEnabled(OptionID.CHECK_RECORD, false);
            if (inGameData.isRecorded) {
                textPanel.addPara(i18n.get("hasRecorded"), Misc.getNegativeHighlightColor());
            }
            if (!inGameData.reachMaxLevel()) {
                textPanel.addPara(i18n.get("notMaxLevel"), Misc.getNegativeHighlightColor());
            }
        }
        textPanel.addPara(i18n.get("inGameData"),
                Misc.getHighlightColor(),
                "" + inGameData.curCycle,
                Misc.getDGSCredits(inheritData.inheritCredits),
                "" + inheritData.ships.size(),
                "" + inheritData.fighters.size(),
                "" + inheritData.weapons.size(),
                "" + inheritData.hullmods.size(),
                inheritData.isHardMode ? "Yes" : "No");
        options.addOption(i18n.get("help"), OptionID.HELP);
        addLeaveButton();
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        options.clearOptions();
        textPanel.clear();
        OptionID selectedOption = (OptionID) optionData;
        switch (selectedOption) {
            case CHECK_INHERIT_DATA:
                if (lastInheritData == null) {
                    lastInheritData = UNGP_InheritData.Load();
                }
                if (lastInheritData != null) {
                    textPanel.addPara(i18n.get("inheritData"),
                            Misc.getHighlightColor(),
                            "" + lastInheritData.cycle,
                            Misc.getDGSCredits(lastInheritData.inheritCredits),
                            "" + lastInheritData.ships.size(),
                            "" + lastInheritData.fighters.size(),
                            "" + lastInheritData.weapons.size(),
                            "" + lastInheritData.hullmods.size(),
                            lastInheritData.isHardMode ? "Yes" : "No");
                    if (!inGameData.passedInheritTime && !inGameData.inherited) {
                        options.addSelector(i18n.get("inheritCredits"), inheritCreditsSelector,
                                Misc.getButtonTextColor(), 250f, 50f,
                                0f, 75f, ValueDisplayMode.VALUE, null);
                        options.addSelector(i18n.get("inheritBPs"), inheritBPSelector,
                                Misc.getButtonTextColor(), 250f, 50f,
                                0f, 75f, ValueDisplayMode.VALUE, null);
                        if (lastInheritData.isHardMode) {
                            isHardMode = true;
                        }
                        options.addOption(i18n.get("switchHardMode") + "" + (isHardMode ? i18n.get("on") : i18n.get("off")), OptionID.SWITCH_MODE);
                        if (lastInheritData.isHardMode) {
                            options.setEnabled(OptionID.SWITCH_MODE, false);
                        }
                        options.addOption(i18n.get("startInherit"), OptionID.INHERIT);
                    } else {
                        if (inGameData.inherited) {
                            textPanel.addPara(i18n.get("hasInherited"), Misc.getNegativeHighlightColor());
                        } else {
                            textPanel.addPara(i18n.get("hasPassedTime"), Misc.getNegativeHighlightColor());
                        }
                    }
                } else {
                    textPanel.addPara(i18n.get("noInherit"), Misc.getNegativeHighlightColor());
                }

                addBackButton(OptionID.BACK_TO_MENU);
                break;
            case CHECK_RECORD:
                textPanel.addPara(i18n.get("recordInfo"));
                textPanel.addPara(i18n.get("inGameData"),
                        Misc.getHighlightColor(),
                        "" + inGameData.curCycle,
                        Misc.getDGSCredits(inheritData.inheritCredits),
                        "" + inheritData.ships.size(),
                        "" + inheritData.fighters.size(),
                        "" + inheritData.weapons.size(),
                        "" + inheritData.hullmods.size(),
                        inheritData.isHardMode ? "Yes" : "No");
                options.addOption(i18n.get("startRecord"), OptionID.START_RECORD);
                options.addOptionConfirmation(OptionID.START_RECORD, i18n.get("recordConfirmInfo"), i18n.get("confirm"), i18n.get("cancel"));
                addBackButton(OptionID.BACK_TO_MENU);
                break;
            case HELP:
                textPanel.addPara(i18n.get("helpInfo"));
                addBackButton(OptionID.BACK_TO_MENU);
                break;
            case SWITCH_MODE:
                isHardMode = !isHardMode;
                optionSelected(null, OptionID.CHECK_INHERIT_DATA);
                break;
            case INHERIT:
                inherit();
                addLeaveButton();
                break;
            case START_RECORD:
                inGameData.isRecorded = true;
                UNGP_InheritData.Save(inheritData);
                Global.getSoundPlayer().playUISound("ui_rep_raise", 1, 1);
                textPanel.addPara(i18n.get("recordSuccess"));
                addLeaveButton();
                break;
            case BACK_TO_MENU:
                initMenu();
                break;
            case LEAVE:
                dialog.dismiss();
                break;
        }

    }

    private void inherit() {
        int creditsInherited = (int) (lastInheritData.inheritCredits * creditsSelecterValue * 0.01f);
        sector.getPlayerFleet().getCargo().getCredits().add(creditsInherited);
        AddRemoveCommodity.addCreditsGainText(creditsInherited, textPanel);
        FactionAPI player = sector.getPlayerFaction();
        float inheritBPPercent = bpSelecterValue * 0.01f;
        int shipAmount = (int) (inheritBPPercent * lastInheritData.ships.size());
        int fighterAmount = (int) (inheritBPPercent * lastInheritData.fighters.size());
        int weaponAmount = (int) (inheritBPPercent * lastInheritData.weapons.size());
        int hullmodAmount = (int) (inheritBPPercent * lastInheritData.hullmods.size());
        List<String> curShipBps = new ArrayList<>();
        List<String> curFighterBps = new ArrayList<>();
        List<String> curWeaponBps = new ArrayList<>();
        List<String> curHullmodBps = new ArrayList<>();
        SettingsAPI setting = Global.getSettings();

        for (ShipHullSpecAPI spec : setting.getAllShipHullSpecs()) {
            curShipBps.add(spec.getHullId());
        }
        for (FighterWingSpecAPI spec : setting.getAllFighterWingSpecs()) {
            curFighterBps.add(spec.getId());
        }
        for (WeaponSpecAPI spec : setting.getAllWeaponSpecs()) {
            curWeaponBps.add(spec.getWeaponId());
        }
        for (HullModSpecAPI spec : setting.getAllHullModSpecs()) {
            curHullmodBps.add(spec.getId());
        }


        int inheritedShip = 0;
        int inheritedFighter = 0;
        int inheritedWeapon = 0;
        int inheritedHullmod = 0;

        WeightedRandomPicker<String> picker = new WeightedRandomPicker<>();
        for (String bp : lastInheritData.ships) {
            if (curShipBps.contains(bp) && !player.knowsShip(bp)) {
                picker.add(bp);
            }
        }
        for (int i = 0; i < shipAmount; i++) {
            if (picker.isEmpty()) break;
            String bpId = picker.pickAndRemove();
            player.addKnownShip(bpId, true);
            inheritedShip++;
        }
        picker = new WeightedRandomPicker<>();
        for (String bp : lastInheritData.fighters) {
            if (curFighterBps.contains(bp) && !player.knowsFighter(bp)) {
                picker.add(bp);
            }
        }
        for (int i = 0; i < fighterAmount; i++) {
            if (picker.isEmpty()) break;
            String bpId = picker.pickAndRemove();
            player.addKnownFighter(bpId, true);
            inheritedFighter++;
        }
        picker = new WeightedRandomPicker<>();
        for (String bp : lastInheritData.weapons) {
            if (curWeaponBps.contains(bp) && !player.knowsWeapon(bp)) {
                picker.add(bp);
            }
        }
        for (int i = 0; i < weaponAmount; i++) {
            if (picker.isEmpty()) break;
            String bpId = picker.pickAndRemove();
            player.addKnownWeapon(bpId, true);
            inheritedWeapon++;
        }
        picker = new WeightedRandomPicker<>();
        for (String bp : lastInheritData.hullmods) {
            if (curHullmodBps.contains(bp) && !player.knowsHullMod(bp)) {
                picker.add(bp);
            }
        }
        for (int i = 0; i < hullmodAmount; i++) {
            if (picker.isEmpty()) break;
            String bpId = picker.pickAndRemove();
            player.addKnownHullMod(bpId);
            inheritedHullmod++;
        }

        textPanel.setFontSmallInsignia();
        textPanel.addPara(i18n.get("inheritedBP"), Misc.getPositiveHighlightColor(), Misc.getHighlightColor(),
                "" + inheritedShip,
                "" + inheritedFighter,
                "" + inheritedWeapon,
                "" + inheritedHullmod);

        sector.getPlayerStats().addPoints(lastInheritData.cycle - 1);
        textPanel.addPara(i18n.get("inheritedPoints"), Misc.getPositiveHighlightColor(), Misc.getHighlightColor(),
                lastInheritData.cycle - 1 + "");
        textPanel.setFontInsignia();

        textPanel.addPara(i18n.get("hardModeYes"), Misc.getNegativeHighlightColor());

        textPanel.addPara(i18n.get("deleteRecord"), Misc.getNegativeHighlightColor());

        Global.getSoundPlayer().playUISound("ui_acquired_blueprint", 1, 1);

        inGameData.curCycle = lastInheritData.cycle;
        inGameData.inherited = true;
        inGameData.isHardMode = isHardMode;

        inGameData.shouldDeleteRecordNextSave = true;
    }

    @Override
    public void optionMousedOver(String optionText, Object optionData) {

    }

    @Override
    public void advance(float amount) {
        if (options.hasSelector(inheritCreditsSelector)) {
            creditsSelecterValue = options.getSelectorValue(inheritCreditsSelector);
        }
        if (options.hasSelector(inheritBPSelector)) {
            bpSelecterValue = options.getSelectorValue(inheritBPSelector);
        }
        if (options.hasOption(OptionID.INHERIT)) {
            int creditsInherited = (int) (lastInheritData.inheritCredits * creditsSelecterValue * 0.01f);
            int bpInherited = (int) ((lastInheritData.ships.size() + lastInheritData.fighters.size() + lastInheritData.weapons.size() + lastInheritData.hullmods.size()) * bpSelecterValue * 0.01f);
            String confirmText = i18n.format("inheritConfirmInfo0", "" + creditsInherited, "" + bpInherited);
            if (isHardMode) {
                confirmText += i18n.get("inheritConfirmInfo1");
            }
            options.addOptionConfirmation(OptionID.INHERIT, confirmText, i18n.get("confirm"), i18n.get("cancel"));
        }
    }

    @Override
    public void backFromEngagement(EngagementResultAPI battleResult) {

    }

    private void addLeaveButton() {
        options.addOption(i18n.get("leave"), OptionID.LEAVE);
    }

    private void addBackButton(OptionID warpOption) {
        options.addOption(i18n.get("back"), warpOption);
    }

    @Override
    public Object getContext() {
        return null;
    }

    @Override
    public Map<String, MemoryAPI> getMemoryMap() {
        return null;
    }
}
