package net.sf.colossus.gui;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.WindowConstants;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.sf.colossus.common.Options;
import net.sf.colossus.game.PlayerColor;
import net.sf.colossus.guiutil.KFrame;
import net.sf.colossus.util.HTMLColor;
import net.sf.colossus.util.Split;


@SuppressWarnings("serial")
class PreferencesWindow extends KFrame implements ItemListener, ActionListener
{
    /**
     * Gridbag constraints for a vertical filling (use with empty JPanel).
     */
    private static final GridBagConstraints FILL_CONSTRAINTS = new GridBagConstraints();
    static
    {
        FILL_CONSTRAINTS.gridx = 1; // first in new line
        FILL_CONSTRAINTS.weighty = 1; // expand vertically (others should have weighty set to 0)
    }

    /**
     * Gridbag constraints for a vertical spacer (use with empty JPanel).
     */
    private static final GridBagConstraints SPACER_CONSTRAINTS = new GridBagConstraints();
    static
    {
        SPACER_CONSTRAINTS.gridx = 1; // first in new line
        SPACER_CONSTRAINTS.ipady = 10; // expand vsize by this many pixels
    }

    /**
     * Gridbag constraints for the controls itself.
     */
    private static final GridBagConstraints CONTROL_CONSTRAINTS = new GridBagConstraints();
    static
    {
        CONTROL_CONSTRAINTS.gridx = 1; // first in new line
        CONTROL_CONSTRAINTS.weightx = 1; // expand cell horizontally to use width of pane
        CONTROL_CONSTRAINTS.anchor = GridBagConstraints.NORTHWEST; // align top left
        CONTROL_CONSTRAINTS.insets = new Insets(0, 5, 0, 5); // add a bit extra space around it
    }

    /**
     * Gridbag constraints for nested panels.
     */
    private static final GridBagConstraints SUBPANEL_CONSTRAINTS = new GridBagConstraints();
    static
    {
        SUBPANEL_CONSTRAINTS.gridx = 1; // first in new line
        SUBPANEL_CONSTRAINTS.weightx = 1; // expand cell horizontally to use width of pane
        SUBPANEL_CONSTRAINTS.fill = GridBagConstraints.BOTH; // panel should use all of cell
    }

    private Options options;
    private final ClientGUI gui;
    private final Map<String, JCheckBox> prefCheckboxes = new HashMap<String, JCheckBox>();
    private final Map<String, JRadioButton> prefRadioButtons = new HashMap<String, JRadioButton>();
    private JButton closeButton;
    private Box lfBox; // Look & Feel
    private Box rcModes; // Recruit Chit modes
    private Box mcModes; // Move confirmation modes
    private Box nextSplitModes; // Modes for next key during split phase
    private JPanel favColorPane;
    private int activePaneIndex;
    private List<PlayerColor> favoriteColors;
    private List<PlayerColor> colorsLeft;

    PreferencesWindow(Options options, ClientGUI clientGui)
    {
        super("Preferences");

        this.options = options;
        this.gui = clientGui;

        getContentPane().add(new JLabel("Dummy"));

        setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);

        setupGUI();

        pack();

