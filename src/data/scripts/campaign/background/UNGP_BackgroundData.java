package data.scripts.campaign.background;

public class UNGP_BackgroundData {
    private String id;
    private String name;
    private String description;
    private float inherit_credits;
    private float inherit_bps;
    private int required_sp;
    private int required_lv;
    private UNGP_BackgroundPluginAPI plugin;
    private String note;


    public UNGP_BackgroundData(String id, String name, String description, float inherit_credits, float inherit_bps, int required_sp, int required_lv, UNGP_BackgroundPluginAPI plugin, String note) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.inherit_credits = inherit_credits;
        this.inherit_bps = inherit_bps;
        this.required_sp = required_sp;
        this.required_lv = required_lv;
        this.plugin = plugin;
        this.note = note;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public float getInheritCredits() {
        return inherit_credits;
    }

    public float getInheritBps() {
        return inherit_bps;
    }

    public int getRequiredSp() {
        return required_sp;
    }

    public int getRequiredLv() {
        return required_lv;
    }

    public UNGP_BackgroundPluginAPI getPlugin() {
        return plugin;
    }

    public String getNote() {
        return note;
    }
}
