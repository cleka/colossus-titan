package net.sf.colossus.util;


import java.awt.Color;
import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Class HTMLColor contains constant Colors defined by the W3C.
 *
 * @author David Ripton
 */
public final class HTMLColor // NO_UCD
{
    private static final Logger LOGGER = Logger.getLogger(HTMLColor.class
        .getName());

    // Colors defined in the standard.
    public static final Color aliceBlue = new Color(240, 248, 255);
    public static final Color antiqueWhite = new Color(250, 235, 215);
    public static final Color aquamarine = new Color(127, 255, 212);
    public static final Color azure = new Color(240, 255, 255);
    public static final Color beige = new Color(245, 245, 220);
    public static final Color bisque = new Color(255, 228, 196);
    public static final Color black = new Color(0, 0, 0);
    public static final Color blanchedAlmond = new Color(255, 235, 205);
    public static final Color blue = new Color(0, 0, 255);
    public static final Color blueViolet = new Color(138, 43, 226);
    public static final Color brown = new Color(165, 42, 42);
    public static final Color burlyWood = new Color(222, 184, 135);
    public static final Color cadetBlue = new Color(95, 158, 160);
    public static final Color chartReuse = new Color(127, 255, 0);
    public static final Color chocolate = new Color(210, 105, 30);
    public static final Color coral = new Color(255, 127, 80);
    public static final Color cornflowerBlue = new Color(100, 149, 237);
    public static final Color cornsilk = new Color(255, 248, 220);
    public static final Color crimson = new Color(237, 164, 61);
    public static final Color cyan = new Color(0, 255, 255);
    public static final Color darkBlue = new Color(0, 0, 139);
    public static final Color darkCyan = new Color(0, 139, 139);
    public static final Color darkGoldenRod = new Color(184, 134, 11);
    public static final Color darkGray = new Color(167, 167, 167);
    public static final Color darkGreen = new Color(0, 100, 0);
    public static final Color darkKhaki = new Color(189, 183, 107);
    public static final Color darkMagenta = new Color(139, 0, 139);
    public static final Color darkOliveGreen = new Color(85, 107, 47);
    public static final Color darkOrange = new Color(255, 140, 0);
    public static final Color darkOrchid = new Color(153, 50, 204);
    public static final Color darkRed = new Color(139, 0, 0);
    public static final Color darkSalmon = new Color(233, 150, 122);
    public static final Color darkSeaGreen = new Color(143, 188, 143);
    public static final Color darkSlateBlue = new Color(72, 61, 139);
    public static final Color darkSlateGray = new Color(47, 79, 79);
    public static final Color darkTurquoise = new Color(0, 206, 209);
    public static final Color darkViolet = new Color(148, 0, 211);
    public static final Color deepPink = new Color(255, 20, 147);
    public static final Color deepSkyBlue = new Color(0, 191, 255);
    public static final Color dimGray = new Color(105, 105, 105);
    public static final Color dodgerBlue = new Color(30, 144, 255);
    public static final Color fireBrick = new Color(178, 34, 34);
    public static final Color floralWhite = new Color(255, 250, 240);
    public static final Color forestGreen = new Color(34, 139, 34);
    public static final Color gainsboro = new Color(220, 220, 220);
    public static final Color ghostWhite = new Color(248, 248, 255);
    public static final Color gold = new Color(255, 215, 0);
    public static final Color goldenRod = new Color(218, 165, 32);
    public static final Color grey = new Color(190, 190, 190);
    public static final Color green = new Color(0, 255, 0);
    public static final Color greenYellow = new Color(173, 255, 47);
    public static final Color honeyDew = new Color(240, 255, 240);
    public static final Color hotPink = new Color(255, 105, 180);
    public static final Color indianRed = new Color(205, 92, 92);
    public static final Color indigo = new Color(75, 0, 130);
    public static final Color ivory = new Color(255, 255, 240);
    public static final Color khaki = new Color(240, 230, 140);
    public static final Color lavender = new Color(230, 230, 250);
    public static final Color lavenderBlush = new Color(255, 240, 245);
    public static final Color lawnGreen = new Color(124, 252, 0);
    public static final Color lemonChiffon = new Color(255, 250, 205);
    public static final Color lightBlue = new Color(173, 216, 230);
    public static final Color lightCoral = new Color(240, 128, 128);
    public static final Color lightCyan = new Color(224, 255, 255);
    public static final Color lightGoldenRod = new Color(238, 221, 130);
    public static final Color lightGoldenRodYellow = new Color(250, 250, 210);
    public static final Color lightGreen = new Color(144, 238, 144);
    public static final Color lightGray = new Color(211, 211, 211);
    public static final Color lightPink = new Color(255, 182, 193);
    public static final Color lightSalmon = new Color(255, 160, 122);
    public static final Color lightSeaGreen = new Color(32, 178, 170);
    public static final Color lightSkyBlue = new Color(135, 206, 250);
    public static final Color lightSlateBlue = new Color(132, 112, 255);
    public static final Color lightSlateGray = new Color(119, 136, 153);
    public static final Color lightSteelBlue = new Color(176, 196, 222);
    public static final Color lightYellow = new Color(255, 255, 224);
    public static final Color limeGreen = new Color(50, 205, 50);
    public static final Color linen = new Color(250, 240, 230);
    public static final Color magenta = new Color(255, 0, 255);
    public static final Color maroon = new Color(176, 48, 96);
    public static final Color mediumAquaMarine = new Color(102, 205, 170);
    public static final Color mediumBlue = new Color(0, 0, 205);
    public static final Color mediumOrchid = new Color(186, 85, 211);
    public static final Color mediumPurple = new Color(147, 112, 219);
    public static final Color mediumSeaGreen = new Color(60, 179, 113);
    public static final Color mediumSlateBlue = new Color(123, 104, 238);
    public static final Color mediumSpringGreen = new Color(0, 250, 154);
    public static final Color mediumTurquoise = new Color(72, 209, 204);
    public static final Color mediumVioletRed = new Color(199, 21, 133);
    public static final Color midnightBlue = new Color(25, 25, 112);
    public static final Color mintCream = new Color(245, 255, 250);
    public static final Color mistyRose = new Color(255, 228, 225);
    public static final Color moccasin = new Color(255, 228, 181);
    public static final Color navajoWhite = new Color(255, 222, 173);
    public static final Color navy = new Color(0, 0, 128);
    public static final Color oldLace = new Color(253, 245, 230);
    public static final Color olive = new Color(128, 128, 0);
    public static final Color oliveDrab = new Color(107, 142, 35);
    public static final Color orange = new Color(255, 165, 0);
    public static final Color orangeRed = new Color(255, 69, 0);
    public static final Color orchid = new Color(218, 112, 214);
    public static final Color paleGoldenRod = new Color(238, 232, 170);
    public static final Color paleGreen = new Color(152, 251, 152);
    public static final Color paleTurquoise = new Color(175, 238, 238);
    public static final Color paleVioletRed = new Color(219, 112, 147);
    public static final Color papayaWhip = new Color(255, 239, 213);
    public static final Color peachPuff = new Color(255, 218, 185);
    public static final Color peru = new Color(205, 133, 63);
    public static final Color pink = new Color(255, 192, 203);
    public static final Color plum = new Color(221, 160, 221);
    public static final Color powderBlue = new Color(176, 224, 230);
    public static final Color purple = new Color(160, 32, 240);
    public static final Color red = new Color(255, 0, 0);
    public static final Color rosyBrown = new Color(188, 143, 143);
    public static final Color royalBlue = new Color(65, 105, 225);
    public static final Color saddleBrown = new Color(139, 69, 19);
    public static final Color salmon = new Color(250, 128, 114);
    public static final Color sandyBrown = new Color(244, 164, 96);
    public static final Color seaGreen = new Color(46, 139, 87);
    public static final Color seaShell = new Color(255, 245, 238);
    public static final Color sienna = new Color(160, 82, 45);
    public static final Color silver = new Color(230, 232, 250);
    public static final Color skyBlue = new Color(135, 206, 235);
    public static final Color slateBlue = new Color(106, 90, 205);
    public static final Color slateGray = new Color(112, 128, 144);
    public static final Color snow = new Color(255, 250, 250);
    public static final Color springGreen = new Color(0, 255, 127);
    public static final Color steelBlue = new Color(70, 130, 180);
    public static final Color tan = new Color(210, 180, 140);
    public static final Color teal = new Color(0, 128, 128);
    public static final Color thistle = new Color(216, 191, 216);
    public static final Color tomato = new Color(255, 99, 71);
    public static final Color turquoise = new Color(64, 224, 208);
    public static final Color violet = new Color(238, 130, 238);
    public static final Color violetRed = new Color(208, 32, 144);
    public static final Color wheat = new Color(245, 222, 179);
    public static final Color white = new Color(255, 255, 255);
    public static final Color whiteSmoke = new Color(245, 245, 245);
    public static final Color yellow = new Color(255, 255, 0);
    public static final Color yellowGreen = new Color(154, 205, 50);

