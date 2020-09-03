package data.scripts.utils;

import org.lazywizard.lazylib.ui.FontException;
import org.lazywizard.lazylib.ui.LazyFont;
import org.lazywizard.lazylib.ui.LazyFont.DrawableString;

public class UNGPFont {
    public static DrawableString ORBITRON;
    private static LazyFont orbitronFont;

    static {
        try {
            orbitronFont = LazyFont.loadFont("graphics/fonts/ungp_orbitron.fnt");
            ORBITRON = getOrbitronFont().createText();
        } catch (FontException ex) {
        }
    }

    public static LazyFont getOrbitronFont() {
        return orbitronFont;
    }

    public static DrawableString getORBITRON() {
        return ORBITRON;
    }
}
