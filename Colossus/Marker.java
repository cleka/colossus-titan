import java.awt.*;

/**
 * Class Marker implements the GUI for a legion marker.
 * @version $Id$
 * @author David Ripton
 */

public class Marker extends Chit
{
    private Legion legion;
    private static Font font;
    private static Font oldFont;
    private static int fontHeight; 
    private static int fontWidth;


    public Marker(int scale, String id, Container container, Legion legion)
    {
        super(scale, id, container);
        this.legion = legion;
        setBackground(Color.black);
    }


    public void setLegion(Legion legion)
    {
        this.legion = legion;
    }


    /** Show the height of the legion. */
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        if (legion == null)
        {
            return;
        }

        String height = Integer.toString(legion.getHeight());
        Rectangle rect = getBounds();

        // Construct a font 1.5 times the size of the current font.
        if (font == null)
        {
            oldFont = g.getFont();
            String name = oldFont.getName();
            int size = oldFont.getSize();
            int style = oldFont.getStyle();
            font = new Font(name, style, (3 * size) >> 1);
            g.setFont(font);
            FontMetrics fontMetrics = g.getFontMetrics();
            fontHeight = fontMetrics.getAscent();
            fontWidth = fontMetrics.stringWidth(height);
        }
        else
        {
            g.setFont(font);
        }

        // Show height in white.
        g.setColor(Color.white);

        g.drawString(height, rect.x + ((rect.width * 3) >> 2)  -
            (fontWidth >> 1), rect.y + rect.height * 2 / 3 +
            (fontHeight / 2));

        // Restore the font.
        g.setFont(oldFont);
    }
}
