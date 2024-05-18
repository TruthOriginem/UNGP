package ungp.scripts.campaign.ui;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.FaderUtil;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.opengl.GL11;
import ungp.scripts.campaign.specialist.items.UNGP_RuleItem;
import ungp.scripts.campaign.specialist.rules.UNGP_RulesManager;

import java.awt.*;

import static ungp.scripts.campaign.specialist.rules.UNGP_RulesManager.URule;

public class UNGP_RulePickItemPanelPlugin extends BaseCustomUIPanelPlugin {
    public static final float ITEM_SIZE = 112f;
    public static final float RULE_ICON_SIZE = 64f;
    private PositionAPI p;
    private URule rule;
    private CustomPanelAPI panel;
    private ButtonAPI linkedButton;
    private ButtonPressListener pressListener;
    private SpriteAPI ruleSprite;
    protected FaderUtil highlightBlinker = new FaderUtil(0f, 0.25f, 0.25f, true, true);


    public UNGP_RulePickItemPanelPlugin(URule rule, ButtonPressListener pressListener) {
        this.rule = rule;
        this.ruleSprite = Global.getSettings().getSprite(rule.getSpritePath());
        this.pressListener = pressListener;
    }

    public void init(CustomPanelAPI panel) {
        this.panel = panel;

        TooltipMakerAPI element = panel.createUIElement(p.getWidth(), p.getHeight(), false);
        Color borderAndDarkColor = Misc.scaleColorOnly(rule.getCorrectColor(), 0.2f);
        linkedButton = element.addAreaCheckbox("", rule.getId(), rule.getCorrectColor(),
                                               borderAndDarkColor, Misc.getBrightPlayerColor(),
                                               p.getWidth(), p.getHeight(), 0f);
        // area check box 会有1像素的空档
        linkedButton.getPosition().setXAlignOffset(-0.5f);
        linkedButton.setCustomData(rule);
        linkedButton.setButtonPressedSound(null);
        if (rule.isPositive()) {
            element.addTooltipToPrevious(UNGP_RuleItem.createRuleItemTooltip(rule), TooltipMakerAPI.TooltipLocation.BELOW);
        } else {
            element.addTooltipToPrevious(UNGP_RuleItem.createRuleItemTooltip(rule), TooltipMakerAPI.TooltipLocation.ABOVE);
        }
        panel.addUIElement(element);

        // 规则名渲染
        TooltipMakerAPI labelElement = panel.createUIElement(p.getWidth(), p.getHeight(), false);
        labelElement.addSpacer(p.getHeight() * 0.08f);
        LabelAPI labelAPI = labelElement.addPara(rule.getName(), rule.getCorrectColor(), 0f);
        labelAPI.setAlignment(Alignment.MID);
        panel.addUIElement(labelElement);

        // Cost点数渲染
        TooltipMakerAPI costElement = panel.createUIElement(p.getWidth(), 22f, false);
        costElement.setParaOrbitronLarge();
        costElement.addPara(rule.getCostString(), rule.getCostColor(), 0f).setAlignment(Alignment.MID);
        panel.addUIElement(costElement);

        // 让高亮（扫描）开始起效
        highlightBlinker.fadeIn();
    }

    public static CustomPanelAPI createCustom(URule rule, ButtonPressListener pressListener) {
        UNGP_RulePickItemPanelPlugin plugin = new UNGP_RulePickItemPanelPlugin(rule, pressListener);
        CustomPanelAPI panel = Global.getSettings().createCustom(ITEM_SIZE, ITEM_SIZE, plugin);
        plugin.init(panel);
        return panel;
    }

    @Override
    public void positionChanged(PositionAPI position) {
        this.p = position;
    }

    @Override
    public void advance(float amount) {
        highlightBlinker.advance(amount * 0.2f);
    }

    @Override
    public void render(float alphaMult) {
        if (p == null) return;
        boolean isEnabled = linkedButton.isEnabled();
        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        if (!isEnabled) {
            ruleSprite.setColor(Color.gray);
        } else {
            ruleSprite.setColor(Color.white);
        }
        ruleSprite.setAlphaMult(alphaMult);
        ruleSprite.setSize(RULE_ICON_SIZE, RULE_ICON_SIZE);
        float cx = p.getX() + p.getWidth() * 0.5f;
        float cy = p.getY() + p.getHeight() * 0.5f;
        ruleSprite.renderAtCenter(cx, cy);

        //
        boolean isGoldenOrMilestone = rule.isGolden() || rule.isMilestone();
        if (isGoldenOrMilestone && isEnabled) {
            if (highlightBlinker.isFadingIn()) {
                float factor = highlightBlinker.getBrightness();
                ruleSprite.setColor(UNGP_RulesManager.getGoldenColor());
                ruleSprite.setAdditiveBlend();

                float tmp_h = 0.1f; // 扫描条的宽度
                float tmp_y = 1f + tmp_h - (1f + 2f * tmp_h) * factor;
                float th = tmp_h;
                float ty = MathUtils.clamp(tmp_y, 0f, 1f);
                if (tmp_y > 1f - tmp_h) {
                    th = tmp_y > 1f ? 0f : 1f - tmp_y;
                }
                if (tmp_y < 0f) {
                    th = tmp_h + tmp_y;
                }
                ruleSprite.renderRegionAtCenter(cx, cy, 0f, ty, 1f, th);
            }
        }

        GL11.glPopMatrix();
    }

    @Override
    public void renderBelow(float alphaMult) {
        if (p == null) return;
        //        GL11.glPushMatrix();
        //        GL11.glDisable(GL11.GL_TEXTURE_2D);
        //        GL11.glEnable(GL11.GL_BLEND);
        //        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        //        Misc.renderQuad(p.getX(), p.getY(), p.getWidth(), p.getHeight(), Color.cyan, alphaMult);
        //        GL11.glPopMatrix();
    }

    public ButtonAPI getLinkedButton() {
        return linkedButton;
    }

    @Override
    public void buttonPressed(Object buttonId) {
        String soundId;
        if (rule.isMilestone()) {
            soundId = linkedButton.isChecked() ? "ui_milestone_pickup" : "ui_milestone_drop";
        } else if (rule.isGolden()) {
            soundId = linkedButton.isChecked() ? "ui_golden_pickup" : "ui_golden_drop";
        } else if (rule.isPositive()) {
            soundId = linkedButton.isChecked() ? "ui_positive_pickup" : "ui_positive_drop";
        } else {
            soundId = linkedButton.isChecked() ? "ui_negative_pickup" : "ui_negative_drop";
        }
        Global.getSoundPlayer().playUISound(soundId, 1f, 1f);
        pressListener.notifyPressed(linkedButton);
    }

    public interface ButtonPressListener {
        void notifyPressed(ButtonAPI button);
    }
}
