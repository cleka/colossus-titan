package net.sf.colossus.webclient;


import java.awt.Container;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.logging.Logger;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import net.sf.colossus.common.Options;
import net.sf.colossus.guiutil.KDialog;
import net.sf.colossus.guiutil.SaveWindow;

public class PreferencesDialog extends KDialog
{
    private static final Logger LOGGER = Logger
        .getLogger(PreferencesDialog.class.getName());

    // they are used both as button text and key in Options
    public final static String AutoLoginCBText = "Auto-login on start";
    public final static String AutoGamePaneCBText = "After login Game pane";

    private final Options options;

    private JCheckBox autoLoginCB;
    private JCheckBox autoGamePaneCB;

    private SaveWindow saveWindow;

    public PreferencesDialog(Frame owner, Options options)
    {
        super(owner, "Preferences", false);
        // TODO Auto-generated constructor stub

        this.options = options;

        setupGUI();

        LOGGER.info("WebClient Preferences Dialog instantiated.");
    }

    private void setupGUI()
    {
        Container contentPane = getContentPane();

        JPanel preferencesPanel = new JPanel(new GridLayout(0, 2));
        preferencesPanel.setBorder(new TitledBorder("Preferences"));



        preferencesPanel.add(new JLabel("test"));
        preferencesPanel.add(new JLabel("test2"));

        boolean alos = this.options.getOption(AutoLoginCBText);
        autoLoginCB = new JCheckBox(AutoLoginCBText, alos);
        autoLoginCB.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                options.setOption(AutoLoginCBText, autoLoginCB.isSelected());
            }
        });

        preferencesPanel.add(autoLoginCB);
        preferencesPanel.add(new JLabel(""));

        boolean algp = this.options.getOption(AutoGamePaneCBText);
        autoGamePaneCB = new JCheckBox(AutoGamePaneCBText, algp);
        autoGamePaneCB.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                options.setOption(AutoGamePaneCBText,
                    autoGamePaneCB.isSelected());
            }
        });

        preferencesPanel.add(autoGamePaneCB);
        preferencesPanel.add(new JLabel(""));

        contentPane.add(preferencesPanel);

        addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                setVisible(false);
            }
        });

        pack();
        saveWindow = new SaveWindow(options, "PickLord");
        Point location = saveWindow.loadLocation();
        if (location == null)
        {
            centerOnScreen();
        }
        else
        {
            setLocation(location);
        }
        setVisible(false);
        repaint();
    }

    public void toggleVisible()
    {
        setVisible(!isVisible());
    }

    @Override
    public void dispose()
    {
        saveWindow.saveLocation(getLocation());
        super.dispose();
    }

}
