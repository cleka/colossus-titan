package net.sf.colossus.gui;


import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import net.sf.colossus.guiutil.KDialog;
import net.sf.colossus.variant.MasterBoardTerrain;
import net.sf.colossus.variant.MasterHex;
import net.sf.colossus.variant.Variant;


/**
 * Common class for displaying recruit trees information.
 *
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
        getContentPane().setLayout(
            new BoxLayout(getContentPane(), BoxLayout.X_AXIS));

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                dispose();
            }
        });
    }

    void doOneTerrain(MasterBoardTerrain terrain, MasterHex hex,
        Variant variant, ClientGUI gui)
    {
        assert SwingUtilities.isEventDispatchThread() : "GUI code should only run on the EDT";

        JFrame frame = (JFrame)this.getParent();
        getContentPane().add(
            new HexRecruitTreePanel(BoxLayout.Y_AXIS, terrain, hex, frame,
                true, variant, gui));
    }

    @Override
    public void dispose()
    {
        for (Component c : getContentPane().getComponents())
        {
            if (c instanceof HexRecruitTreePanel)
            {
                ((HexRecruitTreePanel)c).closeCreatureWindows();
            }
        }
        super.dispose();
    }
}
