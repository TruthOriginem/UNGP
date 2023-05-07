package ungp.scripts.ui;

import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HorizontalButtonGroup {
    protected List<ButtonAPI> buttons;

    public HorizontalButtonGroup() {
        this.buttons = new ArrayList<>();
    }

    public HorizontalButtonGroup(ButtonAPI... buttons) {
        this.buttons = Arrays.asList(buttons);
    }

    public void addButton(ButtonAPI button) {
        buttons.add(button);
    }

    public void updateTooltip(TooltipMakerAPI tooltip, float buttonPad) {
        for (int i = 1; i < buttons.size(); i++) {
            ButtonAPI button = buttons.get(i);
            ButtonAPI prevButton = buttons.get(i - 1);
            button.getPosition().rightOfBottom(prevButton, buttonPad);
        }
        tooltip.addSpacer(0f);
        tooltip.getPrev().getPosition().belowLeft(buttons.get(0), 0);
    }
}
