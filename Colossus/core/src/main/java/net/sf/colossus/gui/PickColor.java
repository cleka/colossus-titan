package net.sf.colossus.gui;


import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JFrame;

import net.sf.colossus.common.Constants;
import net.sf.colossus.common.IOptions;
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
    private static final Logger LOGGER = Logger.getLogger(PickColor.class
        .getName());

    private final JFrame parentFrame;
    private final String playerName;
    private final List<PlayerColor> colorsLeft;
    private final IOptions options;
    private final PickColorCallback callback;
    private final boolean allowNull;

    private final SaveWindow saveWindow;

    public PickColor(final JFrame parentFrame, final String playerName,
        final List<PlayerColor> colorsLeft, final IOptions options,
        final PickColorCallback callback, final boolean allowNull)
    {
        super(parentFrame, playerName + ", Pick a Color", true);

        this.parentFrame = parentFrame;
        this.playerName = playerName;
        this.colorsLeft = colorsLeft;
        // this.colorsLeft = new ArrayList<PlayerColor>(colorsLeft);
        this.options = options;
        this.callback = callback;
        this.allowNull = allowNull;

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
                        PlayerColor color = PlayerColor.getByName(e
                            .getActionCommand());
                        cleanup(color);
                    }
                });
                contentPane.add(button);
            }
        }

        // Don't allow exiting without making a choice, or the game will hang
        addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                cleanup();
            }
        });

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

    public void cleanup()
    {
        cleanup(null);
    }

    public void cleanup(PlayerColor color)
    {
        saveWindow.saveLocation(getLocation());
        dispose();
        if (callback != null)
        {
            if (color != null || allowNull)
            {
                callback.tellPickedColor(color);
            }
            else
            {
                new PickColor(parentFrame, playerName, colorsLeft, options,
                    callback, allowNull);
            }
        }
        else
        {
            LOGGER.warning("Callback is null !?!??");
        }
    }

    public static abstract class PickColorCallback
    {
        public abstract void tellPickedColor(PlayerColor color);
    }
}
