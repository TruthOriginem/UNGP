package ungp.impl.saves;

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
import ungp.scripts.campaign.UNGP_InGameData;
import ungp.scripts.campaign.inherit.UNGP_InheritData;
import ungp.api.saves.UNGP_DataSaverAPI;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static ungp.scripts.utils.Constants.root_i18n;

/**
 * 蓝图数据保存器，用于保存和继承玩家的舰船、战机、武器和船体改装的蓝图信息
 * Blueprint data saver for saving and inheriting player's ship, fighter, weapon and hullmod blueprint information
 */
public class UNGP_BlueprintsDataSaver implements UNGP_DataSaverAPI {

    // 各种蓝图列表
    public List<String> fighters = new ArrayList<>();
    public List<String> ships = new ArrayList<>();
    public List<String> weapons = new ArrayList<>();
    public List<String> hullmods = new ArrayList<>();

    // 蓝图总数
    public int bpAmount = 0;

    /**
     * 基于当前游戏状态创建数据保存器
     *
     * @param inGameData 当前游戏数据
     * @return 新的数据保存器实例
     */
    @Override
    public UNGP_DataSaverAPI createSaverBasedOnCurrentGame(UNGP_InGameData inGameData) {
        UNGP_BlueprintsDataSaver dataSaver = new UNGP_BlueprintsDataSaver();
        FactionAPI playerFaction = Global.getSector().getPlayerFaction();
        dataSaver.ships = new ArrayList<>(playerFaction.getKnownShips());
        dataSaver.fighters = new ArrayList<>(playerFaction.getKnownFighters());
        dataSaver.weapons = new ArrayList<>(playerFaction.getKnownWeapons());
        dataSaver.hullmods = new ArrayList<>(playerFaction.getKnownHullMods());
        dataSaver.updateBlueprintAmount();
        return dataSaver;
    }

    /**
     * 更新蓝图总数
     */
    public void updateBlueprintAmount() {
        bpAmount = ships.size() +
                fighters.size() +
                weapons.size() +
                hullmods.size();
    }

    /**
     * 将另一个蓝图数据保存器的内容添加到当前实例中
     *
     * @param other 另一个蓝图数据保存器
     * @return 新增的蓝图数量
     */
    public int add(UNGP_BlueprintsDataSaver other) {
        int lastBpAmount = bpAmount;
        // 使用removeAll和addAll确保不会重复添加相同的蓝图
        fighters.removeAll(other.fighters);
        fighters.addAll(other.fighters);
        ships.removeAll(other.ships);
        ships.addAll(other.ships);
        weapons.removeAll(other.weapons);
        weapons.addAll(other.weapons);
        hullmods.removeAll(other.hullmods);
        hullmods.addAll(other.hullmods);
        updateBlueprintAmount();
        return bpAmount - lastBpAmount;
    }

    /**
     * 创建空的数据保存器
     *
     * @return 空的数据保存器实例
     */
    @Override
    public UNGP_DataSaverAPI createEmptySaver() {
        return new UNGP_BlueprintsDataSaver();
    }

    /**
     * 从存档点槽位加载数据
     *
     * @param jsonObject 包含数据的JSON对象
     * @throws JSONException JSON解析异常
     */
    @Override
    public void loadDataFromSavepointSlot(JSONObject jsonObject) throws JSONException {
        // 清空现有数据
        ships.clear();
        fighters.clear();
        weapons.clear();
        hullmods.clear();

        // 从JSON对象中加载各种蓝图数据
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
        updateBlueprintAmount();
    }

    /**
     * 将数据保存到存档点槽位
     *
     * @param jsonObject 用于保存数据的JSON对象
     * @throws JSONException JSON处理异常
     */
    @Override
    public void saveDataToSavepointSlot(JSONObject jsonObject) throws JSONException {
        jsonObject.put("ships", ships);
        jsonObject.put("fighters", fighters);
        jsonObject.put("weapons", weapons);
        jsonObject.put("hullmods", hullmods);
    }

    /**
     * 从保存器开始继承数据
     *
     * @param root   用于显示继承信息的工具提示制造器
     * @param params 包含继承参数的映射
     */
    @Override
    public void startInheritDataFromSaver(TooltipMakerAPI root, Map<String, Object> params) {
        FactionAPI player = Global.getSector().getPlayerFaction();
        float inheritBPFactor = (float) params.get("inheritBPFactor");

        // 计算每种蓝图需要继承的数量
        int shipAmount = (int) (inheritBPFactor * ships.size());
        int fighterAmount = (int) (inheritBPFactor * fighters.size());
        int weaponAmount = (int) (inheritBPFactor * weapons.size());
        int hullmodAmount = (int) (inheritBPFactor * hullmods.size());

        // 获取当前游戏中所有可用的蓝图
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

        // 统计实际继承的蓝图数量
        int inheritedShip = 0;
        int inheritedFighter = 0;
        int inheritedWeapon = 0;
        int inheritedHullmod = 0;

        // 继承舰船蓝图
        WeightedRandomPicker<String> picker = new WeightedRandomPicker<>();
        for (String bp : ships) {
            // 只继承当前游戏中存在且玩家尚未拥有的蓝图
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

        // 继承战机蓝图
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

        // 继承武器蓝图
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

        // 继承船体改装蓝图
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

        // 显示继承结果
        root.setParaSmallInsignia();
        root.addPara(root_i18n.get("inheritedBP"), 0f, Misc.getPositiveHighlightColor(), Misc.getHighlightColor(),
                     "" + inheritedShip,
                     "" + inheritedFighter,
                     "" + inheritedWeapon,
                     "" + inheritedHullmod);
    }

    /**
     * 添加保存器信息到工具提示
     *
     * @param root    用于添加信息的工具提示制造器
     * @param descKey 描述键
     */
    @Override
    public void addSaverInfo(TooltipMakerAPI root, String descKey) {
        TooltipMakerAPI section = root.beginImageWithText("graphics/icons/reports/exports24.png", 24f);
        section.addPara(root_i18n.get(descKey + "_3"), 3f);
        root.addImageWithText(5f);
        root.addPara(root_i18n.get("data_bps"), 5f, Misc.getHighlightColor(),
                     UNGP_InheritData.BULLETED_PREFIX + ships.size(),
                     UNGP_InheritData.BULLETED_PREFIX + fighters.size(),
                     UNGP_InheritData.BULLETED_PREFIX + weapons.size(),
                     UNGP_InheritData.BULLETED_PREFIX + hullmods.size());
    }
}