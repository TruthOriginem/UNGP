package data.scripts.campaign.background;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.graphics.SpriteAPI;
import data.scripts.ungpbackgrounds.UNGP_BackgroundPluginAPI;
import data.scripts.ungpbackgrounds.impl.UNGP_Nothing;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

import static data.scripts.utils.UNGPUtils.EMPTY;
import static data.scripts.utils.UNGPUtils.isEmpty;

public class UNGP_BackgroundManager {
    private static final String FILE_PATH = "data/campaign/UNGP_backgrounds.csv";

    private static final Map<String, UNGP_Background> BACKGROUND_MAP = new HashMap<>();

    private static UNGP_Background DEFAULT_BACKGROUND;

    public static void loadAllBackgrounds() {
        ClassLoader classLoader = Global.getSettings().getScriptClassLoader();
        try {
            JSONArray backgroundJsonRows = Global.getSettings().getMergedSpreadsheetDataForMod("id", FILE_PATH, "ungp");
            for (int i = 0, len = backgroundJsonRows.length(); i < len; i++) {
                JSONObject row = backgroundJsonRows.getJSONObject(i);
                String id = row.optString("id", EMPTY);
                if (isEmpty(id)) continue;

                String name = row.optString("name", EMPTY);
                float order = (float) row.optDouble("order", 10000);
                String shortDesc = row.optString("short", EMPTY);
                String desc = row.optString("desc", EMPTY);
                if (isEmpty(desc)) {
                    desc = shortDesc;
                }
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
                UNGP_BackgroundPluginAPI backgroundPlugin = null;
                if (!isEmpty(effectPluginName)) {
                    Class<?> effectClass = classLoader.loadClass(effectPluginName);
                    backgroundPlugin = (UNGP_BackgroundPluginAPI) effectClass.newInstance();
                } else {
                    backgroundPlugin = new UNGP_Nothing();
                }
                if (isEmpty(spritePath)) {
                    spritePath = "graphics/portraits/portrait_generic.png";
                }
                final SpriteAPI sprite = Global.getSettings().getSprite(spritePath);
                if (sprite == null || sprite.getHeight() < 1) {
                    Global.getSettings().loadTexture(spritePath);
                }

                UNGP_Background background = new UNGP_Background(id, order, name, shortDesc, desc, spritePath, source,
                                                                 extra1, extra2, tagList, backgroundPlugin);
                BACKGROUND_MAP.put(id, background);
                if (id.contentEquals("nothing")) {
                    DEFAULT_BACKGROUND = background;
                }
            }
        } catch (Exception e) {
            Global.getLogger(UNGP_BackgroundManager.class).error(e);
            throw new RuntimeException("Failed to load UNGP backgrounds:", e);
        }
        if (DEFAULT_BACKGROUND == null) {
            throw new RuntimeException("Failed to find default UNGP background: nothing");
        }
    }

    public static List<UNGP_Background> getSortedBackgroundsCopy() {
        ArrayList<UNGP_Background> backgrounds = new ArrayList<>(BACKGROUND_MAP.values());
        Collections.sort(backgrounds, new Comparator<UNGP_Background>() {
            @Override
            public int compare(UNGP_Background o1, UNGP_Background o2) {
                return Float.compare(o1.getOrder(), o2.getOrder());
            }
        });
        return backgrounds;
    }


    public static UNGP_Background getDefaultBackground() {
        return DEFAULT_BACKGROUND;
    }
}
