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

import net.sf.colossus.common.Constants;
import net.sf.colossus.common.IOptions;
import net.sf.colossus.common.Options;
import net.sf.colossus.game.PlayerColor;
import net.sf.colossus.guiutil.KDialog;
import net.sf.colossus.guiutil.SaveWindow;


/**
 * Class PickColor lets a player choose a color of legion markers.
 *
 * @author David Ripton
 */
@SuppressWarnings("serial")
final class PickColor extends KDialog
{
    private PlayerColor color;
    private final SaveWindow saveWindow;

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
                button.setBackground(curColor.getBackgroundColor());
                button.setForeground(curColor.getForegroundColor());
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

    static PlayerColor pickColor(JFrame parentFrame,
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

    public static void main(String[] args)
    {
        Logger logger = Logger.getLogger(PickColor.class.getName());
        List<PlayerColor> colorsLeft = Arrays.asList(PlayerColor.values());
        Options options = new Options("Player");
        PlayerColor color = pickColor(new JFrame(), "Player", colorsLeft,
            options);
        logger.info("Picked " + color.getName());
        System.exit(0);
    }
}
