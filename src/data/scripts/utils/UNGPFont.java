package data.scripts.utils;

import org.lazywizard.lazylib.ui.FontException;
import org.lazywizard.lazylib.ui.LazyFont;
import org.lazywizard.lazylib.ui.LazyFont.DrawableString;

public class UNGPFont {
    public static DrawableString ORBITRON;

    public static void Load() {
        try {
            LazyFont fontdraw = LazyFont.loadFont("graphics/fonts/ungp_orbitron.fnt");
            ORBITRON = fontdraw.createText();
        } catch (FontException ex) {
        }
    }


}
