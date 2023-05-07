package ungp.scripts.campaign.everyframe;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CoreUITabId;
import com.fs.starfarer.api.campaign.listeners.CampaignInputListener;
import com.fs.starfarer.api.campaign.listeners.CampaignUIRenderingListener;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.Rectangle;
import ungp.scripts.campaign.specialist.intel.UNGP_SpecialistIntel;
import ungp.scripts.campaign.specialist.rules.UNGP_RulesManager;
import ungp.scripts.campaign.specialist.rules.UNGP_RulesManager.URule;
import ungp.scripts.utils.UNGP_Progress;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static ungp.scripts.campaign.specialist.UNGP_SpecialistSettings.Difficulty;

public class UNGP_SpecialistWidgetPlugin implements EveryFrameScript, CampaignUIRenderingListener, CampaignInputListener {
    private static final float BASIC_ICON_ALPHA = 0.4f;
    private static final float SMALL_ICON_SIZE = 32f;
    private static final Rectangle WIDGET_RECT;
    private UNGP_Progress moveOverAndWaitProgress;
    private SpriteAPI layer0;
    private SpriteAPI layer1;
    private float targetIconAlphaMult;
    private float currentIconAlphaMult;
    private float elapsed = 0f;

    static {
        WIDGET_RECT = new Rectangle(10, (int) (Global.getSettings().getScreenHeightPixels()) - 120, 80, 80);
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return true;
    }

    public UNGP_SpecialistWidgetPlugin() {
        moveOverAndWaitProgress = new UNGP_Progress(0.2f);
        targetIconAlphaMult = 0f;
        currentIconAlphaMult = 0f;
        layer0 = Global.getSettings().getSprite("fx", "UNGP_specialist_icon_layer0");
        layer1 = Global.getSettings().getSprite("fx", "UNGP_specialist_icon_layer1");
    }

    public static boolean inWidgetRect(int x, int y) {
        return WIDGET_RECT.contains(x, y);
    }

    @Override
    public void advance(float amount) {
        if (!UNGP_RulesManager.isSpecialistMode()) return;
        if (Global.getSector().isFastForwardIteration()) return;
        if (Global.getSector().isPaused()) return;
        elapsed += amount;
        targetIconAlphaMult = BASIC_ICON_ALPHA;
        if (inWidgetRect(Mouse.getX(), Mouse.getY())) {
            targetIconAlphaMult = BASIC_ICON_ALPHA + moveOverAndWaitProgress.getProgress() * (1f - BASIC_ICON_ALPHA);
            moveOverAndWaitProgress.advance(amount);
        } else {
            moveOverAndWaitProgress.reset();
        }
        targetIconAlphaMult = Math.min(1f, Math.max(0f, targetIconAlphaMult));
        currentIconAlphaMult = Misc.interpolate(currentIconAlphaMult, targetIconAlphaMult, 5f * amount);
    }

    private void renderIcons(List<URule> bonusRules, float y) {
        for (int i = 0; i < bonusRules.size(); i++) {
            float x = WIDGET_RECT.getX() + i * (SMALL_ICON_SIZE + 3f);
            SpriteAPI icon = Global.getSettings().getSprite(bonusRules.get(i).getSpritePath());
            icon.setSize(SMALL_ICON_SIZE, SMALL_ICON_SIZE);
            icon.setAlphaMult(0.7f);
            icon.render(x, y);
        }
    }

    @Override
    public int getListenerInputPriority() {
        return 1000;
    }

    @Override
    public void processCampaignInputPreCore(List<InputEventAPI> events) {
    }

    @Override
    public void processCampaignInputPreFleetControl(List<InputEventAPI> events) {
        if (UNGP_RulesManager.isSpecialistMode()) {
            for (InputEventAPI event : events) {
                if (event.isLMBDownEvent() && inWidgetRect(event.getX(), event.getY())) {
                    Global.getSector().getCampaignUI().showCoreUITab(CoreUITabId.INTEL, UNGP_SpecialistIntel.getInstance());
                    Global.getSoundPlayer().playUISound("ui_button_pressed", 1f, 1f);
                    event.consume();
                    break;
                }
            }
        }
    }

