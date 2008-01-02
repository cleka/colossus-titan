package net.sf.colossus.client;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.sf.colossus.util.KFrame;
import net.sf.colossus.util.Options;

public class PreferencesWindow extends KFrame
    implements ItemListener, ActionListener
{
    private IOptions options;
    private Client client;
    private Map prefCheckboxes = new HashMap();
    private JButton closeButton;
    private Box lfBox;    // Look & Feel
    private Box rcModes;  // Recruit Chit modes
    
    public PreferencesWindow(IOptions options, Client client)
    {
        super("Preferences");
        
        this.options = options;
        this.client = client;
        
        getContentPane().add(new JLabel("Dummy"));
        
        setDefaultCloseOperation(KFrame.HIDE_ON_CLOSE);
        
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
        pane.add(cb);
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
        cont.add(rb);
        boolean selected = (text.equals(current));
        rb.setSelected(selected);
    }

    private void setupGUI()
    {
        JTabbedPane tabbedPane = new JTabbedPane();
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(tabbedPane, BorderLayout.CENTER);

        // Battle Map tab:
        JPanel battlePane = new JPanel(new GridLayout(0, 1));
        tabbedPane.addTab("Battle", battlePane);
        addCheckBox(battlePane, Options.useColoredBorders);
        addCheckBox(battlePane, Options.doNotInvertDefender);
        battlePane.add(new JLabel(""));
        addCheckBox(battlePane, Options.showHitThreshold, true, true);
        addCheckBox(battlePane, Options.showDiceAjustmentsTerrain, true, true);
        addCheckBox(battlePane, Options.showDiceAjustmentsRange, true, true);
        battlePane.add(Box.createVerticalGlue());

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
        JPanel apPane = new JPanel(new GridLayout(0, 1));
        tabbedPane.addTab("Autoplay", apPane);
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
        apPane.add(Box.createVerticalGlue());
        
        // Graphics/View tab
        Box viewPane = new Box(BoxLayout.Y_AXIS);
        tabbedPane.addTab("Graphics/View", viewPane);

        //   - graphics features panel in Garhpics tab
        JPanel graphicsPane = new JPanel(new GridLayout(0, 1));
        graphicsPane.setBorder(new TitledBorder("Graphics features"));
        graphicsPane.setAlignmentX(LEFT_ALIGNMENT);
        addCheckBox(graphicsPane, Options.antialias);
        addCheckBox(graphicsPane, Options.useOverlay);
        addCheckBox(graphicsPane, Options.useSVG);
        addCheckBox(graphicsPane, Options.noBaseColor);
        viewPane.add(graphicsPane);
       
        //   - "Show recruit preview chits ..." panel in Graphics tab
        ButtonGroup group = new ButtonGroup();
        rcModes = new Box(BoxLayout.Y_AXIS);
        rcModes.setAlignmentX(LEFT_ALIGNMENT);
        rcModes.setBorder(new TitledBorder(Options.showRecruitChitsSubmenu));
        viewPane.add(rcModes);

        String current = options.getStringOption(Options.showRecruitChitsSubmenu);
        // NOTE! : event handling is based on that the RB is partof this rcModes Box!
        addRadioButton(rcModes, group, Options.showRecruitChitsNone, "", current);
        addRadioButton(rcModes, group, Options.showRecruitChitsStrongest, "", current);
        addRadioButton(rcModes, group, Options.showRecruitChitsRecruitHint, "", current);
        addRadioButton(rcModes, group, Options.showRecruitChitsAll, "", current);
 
        //   -- misc. Panel in Graphics tab: 
        //        so far only dubious as blanks
        Box miscPane = new Box(BoxLayout.Y_AXIS);
        viewPane.add(miscPane);
        miscPane.setBorder(new TitledBorder("Misc."));
        miscPane.setAlignmentX(LEFT_ALIGNMENT);
        //  The "dubious as blanks" option makes only sense with the 
        //    "view what SplitPrediction tells us" mode => otherwise inactive.
        boolean avail = (client.getViewMode() == Options.viewableEverNum);
        addCheckBox(miscPane, Options.dubiousAsBlanks, avail, false); 
        // , KeyEvent.VK_D);
        viewPane.add(Box.createVerticalGlue());

        
        // Window tab
        Box windowPane = new Box(BoxLayout.Y_AXIS);
        tabbedPane.addTab("Windows", windowPane);

        addCheckBox(windowPane, Options.stealFocus); // KeyEvent.VK_F

        //   Look & Feel panel in Window tab
        ButtonGroup lfGroup = new ButtonGroup();
        lfBox = new Box(BoxLayout.Y_AXIS);
        windowPane.add(lfBox);
        lfBox.setAlignmentX(LEFT_ALIGNMENT);
        lfBox.setBorder(new TitledBorder("Look & Feel"));
        lfBox.add(new JLabel("Choose your preferred \"Look & Feel\":"));
        String currentLF = UIManager.getLookAndFeel().getName();
        UIManager.LookAndFeelInfo[] lfInfo =
            UIManager.getInstalledLookAndFeels();
        for (int i = 0; i < lfInfo.length; i++)
        {
            // NOTE! : event handling is based on that the RB is partof this lfBox Box!
            addRadioButton(lfBox, lfGroup, lfInfo[i].getName(),
                lfInfo[i].getClassName(), currentLF);          
        }

        int oldValue = Scale.get();
        int min = 5;
        int max = 25;
        int step = 1;
        ScaleValue scalePane = new ScaleValue(oldValue, min, max, step);
        scalePane.setAlignmentX(LEFT_ALIGNMENT);
        windowPane.add(scalePane);

        windowPane.add(Box.createVerticalGlue());
        
        closeButton = new JButton("Close");
        closeButton.addActionListener(this);
        getContentPane().add(closeButton, BorderLayout.SOUTH);
    }
    
    public void dispose()
    {
//        cleanPrefCBListeners();
        super.dispose();
        options = null;
        client = null;
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
            else if ( sourceJC.getParent() == lfBox )
            {
                client.setLookAndFeel(text);
            }
        }
    }
    
    class ScaleValue extends JPanel
        implements ChangeListener, ActionListener
    {
        SpinnerNumberModel model;
        JSpinner spinner;
        int newValue;
        int oldValue;
        
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
                    client.setOption(Options.scale, newValue);
                    Scale.set(newValue);
                    net.sf.colossus.util.ResourceLoader.purgeImageCache();
                    client.rescaleAllWindows();
                }
                oldValue = newValue;
            }
        }
    }
}
