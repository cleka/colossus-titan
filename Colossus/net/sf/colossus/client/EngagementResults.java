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
    private Client client;


    EngagementResults(
        JFrame frame,
        Client client, 
        String winnerId,         // null if mutual elimination
        String method,           // flee, concede, negotiate, fight
        int points)
    {
        super(frame, "Engagement Status", false);

        this.client = client;

        pack();
        setBackground(Color.lightGray);
        addWindowListener(this);

        Container contentPane = getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));

        updateData(winnerId, method, points);
    }


    void updateData(String winnerId, String method, int points)
    {
        String hexLabel = client.getBattleSite();
        String attackerId = client.getAttackerMarkerId();
        String defenderId = client.getDefenderMarkerId();
        int battleTurn = client.getBattleTurnNumber();
        int gameTurn = client.getTurnNumber();

        // TODO
        java.util.List attackerStartingContents = new ArrayList();
        java.util.List defenderStartingContents = new ArrayList();

        java.util.List attackerEndingContents = client.getLegionImageNames(
            attackerId);
        java.util.List defenderEndingContents = client.getLegionImageNames(
            defenderId);

        String loserId = null;
        if (winnerId != null && winnerId.equals(attackerId))
        {
            loserId = defenderId;
        }
        else if (winnerId != null && winnerId.equals(defenderId))
        {
            loserId = attackerId;
        }

        Container contentPane = getContentPane();

        JLabel label0 = new JLabel("Turn " + gameTurn);
        contentPane.add(label0);
        
        JLabel label1 = new JLabel(attackerId + " attacked " + defenderId + 
            " in " + MasterBoard.getHexByLabel(hexLabel).getDescription());
        contentPane.add(label1);

        JLabel label2 = new JLabel();
        if (method.equals("flee"))
        {
            label2.setText(winnerId + " won when " + loserId + " fled");
        }
        else if (method.equals("concede"))
        {
            label2.setText(winnerId + " won when " + loserId + " conceded");
        }
        else if (method.equals("negotiate"))
        {
            if (winnerId != null)
            {
                label2.setText(winnerId + " won a negotiated settlement");
            }
            else
            {
                label2.setText("Negotiated mutual elimination");
            }
        }
        else if (method.equals("fight"))
        {
            if (winnerId != null)
            {
                label2.setText(winnerId + " won the battle in " + battleTurn + 
                    " turns");
            }
            else
            {
                label2.setText("Mutual elimination in " + battleTurn + 
                    " turns");
            }
        }
        else
        {
            label2.setText("bogus method");
        }
        contentPane.add(label2);

        JLabel label3 = new JLabel(winnerId + " earned " + points + 
            " points");
        contentPane.add(label3);

        showLegionContents(attackerId, attackerStartingContents,
            contentPane, true);

        showLegionContents(defenderId, defenderStartingContents,
            contentPane, true);

        showLegionContents(attackerId, attackerEndingContents,
            contentPane, false);

        showLegionContents(defenderId, defenderEndingContents,
            contentPane, false);


        JButton prevButton = new JButton("Previous");
        prevButton.addActionListener(this);
        contentPane.add(prevButton);

        JButton nextButton = new JButton("Next");
        nextButton.addActionListener(this);
        contentPane.add(nextButton);

        JButton dismissButton = new JButton("Dismiss");
        dismissButton.addActionListener(this);
        contentPane.add(dismissButton);

        pack();
        setVisible(true);
    }


    void showLegionContents(String markerId, java.util.List imageNames, 
        Container contentPane, boolean starting)
    {
        JLabel label = new JLabel(starting ? "Starting" : "Final" + 
            " contents of " + markerId);
        contentPane.add(label);

        Box panel = new Box(BoxLayout.X_AXIS);
        contentPane.add(panel);

        int scale = 4 * Scale.get();

        Iterator it = imageNames.iterator();
        while (it.hasNext())
        {
            String imageName = (String)it.next();
            Chit chit = new Chit(scale, imageName, this);
            panel.add(chit);
        }
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
