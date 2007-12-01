package net.sf.colossus.client;


import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.sf.colossus.util.KDialog;
import net.sf.colossus.util.Options;


public class AutoInspector extends KDialog
{
    private IOptions options;
    
    private SaveWindow saveWindow;
    
    private JScrollPane scrollPane;

    private String playerName;

    private int viewMode;
    
    private boolean dubiousAsBlanks;

   
    public AutoInspector(JFrame frame, IOptions options, 
    		String playerName, int viewMode, boolean dubiousAsBlanks)
    {
        super(frame, "Inspector", false);

        this.options = options;
        this.playerName = playerName;
        this.viewMode = viewMode;
        this.dubiousAsBlanks = dubiousAsBlanks;
       
        setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        addWindowListener(new WindowAdapter(){
            public void windowClosing(WindowEvent e) 
            {
                AutoInspector.this.options.setOption(Options.showAutoInspector,
                    false);
            }
        });

        saveWindow = new SaveWindow(options, "AutoInspector");
        Point location = getUpperRightCorner(550);
        saveWindow.restore(this, location);
        
        scrollPane = new JScrollPane();
        
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(scrollPane, BorderLayout.CENTER);

        setVisible(true);
    }

    public void dispose()
    {
        saveWindow.saveSize(getSize());
        saveWindow.saveLocation(getLocation());
        super.dispose();
    }
    
    public Dimension getPreferredSize() 
    {
        // this is just a first go to have some size at all
        // TODO do a decent estimate of a legion's size
        return new Dimension(550, 110);
    }
    
    public void showLegion(LegionInfo legion) 
    {
        scrollPane.getViewport().removeAll();
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        LegionInfoPanel liPanel = new LegionInfoPanel(legion, 4 * Scale.get(),
                5, 2, false, viewMode, playerName, dubiousAsBlanks);
        panel.add(liPanel);
        String valueText = liPanel.getValueText();
        String ownerText = legion.isMyLegion() ? 
                "" : " [" + legion.getPlayerName() + "]";
        setTitle("Inspector: Legion " + legion.getMarkerId() + 
                valueText + ownerText);
        liPanel = null;
        
        scrollPane.getViewport().add(panel);
        repaint();
    }

    public void showHexRecruitTree(GUIMasterHex hex) 
    {
        setTitle("Inspector");
        scrollPane.getViewport().removeAll();
        MasterHex hexModel = hex.getMasterHexModel();
        scrollPane.getViewport().add(new HexRecruitTreePanel(BoxLayout.X_AXIS,
                hexModel.getTerrain(),hexModel.getLabel(), new MouseAdapter()
        {
        	    // nothing to do
        }));
        repaint();
    }

    // public so that client can update it when option is changed.
    public void setDubiousAsBlanks(boolean newVal)
    {
        this.dubiousAsBlanks = newVal;
    }
}
