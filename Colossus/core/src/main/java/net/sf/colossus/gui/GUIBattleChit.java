package net.sf.colossus.gui;


import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.client.Client;
import net.sf.colossus.common.Constants;
import net.sf.colossus.game.BattleUnit;
import net.sf.colossus.game.PlayerColor;
import net.sf.colossus.util.HTMLColor;


public class GUIBattleChit extends Chit
{
    private static final Logger LOGGER = Logger.getLogger(GUIBattleChit.class
        .getName());

    private final BattleUnit battleUnit;
    private static Font font;
    private static Font oldFont;
    private static int fontHeight;
    private int hits = 0;
    private final Color color;
    private static BasicStroke borderStroke;
    private Rectangle midRect;
    private Rectangle outerRect;
    private int strikeNumber; // Number required for successful strike.
    private int numDice; // modifier for number of Dice rolled.
    private StrikeDie strikeDie; // Graphical representation of strikeNumber.
    private StrikeDie strikeAdjDie; // representation of dice gained or lost.
    private final int scale;

    // inner scale divided by border thickness
    private static final int borderRatio = 20;
    private static boolean useColoredBorders = false;

    public GUIBattleChit(int scale, String id, boolean inverted,
        PlayerColor playerColor, Client client, BattleUnit battleUnit)
    {
        super(scale, id, inverted, client);
        if (id == null)
        {
            LOGGER.log(Level.WARNING, "Created GUIBattleChit with null id!");
        }
        this.battleUnit = battleUnit;

        battleUnit.addListener(battleUnit.new Listener()
        {
            @Override
            public void actOnHitOrDeadChanged()
            {
                // TODO Auto-generated method stub
                updateAndRepaint();
            }
        });

        this.scale = scale;

        this.color = HTMLColor.stringToColor(playerColor.getName()
            + "Colossus");

        setBackground(Color.WHITE);

    }

    // TODO does asking from BattleUnit / BattleCreature give same result?
    public String getCreatureName()
    {
        String id = getId();
        if (id.startsWith(Constants.titan))
        {
            id = Constants.titan;
        }

        String buName = battleUnit.getType().getName();
        if (!buName.equals(id))
        {
            LOGGER.warning("own name is " + id + " but battleUnit gave us "
                + buName + "!");
        }
        return id;
    }

    public String getDescription()
    {
        return battleUnit.getType().getName() + " in "
            + battleUnit.getCurrentHex().getLabel();
    }

    @Override
    public String toString()
    {
        return getDescription();
    }

    public int getTag()
    {
        return battleUnit.getTag();
    }

    public BattleUnit getBattleUnit()
    {
        return battleUnit;
    }

    public void updateAndRepaint()
    {
        this.hits = battleUnit.getHits();
        setDead(battleUnit.isDead());
    }

    @Override
    public void setDead(boolean dead)
    {
        if (dead)
        {
            // TODO this looks like a bad trick: set chit's hit to 0 so that
            // no number is painted. Shouldn't that "paint nr or not" rather
            // in chit itself decide based on "hits" whether to paint the
            // number?
            this.hits = 0;
        }
        // Chit.setDead() triggers repaint
        super.setDead(dead);
    }

    @Override
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
            int fifthChitSize = (rect.height > rect.width ? (rect.width - 8) / 5
                : (rect.height - 8) / 5);
            int style = oldFont.getStyle();
            font = new Font(name, style, fifthChitSize * 2);
            g2.setFont(font);
            FontMetrics fontMetrics = g2.getFontMetrics();
            fontHeight = fontMetrics.getAscent();

            String hitString = Integer.toString(hits);
            int hitsFontWidth = fontMetrics.stringWidth(hitString);

            // Setup spaces to show Hits and Strike Target.
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
                Rectangle hitRect = new Rectangle(rect.x + 2, rect.y + 2
                    + (inverted ? fifthChitSize + 2 : 0), hitsFontWidth,
                    fontHeight);

                // Provide a high-contrast background for the number.
                g2.setColor(Color.WHITE);
                g2.fillRect(hitRect.x, hitRect.y, hitRect.width,
                    hitRect.height);

                // Show number of hits taken in red.
                g2.setColor(Color.RED);
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
            g2.setColor(Color.BLACK);
            g2.setStroke(oneWide);
            g2.drawRect(outerRect.x, outerRect.y, outerRect.width,
                outerRect.height);
        }
    }

    @Override
    public void setLocation(Point point)
    {
        outerRect.setLocation(point);
        setBounds(outerRect);
    }

    @Override
    public boolean contains(Point point)
    {
        return outerRect.contains(point);
    }

    @Override
    public Rectangle getBounds()
    {
        return outerRect;
    }

    @Override
    public void setBounds(Rectangle outerRect)
    {
        this.outerRect = outerRect;
        int innerScale = (int)(outerRect.width / (1.0 + 2.0 / borderRatio));
        // avoid rescaling if possible
        if (innerScale > 50 && innerScale < 70)
        {
            innerScale = 60;
        }
        borderStroke = new BasicStroke(
            (int)Math.ceil((outerRect.width - innerScale) / 2.0));
        Point center = new Point(outerRect.x + outerRect.width / 2,
            outerRect.y + outerRect.height / 2);
        rect = new Rectangle(center.x - innerScale / 2, center.y - innerScale
            / 2, innerScale, innerScale);
        int midScale = (int)(Math.round((scale + innerScale) / 2.0));
        midRect = new Rectangle(center.x - midScale / 2, center.y - midScale
            / 2, midScale, midScale);
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

    protected static void setUseColoredBorders(boolean bval)
    {
        useColoredBorders = bval;
    }
}
