import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Class PickRecruit allows a player to pick a creature to recruit.
 * @version $Id$
 * @author David Ripton
 */


public class PickRecruit extends JDialog implements MouseListener,
    WindowListener
{
    private int numEligible;
    private Creature [] recruits;
    private MediaTracker tracker;
    private boolean imagesLoaded;
    private Player player;
    private Legion legion;
    private Chit [] recruitChits;
    private Marker legionMarker;
    private Chit [] legionChits;
    private int scale = 60;
    private JFrame parentFrame;
    private GridBagLayout gridbag = new GridBagLayout();
    private GridBagConstraints constraints = new GridBagConstraints();
    private boolean dialogLock;


    public PickRecruit(JFrame parentFrame, Legion legion)
    {
        super(parentFrame, legion.getPlayer().getName() +
            ": Pick Recruit in " + legion.getCurrentHex().getDescription(),
            true);

        if (!legion.canRecruit())
        {
            dispose();
            return;
        }

        this.parentFrame = parentFrame;

        recruits = new Creature[5];

        numEligible = Game.findEligibleRecruits(legion, recruits);

        this.legion = legion;
        player = legion.getPlayer();

        addMouseListener(this);
        addWindowListener(this);

        Container contentPane = getContentPane();

        contentPane.setLayout(gridbag);

        pack();

        setBackground(Color.lightGray);

        int height = legion.getHeight();

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
            constraints.gridy = 0;
            gridbag.setConstraints(legionChits[i], constraints);
            contentPane.add(legionChits[i]);
        }


        recruitChits = new Chit[numEligible];

        // There are height + 1 chits in the top row.  There
        // are numEligible chits / labels to place beneath.
        // So we have (height + 1) - numEligible empty 
        // columns, half of which we'll put in front.
        int leadSpace = ((height + 1) - numEligible) / 2;
        if (leadSpace < 0)
        {
            leadSpace = 0;
        }

        for (int i = 0; i < numEligible; i++)
        {
            recruitChits[i] = new Chit(scale, recruits[i].getImageName(),
                this);

            constraints.gridx = leadSpace + i;
            constraints.gridy = 1;
            gridbag.setConstraints(recruitChits[i], constraints);
            contentPane.add(recruitChits[i]);
            recruitChits[i].addMouseListener(this);
            int count = recruits[i].getCount();
            JLabel countLabel = new JLabel(Integer.toString(count), 
                JLabel.CENTER);
            constraints.gridy = 2;
            gridbag.setConstraints(countLabel, constraints);
            contentPane.add(countLabel);
        }

        tracker = new MediaTracker(this);

        for (int i = 0; i < numEligible; i++)
        {
            tracker.addImage(recruitChits[i].getImage(), 0);
        }
        for (int i = 0; i < height; i++)
        {
            tracker.addImage(legionChits[i].getImage(), 0);
        }
        tracker.addImage(legionMarker.getImage(), 0);

        try
        {
            tracker.waitForAll();
        }
        catch (InterruptedException e)
        {
            JOptionPane.showMessageDialog(parentFrame, e.toString() + 
                " waitForAll was interrupted");
        }
        imagesLoaded = true;

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
            if (recruitChits[i] == source && !dialogLock)
            {
                // Prevent multiple clicks from yielding multiple recruits.
                dialogLock = true;

                // Recruit the chosen creature.
                Game.doRecruit(recruits[i], legion, parentFrame);

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
