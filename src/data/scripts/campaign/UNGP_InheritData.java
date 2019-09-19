package data.scripts.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import org.json.JSONArray;
import org.lazywizard.lazylib.JSONUtils;
import org.lazywizard.lazylib.JSONUtils.CommonDataJSONObject;

import java.util.ArrayList;
import java.util.List;

public class UNGP_InheritData {
    private static final String FILE_NAME = "UNGP_inherit";

    public int cycle;
    public boolean isHardMode;
    public int inheritCredits;
    public List<String> fighters;
    public List<String> ships;
    public List<String> weapons;
    public List<String> hullmods;

    public static UNGP_InheritData GetInheritData(UNGP_InGameData inGameData) {
        UNGP_InheritData inheritData = new UNGP_InheritData();
        inheritData.cycle = inGameData.curCycle + 1;
        inheritData.isHardMode = inGameData.isHardMode;
        inheritData.inheritCredits = (int) Global.getSector().getPlayerFleet().getCargo().getCredits().get();
        FactionAPI playerFaction = Global.getSector().getPlayerFaction();
        inheritData.ships = new ArrayList<>(playerFaction.getKnownShips());
        inheritData.fighters = new ArrayList<>(playerFaction.getKnownFighters());
        inheritData.weapons = new ArrayList<>(playerFaction.getKnownWeapons());
        inheritData.hullmods = new ArrayList<>(playerFaction.getKnownHullMods());

        return inheritData;

    }

    public static boolean Save(UNGP_InheritData inheritData) {
        try {
            CommonDataJSONObject jsonObject = new CommonDataJSONObject(FILE_NAME);
            jsonObject.put("cycle", inheritData.cycle);
            jsonObject.put("isHardMode", inheritData.isHardMode);
            jsonObject.put("inheritCredits", inheritData.inheritCredits);
            jsonObject.put("ships", inheritData.ships);
            jsonObject.put("fighters", inheritData.fighters);
            jsonObject.put("weapons", inheritData.weapons);
            jsonObject.put("hullmods", inheritData.hullmods);

            jsonObject.save();
            return true;

        } catch (Exception ignored) {
            return false;
        }
    }

    public static UNGP_InheritData Load() {
        UNGP_InheritData inheritData = new UNGP_InheritData();
        try {
            CommonDataJSONObject jsonObject = JSONUtils.loadCommonJSON(FILE_NAME);
            if (!jsonObject.has("cycle")) {
                return null;
            }
            inheritData.cycle = jsonObject.getInt("cycle");
            inheritData.isHardMode = jsonObject.getBoolean("isHardMode");
            inheritData.inheritCredits = jsonObject.getInt("inheritCredits");
            JSONArray array;
            ArrayList<String> arrayList;

            array = jsonObject.getJSONArray("ships");
            arrayList = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                arrayList.add(array.getString(i));
            }
            inheritData.ships = new ArrayList<>(arrayList);

            array = jsonObject.getJSONArray("fighters");
            arrayList = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                arrayList.add(array.getString(i));
            }
            inheritData.fighters = new ArrayList<>(arrayList);

            array = jsonObject.getJSONArray("weapons");
            arrayList = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                arrayList.add(array.getString(i));
            }
            inheritData.weapons = new ArrayList<>(arrayList);


            array = jsonObject.getJSONArray("hullmods");
            arrayList = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                arrayList.add(array.getString(i));
            }
            inheritData.hullmods = new ArrayList<>(arrayList);

            return inheritData;

        } catch (Exception ignored) {
            return null;
        }

    }

    public static void Delete() {
        if (Global.getSettings().fileExistsInCommon(FILE_NAME))
            Global.getSettings().deleteTextFileFromCommon(FILE_NAME);
    }
}