    // My additions.
    public static final Color darkYellow = new Color(200, 200, 0);
    public static final Color lightOlive = new Color(150, 150, 0);
    public static final Color brambleGreen1 = new Color(0, 200, 0);
    public static final Color brambleGreen2 = new Color(0, 150, 0);

    public static final Color springBlue = new Color(50, 100, 225);
    public static final Color ankylosaurPurple = new Color(117, 70, 136);
    public static final Color deinosuchusBrown = new Color(193, 121, 43);
    public static final Color ceratopsianBrown = new Color(181, 181, 81);

    // More additions, for Creatures (based on Chit)
    public static final Color ogreRed = new Color(128, 0, 0);
    public static final Color hydraOrange = new Color(255, 132, 21);
    public static final Color behemothGreen = new Color(2, 129, 2);
    public static final Color centaurGold = new Color(129, 129, 1);
    public static final Color colossusPink = new Color(207, 6, 207);
    public static final Color giantBlue = new Color(3, 3, 213);

    // The Player Colors (based on Chit)
    public static final Color BlackColossus = HTMLColor.black;
    public static final Color BlueColossus = new Color(16, 24, 123);
    public static final Color BrownColossus = new Color(120, 40, 40);
    public static final Color GoldColossus = new Color(165, 148, 49);
    public static final Color GreenColossus = new Color(24, 173, 66);
    public static final Color RedColossus = new Color(189, 0, 24);
    // newer Player Colors
    public static final Color OrangeColossus = hydraOrange;
    public static final Color PurpleColossus = colossusPink;
    public static final Color SilverColossus = new Color(153, 153, 153);
    public static final Color SkyColossus = skyBlue;
    public static final Color PineColossus = forestGreen;
    public static final Color IndigoColossus = indigo;

    public static Color stringToColor(String colorName)
    {
        Color theColor;
        try
        {
            Class<?> htmlColor = Class
                .forName("net.sf.colossus.util.HTMLColor");
            Field fieldColor = htmlColor.getDeclaredField(colorName);
            theColor = (Color)fieldColor.get(null);
        }
        catch (Exception e)
        {
            LOGGER.log(Level.WARNING, "I know nothing about color \""
                + colorName + "\", : " + e);
            theColor = Color.black;
        }
        return theColor;
    }

    /** returns "#rrggbb" string that JTextPane can display */
    public static String colorToCode(Color c)
    {
        // mask alpha out, fill with zeros to length 7, cut rightmost 6.
        return ("#" + Integer.toHexString((c.getRGB() & 0xffffff) + 0x1000000)
            .substring(1));
    }

    public static Color invertRGBColor(Color c)
    {
        Color c2 = new Color(255 - c.getRed(), 255 - c.getGreen(),
            255 - c.getBlue());
        return c2;
    }
}
