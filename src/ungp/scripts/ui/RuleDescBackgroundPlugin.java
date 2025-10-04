package ungp.scripts.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.opengl.GL11;
import ungp.scripts.campaign.everyframe.UNGP_UITimeScript;
import ungp.scripts.campaign.specialist.UNGP_SpecialistSettings;
import ungp.scripts.campaign.specialist.rules.UNGP_RulesManager;

/**
 * Will be initiated each frame so advance does nothing...
 */
public class RuleDescBackgroundPlugin extends BaseCustomUIPanelPlugin {
    private PositionAPI position;
    private UNGP_RulesManager.URule rule;
    private boolean isCodex;

    public RuleDescBackgroundPlugin(PositionAPI position, UNGP_RulesManager.URule rule) {
        this(position, rule, false);
    }

    public RuleDescBackgroundPlugin(PositionAPI position, UNGP_RulesManager.URule rule, boolean isCodex) {
        this.position = position;
        this.rule = rule;
        this.isCodex = isCodex;
    }

    @Override
    public void renderBelow(float alphaMult) {
        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        float frameFactor = UNGP_UITimeScript.getFactor("6secs");
        float x = position.getX();
        float width = position.getWidth();
        float y = position.getY();
        float height = position.getHeight();
        Misc.renderQuad(x, y, width, height,
                        rule.getCorrectColor(), (float) Math.sin(Math.PI * frameFactor) * 0.06f + 0.04f);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        if (!isCodex) {
            UNGP_SpecialistSettings.Difficulty difficulty = UNGP_RulesManager.getGlobalDifficulty();
            if (difficulty != null) {
                SpriteAPI sprite = Global.getSettings().getSprite(difficulty.spritePath);
                float size = 80f;
                sprite.setWidth(size);
                sprite.setHeight(size);
                sprite.setAlphaMult(0.2f);
                sprite.render(x + width - size, y + height - size);
            }
        } else {
            SpriteAPI sprite = Global.getSettings().getSprite(rule.getSpritePath());
            float size = 80f;
            sprite.setWidth(size);
            sprite.setHeight(size);
            sprite.setAlphaMult(0.8f);
            sprite.render(x + width - size, y + height - size);
        }
        GL11.glPopMatrix();
    }

    public static void addToTooltip(TooltipMakerAPI tooltip, UNGP_RulesManager.URule rule, boolean isCodex) {
        RuleDescBackgroundPlugin instance = new RuleDescBackgroundPlugin(tooltip.getPosition(), rule, isCodex);
        CustomPanelAPI custom = Global.getSettings().createCustom(0f, 0f, instance);
        tooltip.addCustom(custom, 0f);
    }
}
