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
    private JLabel [] colorLabel = new JLabel[6];

    private static final Color [] background = { Color.black, Color.blue,
        HTMLColor.brown, Color.yellow, Color.green, Color.red };
    private static final Color [] foreground = { Color.white, Color.white,
        Color.white, Color.black, Color.black, Color.black };

    private static final int [] colorMnemonics =
        {KeyEvent.VK_B, KeyEvent.VK_L, KeyEvent.VK_O, KeyEvent.VK_G,
            KeyEvent.VK_E, KeyEvent.VK_R};

    private static String color;


    private PickColor(JFrame parentFrame, String playerName, Set colorsLeft)
    {
        super(parentFrame, playerName + ", Pick a Color", true);

        color = null;

        setBackground(Color.lightGray);
        pack();

        Container contentPane = getContentPane();
        contentPane.setLayout(new FlowLayout());

        for (int i = 0; i < 6; i++)
        {
            if (colorsLeft.contains(Constants.colorNames[i]))
            {
                JButton button = new JButton();
                int scale = Scale.get();
                button.setPreferredSize(new Dimension(7 * scale, 4 * scale));
                button.setText(Constants.colorNames[i]);
                button.setMnemonic(colorMnemonics[i]);
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
        Set colorsLeft)
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
        for (int i = 0; i < 6; i++)
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
}
