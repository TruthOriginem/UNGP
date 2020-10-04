package data.scripts.campaign.items;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.CargoTransferHandlerAPI;
import com.fs.starfarer.api.campaign.impl.items.BaseSpecialItemPlugin;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.scripts.campaign.hardmode.UNGP_RulesManager.URule;

import java.awt.*;

import static data.scripts.utils.UNGPFont.ORBITRON;

public class UNGP_RuleItem extends BaseSpecialItemPlugin {
    public static final String ID = "ungp_ruleitem";


    protected URule rule;

    @Override
    public void init(CargoStackAPI stack) {
        super.init(stack);
        rule = URule.getByID(stack.getSpecialDataIfSpecial().getData());
    }

    @Override
    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, CargoTransferHandlerAPI transferHandler, Object stackSource, boolean useGray) {
        float opad = 10f;
        if (rule == null) return;
        tooltip.setTitleOrbitronLarge();
        tooltip.addTitle(rule.getName());

        Color c = Misc.getTextColor();
        rule.addPreDesc(tooltip, opad);

        rule.addDesc(tooltip, opad * 2f, "        ");

        rule.addCost(tooltip, opad * 2f);
//        tooltip.addPara()
    }

    @Override
    public void render(float x, float y, float w, float h, float alphaMult, float glowMult, SpecialItemRendererAPI renderer) {
        if (rule == null) return;
        SpriteAPI sprite = Global.getSettings().getSprite(rule.getSpritePath());
        float cx = x + w / 2f;
        float cy = y + h / 2f;
        if (sprite != null) {
            sprite.setSize(64f, 64f);
            sprite.setNormalBlend();
            sprite.setAlphaMult(alphaMult);
            sprite.renderAtCenter(cx, cy);
//            renderer.renderScanlines(sprite, cx, cy, alphaMult);

//            if (glowMult > 0) {
//                sprite.setAlphaMult(alphaMult * glowMult * 0.5f);
//                sprite.setAdditiveBlend();
//                sprite.renderAtCenter(cx, cy);
//            }
        }
        float fontSize = ORBITRON.getFontSize();
        String costString = rule.getCost() > 0 ? "+" + rule.getCost() : "" + rule.getCost();
        ORBITRON.setText(costString);
        ORBITRON.setMaxWidth(32f);
        ORBITRON.setMaxHeight(32f);
        ORBITRON.setColor(rule.isBonus() ? Misc.getNegativeHighlightColor() : Misc.getHighlightColor());
        ORBITRON.draw(cx + 24, cy - 24);
        ORBITRON.setColor(rule.getBorderColor());
        ORBITRON.setText(rule.isBonus() ? "P" : "N");
//        ORBITRON16.setMaxWidth(40f);
//        ORBITRON16.setMaxHeight(40f);
        ORBITRON.draw(cx - 22 - fontSize, cy + 22 + fontSize);


        sprite = Global.getSettings().getSprite(spec.getIconName());
        if (sprite != null) {
            if (rule.getId().equals("lobster_perday")) {
                sprite.setColor(Color.yellow);
            } else {
                sprite.setColor(rule.getBorderColor());
            }
            sprite.setNormalBlend();
            sprite.setAlphaMult(alphaMult);
            sprite.renderAtCenter(cx, cy);
            if (glowMult > 0) {
                sprite.setAlphaMult(alphaMult * glowMult * 0.5f);
                sprite.setAdditiveBlend();
                sprite.renderAtCenter(cx, cy);
            }
        }


    }

    @Override
    public String getDesignType() {
        return null;
    }
}
