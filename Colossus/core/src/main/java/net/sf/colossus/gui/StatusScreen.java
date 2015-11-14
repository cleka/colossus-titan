package net.sf.colossus.gui;


import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import net.sf.colossus.client.PlayerClientSide;
import net.sf.colossus.common.IOptions;
import net.sf.colossus.common.Options;
import net.sf.colossus.game.Player;
import net.sf.colossus.guiutil.KDialog;
import net.sf.colossus.guiutil.SaveWindow;


/**
 * Class StatusScreen displays some information about the current game.
 *
 * @author David Ripton
 */
@SuppressWarnings("serial")
final class StatusScreen extends KDialog
{
    private final int numPlayers;

    // TODO it would probably be nicer to just have a JPanel for each player, then mapped
    //      via a Map<Player,PlayerPanel> -- this way things would be grouped better and
    //      the need for the index-based access to players would be gone
    private final JLabel[] nameLabel;
    private final JLabel[] typeLabel;
    private final JLabel[] towerLabel;
    private final JLabel[] elimLabel;
    private final JLabel[] legionsLabel;
    private final JLabel[] markersLabel;
    private final JLabel[] creaturesLabel;
    private final JLabel[] pointsLabel;
    private final JLabel[] titanLabel;
    private final JLabel[] scoreLabel;

    private final JLabel activePlayerLabel;
    private final JLabel turnLabel;
    private final JLabel phaseLabel;
    private final JLabel battleActivePlayerLabel;
    private final JLabel battleTurnLabel;
    private final JLabel battlePhaseLabel;

    private final ClientGUI gui;

    private Point location;
    private Dimension size;
    private final SaveWindow saveWindow;

