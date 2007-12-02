package net.sf.colossus.client;


import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowListener;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JFrame;

import net.sf.colossus.server.Constants;
import net.sf.colossus.util.HTMLColor;
import net.sf.colossus.util.KDialog;
import net.sf.colossus.util.Options;


/**
 * Class PickColor lets a player choose a color of legion markers.
 * @version $Id$
 * @author David Ripton
 */


final class PickColor extends KDialog implements WindowListener, ActionListener
{
    private static final Color[] background;
    private static final Color[] foreground;
    private static String color;
    private SaveWindow saveWindow;

    static
    {
        background = new Color[Constants.MAX_MAX_PLAYERS];
        foreground = new Color[Constants.MAX_MAX_PLAYERS];

        for (int i = 0; i < Constants.MAX_MAX_PLAYERS; i++)
        {
            background[i] = HTMLColor.stringToColor(Constants.colorNames[i] +
                "Colossus");
            int sum =
                background[i].getRed() +
                background[i].getGreen() +
                background[i].getBlue();
            foreground[i] = (sum > 200 ? Color.black : Color.white);
        }
    }

    private PickColor(JFrame parentFrame, String playerName,
        List colorsLeft, IOptions options)
    {
        super(parentFrame, playerName + ", Pick a Color", true);

        color = null;

        setBackground(Color.lightGray);
        pack();

        Container contentPane = getContentPane();
        contentPane.setLayout(new FlowLayout());

        for (int i = 0; i < Constants.MAX_MAX_PLAYERS; i++)
        {
            if (colorsLeft.contains(Constants.colorNames[i]))
            {
                JButton button = new JButton();
                int scale = Scale.get();
                button.setPreferredSize(new Dimension(7 * scale, 4 * scale));
                button.setText(Constants.colorNames[i]);
                button.setMnemonic(Constants.colorMnemonics[i]);
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

    static String pickColor(JFrame parentFrame, String playerName,
        List colorsLeft, IOptions options)
    {
        new PickColor(parentFrame, playerName, colorsLeft, options);
        return color;
    }

    static String getColorName(int i)
    {
        if (i >= 0 && i < Constants.colorNames.length)
        {
            return Constants.colorNames[i];
        }
        return null;
    }

    static Color getForegroundColor(String colorName)
    {
        for (int i = 0; i < Constants.colorNames.length; i++)
        {
            if (colorName.equals(Constants.colorNames[i]))
            {
                return foreground[i];
            }
        }
        return null;
    }

    static Color getBackgroundColor(String colorName)
    {
        for (int i = 0; i < Constants.colorNames.length; i++)
        {
            if (colorName.equals(Constants.colorNames[i]))
            {
                return background[i];
            }
        }
        return null;
    }

    public void actionPerformed(ActionEvent e)
    {
        color = e.getActionCommand();
        saveWindow.saveLocation(getLocation());
        dispose();
    }

    public static void main(String[] args)
    {
        Logger logger = Logger.getLogger(PickColor.class.getName());
        List colorsLeft = Arrays.asList(Constants.colorNames);
        Options options = new Options("Player");
        String color = pickColor(new JFrame(), "Player", colorsLeft, options);
        logger.info("Picked " + color);
        System.exit(0);
    }
}
