package data.scripts.campaign.specialist.rules;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.graphics.SpriteAPI;
import data.scripts.ungprules.impl.UNGP_BaseRuleEffect;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class UNGP_RuleInfoLoader {
    private static final String EMPTY = "[EMPTY]";
    private static final String FILE_PATH = "data/campaign/UNGP_rules.csv";

    /**
     * 规则的数据类
     */
    public static final class UNGP_RuleInfo {
        private String id = EMPTY;
        private String name = EMPTY;
        private String shortDesc = EMPTY;
        private String desc = EMPTY;
        private int cost = 0;
        private boolean isBonus = false;
        private boolean isGolden = false;
        private String spritePath = EMPTY;
        private String extra1 = EMPTY;
        private String extra2 = EMPTY;
        private String source = EMPTY;
        private UNGP_BaseRuleEffect effectPlugin;
        private List<String> tags;

        public UNGP_RuleInfo() {
        }

        public UNGP_RuleInfo(String id, String name, String shortDesc, String desc, int cost, boolean isBonus, boolean isGolden, String spritePath, String extra1, String extra2, String source, UNGP_BaseRuleEffect effectPlugin, List<String> tags) {
            this.id = id;
            this.name = name;
            this.shortDesc = shortDesc;
            this.desc = desc;
            this.cost = cost;
            this.spritePath = spritePath;
            this.isBonus = isBonus;
            this.isGolden = isGolden;
            this.extra1 = extra1;
            this.extra2 = extra2;
            this.source = source;
            this.effectPlugin = effectPlugin;
            this.tags = tags;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getShortDesc() {
            return shortDesc;
        }

        public String getDesc() {
            return desc;
        }

        public String getExtra1() {
            return extra1;
        }

        public String getExtra2() {
            return extra2;
        }

        public String getSource() {
            return source;
        }

        public String getSpritePath() {
            return spritePath;
        }

        public boolean isBonus() {
            return isBonus;
        }

        public int getCost() {
            return cost;
        }

        public UNGP_BaseRuleEffect getEffectPlugin() {
            return effectPlugin;
        }

        public boolean isGolden() {
            return isGolden;
        }

        public List<String> getTags() {
            return tags;
        }
    }

    /**
     * 需要在OnApplicationLoad里调用，读取一遍csv内容
     */
    public static List<UNGP_RuleInfo> LoadAllInfos() {
        List<UNGP_RuleInfo> infos = new ArrayList<>();
        try {
            ClassLoader classLoader = Global.getSettings().getScriptClassLoader();
            JSONArray ruleInfos = Global.getSettings().getMergedSpreadsheetDataForMod("id", FILE_PATH, "ungp");

            for (int i = 0, len = ruleInfos.length(); i < len; i++) {
                JSONObject row = ruleInfos.getJSONObject(i);
                String id = row.optString("id", EMPTY);
                if (isEmpty(id)) continue;
                String name = row.optString("name", EMPTY);
                String shortDesc = row.optString("short", EMPTY);
                String desc = row.optString("desc", EMPTY);
                int cost = row.optInt("cost", 0);
                boolean isBonus = row.optBoolean("isBonus", false);
                boolean isGolden = row.optBoolean("isGolden", false);
                String spritePath = row.optString("spritePath", EMPTY);
                String extra1 = row.optString("extra1", EMPTY);
                String extra2 = row.optString("extra2", EMPTY);
                String source = row.optString("source", EMPTY);

                String effectPluginName = row.getString("effectPlugin");

                String tags = row.optString("tags", EMPTY);
                List<String> tagList = new ArrayList<>();
                if (!isEmpty(tags)) {
                    String[] tagArray = tags.split(",");
                    for (String s : tagArray) {
                        tagList.add(s.trim());
                    }
                }
                // load class
                Class<?> effectClass = classLoader.loadClass(effectPluginName);

                UNGP_BaseRuleEffect ruleEffect = (UNGP_BaseRuleEffect) effectClass.newInstance();
                if (isEmpty(spritePath)) {
                    spritePath = "graphics/ui/icons/64x_xcircle.png";
                }
                final SpriteAPI sprite = Global.getSettings().getSprite(spritePath);
                if (sprite == null || sprite.getHeight() < 1) {
                    Global.getSettings().loadTexture(spritePath);
                }
                UNGP_RuleInfo info = new UNGP_RuleInfo(id, name, shortDesc, desc, cost, isBonus, isGolden, spritePath, extra1, extra2, source, ruleEffect, tagList);
                infos.add(info);
            }
        } catch (Exception e) {
            Global.getLogger(UNGP_RuleInfoLoader.class).error(e);
            throw new RuntimeException("Failed to load UNGP rules:", e);
        }

        return infos;
    }

    public static boolean isEmpty(String target) {
        return target == null || target.isEmpty() || target.equals(EMPTY);
    }
}
