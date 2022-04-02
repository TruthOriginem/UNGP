package data.scripts.campaign.specialist.challenges;

import com.fs.starfarer.api.Global;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Only used for loading challenge info.
 */
public class UNGP_ChallengeLoader {
    public static final String EMPTY = "[EMPTY]";
    private static final String FILE_PATH = "data/campaign/UNGP_challenges.csv";

    public static Map<String, UNGP_ChallengeInfo> loadAllInfos() {
        Map<String, UNGP_ChallengeInfo> challengeInfoMap = new HashMap<>();
        try {
            JSONArray challengeInfos = Global.getSettings().getMergedSpreadsheetDataForMod("id", FILE_PATH, "ungp");
            for (int i = 0, len = challengeInfos.length(); i < len; i++) {
                JSONObject row = challengeInfos.getJSONObject(i);
                String id = row.optString("id", EMPTY);
                if (isEmpty(id)) continue;
                String rulesRequired = row.optString("rulesRequired", EMPTY);
                if (isEmpty(rulesRequired)) continue;
                List<String> rulesRequiredList = new ArrayList<>();
                if (!isEmpty(rulesRequired)) {
                    String[] tagArray = rulesRequired.split(",");
                    for (String s : tagArray) {
                        rulesRequiredList.add(s.trim());
                    }
                }
                String name = row.optString("name", EMPTY);
                int positiveLimitation = row.optInt("positiveLimitation", -1);
                int durationByMonth = row.optInt("durationByMonth", -1);
                boolean needMaxLevel = row.optBoolean("needMaxLevel", true);
                boolean canReselectRules = row.optBoolean("canReselectRules", true);
                String milestoneToUnlock = row.optString("milestoneToUnlock", "");
                UNGP_ChallengeInfo challengeInfo = new UNGP_ChallengeInfo(id, name, rulesRequiredList, positiveLimitation, durationByMonth, needMaxLevel, canReselectRules, milestoneToUnlock);
                if (challengeInfo.isValid()) {
                    challengeInfoMap.put(id, challengeInfo);
                }
            }
        } catch (Exception e) {
            Global.getLogger(UNGP_ChallengeLoader.class).error(e);
            throw new RuntimeException("Failed to load UNGP challenges:", e);
        }
        return challengeInfoMap;
    }

    public static boolean isEmpty(String target) {
        return target == null || target.isEmpty() || target.contentEquals(EMPTY);
    }
}
