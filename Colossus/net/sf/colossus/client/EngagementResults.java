package net.sf.colossus.client;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

import net.sf.colossus.util.KDialog;
import net.sf.colossus.util.Log;

/**
 * Post-engagement status dialog.
 * @version $Id$
 * @author David Ripton
 */

final class EngagementResults extends KDialog implements ActionListener,
    WindowListener
{
    EngagementResults(
        JFrame frame,
        Client client, 
        String winnerId,         // Null if mutual elimination
        String method,           // flee, concede, negotiate, fight
        int points)
    {
        super(frame, "Engagement Status", false);

        String hexLabel = client.getBattleSite();
        String attackerId = client.getAttackerMarkerId();
        String defenderId = client.getDefenderMarkerId();
        int turns = client.getBattleTurnNumber();
        java.util.List attackerStartingContents = null;
//            client.getAttackerStartingContents();
        java.util.List defenderStartingContents = null;
//            client.getDefenderStartingContents();

        String loserId = null;
        if (winnerId.equals(attackerId))
        {
            loserId = defenderId;
        }
        else if (winnerId.equals(defenderId))
        {
            loserId = attackerId;
        }

        pack();
        setBackground(Color.lightGray);
        addWindowListener(this);

        centerOnScreen();

        Container contentPane = getContentPane();
        int scale = 4 * Scale.get();
        
        JLabel label1 = new JLabel(attackerId + " attacked " + defenderId + 
            " in " + MasterBoard.getHexByLabel(hexLabel).getDescription());
        contentPane.add(label1);

        JLabel label2 = new JLabel();
        if (method.equals("flee"))
        {
            label2.setText(winnerId + " won when " + loserId + "fled");
        }
        else if (method.equals("concede"))
        {
            label2.setText(winnerId + " won when " + loserId + "conceded");
        }
        else if (method.equals("negotiate"))
        {
            label2.setText(winnerId + " won a negotiated settlement");
        }
        else if (method.equals("fight"))
        {
            label2.setText(winnerId + " won the battle in " + turns + 
                " turns");
        }
        else
        {
            label2.setText("bogus method");
        }
        contentPane.add(label2);

        JLabel label3 = new JLabel(winnerId + " earned " + points + 
            " points");
        contentPane.add(label3);

        JLabel label4 = new JLabel("Starting contents of " + attackerId);
        contentPane.add(label4);

        JPanel panel1 = new JPanel();
        panel1.setLayout(new BoxLayout(panel1, BoxLayout.X_AXIS));
        contentPane.add(panel1);

        Iterator it = attackerStartingContents.iterator();
        while (it.hasNext())
        {
            String imageName = (String)it.next();
            Chit chit = new Chit(scale, imageName, this);
            panel1.add(chit);
            chit.addMouseListener(this);
        }

        JLabel label5 = new JLabel("Starting contents of " + defenderId);
        contentPane.add(label5);

        JPanel panel2 = new JPanel();
        panel2.setLayout(new BoxLayout(panel2, BoxLayout.X_AXIS));
        contentPane.add(panel1);

        it = defenderStartingContents.iterator();
        while (it.hasNext())
        {
            String imageName = (String)it.next();
            Chit chit = new Chit(scale, imageName, this);
            panel2.add(chit);
            chit.addMouseListener(this);
        }

        JButton dismissButton = new JButton("Dismiss");
        dismissButton.addActionListener(this);
        contentPane.add(dismissButton);

        pack();
        setVisible(true);
    }


    public void actionPerformed(ActionEvent e)
    {
        dispose();
    }
    
    public void windowClosing(WindowEvent e)
    {
        dispose();
    }
}
