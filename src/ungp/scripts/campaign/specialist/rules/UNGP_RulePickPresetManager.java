package ungp.scripts.campaign.specialist.rules;

import com.fs.starfarer.api.Global;
import org.json.JSONArray;
import org.lazywizard.lazylib.JSONUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static ungp.scripts.campaign.specialist.rules.UNGP_RulesManager.URule;

/**
 * 管理规则选择预设的保存和加载
 * Handles saving and loading of rule selection presets
 */
public class UNGP_RulePickPresetManager {
    /**
     * 保存规则预设到文件
     * Saves a list of rules as a preset to a file
     *
     * @param presetSlot 预设槽位名称 The name of the preset slot
     * @param rules 要保存的规则列表 The list of rules to save
     */
    public static void save(String presetSlot, List<URule> rules) {
        try (JSONUtils.CommonDataJSONObject jsonObject = JSONUtils.loadCommonJSON(getSaveFileName(presetSlot))) {
            // 将规则ID转换为字符串集合以便保存
            Set<String> ruleStrings = new HashSet<>();
            for (URule rule : rules) {
                ruleStrings.add(rule.getId());
            }
            // 保存到JSON对象并写入文件
            jsonObject.put("rules", ruleStrings);
            jsonObject.save();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 从文件加载规则预设
     * Loads a list of rules from a preset file
     *
     * @param presetSlot 预设槽位名称 The name of the preset slot
     * @return 加载的规则列表，如果加载失败则返回空列表 The loaded list of rules, or an empty list if loading fails
     */
    public static List<URule> load(String presetSlot) {
        try (JSONUtils.CommonDataJSONObject jsonObject = JSONUtils.loadCommonJSON(getSaveFileName(presetSlot))) {
            // 从JSON对象读取规则ID数组
            JSONArray rulesArray = jsonObject.getJSONArray("rules");
            Set<String> ruleStrings = new HashSet<>();
            for (int i = 0; i < rulesArray.length(); i++) {
                ruleStrings.add(rulesArray.optString(i, ""));
            }

            // 根据ID匹配并构建规则对象列表
            List<URule> rules = new ArrayList<>();
            for (URule rule : UNGP_RulesManager.getAllRulesCopy()) {
                if (ruleStrings.contains(rule.getId())) {
                    rules.add(rule);
                }
            }
            return rules;
        } catch (Exception e) {
            // 发生异常时返回空列表
            return new ArrayList<>();
        }
    }

    /**
     * 检查指定的预设是否存在
     * Checks if a preset with the given slot name exists
     *
     * @param presetSlot 预设槽位名称 The name of the preset slot
     * @return 如果预设文件存在则返回true，否则返回false True if the preset file exists, false otherwise
     */
    public static boolean isPresetExists(String presetSlot) {
        return Global.getSettings().fileExistsInCommon(getSaveFileName(presetSlot));
    }


    /**
     * 获取预设文件名
     * Gets the filename for a preset slot
     *
     * @param presetSlot 预设槽位名称 The name of the preset slot
     * @return 预设文件名 The preset filename
     */
    public static String getSaveFileName(String presetSlot) {
        return "UNGP_rules_" + presetSlot;
    }
}
