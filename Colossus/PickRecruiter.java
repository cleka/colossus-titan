import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Class PickRecruiter allows a player to choose which creature(s) recruit.
 * @version $Id$
 * @author David Ripton
 */


public class PickRecruiter extends JDialog implements MouseListener,
    WindowListener
{
    private int numEligible;
    private Critter [] recruiters;
    private Player player;
    private Legion legion;
    private Chit [] recruiterChits;
    private Marker legionMarker;
    private Chit [] legionChits;
    private int scale = 60;
    private int height;
    private GridBagLayout gridbag = new GridBagLayout(); 
    private GridBagConstraints constraints = new GridBagConstraints();


    public PickRecruiter(JFrame parentFrame, Legion legion, int numEligible,
        Critter [] recruiters)
    {
        super(parentFrame, legion.getPlayer().getName() +
            ": Pick Recruiter", true);

        this.legion = legion;
        player = legion.getPlayer();

        this.numEligible = numEligible;
        this.recruiters = recruiters;

        addMouseListener(this);
        addWindowListener(this);

        Container contentPane = getContentPane();

        contentPane.setLayout(gridbag);

        pack();

        setBackground(Color.lightGray);

        height = legion.getHeight();

        setResizable(false);

        legionMarker = new Marker(scale, legion.getImageName(), this, legion);
        constraints.gridx = GridBagConstraints.RELATIVE;
        constraints.gridy = 0;
        gridbag.setConstraints(legionMarker, constraints);
        contentPane.add(legionMarker);
        
        legionChits = new Chit[height];
        for (int i = 0; i < height; i++)
        {
            legionChits[i] = new Chit(scale, 
                legion.getCritter(i).getImageName(), this);
            constraints.gridx = GridBagConstraints.RELATIVE;
            constraints.gridy = 0;
            gridbag.setConstraints(legionChits[i], constraints);
            contentPane.add(legionChits[i]);
        }

        recruiterChits = new Chit[numEligible];

        // There are height + 1 chits in the top row.  There
        // are numEligible chits to place beneath.
        // So we have (height + 1) - numEligible empty 
        // columns, half of which we'll put in front.
        int leadSpace = ((height + 1) - numEligible) / 2;
        if (leadSpace < 0)
        {
            leadSpace = 0;
        }

        for (int i = 0; i < numEligible; i++)
        {
            recruiterChits[i] = new Chit(scale, recruiters[i].getImageName(),
                this);
            constraints.gridx = leadSpace + i;
            constraints.gridy = 1;
            gridbag.setConstraints(recruiterChits[i], constraints);
            contentPane.add(recruiterChits[i]);
            recruiterChits[i].addMouseListener(this);
        }

        pack();
        
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(new Point(d.width / 2 - getSize().width / 2,
            d.height / 2 - getSize().height / 2));

        setVisible(true);
        repaint();
    }


    public void mousePressed(MouseEvent e)
    {
        Object source = e.getSource();
        for (int i = 0; i < numEligible; i++)
        {
            if (recruiterChits[i] == source)
            {
                // The selected recruiter will be placed in the 0th 
                // position of the array.
                recruiters[0] = recruiters[i];

                // Then exit.
                dispose();
                return;
            }
        }
    }


    public void mouseEntered(MouseEvent e)
    {
    }


    public void mouseExited(MouseEvent e)
    {
    }


    public void mouseClicked(MouseEvent e)
    {
    }


    public void mouseReleased(MouseEvent e)
    {
    }


    public void windowActivated(WindowEvent e)
    {
    }


    public void windowClosed(WindowEvent e)
    {
    }


    public void windowClosing(WindowEvent e)
    {
        dispose();
    }


    public void windowDeactivated(WindowEvent e)
    {
    }


    public void windowDeiconified(WindowEvent e)
    {
    }


    public void windowIconified(WindowEvent e)
    {
    }


    public void windowOpened(WindowEvent e)
    {
    }
}
