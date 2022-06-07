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
    private transient UNGP_Background background;

    public UNGP_BackgroundIntel(StarSystemAPI system) {
        readResolve();
        systemLocation = system == null ? null : system.getHyperspaceAnchor();
    }

    void readResolve() {
        background = UNGP_BackgroundManager.getPlayerBackground();
    }

    @Override
    public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        info.addSectionHeading(backgrounds_i18n.get("background"), Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(),
                               Alignment.MID, 0f);
        background.addShortDescTooltipWithIcon(info, 10f);
    }

    @Override
    public void createIntelInfo(TooltipMakerAPI info, ListInfoMode mode) {
        info.addPara(background.getName(), background.getNameColor(), 0f);
    }

    @Override
    public String getIcon() {
        return background.getSpritePath();
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
