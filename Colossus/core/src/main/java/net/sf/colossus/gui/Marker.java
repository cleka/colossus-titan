package net.sf.colossus.gui;


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
import net.sf.colossus.game.Legion;


/**
 * Class Marker implements the GUI for a legion marker.
 *
 * TODO this really represents a whole legion (since it shows the height), so
 *      it probably should store a Legion object instead of the marker ID
 * TODO after carve out of GUI stuff Marker should probably not be accessed
 *      by client at all - need cleanup with Legion ?
 *
 * @author David Ripton
 */
public final class Marker extends Chit
{
    private static final Logger LOGGER = Logger.getLogger(Marker.class
        .getName());

    private final Legion legion;
    private final boolean showHeight;
    private Font font;
    private int fontHeight;
    private int fontWidth;
    String hexLabel;
    private boolean highlight;

    /*
     * TODO get rid of markerId argument, derive it from legion argument
     * here by ourselves.
     * Not possible yet, at least in SplitLegion, PickMarker and RevealEvent
     * for legion destroyed in battle there is no child legion yet/any more.
     */

    /**
     * Construct a marker without a client.
     * Use this constructor as a bit of documentation when
     * explicitly not wanting a height drawn on the Marker.
     *
     * Use case: The dialogs where legion height is not so important or legion
     * does not even exist (PickMarker, SplitLegion, in RevealEvent for the
     * destroyed legion)
     */
    Marker(Legion legion, int scale, String id)
    {
        this(legion, scale, id, null, false, false);
    }

    /**
     * Construct a marker with a client (to be able to ask for
     * doNotInvertOption) but showHeight set to false and
     * specified inverted display (for defender)
     *
     * Use case: Marker on the battle map
     *
     * @param client A client, only used to ask for options
     */
    Marker(Legion legion, int scale, String id, boolean inverted, Client client)
    {
        this(legion, scale, id, client, inverted, false);
    }

    /**
     * Construct a marker where height is shown - will be asked from legion.
     * Sometimes (on the master board, for example) heights should be shown,
     * and sometimes (in some dialogs, especially when there is no real legion
     * behind it (e.g. pickMarker, splitLegion)) they should be omitted
     * (or cannot even be asked).
     *
     * Use case: Mostly MasterBoard and some dialogs where height is
     * interesting:  Concede/Flee, Negotiate and replyToProposal
     *
     * @param client A client, only used to ask for options
     */
    Marker(Legion legion, int scale, String id, Client client,
        boolean showHeight)
    {
        this(legion, scale, id, client, false, showHeight);
    }

    /**
     * Construct a marker
     * @param id the marker label (like Bk05 or Bk05-Green)
     * @scale the Scale of chit
     * @param showHeight set true will add the height of the stack
     * @param inverted set to true (defender legion) will normally invert
     * the marker but NOT if doNotInvertDefender option is true
     */
    private Marker(Legion legion, int scale, String id, Client client,
        boolean inverted, boolean showHeight)
    {
        super(scale, id, inverted, client);

        assert (!showHeight || legion != null) : "for showHeight true, "
            + "legion must not be null!";

        this.legion = legion;
        this.showHeight = showHeight;

        setBackground(Color.BLACK);
        if (id.contains("Black") || (id.length() == 4 && id.startsWith("Bk")))
        {
            setBorderColor(Color.white);
        }
    }

    /** this is only used by Battle markers marking entrances. */
    void setLocation(Point point, String hexLabel)
    {
        setLocation(point);
        this.hexLabel = hexLabel;

    }

    /* Highlight borders of Markers on BattleMap */
    void highlightMarker()
    {
        highlight = true;
    }

    void resetMarkerHighlight()
    {
        highlight = false;
    }

    @Override
    public void paintComponent(Graphics g)
    {
        LOGGER.log(Level.FINEST, "Painting marker");
        Graphics2D g2 = (Graphics2D)g;
        if (highlight)
        {
            g2.setColor(Color.RED);
            Rectangle rect = getBounds();
            g.fillRect(rect.x - 4, rect.y - 4, rect.width + 8, rect.height + 8);
        }
        super.paintComponent(g2);

        if (!showHeight)
        {
            return; //no height labels wanted
        }

        int legionHeight = legion.getHeight();
        String legionHeightString = Integer.toString(legionHeight);
        LOGGER.log(Level.FINEST, "Height is " + legionHeightString);

        // Construct a font 1.5 times the size of the current font.
        Font oldFont = g.getFont();
        if (font == null)
        {
            String name = oldFont.getName();
            int size = oldFont.getSize();
            int style = oldFont.getStyle();
            font = new Font(name, style, 3 * size / 2);
            g.setFont(font);
            FontMetrics fontMetrics = g.getFontMetrics();
            // XXX getAscent() seems to return too large a number
            // Test this 80% fudge factor on multiple platforms.
            fontHeight = 4 * fontMetrics.getAscent() / 5;
            fontWidth = fontMetrics.stringWidth(legionHeightString);
            if (LOGGER.isLoggable(Level.FINEST))
            {
                LOGGER.log(Level.FINEST, "New font set: " + font);
                LOGGER.log(Level.FINEST, "New font height: " + fontHeight);
                LOGGER.log(Level.FINEST, "New font width: " + fontWidth);
            }
        }
        else
        {
            g.setFont(font);
        }

        if (LOGGER.isLoggable(Level.FINEST))
        {
            LOGGER.log(Level.FINEST, "Our rectangle is: " + rect);
        }
        int x = rect.x + rect.width * 3 / 4 - fontWidth / 2;
        int y = rect.y + rect.height * 2 / 3 + fontHeight / 2;

        // Provide a high-contrast background for the number.
        g.setColor(Color.white);
        g.fillRect(x, y - fontHeight, fontWidth, fontHeight);

        // Show height in black.
        g.setColor(Color.black);
        g.drawString(legionHeightString, x, y);

        // Restore the font.
        g.setFont(oldFont);
    }
}
