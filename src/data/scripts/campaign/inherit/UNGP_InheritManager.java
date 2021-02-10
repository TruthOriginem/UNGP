package data.scripts.campaign.inherit;

import com.fs.starfarer.api.Global;
import org.json.JSONArray;
import org.lazywizard.lazylib.JSONUtils;

import java.util.ArrayList;

import static data.scripts.campaign.inherit.UNGP_InheritData.DEFAULT_NAME;

/**
 * 管理三个重生点槽位
 */
public class UNGP_InheritManager {
    public static UNGP_InheritData InheritData_slot0 = null;
    public static UNGP_InheritData InheritData_slot1 = null;
    public static UNGP_InheritData InheritData_slot2 = null;
    private static final String FILE_NAME_PREFIX = "UNGP_inherit";


    public static void LoadAllSlots() {
        InheritData_slot0 = Load(0);
        InheritData_slot1 = Load(1);
        InheritData_slot2 = Load(2);
    }

    public static void ClearSlots() {
        InheritData_slot0 = null;
        InheritData_slot1 = null;
        InheritData_slot2 = null;
    }

    /***
     * 获取指定槽位的data
     * @param slotID
     * @return
     */
    public static UNGP_InheritData Get(int slotID) {
        if (slotID == 0) {
            return InheritData_slot0;
        } else if (slotID == 1) {
            return InheritData_slot1;
        } else if (slotID == 2) {
            return InheritData_slot2;
        } else {
            return null;
        }
    }

    /***
     * 保存重生点到指定槽位
     * @param inheritData
     * @param slotID
     */
    public static void Save(UNGP_InheritData inheritData, int slotID) {
        try {
            JSONUtils.CommonDataJSONObject jsonObject = new JSONUtils.CommonDataJSONObject(getSaveFileName(slotID));
            jsonObject.put("ungp_id", inheritData.ungp_id);
            jsonObject.put("lastPlayerName", inheritData.lastPlayerName);
            jsonObject.put("cycle", inheritData.cycle);
            jsonObject.put("isHardMode", inheritData.isHardMode);
            jsonObject.put("inheritCredits", inheritData.inheritCredits);
            jsonObject.put("ships", inheritData.ships);
            jsonObject.put("fighters", inheritData.fighters);
            jsonObject.put("weapons", inheritData.weapons);
            jsonObject.put("hullmods", inheritData.hullmods);

            jsonObject.save();
        } catch (Exception ignored) {
        }
    }

    /***
     * 读取指定槽位的重生点
     * @param slotID
     * @return
     */
    public static UNGP_InheritData Load(int slotID) {
        UNGP_InheritData inheritData = new UNGP_InheritData();
        try {
            JSONUtils.CommonDataJSONObject jsonObject = JSONUtils.loadCommonJSON(getSaveFileName(slotID));
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

    public static boolean SavePointsExist() {
        return Global.getSettings().fileExistsInCommon(getSaveFileName(0)) ||
                Global.getSettings().fileExistsInCommon(getSaveFileName(1)) ||
                Global.getSettings().fileExistsInCommon(getSaveFileName(2));
    }

    private static String getSaveFileName(int slotID) {
        String fileName = FILE_NAME_PREFIX;
        if (slotID > 0) {
            fileName = fileName + slotID;
        }
        return fileName;
    }
}
