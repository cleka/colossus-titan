package net.sf.colossus.client;


import java.awt.*;


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


    BattleChit(int scale, String id, Container container, boolean inverted,
        int tag)
    {
        super(scale, id, container, inverted);
        this.tag = tag;
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

    void setDead(boolean dead)
    {
        super.setDead(dead);
        if (dead)
        {
            setHits(0);
        }
    }


    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        if (hits > 0 && !isDead())
        {
            String hitString = Integer.toString(hits);
            Rectangle rect = getBounds();
            FontMetrics fontMetrics;

            // Construct a font twice the size of the current font.
            if (font == null)
            {
                oldFont = g.getFont();
                String name = oldFont.getName();
                int size = oldFont.getSize();
                int style = oldFont.getStyle();
                font = new Font(name, style, 2 * size);
                g.setFont(font);
                fontMetrics = g.getFontMetrics();
                fontHeight = 4 * fontMetrics.getAscent() / 5;
            }
            else
            {
                g.setFont(font);
                fontMetrics = g.getFontMetrics();
            }
            int fontWidth = fontMetrics.stringWidth(hitString);

            // Provide a high-contrast background for the number.
            g.setColor(Color.white);
            g.fillRect(rect.x + (rect.width - fontWidth) / 2,
                rect.y + (rect.height - fontHeight) / 2,
                fontWidth, fontHeight);

            // Show number of hits taken in red.
            g.setColor(Color.red);
            g.drawString(hitString, rect.x + (rect.width - fontWidth) / 2,
                rect.y + (rect.height + fontHeight) / 2);

            // Restore the font.
            g.setFont(oldFont);
        }
    }
}
