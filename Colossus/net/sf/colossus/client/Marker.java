package net.sf.colossus.client;


import java.awt.*;


/**
 * Class Marker implements the GUI for a legion marker.
 * @version $Id$
 * @author David Ripton
 */

public final class Marker extends Chit
{
    private Font font;
    private Font oldFont;
    private int fontHeight;
    private int fontWidth;
    private Client client;


    public Marker(int scale, String id, Container container, Client client)
    {
        super(scale, id, container);
        setBackground(Color.black);
        this.client = client;
    }


    /** Show the height of the legion. */
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        // Draw a one-pixel-wide border around the outside
        // edge of the marker.  Use black, unless the marker
        // is black, in which case use white.
        if (getId().startsWith("Bk"))
        {
            g.setColor(Color.white);
        }
        else
        {
            g.setColor(Color.black);
        }
        Rectangle rect = getBounds();
        g.drawRect(rect.x, rect.y, rect.width, rect.height);

        if (client == null)
        {
            return;
        }

        String height = Integer.toString(client.getLegionHeight(getId()));

        // Construct a font 1.5 times the size of the current font.
        if (font == null)
        {
            oldFont = g.getFont();
            String name = oldFont.getName();
            int size = oldFont.getSize();
            int style = oldFont.getStyle();
            font = new Font(name, style, 3 * size / 2);
            g.setFont(font);
            FontMetrics fontMetrics = g.getFontMetrics();
            // XXX getAscent() seems to return too large a number
            // Test this 80% fudge factor on multiple platforms.
            fontHeight = 4 * fontMetrics.getAscent() / 5;
            fontWidth = fontMetrics.stringWidth(height);
        }
        else
        {
            g.setFont(font);
        }

        int x = rect.x + rect.width * 3 / 4 - fontWidth / 2;
        int y = rect.y + rect.height * 2 / 3 + fontHeight / 2;

        // Provide a high-contrast background for the number.
        g.setColor(Color.white);
        g.fillRect(x, y - fontHeight, fontWidth, fontHeight);

        // Show height in black.
        g.setColor(Color.black);
        g.drawString(height, x, y);

        // Restore the font.
        g.setFont(oldFont);
    }
}
