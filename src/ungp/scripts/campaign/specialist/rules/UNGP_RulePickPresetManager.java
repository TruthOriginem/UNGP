package ungp.scripts.campaign.specialist.rules;

import com.fs.starfarer.api.Global;
import org.json.JSONArray;
import org.lazywizard.lazylib.JSONUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static ungp.scripts.campaign.specialist.rules.UNGP_RulesManager.URule;

public class UNGP_RulePickPresetManager {
    public static void save(String presetSlot, List<URule> rules) {
        try (JSONUtils.CommonDataJSONObject jsonObject = JSONUtils.loadCommonJSON(getSaveFileName(presetSlot))) {
            Set<String> ruleStrings = new HashSet<>();
            for (URule rule : rules) {
                ruleStrings.add(rule.getId());
            }
            jsonObject.put("rules", ruleStrings);
            jsonObject.save();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static List<URule> load(String presetSlot) {
        try (JSONUtils.CommonDataJSONObject jsonObject = JSONUtils.loadCommonJSON(getSaveFileName(presetSlot))) {
            JSONArray rulesArray = jsonObject.getJSONArray("rules");
            Set<String> ruleStrings = new HashSet<>();
            for (int i = 0; i < rulesArray.length(); i++) {
                ruleStrings.add(rulesArray.optString(i, ""));
            }
            List<URule> rules = new ArrayList<>();
            for (URule rule : UNGP_RulesManager.getAllRulesCopy()) {
                if (ruleStrings.contains(rule.getId())) {
                    rules.add(rule);
                }
            }
            return rules;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public static boolean isPresetExists(String presetSlot) {
        return Global.getSettings().fileExistsInCommon(getSaveFileName(presetSlot));
    }


    public static String getSaveFileName(String presetSlot) {
        return "UNGP_rules_" + presetSlot;
    }
}
