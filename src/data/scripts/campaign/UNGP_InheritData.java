package data.scripts.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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


    public static UNGP_InheritData CreateInheritData(UNGP_InGameData inGameData) {
        UNGP_InheritData inheritData = new UNGP_InheritData();
        inheritData.ungp_id = UUID.randomUUID().toString();
        inheritData.lastPlayerName = Global.getSector().getPlayerPerson().getNameString();
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

    /*    private static String getSaveFileName() {
        String fileName = FILE_NAME_PREFIX;
        if (CURRENT_SLOT_ID > 0) {
            fileName = fileName + CURRENT_SLOT_ID;
        }
        return fileName;
    }

   public static void Save(UNGP_InheritData inheritData) {
        UNGP_InheritManager.Save(inheritData, CURRENT_SLOT_ID);
    }

    public static UNGP_InheritData Load() {
        UNGP_InheritData inheritData = new UNGP_InheritData();
        try {
            CommonDataJSONObject jsonObject = JSONUtils.loadCommonJSON(getSaveFileName());
            if (!jsonObject.has("cycle")) {
                return null;
            }
            inheritData.ungp_id = jsonObject.optString("ungp_id", "[Empty]");
            inheritData.lastPlayerName = jsonObject.optString("lastPlayerName", DEFAULT_NAME);
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
        if (InheritDataExists())
            Global.getSettings().deleteTextFileFromCommon(getSaveFileName());
    }

    public static boolean InheritDataExists() {
        return Global.getSettings().fileExistsInCommon(getSaveFileName());
    }*/
}
