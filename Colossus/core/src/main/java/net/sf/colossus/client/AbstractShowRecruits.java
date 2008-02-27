package net.sf.colossus.client;


import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import net.sf.colossus.util.KDialog;
import net.sf.colossus.variant.MasterBoardTerrain;
import net.sf.colossus.variant.MasterHex;


/**
 * Common class for displaying recruit trees information.
 * @version $Id$
 * @author David Ripton
 * @author Barrie Treloar
 */
public abstract class AbstractShowRecruits extends KDialog
{

    AbstractShowRecruits(JFrame parentFrame)
    {
        super(parentFrame, "Recruits", false);

        assert SwingUtilities.isEventDispatchThread() : "GUI code should only run on the EDT";
        
        setBackground(Color.lightGray);
        addWindowListener(this);
        getContentPane().setLayout(
            new BoxLayout(getContentPane(), BoxLayout.X_AXIS));

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        addMouseListener(new MouseAdapter(){
            @Override
            public void mouseClicked(MouseEvent e)
            {
                dispose();
            }
        });
    }

    void doOneTerrain(MasterBoardTerrain terrain, MasterHex hex)
    {
        assert SwingUtilities.isEventDispatchThread() : "GUI code should only run on the EDT";
        
        getContentPane().add(
            new HexRecruitTreePanel(BoxLayout.Y_AXIS, terrain, hex, this));
    }
}
