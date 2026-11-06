package ungp.scripts.campaign.specialist.challenges;

import com.fs.starfarer.api.Global;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Only used for loading challenge info.
 */
public class UNGP_ChallengeLoader {
    public static final String EMPTY = "[EMPTY]";
    private static final String FILE_PATH = "data/campaign/UNGP_challenges.csv";
    private static final String[] EXPECTED_COLUMNS = {
            "id", "name", "rulesRequired", "positiveLimitation", "durationByMonth",
            "dpToDefeat", "needMaxLevel", "canReselectRules", "milestoneToUnlock"
    };

    public static Map<String, UNGP_ChallengeInfo> loadAllInfos() {
        Map<String, UNGP_ChallengeInfo> challengeInfoMap = new HashMap<>();
        try {
            JSONArray challengeInfos = Global.getSettings().getMergedSpreadsheetDataForMod("id", FILE_PATH, "ungp");
            checkColumns(challengeInfos);
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
                int dpToDefeat = row.optInt("dpToDefeat", -1);
                boolean needMaxLevel = row.optBoolean("needMaxLevel", true);
                boolean canReselectRules = row.optBoolean("canReselectRules", true);
                String milestoneToUnlock = row.optString("milestoneToUnlock", "");
                UNGP_ChallengeInfo challengeInfo = new UNGP_ChallengeInfo(id, name, rulesRequiredList, positiveLimitation, durationByMonth, dpToDefeat, needMaxLevel, canReselectRules, milestoneToUnlock);
                if (challengeInfo.isValid()) {
                    challengeInfoMap.put(id, challengeInfo);
                } else {
                    Global.getLogger(UNGP_ChallengeLoader.class).warn("Skipped UNGP challenge '" + id
                            + "': unknown rule in rulesRequired='" + rulesRequired + "' or milestoneToUnlock='" + milestoneToUnlock + "'");
                }
            }
        } catch (Exception e) {
            Global.getLogger(UNGP_ChallengeLoader.class).error(e);
            throw new RuntimeException("Failed to load UNGP challenges:", e);
        }
        return challengeInfoMap;
    }

    /**
     * 表头缺列时每个字段都会被 optString/optInt 静默填成默认值，行数据整体错位也不会报错。
     * 一旦列名对不上就在这里点名，而不是等到某条挑战从界面上凭空消失。
     */
    private static void checkColumns(JSONArray challengeInfos) {
        if (challengeInfos.length() == 0) {
            return;
        }
        try {
            JSONObject firstRow = challengeInfos.getJSONObject(0);
            for (String column : EXPECTED_COLUMNS) {
                if (!firstRow.has(column)) {
                    Global.getLogger(UNGP_ChallengeLoader.class).warn("Column '" + column + "' is missing in " + FILE_PATH
                            + "; every row will use its default value for it. Present columns: " + listColumns(firstRow));
                }
            }
        } catch (Exception e) {
            Global.getLogger(UNGP_ChallengeLoader.class).warn("Failed to check the columns of " + FILE_PATH, e);
        }
    }

    private static String listColumns(JSONObject row) {
        StringBuilder builder = new StringBuilder();
        for (Iterator<?> it = row.keys(); it.hasNext(); ) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(it.next());
        }
        return builder.toString();
    }

    public static boolean isEmpty(String target) {
        return target == null || target.isEmpty() || target.contentEquals(EMPTY);
    }
}
