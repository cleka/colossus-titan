package net.sf.colossus.client;


import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

import net.sf.colossus.util.HTMLColor;
import net.sf.colossus.util.KDialog;
import net.sf.colossus.server.Constants;


/**
 * Class PickColor lets a player choose a color of legion markers.
 * @version $Id$
 * @author David Ripton
 */


final class PickColor extends KDialog implements WindowListener, ActionListener
{
    private JLabel [] colorLabel = new JLabel[Constants.MAX_MAX_PLAYERS];

    private static final Color [] background;
    private static final Color [] foreground;

    private static String color;

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
        java.util.List colorsLeft)
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
        centerOnScreen();
        addWindowListener(this);
        setVisible(true);
    }


    static String pickColor(JFrame parentFrame, String playerName,
        java.util.List colorsLeft)
    {
        new PickColor(parentFrame, playerName, colorsLeft);
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


    private int colorNumber(String colorName)
    {
        for (int i = 0; i < Constants.MAX_MAX_PLAYERS; i++)
        {
            if (colorName.equals(Constants.colorNames[i]))
            {
                return i;
            }
        }

        return -1;
    }


    public void actionPerformed(ActionEvent e)
    {
        color = e.getActionCommand();
        dispose();
    }


    public static void main(String [] args)
    {
        java.util.List colorsLeft = Arrays.asList(Constants.colorNames);
        String color = pickColor(new JFrame(), "Player", colorsLeft);
        System.out.println("Picked " + color);
        System.exit(0);
    }
}
