package net.sf.colossus.client;


import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;

import net.sf.colossus.server.Constants;
import net.sf.colossus.server.Creature;
import net.sf.colossus.util.HTMLColor;


/**
 * Class BattleChit implements the GUI for a Titan chit representing
 * a creature on a BattleMap.
 * @version $Id$
 * @author David Ripton
 */

final class BattleChit extends Chit
{
    private int tag;
    private static Font font;
    private static Font oldFont;
    private static int fontHeight;
    private int hits = 0;
    private String currentHexLabel;
    private String startingHexLabel;
    private boolean moved;
    private boolean struck;
    private String colorName;
    private Color color;
    private static BasicStroke borderStroke;
    private Rectangle midRect;
    private Rectangle outerRect;
    private int scale;

    // inner scale divided by border thickness
    static final int borderRatio = 20;
    private static boolean useColoredBorders = false;

    BattleChit(int scale, String id, Container container, boolean inverted,
            int tag, String currentHexLabel, String colorName, Client client)
    {
        super(scale, id, container, inverted);
        this.scale = scale;
        this.tag = tag;
        this.currentHexLabel = currentHexLabel;
        this.colorName = colorName;
        this.client = client;
        color = HTMLColor.stringToColor(colorName + "Colossus");
        setBackground(Color.white);
    }

    int getTag()
    {
        return tag;
    }

    int getHits()
    {
        return hits;
    }

    void setHits(int hits)
    {
        this.hits = hits;
        repaint();
    }

    boolean wouldDieFrom(int hits)
    {
        return (hits + getHits() >= getPower());
    }

    void setDead(boolean dead)
    {
        super.setDead(dead);
        if (dead)
        {
            setHits(0);
        }
    }

    String getCurrentHexLabel()
    {
        return currentHexLabel;
    }

    String getStartingHexLabel()
    {
        return startingHexLabel;
    }

    void setHexLabel(String hexLabel)
    {
        this.currentHexLabel = hexLabel;
    }

    void setCurrentHexLabel(String hexLabel)
    {
        this.currentHexLabel = hexLabel;
    }

    void moveToHex(String hexLabel)
    {
        if (!hexLabel.equals(startingHexLabel))
        {
            startingHexLabel = currentHexLabel;
        }
        currentHexLabel = hexLabel;
    }

    boolean hasMoved()
    {
        return moved;
    }

    void setMoved(boolean moved)
    {
        this.moved = moved;
    }

    boolean hasStruck()
    {
        return struck;
    }

    void setStruck(boolean struck)
    {
        this.struck = struck;
    }

    public String getCreatureName()
    {
        if (getId().startsWith(Constants.titan))
        {
            return Constants.titan;
        }
        return getId();
    }

    public String getName()
    {
        return getCreatureName();
    }

    boolean isTitan()
    {
        return getCreatureName().equals(Constants.titan);
    }

    int getPower()
    {
        if (getId().startsWith("Titan-"))
        {
            return getTitanPower();
        }
        else
        {
            return getCreature().getPower();
        }
    }

    int getSkill()
    {
        return getCreature().getSkill();
    }

    int getPointValue()
    {
        return getPower() * getSkill();
    }

    boolean isRangestriker()
    {
        return getCreature().isRangestriker();
    }

    // XXX Titans
    Creature getCreature()
    {
        Creature creature = Creature.getCreatureByName(getCreatureName());
        return creature;
    }

    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D)g;

        if (hits > 0 && !isDead())
        {
            String hitString = Integer.toString(hits);
            FontMetrics fontMetrics;

            // Construct a font twice the size of the current font.
            if (font == null)
            {
                oldFont = g2.getFont();
                String name = oldFont.getName();
                int size = oldFont.getSize();
                int style = oldFont.getStyle();
                font = new Font(name, style, 2 * size);
                g2.setFont(font);
                fontMetrics = g2.getFontMetrics();
                fontHeight = 4 * fontMetrics.getAscent() / 5;
            }
            else
            {
                g2.setFont(font);
                fontMetrics = g2.getFontMetrics();
            }
            int fontWidth = fontMetrics.stringWidth(hitString);

            // Provide a high-contrast background for the number.
            g2.setColor(Color.white);
            g2.fillRect(rect.x + (rect.width - fontWidth) / 2,
                    rect.y + (rect.height - fontHeight) / 2,
                    fontWidth, fontHeight);

            // Show number of hits taken in red.
            g2.setColor(Color.red);
            g2.drawString(hitString, rect.x + (rect.width - fontWidth) / 2,
                    rect.y + (rect.height + fontHeight) / 2);

            // Restore the font.
            g2.setFont(oldFont);

        }
        if (useColoredBorders)
        {
            // Draw border using player color.
            g2.setColor(color);
            g2.setStroke(borderStroke);
            g2.drawRect(midRect.x, midRect.y, midRect.width, midRect.height);
            g2.setColor(Color.black);
            g2.setStroke(oneWide);
            g2.drawRect(outerRect.x, outerRect.y, outerRect.width,
                    outerRect.height);
        }
    }

    public void setLocation(Point point)
    {
        outerRect.setLocation(point);
        setBounds(outerRect);
    }

    public boolean contains(Point point)
    {
        return outerRect.contains(point);
    }

    public Rectangle getBounds()
    {
        return outerRect;
    }

    public void setBounds(Rectangle outerRect)
    {
        this.outerRect = outerRect;
        int innerScale = (int)(outerRect.width / (1.0 + 2.0 / borderRatio));
        // avoid rescaling if possible
        if (innerScale > 50 && innerScale < 70)
        {
            innerScale = 60;
        }
        borderStroke = new BasicStroke((int)Math.ceil(
                (outerRect.width - innerScale) / 2.0));
        Point center = new Point(outerRect.x + outerRect.width / 2,
                outerRect.y + outerRect.height / 2);
        rect = new Rectangle(center.x - innerScale / 2,
                center.y - innerScale / 2, innerScale, innerScale);
        int midScale = (int)(Math.round((scale + innerScale) / 2.0));
        midRect = new Rectangle(center.x - midScale / 2,
                center.y - midScale / 2, midScale, midScale);
    }

    public String getDescription()
    {
        return getCreatureName() + " in " + getCurrentHexLabel();
    }

    public String toString()
    {
        return getDescription();
    }

    public static void setUseColoredBorders(boolean bval)
    {
        useColoredBorders = bval;
    }
}
