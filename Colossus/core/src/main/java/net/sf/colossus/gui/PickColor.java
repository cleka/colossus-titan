package net.sf.colossus.gui;


import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowListener;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JFrame;

import net.sf.colossus.client.IOptions;
import net.sf.colossus.server.Constants;
import net.sf.colossus.util.HTMLColor;
import net.sf.colossus.util.KDialog;
import net.sf.colossus.util.Options;


/**
 * Class PickColor lets a player choose a color of legion markers.
 * @version $Id$
 * @author David Ripton
 */
@SuppressWarnings("serial")
final class PickColor extends KDialog implements WindowListener,
    ActionListener
{
    // TODO the next two arrays should be members in Constants.PlayerColor
    private static final Color[] background;
    private static final Color[] foreground;
    private Constants.PlayerColor color;
    private final SaveWindow saveWindow;

    static
    {
        background = new Color[Constants.MAX_MAX_PLAYERS];
        foreground = new Color[Constants.MAX_MAX_PLAYERS];

        for (int i = 0; i < Constants.MAX_MAX_PLAYERS; i++)
        {
            background[i] = HTMLColor.stringToColor(Constants.PlayerColor.values()[i].getName()
                + "Colossus");
            int sum = background[i].getRed() + background[i].getGreen()
                + background[i].getBlue();
            foreground[i] = (sum > 200 ? Color.black : Color.white);
        }
    }

    private PickColor(JFrame parentFrame, String playerName,
        List<Constants.PlayerColor> colorsLeft, IOptions options)
    {
        super(parentFrame, playerName + ", Pick a Color", true);

        color = null;

        setBackground(Color.lightGray);
        pack();

        Container contentPane = getContentPane();
        contentPane.setLayout(new GridLayout(3, 4));

        for (int i = 0; i < Constants.MAX_MAX_PLAYERS; i++)
        {
            final Constants.PlayerColor curColor = Constants.PlayerColor.values()[i];
            if (colorsLeft.contains(curColor))
            {
                JButton button = new JButton();
                int scale = Scale.get();
                button.setPreferredSize(new Dimension(7 * scale, 3 * scale));
                button.setText(curColor.getName());
                button.setMnemonic(curColor.getMnemonic());
                button.setBackground(background[i]);
                button.setForeground(foreground[i]);
                button.addActionListener(this);
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
        addWindowListener(this);
        setVisible(true);
    }

    private Constants.PlayerColor getColor()
    {
        return color;
    }

    static synchronized Constants.PlayerColor pickColor(JFrame parentFrame,
        String playerName, List<Constants.PlayerColor> colorsLeft, IOptions options)
    {
        PickColor pc = new PickColor(parentFrame, playerName, colorsLeft,
            options);
        return pc.getColor();
    }

    static String getColorName(int i)
    {
        if (i >= 0 && i < Constants.PlayerColor.values().length)
        {
            return Constants.PlayerColor.values()[i].getName();
        }
        return null;
    }

    static Color getForegroundColor(Constants.PlayerColor playerColor)
    {
        for (int i = 0; i < Constants.PlayerColor.values().length; i++)
        {
            if (playerColor.equals(Constants.PlayerColor.values()[i]))
            {
                return foreground[i]; // TODO this could probably be expressed via playerColor.ordinal()
            }
        }
        return null;
    }

    static Color getBackgroundColor(Constants.PlayerColor color)
    {
        for (int i = 0; i < Constants.PlayerColor.values().length; i++)
        {
            if (color.equals(Constants.PlayerColor.values()[i]))
            {
                return background[i];
            }
        }
        return null;
    }

    public void actionPerformed(ActionEvent e)
    {
        color = Constants.PlayerColor.getByName(e.getActionCommand());
        saveWindow.saveLocation(getLocation());
        dispose();
    }

    public static void main(String[] args)
    {
        Logger logger = Logger.getLogger(PickColor.class.getName());
        List<Constants.PlayerColor> colorsLeft = Arrays.asList(Constants.PlayerColor.values());
        Options options = new Options("Player");
        Constants.PlayerColor color = pickColor(new JFrame(), "Player", colorsLeft, options);
        logger.info("Picked " + color.getName());
        System.exit(0);
    }
}
