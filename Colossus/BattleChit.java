import java.awt.*;

/**
 * Class BattleChit implements the GUI for a Titan chit representing
 * a creature on a BattleMap.
 * @version $Id$
 * @author David Ripton
 */

public class BattleChit extends Chit
{
    private Critter critter;


    public BattleChit(int scale, String id, Container container, 
        Critter critter)
    {
        super(scale, id, container);
        this.critter = critter;
    }


    public Critter getCritter()
    {
        return critter;
    }


    // Override the method inherited from Chit.
    public boolean isDead()
    {
        return critter.isDead();
    }


    //public void paintComponent(Graphics g)
    public void paint(Graphics g)
    {
        //super.paintComponent(g);
        super.paint(g);

        if (critter.getHits() > 0 && !isDead())
        {
            String hitString = Integer.toString(critter.getHits());
            Rectangle rect = getBounds();

            // Construct a font 3 times the size of the current font.
            Font oldFont = g.getFont();
            String name = oldFont.getName();
            int size = oldFont.getSize();
            int style = oldFont.getStyle();
            Font font = new Font(name, style, 3 * size);
            g.setFont(font);

            FontMetrics fontMetrics = g.getFontMetrics();
            int fontHeight = fontMetrics.getAscent();
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
