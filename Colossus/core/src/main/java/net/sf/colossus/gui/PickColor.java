package net.sf.colossus.gui;



import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JFrame;

import net.sf.colossus.client.IOptions;
import net.sf.colossus.game.PlayerColor;
import net.sf.colossus.guiutil.KDialog;
import net.sf.colossus.server.Constants;
import net.sf.colossus.util.HTMLColor;
import net.sf.colossus.util.Options;


/**
 * Class PickColor lets a player choose a color of legion markers.
 * @version $Id$
 * @author David Ripton
 */
@SuppressWarnings("serial")
final class PickColor extends KDialog
{
    // TODO the next two arrays should be members in Constants.PlayerColor
    private static final Color[] background;
    private static final Color[] foreground;
    private PlayerColor color;
    private final SaveWindow saveWindow;

    static
    {
        background = new Color[Constants.MAX_MAX_PLAYERS];
        foreground = new Color[Constants.MAX_MAX_PLAYERS];

        for (int i = 0; i < Constants.MAX_MAX_PLAYERS; i++)
        {
            background[i] = HTMLColor.stringToColor(PlayerColor.values()[i].getName()
                + "Colossus");
            int sum = background[i].getRed() + background[i].getGreen()
                + background[i].getBlue();
            foreground[i] = (sum > 200 ? Color.black : Color.white);
        }
    }

    private PickColor(JFrame parentFrame, String playerName,
        List<PlayerColor> colorsLeft, IOptions options)
    {
        super(parentFrame, playerName + ", Pick a Color", true);

        color = null;

        setBackground(Color.lightGray);
        pack();

        Container contentPane = getContentPane();
        contentPane.setLayout(new GridLayout(3, 4));

        for (int i = 0; i < Constants.MAX_MAX_PLAYERS; i++)
        {
            final PlayerColor curColor = PlayerColor.values()[i];
            if (colorsLeft.contains(curColor))
            {
                JButton button = new JButton();
                int scale = Scale.get();
                button.setPreferredSize(new Dimension(7 * scale, 3 * scale));
                button.setText(curColor.getName());
                button.setMnemonic(curColor.getMnemonic());
                button.setBackground(background[i]);
                button.setForeground(foreground[i]);
                button.addActionListener(new ActionListener()
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        color = PlayerColor.getByName(e.getActionCommand());
                        saveWindow.saveLocation(getLocation());
                        dispose();
                    }
                });
                contentPane.add(button);
            }
        }

        pack();
        saveWindow = new SaveWindow(options, "PickColor");
        Point location = saveWindow.loadLocation();
        if (location == null)
        {
            centerOnScreen();
        }
        else
        {
            setLocation(location);
        }
        setVisible(true);
    }

    private PlayerColor getColor()
    {
        return color;
    }

    static synchronized PlayerColor pickColor(JFrame parentFrame,
        String playerName, List<PlayerColor> colorsLeft, IOptions options)
    {
        PickColor pc = new PickColor(parentFrame, playerName, colorsLeft,
            options);
        return pc.getColor();
    }

    static String getColorName(int i)
    {
        if (i >= 0 && i < PlayerColor.values().length)
        {
            return PlayerColor.values()[i].getName();
        }
        return null;
    }

    static Color getForegroundColor(PlayerColor playerColor)
    {
        for (int i = 0; i < PlayerColor.values().length; i++)
        {
            if (playerColor.equals(PlayerColor.values()[i]))
            {
                return foreground[i]; // TODO this could probably be expressed via playerColor.ordinal()
            }
        }
        return null;
    }

    static Color getBackgroundColor(PlayerColor color)
    {
        for (int i = 0; i < PlayerColor.values().length; i++)
        {
            if (color.equals(PlayerColor.values()[i]))
            {
                return background[i];
            }
        }
        return null;
    }

    public static void main(String[] args)
    {
        Logger logger = Logger.getLogger(PickColor.class.getName());
        List<PlayerColor> colorsLeft = Arrays.asList(PlayerColor.values());
        Options options = new Options("Player");
        PlayerColor color = pickColor(new JFrame(), "Player", colorsLeft, options);
        logger.info("Picked " + color.getName());
        System.exit(0);
    }
}
