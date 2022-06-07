package data.scripts.campaign.background;

import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.inherit.UNGP_InheritData;
import data.scripts.ungpbackgrounds.UNGP_BackgroundPluginAPI;
import data.scripts.utils.UNGPUtils;

import java.awt.*;
import java.util.List;

import static data.scripts.utils.Constants.root_i18n;
import static data.scripts.utils.Constants.backgrounds_i18n;

public class UNGP_Background {
    private String id;
    private float order;
    private String name;
    private String shortDescription;
    private String description;
    private String spritePath;
    private String source;
    private String extra1;
    private String extra2;
    private List<String> tags;
    private UNGP_BackgroundPluginAPI plugin;

    public UNGP_Background(String id, float order, String name, String shortDescription, String description, String spritePath, String source, String extra1, String extra2, List<String> tags, UNGP_BackgroundPluginAPI plugin) {
        this.id = id;
        this.order = order;
        this.name = name;
        this.shortDescription = shortDescription;
        this.description = description;
        this.spritePath = spritePath;
        this.source = source;
        this.extra1 = extra1;
        this.extra2 = extra2;
        this.tags = tags;
        this.plugin = plugin;
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

    public float getOrder() {
        return order;
    }

    public UNGP_BackgroundPluginAPI getPlugin() {
        return plugin;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public String getSpritePath() {
        return spritePath;
    }

    public String getSource() {
        return source;
    }

    public String getExtra1() {
        return extra1;
    }

    public String getExtra2() {
        return extra2;
    }

    public boolean hasTag(String tag) {
        return tags.contains(tag);
    }

    public Color getNameColor() {
        if (plugin != null && plugin.getOverrideNameColor() != null) {
            return plugin.getOverrideNameColor();
        }
        return Misc.getBasePlayerColor();
    }

    public void addShortDescTooltipWithIcon(TooltipMakerAPI tooltip, float pad) {
        TooltipMakerAPI imageWithText = tooltip.beginImageWithText(getSpritePath(), 64f);
        imageWithText.setParaOrbitronLarge();
        imageWithText.addPara(getName(), getNameColor(), 10f);
        imageWithText.getPrev().getPosition().setXAlignOffset(7f);
        imageWithText.setParaFontDefault();
        imageWithText.addPara(getShortDescription(), 5f);
        imageWithText.addSpacer(10f);
        tooltip.addImageWithText(pad);
    }

    public void addShortDescTooltip(TooltipMakerAPI tooltip) {
        tooltip.addPara(getName(), getNameColor(), 10f);
        tooltip.addPara(getShortDescription(), 5f);
    }


    public void addDescriptionTooltip(TooltipMakerAPI tooltip, UNGP_InheritData pickedInheritData) {
        tooltip.setParaOrbitronLarge();
        tooltip.addPara(getName(), getNameColor(), 0f);
        if (!UNGPUtils.isEmpty(source)) {
            tooltip.setParaSmallInsignia();
            tooltip.addPara(backgrounds_i18n.get("source") + getSource(), Misc.getGrayColor(), 0f);
        }
        tooltip.setParaFontDefault();
        tooltip.addPara(getDescription(), 5f);

        UNGP_BackgroundPluginAPI plugin = getPlugin();
        if (plugin != null) {
            plugin.addPostDescTooltip(tooltip, pickedInheritData);
            tooltip.addSpacer(10f);
            addOverallBonusTooltip(tooltip, pickedInheritData, true);
        }
    }

    /**
     * credits & bps & other bonus
     */
    public void addOverallBonusTooltip(TooltipMakerAPI tooltip, UNGP_InheritData pickedInheritData, boolean showLimit) {
        plugin.addInheritCreditsAndBPsTooltip(tooltip, pickedInheritData);
        plugin.addBonusTooltip(tooltip, pickedInheritData, showLimit);
        if (showLimit) {
            tooltip.addSpacer(10f);
            tooltip.setParaOrbitronLarge();
            tooltip.addPara(root_i18n.get("inheritData_0") + "%s", 0f, Misc.getBasePlayerColor(), Misc.getHighlightColor(), "" + pickedInheritData.cycle);
            tooltip.setParaFontDefault();
        }
    }


    public static class BackgroundTooltipCreator implements TooltipMakerAPI.TooltipCreator {

        private UNGP_Background background;
        private UNGP_InheritData pickedInheritData;
        private float width;

        public BackgroundTooltipCreator(UNGP_Background background, UNGP_InheritData pickedInheritData, float width) {
            this.background = background;
            this.pickedInheritData = pickedInheritData;
            this.width = width;
        }

        @Override
        public boolean isTooltipExpandable(Object tooltipParam) {
            return false;
        }

        @Override
        public float getTooltipWidth(Object tooltipParam) {
            return width;
        }

        @Override
        public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
            background.addDescriptionTooltip(tooltip, pickedInheritData);
        }
    }
}
