package net.sf.colossus.client;


import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

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

    public AutoInspector(JFrame frame, IOptions options)
    {
        super(frame, "Inspector", false);

        this.options = options;

        setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        addWindowListener(new WindowAdapter(){
            public void windowClosing(WindowEvent e) 
            {
                AutoInspector.this.options.setOption(Options.showAutoInspector,
                    false);
            }
        });

        saveWindow = new SaveWindow(options, "AutoInspector");
        saveWindow.restore(this, new Point(0,0));
        
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
        return new Dimension(200,50);
    }
    
    public void showLegion(Marker marker, LegionInfo legion) 
    {
        scrollPane.getViewport().removeAll();
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        // TODO for some reason the marker won't display itself properly here
        //panel.add(marker);
        panel.add(new LegionInfoPanel(legion, 4 * Scale.get(), 5, 2, false));
        scrollPane.getViewport().add(panel);
        repaint();
    }
}