        useSaveWindow(options, "Preferences", null);
    }

    private void addCheckBox(Container pane, String name)
    {
        addCheckBox(pane, name, true, false);
    }

    private void addCheckBox(Container pane, String name, boolean enabled,
        boolean defVal)
    {
        JCheckBox cb = new JCheckBox(name);
        cb.setSelected(options.getOption(name, defVal));
        cb.setEnabled(enabled);
        cb.addItemListener(this);
        pane.add(cb, CONTROL_CONSTRAINTS);
        prefCheckboxes.put(name, cb);
    }

    private void addRadioButton(Container cont, ButtonGroup group,
        String text, String cmd, String current)
    {
        JRadioButton rb = new JRadioButton(text);
        if (cmd != null && !cmd.equals(""))
        {
            rb.setActionCommand(cmd);
        }
        rb.addItemListener(this);
        group.add(rb);
        cont.add(rb, CONTROL_CONSTRAINTS);
        boolean selected = (text.equals(current));
        rb.setSelected(selected);
        prefRadioButtons.put(text, rb);
    }

    private void addButton(Container cont, String name, ActionListener al)
    {
        JButton button = new JButton(name);
        button.addActionListener(al);
        cont.add(button);
    }

    private void setupGUI()
    {
        JTabbedPane tabbedPane = new JTabbedPane();
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(tabbedPane, BorderLayout.CENTER);

        // Battle Map tab:
        JPanel battlePane = new JPanel(new GridBagLayout());
        tabbedPane.addTab("Battle", battlePane);
        addCheckBox(battlePane, Options.useColoredBorders);
        addCheckBox(battlePane, Options.doNotInvertDefender);
        battlePane.add(new JPanel(), SPACER_CONSTRAINTS);
        addCheckBox(battlePane, Options.showHitThreshold, true, true);
        addCheckBox(battlePane, Options.showDiceAjustmentsTerrain, true, true);
        addCheckBox(battlePane, Options.showDiceAjustmentsRange, true, true);
        battlePane.add(new JPanel(), FILL_CONSTRAINTS);

        // Autoplay tab:
        /*
        In menu it was like that (saved here for the used key shortcuts)
        JMenu playerMenu = new JMenu("Autoplay");
        playerMenu.setMnemonic(KeyEvent.VK_A);
        menuBar.add(playerMenu);

        addCheckBox(playerMenu, Options.autoPickColor, KeyEvent.VK_C);
        addCheckBox(playerMenu, Options.autoPickMarker, KeyEvent.VK_I);
        addCheckBox(playerMenu, Options.autoPickEntrySide, KeyEvent.VK_E);
        addCheckBox(playerMenu, Options.autoForcedStrike, KeyEvent.VK_K);
        addCheckBox(playerMenu, Options.autoCarrySingle, KeyEvent.VK_Y);
        addCheckBox(playerMenu, Options.autoRangeSingle, KeyEvent.VK_G);
        addCheckBox(playerMenu, Options.autoSummonAngels, KeyEvent.VK_O);
        addCheckBox(playerMenu, Options.autoAcquireAngels, KeyEvent.VK_A);
        addCheckBox(playerMenu, Options.autoRecruit, KeyEvent.VK_R);
        addCheckBox(playerMenu, Options.autoPickRecruiter, KeyEvent.VK_U);
        addCheckBox(playerMenu, Options.autoReinforce, KeyEvent.VK_N);
        addCheckBox(playerMenu, Options.autoPlay, KeyEvent.VK_P);
        */
        JPanel apPane = new JPanel(new GridBagLayout());
        if (!gui.getClient().isSpectator())
        {
            tabbedPane.addTab("Autoplay", apPane);
        }
        addCheckBox(apPane, Options.autoPickColor);
        addCheckBox(apPane, Options.autoPickMarker);
        addCheckBox(apPane, Options.autoPickEntrySide);
        addCheckBox(apPane, Options.autoForcedStrike);
        addCheckBox(apPane, Options.autoCarrySingle);
        addCheckBox(apPane, Options.autoRangeSingle);
        addCheckBox(apPane, Options.autoSummonAngels);
        addCheckBox(apPane, Options.autoAcquireAngels);
        addCheckBox(apPane, Options.autoRecruit);
        addCheckBox(apPane, Options.autoPickRecruiter);
        addCheckBox(apPane, Options.autoReinforce);
        addCheckBox(apPane, Options.autoPlay);
        apPane.add(new JPanel(), FILL_CONSTRAINTS);

        // Color tab
        JPanel colorPane = getColorPane();
        tabbedPane.addTab("Color", colorPane);

        // Graphics/View tab
        JPanel viewPane = new JPanel(new GridBagLayout());
        tabbedPane.addTab("Graphics/View", viewPane);

        //   - graphics features panel in Graphics tab
        JPanel graphicsPane = new JPanel(new GridBagLayout());
        graphicsPane.setBorder(new TitledBorder("Graphics features"));
        graphicsPane.setAlignmentX(LEFT_ALIGNMENT);
        addCheckBox(graphicsPane, Options.antialias);
        addCheckBox(graphicsPane, Options.useOverlay);
        addCheckBox(graphicsPane, Options.noBaseColor);
        viewPane.add(graphicsPane, SUBPANEL_CONSTRAINTS);

        //   - "Show recruit preview chits ..." panel in Graphics tab
        ButtonGroup group = new ButtonGroup();
        rcModes = new Box(BoxLayout.Y_AXIS);
        rcModes.setAlignmentX(LEFT_ALIGNMENT);
        rcModes.setBorder(new TitledBorder(Options.showRecruitChitsSubmenu));
        viewPane.add(rcModes, SUBPANEL_CONSTRAINTS);

        String current = options
            .getStringOption(Options.showRecruitChitsSubmenu);
        // NOTE! : event handling is based on that the RB is partof this rcModes Box!
        addRadioButton(rcModes, group, Options.showRecruitChitsNone, "",
            current);
        addRadioButton(rcModes, group, Options.showRecruitChitsStrongest, "",
            current);
        addRadioButton(rcModes, group, Options.showRecruitChitsRecruitHint,
            "", current);
        addRadioButton(rcModes, group, Options.showRecruitChitsAll, "",
            current);

        //   -- misc. Panel in Graphics tab:
        //        so far only dubious as blanks
        Box miscPane = new Box(BoxLayout.Y_AXIS);
        viewPane.add(miscPane, SUBPANEL_CONSTRAINTS);
        miscPane.setBorder(new TitledBorder("Misc."));
        miscPane.setAlignmentX(LEFT_ALIGNMENT);
        //  The "dubious as blanks" option makes only sense with the
        //    "view what SplitPrediction tells us" mode => otherwise inactive.
        boolean avail2 = (gui.getRawViewMode() != Options.viewableOwnNum);
        addCheckBox(miscPane, Options.localOnlyOwn, avail2, false);
        // , KeyEvent.VK_D);
        boolean avail = (gui.getRawViewMode() == Options.viewableEverNum);
        addCheckBox(miscPane, Options.dubiousAsBlanks, avail, false);
        // , KeyEvent.VK_D);
        addCheckBox(miscPane, Options.showMarker);
        viewPane.add(new JPanel(), FILL_CONSTRAINTS);

        // Window tab
        JPanel windowPane = new JPanel(new GridBagLayout());
        tabbedPane.addTab("Windows", windowPane);

        addCheckBox(windowPane, Options.stealFocus); // KeyEvent.VK_F

        Box notifBox = new Box(BoxLayout.Y_AXIS);
        notifBox.setBorder(new TitledBorder("Notifications"));
        addCheckBox(notifBox, Options.turnStartToFront);
        addCheckBox(notifBox, Options.turnStartBeep);
        addCheckBox(notifBox, Options.turnStartBottomBarYellow, true, true);
        addCheckBox(notifBox, Options.turnStartChatYellow, true, true);

        windowPane.add(notifBox, SUBPANEL_CONSTRAINTS);

        //   Look & Feel panel in Window tab
        ButtonGroup lfGroup = new ButtonGroup();
        lfBox = new Box(BoxLayout.Y_AXIS);
        windowPane.add(lfBox, SUBPANEL_CONSTRAINTS);
        lfBox.setAlignmentX(LEFT_ALIGNMENT);
        lfBox.setBorder(new TitledBorder("Look & Feel"));
        lfBox.add(new JLabel("Choose your preferred \"Look & Feel\":"));
        String currentLF = UIManager.getLookAndFeel().getName();
        UIManager.LookAndFeelInfo[] lnfInfos = UIManager
            .getInstalledLookAndFeels();
        for (LookAndFeelInfo lnfInfo : lnfInfos)
        {
            // NOTE! : event handling is based on that the RB is part of this lnfBox Box!
            addRadioButton(lfBox, lfGroup, lnfInfo.getName(),
                lnfInfo.getClassName(), currentLF);
        }

        int oldValue = Scale.get();
        int min = 5;
        int max = 25;
        int step = 1;
        ScaleValue scalePane = new ScaleValue(oldValue, min, max, step);
        scalePane.setAlignmentX(LEFT_ALIGNMENT);
        windowPane.add(scalePane, SUBPANEL_CONSTRAINTS);

        windowPane.add(new JPanel(), FILL_CONSTRAINTS);

        JPanel nextPane = new JPanel(new GridBagLayout());
        if (!gui.getClient().isSpectator())
        {
            tabbedPane.addTab("Next key", nextPane);
        }
        addCheckBox(nextPane, Options.nextMove, true, true);
        addCheckBox(nextPane, Options.nextMuster, true, true);
        addCheckBox(nextPane, Options.nextSplitAllSplitable, true, true);
        ButtonGroup nextSplitButtonGroup = new ButtonGroup();
        nextSplitModes = new Box(BoxLayout.Y_AXIS);
        nextSplitModes.setBorder(new TitledBorder(Options.nextSplitSubMenu));
        nextSplitModes.setAlignmentX(LEFT_ALIGNMENT);
        nextPane.add(nextSplitModes, SUBPANEL_CONSTRAINTS);
        String nextSplitCur = options
            .getStringOption(Options.nextSplitSubMenu);
        addRadioButton(nextSplitModes, nextSplitButtonGroup,
            Options.nextSplitNoClick, "", nextSplitCur);
        addRadioButton(nextSplitModes, nextSplitButtonGroup,
            Options.nextSplitLeftClick, "", nextSplitCur);
        addRadioButton(nextSplitModes, nextSplitButtonGroup,
            Options.nextSplitRightClick, "", nextSplitCur);

        JPanel confirmationPane = new JPanel(new GridBagLayout());
        if (!gui.getClient().isSpectator())
        {
            tabbedPane.addTab("Confirmations", confirmationPane);
        }
        addCheckBox(confirmationPane, Options.confirmNoRecruit, true, true);
        addCheckBox(confirmationPane, Options.confirmNoSplit, true, true);
        addCheckBox(confirmationPane, Options.confirmConcedeWithTitan, true,
            true);
        ButtonGroup moveButtonGroup = new ButtonGroup();
        mcModes = new Box(BoxLayout.Y_AXIS);
        mcModes.setBorder(new TitledBorder(
            Options.legionMoveConfirmationSubMenu));
        mcModes.setAlignmentX(LEFT_ALIGNMENT);
        confirmationPane.add(mcModes, SUBPANEL_CONSTRAINTS);
        String legionMoveCur = options
            .getStringOption(Options.legionMoveConfirmationSubMenu);

        addRadioButton(mcModes, moveButtonGroup,
            Options.legionMoveConfirmationNoMove, "", legionMoveCur);
        addRadioButton(mcModes, moveButtonGroup,
            Options.legionMoveConfirmationNoUnvisitedMove, "", legionMoveCur);
        addRadioButton(mcModes, moveButtonGroup,
            Options.legionMoveConfirmationNoConfirm, "", legionMoveCur);

        closeButton = new JButton("Close");
        closeButton.addActionListener(this);
        getContentPane().add(closeButton, BorderLayout.SOUTH);

        restoreWhichTabActive(tabbedPane);

    }

    private void restoreWhichTabActive(JTabbedPane tabbedPane)
    {
        String activeTab = options
            .getStringOption(Options.activePreferencesTab);
        int index = -1;
        if (activeTab != null)
        {
            index = tabbedPane.indexOfTab(activeTab);
        }
        if (index != -1)
        {
            tabbedPane.setSelectedIndex(index);
        }
        this.activePaneIndex = tabbedPane.getSelectedIndex();

        tabbedPane.addChangeListener(new ChangeListener()
        {
            public void stateChanged(ChangeEvent e)
            {
                try
                {
                    JTabbedPane theTabbedPane = (JTabbedPane)e.getSource();
                    int newIndex = theTabbedPane.getSelectedIndex();
                    if (newIndex != activePaneIndex)
                    {
                        activePaneIndex = newIndex;
                        String tabName = theTabbedPane.getTitleAt(newIndex);
                        if (tabName != null)
                        {
                            options.setOption(Options.activePreferencesTab,
                                tabName);
                        }
                    }
                }
                catch (NullPointerException nullEx)
                {
                    //
                }
            }
        });

    }

    private JPanel getColorPane()
    {

        JPanel colorPane = new JPanel(new BorderLayout());

        favColorPane = new JPanel();
        favColorPane
            .setLayout(new BoxLayout(favColorPane, BoxLayout.PAGE_AXIS));
        favColorPane.setBorder(BorderFactory
            .createTitledBorder("Favorite Colors"));
        String favorites = options.getStringOption(Options.favoriteColors);
        favoriteColors = null;
        colorsLeft = new ArrayList<PlayerColor>();
        for (PlayerColor playerColor : PlayerColor.values())
        {
            colorsLeft.add(playerColor);
        }
        if (favorites != null)
        {
            favoriteColors = PlayerColor
                .getByName(Split.split(',', favorites));
        }
        else
        {
            favoriteColors = new ArrayList<PlayerColor>();
        }
        ActionListener selectColorAL = new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                selectColor();
            }
        };
        ActionListener clearColorAL = new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                clearColor();
            }
        };
        JPanel colorButtonPane = new JPanel();
        addButton(colorButtonPane, "Select", selectColorAL);
        addButton(colorButtonPane, "Clear All", clearColorAL);
        colorPane.add(colorButtonPane, BorderLayout.NORTH);
        colorPane.add(favColorPane, BorderLayout.CENTER);
        for (int i = 0; i < favoriteColors.size(); i++)
        {
            PlayerColor color = favoriteColors.get(i);
            addColor(color);
            colorsLeft.remove(color);
        }
        return colorPane;
    }

    @Override
    public void dispose()
    {
        super.dispose();
        options = null;
    }

    @Override
    public void setVisible(boolean val)
    {
        // We use hideOnClose, so it never actually gets disposed while
        // client stays alive. Thus, to plug in the the saveOptions when
        // user closes the PreferencesWindow, we have to couple it to the
        // hide/unhide:
        if (!val)
        {
            options.saveOptions();
        }
        else
        {
            if ((this.getExtendedState() & JFrame.ICONIFIED) != 0)
            {
                this.setExtendedState(JFrame.NORMAL);
            }
        }
        super.setVisible(val);
    }

    public void actionPerformed(ActionEvent e)
    {
        setVisible(false);
    }

    public void itemStateChanged(ItemEvent e)
    {
        Object source = e.getSource();
        JComponent sourceJC = (JComponent)source;

        if (source instanceof JCheckBox)
        {
            String text = ((JCheckBox)source).getText();
            boolean selected = (e.getStateChange() == ItemEvent.SELECTED);
            options.setOption(text, selected);
            if (text.equals(Options.turnStartBottomBarYellow) && !selected)
            {
                gui.getBoard().myTurnBottomBarActions(selected);
            }
            if (text.equals(Options.turnStartChatYellow) && !selected)
            {
                gui.notifyWebClientItsPlayersTurn(selected);
            }
        }
        else if (source instanceof JRadioButton)
        {
            String text = ((JRadioButton)source).getActionCommand();
            if (!(e.getStateChange() == ItemEvent.SELECTED))
            {
                // Ignore, because was set to FALSE.
                // In RadioButtons we care only about the new state
            }
            else if (sourceJC.getParent() == rcModes)
            {
                if (text != null)
                {
                    options.setOption(Options.showRecruitChitsSubmenu, text);
                }
            }
            else if (sourceJC.getParent() == lfBox)
            {
                gui.setLookAndFeel(text);
            }
            else if (sourceJC.getParent() == mcModes)
            {
                if (text != null)
                {
                    options.setOption(Options.legionMoveConfirmationSubMenu,
                        text);
                }
            }
            else if (sourceJC.getParent() == nextSplitModes)
            {
                if (text != null)
                {
                    options.setOption(Options.nextSplitSubMenu, text);
                }
            }
        }
    }

    private void saveFavColor()
    {
        if (favoriteColors.size() > 0)
        {
            StringBuilder favorites = new StringBuilder();
            for (PlayerColor color : favoriteColors)
            {
                if (favorites.length() > 0)
                {
                    favorites.append(',');
                }
                favorites.append(color.getName());
            }
            options.setOption(Options.favoriteColors, favorites.toString());
        }
        else
        {
            options.removeOption(Options.favoriteColors);
        }
    }

    private void clearColor()
    {
        favoriteColors = new ArrayList<PlayerColor>();
        favColorPane.removeAll();
        colorsLeft = new ArrayList<PlayerColor>();
        for (PlayerColor color : PlayerColor.values())
        {
            colorsLeft.add(color);
        }
        this.repaint();
        saveFavColor();
    }

    private void unselectColor(JButton button)
    {
        String colorName = button.getText();
        favColorPane.remove(button);
        final PlayerColor color = PlayerColor.getByName(colorName);
        colorsLeft.add(color);
        this.repaint();
        favoriteColors.remove(color);
        saveFavColor();
    }

    private void addColor(PlayerColor color)
    {
        Color realColor = HTMLColor
            .stringToColor(color.getName() + "Colossus");
        JButton button = new JButton(color.getName());
        button.setBackground(realColor);
        int sum = realColor.getRed() + realColor.getGreen()
            + realColor.getBlue();
        button.setForeground(sum > 200 ? Color.black : Color.white);
        ActionListener al = new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                unselectColor((JButton)e.getSource());
            }
        };
        button.addActionListener(al);
        favColorPane.add(button);
    }

    private void selectColor()
    {
        PickColor.PickColorCallback callback = new PickColor.PickColorCallback()
        {
            @Override
            public void tellPickedColor(PlayerColor color)
            {
                doSomethingWithPickedColor(color);
            }
        };
        // Allow null for cancel
        new PickColor(this, "You", colorsLeft, options, callback, true);
    }

    public void doSomethingWithPickedColor(PlayerColor color)
    {
        if (color != null)
        {
            addColor(color);
            favoriteColors.add(color);
            colorsLeft.remove(color);
            saveFavColor();
            this.repaint();
        }
    }

    class ScaleValue extends JPanel implements ChangeListener, ActionListener
    {
        private final SpinnerNumberModel model;
        private final JSpinner spinner;
        private int newValue;
        private int oldValue;

        ScaleValue(int oldValue, int min, int max, int step)
        {
            super();
            this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

            this.oldValue = oldValue;
            this.newValue = oldValue;

            Box scalePane = new Box(BoxLayout.X_AXIS);
            add(scalePane);

            add(new JLabel("Window scale: "));
            this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            model = new SpinnerNumberModel(oldValue, min, max, step);
            spinner = new JSpinner(model);
            add(spinner);
            spinner.addChangeListener(this);

            JButton applyButton = new JButton("Apply");
            applyButton.addActionListener(this);
            add(applyButton);
        }

        public void stateChanged(ChangeEvent e)
        {
            newValue = ((Integer)spinner.getValue()).intValue();
        }

        public void actionPerformed(ActionEvent e)
        {
            if (e.getActionCommand().equals("Apply"))
            {
                if (newValue != oldValue)
                {
                    net.sf.colossus.util.StaticResourceLoader
                        .purgeImageCache();
                    options.setOption(Options.scale, newValue);
                }
                oldValue = newValue;
            }
        }
    }

    public void setCheckBoxValue(String name, boolean value)
    {
        JCheckBox cb;
        cb = prefCheckboxes.get(name);
        if (cb != null)
        {
            cb.setSelected(value);
        }
    }

    public void setRadioButtonValue(String name, boolean value)
    {
        JRadioButton rb;
        rb = prefRadioButtons.get(name);
        if (rb != null)
        {
            rb.setSelected(value);
        }
    }
}
