import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

/**
 * Class PickRecruiter allows a player to choose which creature(s) recruit.
 * @version $Id$
 * @author David Ripton
 */


public class PickRecruiter extends JDialog implements MouseListener,
    WindowListener
{
    private ArrayList recruiters;
    private Player player;
    private Legion legion;
    private ArrayList recruiterChits = new ArrayList();
    private Marker legionMarker;
    private int scale = 60;
    private int height;
    private GridBagLayout gridbag = new GridBagLayout(); 
    private GridBagConstraints constraints = new GridBagConstraints();
    private static Creature recruiter;


    /** recruiters is a list of Creatures */
    private PickRecruiter(JFrame parentFrame, Legion legion,
        ArrayList recruiters)
    {
        super(parentFrame, legion.getPlayer().getName() +
            ": Pick Recruiter", true);

        recruiter = null;

        this.legion = legion;
        player = legion.getPlayer();

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
        
        Collection critters = legion.getCritters();
        Iterator it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            Chit chit = new Chit(scale, critter.getImageName(), this);
            constraints.gridx = GridBagConstraints.RELATIVE;
            constraints.gridy = 0;
            gridbag.setConstraints(chit, constraints);
            contentPane.add(chit);
        }

        // There are height + 1 chits in the top row.  There
        // are numEligible chits to place beneath.
        // So we have (height + 1) - numEligible empty 
        // columns, half of which we'll put in front.
        int numEligible = recruiters.size();
        int leadSpace = ((height + 1) - numEligible) / 2;
        if (leadSpace < 0)
        {
            leadSpace = 0;
        }

        int i = 0;
        it = recruiters.iterator();
        while (it.hasNext())
        {
            Creature recruiter = (Creature)it.next();
            Chit chit = new Chit(scale, recruiter.getImageName(), this);
            recruiterChits.add(chit);
            constraints.gridx = leadSpace + i;
            constraints.gridy = 1;
            gridbag.setConstraints(chit, constraints);
            contentPane.add(chit);
            chit.addMouseListener(this);
            i++;
        }

        pack();
        
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(new Point(d.width / 2 - getSize().width / 2,
            d.height / 2 - getSize().height / 2));

        setVisible(true);
        repaint();
    }
    
    
    public static Creature pickRecruiter(JFrame parentFrame, Legion legion,
        ArrayList recruiters)
    {
        new PickRecruiter(parentFrame, legion, recruiters);
        return recruiter;
    }


    public void mousePressed(MouseEvent e)
    {
        Object source = e.getSource();
        int i = recruiterChits.indexOf(source);
        if (i != -1)
        {
            recruiter = (Creature)recruiters.get(i); 

            // Then exit.
            dispose();
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
