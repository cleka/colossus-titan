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
    private JLabel [] titanLabel;
    private JLabel [] scoreLabel;

    private JLabel activePlayerLabel;
    private JLabel turnLabel;
    private JLabel phaseLabel;
    private JLabel battleActivePlayerLabel;
    private JLabel battleTurnLabel;
    private JLabel battlePhaseLabel;

    private Client client;

    private Point location;
    private Dimension size;
    private SaveWindow saveWindow;


    StatusScreen(JFrame frame, Client client)
    {
        super(frame, "Game Status", false);

        setVisible(false);
        this.client = client;

        // Needs to be set up before calling this.
        numPlayers = client.getNumPlayers();

        addWindowListener(this);

        Container contentPane = getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));

        JPanel turnPane = new JPanel();
        turnPane.setLayout(new GridLayout(0, 3));
        turnPane.setBorder(BorderFactory.createEtchedBorder());
        contentPane.add(turnPane);

        turnPane.add(new JLabel(""));
        turnPane.add(new JLabel("Game"));
        turnPane.add(new JLabel("Battle"));

        turnPane.add(new JLabel("Turn"));
        turnLabel = new JLabel();
        turnPane.add(turnLabel);
        battleTurnLabel = new JLabel();
        turnPane.add(battleTurnLabel);

        turnPane.add(new JLabel("Player"));
        activePlayerLabel = new JLabel();
        turnPane.add(activePlayerLabel);
        battleActivePlayerLabel = new JLabel();
        turnPane.add(battleActivePlayerLabel);

        turnPane.add(new JLabel("Phase"));
        phaseLabel = new JLabel();
        turnPane.add(phaseLabel);
        battlePhaseLabel = new JLabel();
        turnPane.add(battlePhaseLabel);

        JPanel gridPane = new JPanel();
        contentPane.add(gridPane);
        gridPane.setLayout(new GridLayout(10, 0));
        gridPane.setBorder(BorderFactory.createEtchedBorder());

        nameLabel = new JLabel[numPlayers];
        towerLabel = new JLabel[numPlayers];
        colorLabel = new JLabel[numPlayers];
        elimLabel = new JLabel[numPlayers];
        legionsLabel = new JLabel[numPlayers];
        markersLabel = new JLabel[numPlayers];
        creaturesLabel = new JLabel[numPlayers];
        titanLabel = new JLabel[numPlayers];
        scoreLabel = new JLabel[numPlayers];

        gridPane.add(new JLabel("Player"));
        for (int i = 0; i < numPlayers; i++)
        {
            nameLabel[i] = new JLabel();
            nameLabel[i].setOpaque(true);
            gridPane.add(nameLabel[i]);
        }

        gridPane.add(new JLabel("Tower"));
        for (int i = 0; i < numPlayers; i++)
        {
            towerLabel[i] = new JLabel();
            towerLabel[i].setOpaque(true);
            gridPane.add(towerLabel[i]);
        }

        gridPane.add(new JLabel("Color"));
        for (int i = 0; i < numPlayers; i++)
        {
            colorLabel[i] = new JLabel();
            colorLabel[i].setOpaque(true);
            gridPane.add(colorLabel[i]);
        }

        gridPane.add(new JLabel("Elim"));
        for (int i = 0; i < numPlayers; i++)
        {
            elimLabel[i] = new JLabel();
            elimLabel[i].setOpaque(true);
            gridPane.add(elimLabel[i]);
        }

        gridPane.add(new JLabel("Legions"));
        for (int i = 0; i < numPlayers; i++)
        {
            legionsLabel[i] = new JLabel();
            legionsLabel[i].setOpaque(true);
            gridPane.add(legionsLabel[i]);
        }

        gridPane.add(new JLabel("Markers"));
        for (int i = 0; i < numPlayers; i++)
        {
            markersLabel[i] = new JLabel();
            markersLabel[i].setOpaque(true);
            gridPane.add(markersLabel[i]);
        }

        gridPane.add(new JLabel("Creatures"));
        for (int i = 0; i < numPlayers; i++)
        {
            creaturesLabel[i] = new JLabel();
            creaturesLabel[i].setOpaque(true);
            gridPane.add(creaturesLabel[i]);
        }

        gridPane.add(new JLabel("Titan Size"));
        for (int i = 0; i < numPlayers; i++)
        {
            titanLabel[i] = new JLabel();
            titanLabel[i].setOpaque(true);
            gridPane.add(titanLabel[i]);
        }

        gridPane.add(new JLabel("Score"));
        for (int i = 0; i < numPlayers; i++)
        {
            scoreLabel[i] = new JLabel();
            scoreLabel[i].setOpaque(true);
            gridPane.add(scoreLabel[i]);
        }

        updateStatusScreen();

        pack();

        saveWindow = new SaveWindow(client, "StatusScreen");

        if (size == null)
        {
            size = saveWindow.loadSize();
        }
        if (size != null)
        {
            setSize(size);
        }

        if (location == null)
        {
            location = saveWindow.loadLocation();
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
            titanLabel[i].setBackground(color);
            scoreLabel[i].setBackground(color);
        }
    }


    void updateStatusScreen()
    {
        activePlayerLabel.setText(client.getActivePlayerName());
        turnLabel.setText("" + client.getTurnNumberString());
        phaseLabel.setText(client.getPhaseName());
        battleActivePlayerLabel.setText(client.getBattleActivePlayerName());
        battleTurnLabel.setText("" + client.getBattleTurnNumberString());
        battlePhaseLabel.setText(client.getBattlePhaseName());

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
            titanLabel[i].setText("" + info.getTitanPower());
            scoreLabel[i].setText("" + info.getScore());
        }

        repaint();
    }

    public void dispose()
    {
        super.dispose();
        size = getSize();
        saveWindow.saveSize(size);
        location = getLocation();
        saveWindow.saveLocation(location);
    }


    public void windowClosing(WindowEvent e)
    {
        client.setOption(Options.showStatusScreen, false);
    }


    public Dimension getMinimumSize()
    {
        int scale = Scale.get();
        return new Dimension(25 * scale, 20 * scale);
    }

    public Dimension getPreferredSize()
    {
        return getMinimumSize();
    }

    void rescale()
    {
        int scale = Scale.get();
        setSize(getPreferredSize());
        pack();
    }
}
