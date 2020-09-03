package data.scripts.campaign.hardmode;

import com.fs.starfarer.api.Global;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class UNGP_RuleInfos {
    private static final String EMPTY = "[EMPTY]";
    private static final String FILE_PATH = "data/campaign/UNGP_rules.csv";
    private static final List<UNGP_RuleInfo> Loaded_Infos = new ArrayList<>();

    public static class UNGP_RuleInfo {
        private String id = EMPTY;
        private String name = EMPTY;
        private String shortDesc = EMPTY;
        private String desc = EMPTY;
        private int cost = 0;
        private boolean isBonus = false;
        private String spritePath = EMPTY;
        private String extra1 = EMPTY;
        private String extra2 = EMPTY;
        private UNGP_BaseRuleEffect effectPlugin;

        public UNGP_RuleInfo() {
        }

        public UNGP_RuleInfo(String id, String name, String shortDesc, String desc, int cost, boolean isBonus, String spritePath, String extra1, String extra2, UNGP_BaseRuleEffect effectPlugin) {
            this.id = id;
            this.name = name;
            this.shortDesc = shortDesc;
            this.desc = desc;
            this.cost = cost;
            this.spritePath = spritePath;
            this.isBonus = isBonus;
            this.extra1 = extra1;
            this.extra2 = extra2;
            this.effectPlugin = effectPlugin;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getShortDesc() {
            return shortDesc;
        }

        public void setShortDesc(String shortDesc) {
            this.shortDesc = shortDesc;
        }

        public String getDesc() {
            return desc;
        }

        public void setDesc(String desc) {
            this.desc = desc;
        }

        public String getExtra1() {
            return extra1;
        }

        public void setExtra1(String extra1) {
            this.extra1 = extra1;
        }

        public String getExtra2() {
            return extra2;
        }

        public void setExtra2(String extra2) {
            this.extra2 = extra2;
        }

        public String getSpritePath() {
            return spritePath;
        }

        public void setSpritePath(String spritePath) {
            this.spritePath = spritePath;
        }

        public boolean isBonus() {
            return isBonus;
        }

        public void setBonus(boolean bonus) {
            isBonus = bonus;
        }

        public int getCost() {
            return cost;
        }

        public void setCost(int cost) {
            this.cost = cost;
        }

        public UNGP_BaseRuleEffect getEffectPlugin() {
            return effectPlugin;
        }

        public void setEffectPlugin(UNGP_BaseRuleEffect effectPlugin) {
            this.effectPlugin = effectPlugin;
        }
    }

    /**
     * 需要在OnApplicaitonLoad里调用
     */
    public static List<UNGP_RuleInfo> LoadAllInfos() {
        Loaded_Infos.clear();
        try {
            ClassLoader classLoader = Global.getSettings().getScriptClassLoader();
            JSONArray ruleInfos = Global.getSettings().getMergedSpreadsheetDataForMod("id", FILE_PATH, "ungp");
            for (int i = 0; i < ruleInfos.length(); i++) {
                JSONObject row = ruleInfos.getJSONObject(i);
                String id = row.optString("id", EMPTY);
                if (isEmpty(id)) continue;
                String name = row.optString("name", EMPTY);
                String shortDesc = row.optString("short", EMPTY);
                String desc = row.optString("desc", EMPTY);
                int cost = row.optInt("cost", 0);
                boolean isBonus = row.optBoolean("isBonus", false);
                String spritePath = row.optString("spritePath", EMPTY);
                String extra1 = row.optString("extra1", EMPTY);
                String extra2 = row.optString("extra2", EMPTY);
                String effectPluginName = row.getString("effectPlugin");
                Class<?> effectClass = classLoader.loadClass(effectPluginName);
                UNGP_BaseRuleEffect ruleEffect = (UNGP_BaseRuleEffect) effectClass.newInstance();
                if (spritePath.isEmpty() || spritePath.equals(EMPTY)) {
                    spritePath = "graphics/ui/icons/64x_xcircle.png";
                }
                if (Global.getSettings().getSprite(spritePath) == null ||
                        Global.getSettings().getSprite(spritePath).getHeight() < 1) {
                    Global.getSettings().loadTexture(spritePath);
                }
                UNGP_RuleInfo info = new UNGP_RuleInfo(id, name, shortDesc, desc, cost, isBonus, spritePath, extra1, extra2, ruleEffect);
                Loaded_Infos.add(info);
            }
        } catch (Exception e) {
            Global.getLogger(UNGP_RuleInfos.class).error(e);
            throw new RuntimeException("Failed to load UNGP rules...", e);
        }

        return Loaded_Infos;
    }

    private static boolean isEmpty(String target) {
        return target == null || target.isEmpty() || target.equals(EMPTY);
    }
}
