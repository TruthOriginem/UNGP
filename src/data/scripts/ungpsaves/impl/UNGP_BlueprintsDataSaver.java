package data.scripts.ungpsaves.impl;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.campaign.UNGP_InGameData;
import data.scripts.campaign.inherit.UNGP_InheritData;
import data.scripts.ungpsaves.UNGP_DataSaverAPI;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static data.scripts.campaign.UNGP_Settings.d_i18n;

public class UNGP_BlueprintsDataSaver implements UNGP_DataSaverAPI {

    public List<String> fighters = new ArrayList<>();
    public List<String> ships = new ArrayList<>();
    public List<String> weapons = new ArrayList<>();
    public List<String> hullmods = new ArrayList<>();


    @Override
    public UNGP_DataSaverAPI createSaverBasedOnCurrentGame(UNGP_InGameData inGameData) {
        UNGP_BlueprintsDataSaver dataSaver = new UNGP_BlueprintsDataSaver();
        FactionAPI playerFaction = Global.getSector().getPlayerFaction();
        dataSaver.ships = new ArrayList<>(playerFaction.getKnownShips());
        dataSaver.fighters = new ArrayList<>(playerFaction.getKnownFighters());
        dataSaver.weapons = new ArrayList<>(playerFaction.getKnownWeapons());
        dataSaver.hullmods = new ArrayList<>(playerFaction.getKnownHullMods());
        return dataSaver;
    }

    @Override
    public UNGP_DataSaverAPI createEmptySaver() {
        return new UNGP_BlueprintsDataSaver();
    }

    @Override
    public void loadDataFromSavepointSlot(JSONObject jsonObject) throws JSONException {
        ships.clear();
        fighters.clear();
        weapons.clear();
        hullmods.clear();
        JSONArray array;
        array = jsonObject.optJSONArray("ships");
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                ships.add(array.getString(i));
            }
        }

        array = jsonObject.optJSONArray("fighters");
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                fighters.add(array.getString(i));
            }
        }

        array = jsonObject.optJSONArray("weapons");
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                weapons.add(array.getString(i));
            }
        }

        array = jsonObject.optJSONArray("hullmods");
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                hullmods.add(array.getString(i));
            }
        }
    }

    @Override
    public void saveDataToSavepointSlot(JSONObject jsonObject) throws JSONException {
        jsonObject.put("ships", ships);
        jsonObject.put("fighters", fighters);
        jsonObject.put("weapons", weapons);
        jsonObject.put("hullmods", hullmods);
    }

    @Override
    public void startInheritDataFromSaver(TooltipMakerAPI root, Map<String, Object> params) {
        FactionAPI player = Global.getSector().getPlayerFaction();
        float inheritBPPercent = (float) params.get("inheritBPPercent");
        int shipAmount = (int) (inheritBPPercent * ships.size());
        int fighterAmount = (int) (inheritBPPercent * fighters.size());
        int weaponAmount = (int) (inheritBPPercent * weapons.size());
        int hullmodAmount = (int) (inheritBPPercent * hullmods.size());
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
        for (String bp : ships) {
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
        for (String bp : fighters) {
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
        for (String bp : weapons) {
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
        for (String bp : hullmods) {
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

        root.setParaSmallInsignia();
        root.addPara(d_i18n.get("inheritedBP"), 0f, Misc.getPositiveHighlightColor(), Misc.getHighlightColor(),
                     "" + inheritedShip,
                     "" + inheritedFighter,
                     "" + inheritedWeapon,
                     "" + inheritedHullmod);
    }

    @Override
    public void addSaverInfo(TooltipMakerAPI root, String descKey) {
        TooltipMakerAPI section = root.beginImageWithText("graphics/icons/reports/exports24.png", 24f);
        section.addPara(d_i18n.get(descKey + "_3"), 3f);
        root.addImageWithText(5f);
        root.addPara(d_i18n.get("data_bps"), 5f, Misc.getHighlightColor(),
                     UNGP_InheritData.BULLETED_PREFIX + ships.size(),
                     UNGP_InheritData.BULLETED_PREFIX + fighters.size(),
                     UNGP_InheritData.BULLETED_PREFIX + weapons.size(),
                     UNGP_InheritData.BULLETED_PREFIX + hullmods.size());
    }
}
