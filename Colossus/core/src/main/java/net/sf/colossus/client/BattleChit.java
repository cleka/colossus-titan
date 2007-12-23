package net.sf.colossus.client;


import java.awt.BasicStroke;
import java.awt.Color;
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
 * 
 * TODO this is a pretty wild mixture of GUI code with game logic -- there
 * is no representation of the creature in battle in the model, so this GUI
 * class does all that work, too.
 * 
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
    private Color color;
    private static BasicStroke borderStroke;
    private Rectangle midRect;
    private Rectangle outerRect;
    private int strikeNumber; // Number required for successful strike.
    private int numDice; // modifier for number of Dice rolled.
    private StrikeDie strikeDie; // Graphical representation of strikeNumber.
    private StrikeDie strikeAdjDie; // representation of dice gained or lost.
    private int scale;

    // inner scale divided by border thickness
    static final int borderRatio = 20;
    private static boolean useColoredBorders = false;

    BattleChit(int scale, String id, boolean inverted, int tag,
        String currentHexLabel, String colorName, Client client)
    {
        super(scale, id, inverted);
        this.scale = scale;
        this.tag = tag;
        this.currentHexLabel = currentHexLabel;
        this.client = client;
        this.color = HTMLColor.stringToColor(colorName + "Colossus");
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

        if (!isDead())
        {
            // The power and skill are drawn with a font that is one fifth the
            // size of the Chit.
            // Construct a font that is two fifths the size of the chit
            // for drawing Hits and strike die adjustments.
            // All overlays (hits, dice) have gaps of 2.
            oldFont = g2.getFont();
            String name = oldFont.getName();
            int fifthChitSize = (rect.height > rect.width 
                ? (rect.width - 8) / 5
                : (rect.height - 8) / 5);
            int style = oldFont.getStyle();
            font = new Font(name, style, fifthChitSize * 2);
            g2.setFont(font);
            FontMetrics fontMetrics = g2.getFontMetrics();
            fontHeight = fontMetrics.getAscent();
            
            String hitString = Integer.toString(hits);
            int hitsFontWidth = fontMetrics.stringWidth(hitString);

         // Setup spaces to show Hits and Strike Target.
            Rectangle hitRect = new Rectangle();
            if (strikeNumber > 0)
            {
                Rectangle strikeRect = strikeDie.getBounds();
                Point point = new Point(rect.x + rect.width - strikeRect.width
                    - 2, (inverted ? rect.y + fifthChitSize + 4 : rect.y + 2));
                strikeDie.setLocation(point);
                strikeDie.paintComponent(g2);
                if (numDice != 0)
                {
                    String diceString = numDice < 0 ? Integer
                        .toString(numDice) : "+" + Integer.toString(numDice);
                    Point dicePoint = new Point(point.x, point.y
                        + (fifthChitSize * 2) + 2);
                    strikeAdjDie.setLocation(dicePoint);
                    strikeAdjDie.paintComponent(g2);
                    g2.setColor(Color.GREEN);
                    g2.drawString(diceString, dicePoint.x, dicePoint.y
                        + strikeRect.height - 2);
                }          
            }
            if (hits > 0)
            {
                hitRect = new Rectangle(rect.x + 2, rect.y + 2
                    + (inverted ? fifthChitSize + 2 : 0), hitsFontWidth,
                    fontHeight);

                // Provide a high-contrast background for the number.
                g2.setColor(Color.white);
                g2.fillRect(hitRect.x, hitRect.y, hitRect.width,
                    hitRect.height);
                
                // Show number of hits taken in red.
                g2.setColor(Color.red);
                g2.drawString(hitString, hitRect.x, hitRect.y + fontHeight);
            }
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

    public int getStrikeNumber()
    {
        return strikeNumber;
    }

    public void setStrikeNumber(int strikeNumber)
    {
        this.strikeNumber = strikeNumber;
        if (strikeNumber > 0)
        {
            int fifthChitSize = (rect.height > rect.width ? (rect.width - 8) / 5
                : (rect.height - 8) / 5);
            strikeDie = new StrikeDie(fifthChitSize * 2, strikeNumber, "Hit");
            strikeDie.setToolTipText("Test");
            this.add(strikeDie);
            strikeAdjDie = new StrikeDie(fifthChitSize * 2, strikeNumber, 
                "RedBlue");
        }
        else
        {
            strikeDie = null;
            strikeAdjDie = null;
        }
    }

    public void setStrikeDice(int numDice)
    {
        this.numDice = numDice;
    }

}
