import java.awt.*;
import javax.swing.*;

/**
 * Class Marker implements the GUI for a legion marker.
 * @version $Id$
 * @author David Ripton
 */

public class Marker extends Chit
{
    private Legion legion;


    public Marker(int scale, String id, Container container, Legion legion)
    {
        super(scale, id, container);
        this.legion = legion;
    }


    public void setLegion(Legion legion)
    {
        this.legion = legion;
    }


    /** Show the height of the legion. */
    //public void paintComponent(Graphics g)
    public void paint(Graphics g)
    {
        //super.paintComponent(g);
        super.paint(g);

        if (legion == null)
        {
            return;
        }

        String height = Integer.toString(legion.getHeight());
        Rectangle rect = getBounds();

        // Construct a font 1.5 times the size of the current font.
        Font oldFont = g.getFont();
        String name = oldFont.getName();
        int size = oldFont.getSize();
        int style = oldFont.getStyle();
        Font font = new Font(name, style, 3 * size / 2);
        g.setFont(font);

        FontMetrics fontMetrics = g.getFontMetrics();
        int fontHeight = fontMetrics.getAscent();
        int fontWidth = fontMetrics.stringWidth(height);

        // Show height in white.
        g.setColor(Color.white);

        g.drawString(height, rect.x + rect.width * 3 / 4  - fontWidth / 2,
            rect.y + rect.height * 2 / 3 + fontHeight / 2);

        // Restore the font.
        g.setFont(oldFont);
    }
}