    StatusScreen(final JFrame frame, ClientGUI gui, final IOptions options)
    {
        super(frame, "Game Status", false);

        setVisible(false);
        setFocusable(false);

        this.gui = gui;

        // Needs to be set up before calling this.
        numPlayers = gui.getGame().getNumPlayers();

        addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                options.setOption(Options.showStatusScreen, false);
            }
        });

        Container contentPane = getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));

        JPanel turnPane = new JPanel();
        turnPane.setLayout(new GridLayout(0, 3));
        turnPane.setBorder(BorderFactory.createEtchedBorder());
        contentPane.add(turnPane);
        turnPane.setAlignmentX(SwingConstants.LEFT);

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
        gridPane.setAlignmentX(SwingConstants.LEFT);

        nameLabel = new JLabel[numPlayers];
        typeLabel = new JLabel[numPlayers];
        towerLabel = new JLabel[numPlayers];
        elimLabel = new JLabel[numPlayers];
        legionsLabel = new JLabel[numPlayers];
        markersLabel = new JLabel[numPlayers];
        creaturesLabel = new JLabel[numPlayers];
        pointsLabel = new JLabel[numPlayers];
        titanLabel = new JLabel[numPlayers];
        scoreLabel = new JLabel[numPlayers];

        gridPane.add(new JLabel("Player"));

        int j = 0;
        for (Player player : gui.getGameClientSide().getPlayers())
        {
            nameLabel[j] = new JLabel();
            nameLabel[j].setOpaque(true);
            gridPane.add(nameLabel[j]);

            final PlayerClientSide thePlayer = (PlayerClientSide)player;
            nameLabel[j].addMouseListener(new MouseAdapter()
            {
                @Override
                public void mouseClicked(MouseEvent e)
                {
                    new PlayerDetailsDialog(frame, thePlayer,
                        StatusScreen.this.gui);
                }
            });

            j++;
        }

        gridPane.add(new JLabel("Type"));
        for (int i = 0; i < numPlayers; i++)
        {
            typeLabel[i] = new JLabel();
            typeLabel[i].setOpaque(true);
            gridPane.add(typeLabel[i]);
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

        gridPane.add(new JLabel("Creature-Points"));
        for (int i = 0; i < numPlayers; i++)
        {
            pointsLabel[i] = new JLabel();
            pointsLabel[i].setOpaque(true);
            gridPane.add(pointsLabel[i]);
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

        Box bottomPane = new Box(BoxLayout.X_AXIS);
        bottomPane.setAlignmentX(SwingConstants.LEFT);

        JLabel l = new JLabel("Click a player name for more info!");
        l.setHorizontalAlignment(SwingConstants.CENTER);
        l.setAlignmentX(CENTER_ALIGNMENT);

        bottomPane.add(l);
        bottomPane.add(Box.createHorizontalGlue());
        contentPane.add(bottomPane);

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
            typeLabel[i].setBackground(color);
            towerLabel[i].setBackground(color);
            elimLabel[i].setBackground(color);
            legionsLabel[i].setBackground(color);
            markersLabel[i].setBackground(color);
            creaturesLabel[i].setBackground(color);
            pointsLabel[i].setBackground(color);
            titanLabel[i].setBackground(color);
            scoreLabel[i].setBackground(color);
        }
    }

    void updateStatusScreen()
    {
        activePlayerLabel.setText(gui.getGameClientSide().getActivePlayer()
            .getName());
        int turnNumber = gui.getGame().getTurnNumber();
        String turn = "";
        if (turnNumber >= 1)
        {
            turn = "" + turnNumber;
        }
        turnLabel.setText(turn);
        phaseLabel.setText(gui.getGame().getPhase().toString());

        if (gui.getGameClientSide().isBattleOngoing())
        {
            battleActivePlayerLabel.setText(gui.getGameClientSide()
                .getBattleActivePlayer().getName());
            int battleTurnNumber = gui.getGameClientSide()
                .getBattleTurnNumber();
            String battleTurn = "";
            if (battleTurnNumber >= 1)
            {
                battleTurn = "" + battleTurnNumber;
            }
            battleTurnLabel.setText(battleTurn);
            battlePhaseLabel.setText(gui.getGameClientSide().getBattlePhase()
                .toString());
        }
        else
        {
            battleActivePlayerLabel.setText("");
            battleTurnLabel.setText("");
            battlePhaseLabel.setText("");
        }

        int i = 0;
        for (Player player : gui.getGameClientSide().getPlayers())
        {
            if (player.isDead())
            {
                setPlayerLabelBackground(i, Color.RED);
            }
            else if (gui.getGameClientSide().getActivePlayer().equals(player))
            {
                setPlayerLabelBackground(i, Color.YELLOW);
            }
            else
            {
                setPlayerLabelBackground(i, Color.LIGHT_GRAY);
            }

            if (player.getColor() != null)
            {
                setPlayerLabelColors(nameLabel[i], player.getColor()
                    .getBackgroundColor(), player.getColor()
                    .getForegroundColor());
            }
            else
            {
                setPlayerLabelColors(nameLabel[i], Color.LIGHT_GRAY,
                    Color.BLACK);
            }

            nameLabel[i].setText(player.getName());
            if (player.canTitanTeleport())
            {
                nameLabel[i].setText(player.getName() + "*");
            }
            else
            {
                nameLabel[i].setText(player.getName());
            }
            String type = player.getType();
            type = type.substring(type.lastIndexOf('.') + 1);
            typeLabel[i].setText("" + type);
            towerLabel[i].setText("" + player.getStartingTower().getLabel());
            elimLabel[i].setText(player.getPlayersElim());
            legionsLabel[i].setText("" + player.getNumLegions());
            markersLabel[i].setText("" + player.getNumMarkersAvailable());
            creaturesLabel[i].setText("" + player.getNumCreatures());
            pointsLabel[i].setText("" + player.getTotalPointValue());
            titanLabel[i].setText("" + player.getTitanPower());
            scoreLabel[i].setText("" + player.getScore());

            i++;
        }

        repaint();
    }

    @Override
    public void dispose()
    {
        super.dispose();
        size = getSize();
        saveWindow.saveSize(size);
        location = getLocation();
        saveWindow.saveLocation(location);
    }

    @Override
    public Dimension getMinimumSize()
    {
        Dimension d = getSize();
        if (d.width < 150 || d.height < 50)
        {
            return new Dimension(400, 300);
        }
        else
        {
            return d;
        }
    }

    @Override
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
