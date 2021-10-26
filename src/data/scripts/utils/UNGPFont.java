package data.scripts.utils;

import data.scripts.UNGP_modPlugin;
import org.lazywizard.lazylib.ui.FontException;
import org.lazywizard.lazylib.ui.LazyFont;
import org.lazywizard.lazylib.ui.LazyFont.DrawableString;

import java.awt.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class UNGPFont {
    public static DrawableString ORBITRON;
    public static DrawableString ORBITRON_BOLD;
    private static LazyFont orbitronMod;
    private static LazyFont orbitronCore;
    private static final Map<String, DrawableString> DYNAMIC_DRAWABLE_MAP = new HashMap<>();
    private static final Pattern ENG_CHECKER = Pattern.compile("^[\\sa-zA-Z]*");

    public static boolean notOnlyEN(String s) {
        return ENG_CHECKER.matcher(s).matches();
    }

    /**
     * Should be Called at {@link UNGP_modPlugin#onApplicationLoad()}
     */
    public static void init() {
        // 初始化相关字体
        try {
            orbitronMod = LazyFont.loadFont("graphics/fonts/ungp_orbitron.fnt");
            orbitronCore = LazyFont.loadFont("graphics/fonts/orbitron20aabold.fnt");
            ORBITRON = orbitronMod.createText();
            ORBITRON_BOLD = orbitronCore.createText();

        } catch (FontException ex) {
            throw new RuntimeException("Error font loading...");
        }
    }


    public static DrawableString getDynamicDrawable(String content, Color color) {
        String key = content + color.toString();
        DrawableString drawableString = DYNAMIC_DRAWABLE_MAP.get(key);
        if (drawableString == null) {
            drawableString = orbitronMod.createText(content, color, 20, 32f, 32f);
            DYNAMIC_DRAWABLE_MAP.put(key, drawableString);
        }
        return drawableString;
    }

    public static void clearDynamicDrawable() {
        Collection<DrawableString> drawableStrings = DYNAMIC_DRAWABLE_MAP.values();
        for (DrawableString drawableString : drawableStrings) {
            drawableString.dispose();
        }
        DYNAMIC_DRAWABLE_MAP.clear();
    }

    public static LazyFont getOrbitronMod() {
        return orbitronMod;
    }

    public static void drawShadow(DrawableString drawableString, float cx, float cy, float offset) {
        drawableString.draw(cx - offset, cy);
        drawableString.draw(cx + offset, cy);
        drawableString.draw(cx, cy - offset);
        drawableString.draw(cx, cy + offset);
    }
}
