package net.sf.colossus.client;


import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

import net.sf.colossus.util.Split;
import net.sf.colossus.util.KDialog;
import net.sf.colossus.util.Log;
import net.sf.colossus.util.Options;


/**
 * Class StatusScreen displays some information about the current game.
 * @version $Id$
 * @author David Ripton
 */


final class StatusScreen extends KDialog implements WindowListener
{
    private int numPlayers; 

    private JLabel [] nameLabel;
    private JLabel [] towerLabel;
    private JLabel [] colorLabel;
    private JLabel [] elimLabel;
    private JLabel [] legionsLabel;
    private JLabel [] markersLabel;
    private JLabel [] creaturesLabel;
    private JLabel [] valueLabel;
    private JLabel [] titanLabel;
    private JLabel [] scoreLabel;

    private Client client;

    private static Point location;
    private static Dimension size;


    StatusScreen(JFrame frame, Client client)
    {
        super(frame, "Game Status", false);

        setVisible(false);
        this.client = client;

        // Needs to be set up before calling this.
        numPlayers = client.getNumPlayers();

        addWindowListener(this);

        Container contentPane = getContentPane();
        contentPane.setLayout(new GridLayout(10, 0));

        nameLabel = new JLabel[numPlayers];
        towerLabel = new JLabel[numPlayers];
        colorLabel = new JLabel[numPlayers];
        elimLabel = new JLabel[numPlayers];
        legionsLabel = new JLabel[numPlayers];
        markersLabel = new JLabel[numPlayers];
        creaturesLabel = new JLabel[numPlayers];
        titanLabel = new JLabel[numPlayers];
        scoreLabel = new JLabel[numPlayers];
        valueLabel = new JLabel[numPlayers];

        contentPane.add(new JLabel("Player"));
        for (int i = 0; i < numPlayers; i++)
        {
            nameLabel[i] = new JLabel();
            nameLabel[i].setOpaque(true);
            contentPane.add(nameLabel[i]);
        }

        contentPane.add(new JLabel("Tower"));
        for (int i = 0; i < numPlayers; i++)
        {
            towerLabel[i] = new JLabel();
            towerLabel[i].setOpaque(true);
            contentPane.add(towerLabel[i]);
        }

        contentPane.add(new JLabel("Color"));
        for (int i = 0; i < numPlayers; i++)
        {
            colorLabel[i] = new JLabel();
            colorLabel[i].setOpaque(true);
            contentPane.add(colorLabel[i]);
        }

        contentPane.add(new JLabel("Elim"));
        for (int i = 0; i < numPlayers; i++)
        {
            elimLabel[i] = new JLabel();
            elimLabel[i].setOpaque(true);
            contentPane.add(elimLabel[i]);
        }

        contentPane.add(new JLabel("Legions"));
        for (int i = 0; i < numPlayers; i++)
        {
            legionsLabel[i] = new JLabel();
            legionsLabel[i].setOpaque(true);
            contentPane.add(legionsLabel[i]);
        }

        contentPane.add(new JLabel("Markers"));
        for (int i = 0; i < numPlayers; i++)
        {
            markersLabel[i] = new JLabel();
            markersLabel[i].setOpaque(true);
            contentPane.add(markersLabel[i]);
        }

        contentPane.add(new JLabel("Creatures"));
        for (int i = 0; i < numPlayers; i++)
        {
            creaturesLabel[i] = new JLabel();
            creaturesLabel[i].setOpaque(true);
            contentPane.add(creaturesLabel[i]);
        }

        contentPane.add(new JLabel("Value"));
        for (int i = 0; i < numPlayers; i++)
        {
            valueLabel[i] = new JLabel();
            valueLabel[i].setOpaque(true);
            contentPane.add(valueLabel[i]);
        }

        contentPane.add(new JLabel("Titan Size"));
        for (int i = 0; i < numPlayers; i++)
        {
            titanLabel[i] = new JLabel();
            titanLabel[i].setOpaque(true);
            contentPane.add(titanLabel[i]);
        }

        contentPane.add(new JLabel("Score"));
        for (int i = 0; i < numPlayers; i++)
        {
            scoreLabel[i] = new JLabel();
            scoreLabel[i].setOpaque(true);
            contentPane.add(scoreLabel[i]);
        }

        updateStatusScreen();

        pack();

        if (size == null)
        {
            loadSize();
        }
        if (size != null)
        {
            setSize(size);
        }

        if (location == null)
        {
            loadLocation();
        }
        if (location == null)
        {
            lowerRightCorner();
            location = getLocation();
        }
        else
        {
            setLocation(location);
        }

        setVisible(true);
    }


    private void setPlayerLabelBackground(int i, Color color)
    {
        if (nameLabel[i].getBackground() != color)
        {
            nameLabel[i].setBackground(color);
            towerLabel[i].setBackground(color);
            colorLabel[i].setBackground(color);
            elimLabel[i].setBackground(color);
            legionsLabel[i].setBackground(color);
            markersLabel[i].setBackground(color);
            creaturesLabel[i].setBackground(color);
            valueLabel[i].setBackground(color);
            titanLabel[i].setBackground(color);
            scoreLabel[i].setBackground(color);
        }
    }


    void updateStatusScreen()
    {
        for (int i = 0; i < numPlayers; i++)
        {
            PlayerInfo info = client.getPlayerInfo(i);
            Color color;
            if (info.isDead())
            {
                color = Color.red;
            }
            else if (client.getActivePlayerName().equals(info.getName()))
            {
                color = Color.yellow;
            }
            else
            {
                color = Color.lightGray;
            }
            setPlayerLabelBackground(i, color);

            nameLabel[i].setText(info.getName());
            towerLabel[i].setText("" + info.getTower());
            colorLabel[i].setText(info.getColor());
            elimLabel[i].setText(info.getPlayersElim());
            legionsLabel[i].setText("" + info.getNumLegions());
            markersLabel[i].setText("" + info.getNumMarkers());
            creaturesLabel[i].setText("" + info.getNumCreatures());
            valueLabel[i].setText("" + info.getCreatureValue());
            titanLabel[i].setText("" + info.getTitanPower());
            scoreLabel[i].setText("" + info.getScore());
        }

        repaint();
    }

    private void loadSize()
    {
        int x = client.getIntOption(Options.statusScreenSizeX);
        int y = client.getIntOption(Options.statusScreenSizeY);
        size = new Dimension(x, y);
    }

    private void saveSize()
    {
        size = getSize();
        client.setOption(Options.statusScreenSizeX, (int)size.getWidth());
        client.setOption(Options.statusScreenSizeY, (int)size.getHeight());
    }

    private void loadLocation()
    {
        int x = client.getIntOption(Options.statusScreenLocX);
        int y = client.getIntOption(Options.statusScreenLocY);
        if (x >= 0 && y >= 0)
        {
            location = new Point(x, y);
        }
    }

    private void saveLocation()
    {
        location = getLocation();
        client.setOption(Options.statusScreenLocX, location.x);
        client.setOption(Options.statusScreenLocY, location.y);
    }


    public void dispose()
    {
        super.dispose();
        saveSize();
        saveLocation();
    }


    public void windowClosing(WindowEvent e)
    {
        client.setOption(Options.showStatusScreen, false);
    }


    public Dimension getMinimumSize()
    {
        int scale = Scale.get();
        return new Dimension(25 * scale, 15 * scale);
    }

    public Dimension getPreferredSize()
    {
        return getMinimumSize();
    }

    void rescale()
    {
        int scale = Scale.get();
        setSize(25 * scale, 15 * scale);
        pack();
    }
}
