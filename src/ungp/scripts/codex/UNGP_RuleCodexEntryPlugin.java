package ungp.scripts.codex;

import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.impl.codex.CodexDialogAPI;
import com.fs.starfarer.api.impl.codex.CodexEntryV2;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import com.fs.starfarer.api.util.Misc;

import java.util.List;
import java.util.Set;

import static ungp.scripts.campaign.specialist.rules.UNGP_RulesManager.URule;

public class UNGP_RuleCodexEntryPlugin extends CodexEntryV2 implements CustomUIPanelPlugin {
    public static final String ID_PREFIX = "UNGP_codex_rule_";
    protected CustomPanelAPI panel;
    protected UIPanelAPI relatedEntries;
    protected UIPanelAPI box;
    protected CodexDialogAPI codex;

    public UNGP_RuleCodexEntryPlugin(URule uRule) {
        super(ID_PREFIX + uRule.getId(), uRule.getName(), uRule.getSpritePath(), uRule.getId());
    }

    public UNGP_RuleCodexEntryPlugin(String id, String title, String icon) {
        super(id, title, icon);
    }

    public URule getURule() {
        String realId = id.substring(ID_PREFIX.length());
        return URule.getByID(realId);
    }

    @Override
    public void createTitleForList(TooltipMakerAPI info, float width, ListMode mode) {
        URule uRule = getURule();
        info.addPara(getTitle(), uRule.getTooltipTitleColor(), 0f);
        info.addPara(uRule.getRuleTypeName() + "(" + uRule.getCostString() + ")", Misc.getGrayColor(), 0f);
    }

    @Override
    public boolean hasCustomDetailPanel() {
        return true;
    }

    @Override
    public CustomUIPanelPlugin getCustomPanelPlugin() {
        return this;
    }

    @Override
    public void destroyCustomDetail() {
        panel = null;
        relatedEntries = null;
        box = null;
        codex = null;
    }

    @Override
    public void createCustomDetail(CustomPanelAPI panel, UIPanelAPI relatedEntries, CodexDialogAPI codex) {
        this.panel = panel;
        this.relatedEntries = relatedEntries;
        this.codex = codex;

        float opad = 10f;

        float width = panel.getPosition().getWidth();

        float horzBoxPad = 30f;

        // the right width for a tooltip wrapped in a box to fit next to relatedEntries
        // 290 is the width of the related entries widget, but it may be null
        float tw = width - 290f - opad - horzBoxPad + 10f;

        TooltipMakerAPI text = panel.createUIElement(tw, 0, false);
        URule uRule = getURule();
        uRule.addDetailsToTooltip(text, true, true);

        panel.updateUIElementSizeAndMakeItProcessInput(text);

        box = panel.wrapTooltipWithBox(text);
        panel.addComponent(box).inTL(0f, 0f);
        if (relatedEntries != null) {
            panel.addComponent(relatedEntries).inTR(0f, 0f);
        }

        float height = box.getPosition().getHeight();
        if (relatedEntries != null) {
            height = Math.max(height, relatedEntries.getPosition().getHeight());
        }
        panel.getPosition().setSize(width, height);
    }

    @Override
    public boolean matchesTags(Set<String> tags) {
        URule uRule = getURule();
        if (tags.contains("positive") && uRule.isPositive()) return true;
        if (tags.contains("negative") && !uRule.isPositive()) return true;
        if (tags.contains("golden") && uRule.isGolden()) return true;
        if (tags.contains("milestone") && uRule.isMilestone()) return true;
        return tags.isEmpty();
    }

    @Override
    public void positionChanged(PositionAPI position) {

    }

    @Override
    public void renderBelow(float alphaMult) {
    }

    @Override
    public void render(float alphaMult) {

    }

    @Override
    public void advance(float amount) {

    }

    @Override
    public void processInput(List<InputEventAPI> events) {

    }

    @Override
    public void buttonPressed(Object buttonId) {

    }

    @Override
    public boolean isVignetteIcon() {
        return true;
    }

//    @Override
//    public boolean isLocked() {
//        URule uRule = URule.getByID(id);
//        if (uRule != null) {
//            return uRule.isMilestone();
//        }
//        return super.isLocked();
//    }
}