    @Override
    public void processCampaignInputPostCore(List<InputEventAPI> events) {

    }

    @Override
    public void renderInUICoordsBelowUI(ViewportAPI viewport) {

    }

    @Override
    public void renderInUICoordsAboveUIBelowTooltips(ViewportAPI viewport) {

    }

    @Override
    public void renderInUICoordsAboveUIAndTooltips(ViewportAPI viewport) {
        if (!UNGP_RulesManager.isSpecialistMode()) return;
        final CampaignUIAPI campaignUI = Global.getSector().getCampaignUI();
        if (campaignUI.isShowingDialog() || campaignUI.isShowingMenu() || campaignUI.getCurrentCoreTab() != null)
            return;
        Difficulty difficulty = UNGP_RulesManager.getGlobalDifficulty();
        float alphaMult = viewport.getAlphaMult();

        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        float iconCenterX = WIDGET_RECT.getX() + WIDGET_RECT.getWidth() / 2f;
        float iconCenterY = WIDGET_RECT.getY() + WIDGET_RECT.getHeight() / 2f;
        float size = (float) (110f * (1f + 0.1f * FastTrig.sin(this.elapsed)));
        layer0.setAngle(MathUtils.clampAngle(this.elapsed * 60f));
        layer0.setAlphaMult(currentIconAlphaMult * alphaMult);
        layer0.setWidth(size);
        layer0.setHeight(size);
        layer0.setAdditiveBlend();
        layer0.renderAtCenter(iconCenterX, iconCenterY);

        size = (float) (128f * (1f + 0.1f * FastTrig.cos(this.elapsed)));
        layer1.setAngle(MathUtils.clampAngle(-this.elapsed * 30f));
        layer1.setAlphaMult(currentIconAlphaMult * 0.6f * alphaMult);
        layer1.setWidth(size);
        layer1.setHeight(size);
        layer1.setColor(difficulty.color);
        layer1.setAdditiveBlend();
        layer1.renderAtCenter(iconCenterX, iconCenterY);

        layer1.setColor(Color.white);
        layer1.setAngle(MathUtils.clampAngle(this.elapsed * 30f));
        layer1.setSize(size * 1.5f, size * 1.5f);
        layer1.setAlphaMult(currentIconAlphaMult * 0.4f * alphaMult);
        layer1.renderAtCenter(iconCenterX, iconCenterY);

        size = 64f;
        SpriteAPI character = Global.getSettings().getSprite(difficulty.spritePath);
        character.setSize(size, size);
        character.setAlphaMult((float) (0.8f + 0.2f * FastTrig.cos(this.elapsed + 233f)) * alphaMult);
        character.renderAtCenter(iconCenterX, iconCenterY);

        if (inWidgetRect(Mouse.getX(), Mouse.getY())) {
            if (moveOverAndWaitProgress.getProgress() > 0.7f) {
                List<URule> bonusRules = new ArrayList<>();
                List<URule> notBonusRules = new ArrayList<>();
                for (URule rule : UNGP_RulesManager.ACTIVATED_RULES_IN_THIS_GAME) {
                    if (rule.isBonus()) {
                        bonusRules.add(rule);
                    } else {
                        notBonusRules.add(rule);
                    }
                }
                Collections.sort(bonusRules, new UNGP_RulesManager.UNGP_RuleSorter());
                Collections.sort(notBonusRules, new UNGP_RulesManager.UNGP_RuleSorter());

                float iconGroup_y = WIDGET_RECT.getY() - 5f - SMALL_ICON_SIZE;
                renderIcons(bonusRules, iconGroup_y);
                iconGroup_y -= 5f + SMALL_ICON_SIZE;
                renderIcons(notBonusRules, iconGroup_y);
            }
        }
    }
}
