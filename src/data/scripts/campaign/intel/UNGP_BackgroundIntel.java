package data.scripts.campaign.intel;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.background.UNGP_Background;
import data.scripts.campaign.background.UNGP_BackgroundManager;

import java.util.Set;

import static data.scripts.utils.Constants.backgrounds_i18n;

public class UNGP_BackgroundIntel extends BaseIntelPlugin {
    private SectorEntityToken systemLocation;

    public UNGP_BackgroundIntel(StarSystemAPI system) {
        systemLocation = system == null ? null : system.getHyperspaceAnchor();
    }


    @Override
    public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        info.addSectionHeading(backgrounds_i18n.get("background"), Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(),
                               Alignment.MID, 0f);
        UNGP_Background background = UNGP_BackgroundManager.getPlayerBackground();
        if (background != null) {
            background.addShortDescTooltipWithIcon(info, 10f);
        }
    }

    @Override
    public void createIntelInfo(TooltipMakerAPI info, ListInfoMode mode) {
        UNGP_Background background = UNGP_BackgroundManager.getPlayerBackground();
        if (background != null) {
            info.addPara(background.getName(), background.getNameColor(), 0f);
        }
    }

    @Override
    public String getIcon() {
        UNGP_Background background = UNGP_BackgroundManager.getPlayerBackground();
        if (background != null) {
            return background.getSpritePath();
        }
        return null;
    }

    @Override
    public SectorEntityToken getMapLocation(SectorMapAPI map) {
        return systemLocation;
    }

    @Override
    public Set<String> getIntelTags(SectorMapAPI map) {
        Set<String> tags = super.getIntelTags(map);
        tags.add("ungp");
        return tags;
    }
}
