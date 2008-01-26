package net.sf.colossus.client;


import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.BoxLayout;
import javax.swing.JFrame;

import net.sf.colossus.util.KDialog;
import net.sf.colossus.variant.MasterHex;


/**
 * Common class for displaying recruit trees information.
 * @version $Id$
 * @author David Ripton
 * @author Barrie Treloar
 */
public abstract class AbstractShowRecruits extends KDialog implements
    MouseListener, WindowListener
{

    AbstractShowRecruits(JFrame parentFrame)
    {
        super(parentFrame, "Recruits", false);

        setBackground(Color.lightGray);
        addWindowListener(this);
        getContentPane().setLayout(
            new BoxLayout(getContentPane(), BoxLayout.X_AXIS));

        addMouseListener(this);
    }

    void doOneTerrain(String terrain, MasterHex hex)
    {
        getContentPane().add(
            new HexRecruitTreePanel(BoxLayout.Y_AXIS, terrain, hex, this));
    }

    @Override
    public void mouseClicked(MouseEvent e)
    {
        dispose();
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        dispose();
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
        dispose();
    }

    @Override
    public void windowClosing(WindowEvent e)
    {
        dispose();
    }

    @Override
    public void dispose()
    {
        super.dispose();
    }
}
