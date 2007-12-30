package net.sf.colossus.client;


import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sf.colossus.util.KDialog;
import net.sf.colossus.util.Options;


/**
 * Class StatusScreen displays some information about the current game.
 * @version $Id$
 * @author David Ripton
 */

final class StatusScreen extends KDialog implements WindowListener
{
    private final int numPlayers;

    private final JLabel[] nameLabel;
    private final JLabel[] towerLabel;
    private final JLabel[] elimLabel;
    private final JLabel[] legionsLabel;
    private final JLabel[] markersLabel;
    private final JLabel[] creaturesLabel;
    private final JLabel[] titanLabel;
    private final JLabel[] scoreLabel;

    private final JLabel activePlayerLabel;
    private final JLabel turnLabel;
    private final JLabel phaseLabel;
    private final JLabel battleActivePlayerLabel;
    private final JLabel battleTurnLabel;
    private final JLabel battlePhaseLabel;

    private IOracle oracle;
    private IOptions options;
    private Client client;

    private Point location;
    private Dimension size;
    private final SaveWindow saveWindow;

    StatusScreen(JFrame frame, IOracle oracle, IOptions options, Client client)
    {
        super(frame, "Game Status", false);

        setVisible(false);
        setFocusable(false);

        this.oracle = oracle;
        this.options = options;
        this.client = client;

        // Needs to be set up before calling this.
        numPlayers = oracle.getNumPlayers();

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
        gridPane.setLayout(new GridLayout(8, 0));
        gridPane.setBorder(BorderFactory.createEtchedBorder());

        nameLabel = new JLabel[numPlayers];
        towerLabel = new JLabel[numPlayers];
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

        saveWindow = new SaveWindow(options, "StatusScreen");

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

    private void setPlayerLabelColors(JLabel label, Color bgColor,
        Color fgColor)
    {
        if (label.getBackground() != bgColor)
        {
            label.setBackground(bgColor);
        }
        if (label.getForeground() != fgColor)
        {
            label.setForeground(fgColor);
        }
    }

    private void setPlayerLabelBackground(int i, Color color)
    {
        if (towerLabel[i].getBackground() != color)
        {
            towerLabel[i].setBackground(color);
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
        activePlayerLabel.setText(oracle.getActivePlayerName());
        int turnNumber = oracle.getTurnNumber();
        String turn = "";
        if (turnNumber >= 1)
        {
            turn = "" + turnNumber;
        }
        turnLabel.setText(turn);
        phaseLabel.setText(oracle.getPhaseName());
        battleActivePlayerLabel.setText(oracle.getBattleActivePlayerName());
        int battleTurnNumber = oracle.getBattleTurnNumber();
        String battleTurn = "";
        if (battleTurnNumber >= 1)
        {
            battleTurn = "" + battleTurnNumber;
        }
        battleTurnLabel.setText(battleTurn);
        battlePhaseLabel.setText(oracle.getBattlePhaseName());

        for (int i = 0; i < numPlayers; i++)
        {
            PlayerInfo info = client.getPlayerInfo(i);
            Color color, bgcolor, fgcolor;

            if (info.isDead())
            {
                color = Color.RED;
                setPlayerLabelBackground(i, color);
            }
            else
            {
                if (oracle.getActivePlayerName().equals(info.getName()))
                {
                    color = Color.YELLOW;
                }
                else
                {
                    color = Color.LIGHT_GRAY;
                }

                if (!info.getColor().equals("null"))
                {
                    bgcolor = PickColor.getBackgroundColor(info.getColor());
                    fgcolor = PickColor.getForegroundColor(info.getColor());
                }
                else
                {
                    bgcolor = Color.LIGHT_GRAY;
                    fgcolor = Color.BLACK;
                }
                setPlayerLabelBackground(i, color);
                setPlayerLabelColors(nameLabel[i], bgcolor, fgcolor);
            }

            nameLabel[i].setText(info.getName());
            if (info.canTitanTeleport())
            {
                nameLabel[i].setText(info.getName() + "*");
            }
            else
            {
                nameLabel[i].setText(info.getName());
            }
            towerLabel[i].setText("" + info.getTower());
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
        this.options = null;
        this.client = null;
        this.oracle = null;
    }

    public void windowClosing(WindowEvent e)
    {
        options.setOption(Options.showStatusScreen, false);
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
        setSize(getPreferredSize());
        pack();
    }
}
