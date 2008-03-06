package net.sf.colossus.client;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import net.sf.colossus.game.Creature;
import net.sf.colossus.game.Legion;
import net.sf.colossus.util.KDialog;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.Variant;


/**
 * Class BattleAftermathWindow shows a Unified Summon, Recruit, Reinforce, etc 
 * window. 
 * This is still ALPHA.
 * @version $Id: BattleAftermathWindow.java 2900 2008-01-06 10:34:55Z peterbecker $
 * @author Dranathi
 */
public class BattleAftermathWindow extends KDialog implements MouseListener,
    WindowListener, ActionListener
{
    // should be derived from Scale
    private static final int CREATURE_SIZE = 30;

    private final Client client;
    private final LegionClientSide legion;
    private final Variant variant;
    private final List<CritterSource> summonableList;
    private final List<CreatureType> recruits;

    private final List<CritterSource> critterSourceList = new ArrayList<CritterSource>();
    private final List<CritterSource> selectedList = new ArrayList<CritterSource>();
    private static final LegionClientSide CARETAKER = null;
    private static final Chit CRITTERSLOT = new Chit(CREATURE_SIZE,
        "QuestionMarkMask", false, true, false);

    private final JPanel gridPane; // Content Panel
    private final JPanel buttonPane; // Action Buttons
    private final JLabel statusLabel;

    // Left Side is Current State / Selectable chits
    private JPanel legionPane;
    private JPanel acquirablePane;
    private JPanel summonablePane;
    private JPanel recruitPane;

    // Right Side is Result State / Selected chits
    private JPanel newLegionPane;
    private JPanel newAcquirablePane;
    private JPanel newSummonablePane;
    private JPanel newRecruitPane;
    private final int slots;

    public BattleAftermathWindow(JFrame frame, Client client,
        LegionClientSide legion, List<CritterSource> summonables,
        List<CreatureType> recruits)

    {
        super(frame, client.getOwningPlayer().getName()
            + ": Battle Aftermath " + legion, false);

        assert SwingUtilities.isEventDispatchThread() : "Constructor should be called only on the EDT";

        this.variant = client.getGame().getVariant();
        this.client = client;
        this.legion = legion;
        this.summonableList = summonables;
        this.recruits = recruits;
        slots = 7 - legion.getHeight();
        getContentPane().setLayout(new BorderLayout());
        gridPane = new JPanel(new GridLayout(0, 2));
        getContentPane().add(gridPane, BorderLayout.CENTER);
        useSaveWindow(client.getOptions(), "BattleAftermath", null);

        addMouseListener(this);
        addWindowListener(this);

        setBackground(Color.lightGray);

        constructGrid();

        buttonPane = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        getContentPane().add(buttonPane, BorderLayout.SOUTH);

        JButton doneButton = new JButton("Done");
        buttonPane.add(doneButton);
        doneButton.addActionListener(this);

        JButton cancelButton = new JButton("Cancel");
        buttonPane.add(cancelButton);
        cancelButton.addActionListener(this);

        statusLabel = new JLabel("Select Additions to Legion");
        buttonPane.add(statusLabel);
        pack();
        setVisible(true);
        repaint();
    }

    private void constructGrid()
    {
        // Current Legion on Top (applies to Attacker & Defender)
        legionPane = new JPanel();
        legionPane.setBorder(BorderFactory
            .createTitledBorder("Current Legion"));
        for (Creature creature : legion.getCreatures())
        {
            Chit chit = new Chit(CREATURE_SIZE, creature.getType().getName());
            chit.addMouseListener(this);
            legionPane.add(chit);
        }
        gridPane.add(legionPane);
        // Panel for creatures being added.
        newLegionPane = new JPanel();
        legionPane.setBorder(BorderFactory
            .createTitledBorder("Chits being Added"));
        for (int i = 0; i < slots; i++)
        {
            Chit chit = new Chit(CRITTERSLOT);
            newLegionPane.add(chit);
        }
        gridPane.add(newLegionPane);

        // Current Acquirable is next
        // Selected Acquirable on the right.
        acquirablePane = new JPanel();
        newAcquirablePane = new JPanel();
        acquirablePane.setLayout(new BoxLayout(acquirablePane,
            BoxLayout.Y_AXIS));
        acquirablePane.setBorder(BorderFactory
            .createTitledBorder("Acquirables"));
        newAcquirablePane.setBorder(BorderFactory
            .createTitledBorder("Acquisitions"));
        if (legion.decisions != null)
        {
            for (Legion.AcquirableDecision decision : legion.decisions)
            {
                JPanel aquireAtPanel = new JPanel();
                aquireAtPanel.setBorder(BorderFactory
                    .createTitledBorder(decision.getPoints() + ""));
                for (String creature : decision.getNames())
                {
                    Chit chit = new Chit(CREATURE_SIZE, creature);
                    chit.addMouseListener(this);
                    legionPane.add(chit);
                    critterSourceList.add(new CritterSource(variant
                        .getCreatureByName(creature), CARETAKER));
                }
            }
        }
        gridPane.add(acquirablePane);
        // add 1 acquirable slot per decision up to slots available.
        int cntDecisions = 0;
        if (legion.decisions != null)
        {
            cntDecisions = legion.decisions.size();
        }
        for (int i = 0; i < slots && i < cntDecisions; i++)
        {
            Chit chit = new Chit(CRITTERSLOT);
            newAcquirablePane.add(chit);
        }
        gridPane.add(newAcquirablePane);

        // If Attacker, Add Summonable Panels.
        if (client.isMyTurn() && !summonableList.isEmpty())
        {
            // Current Summonable is next
            // Selected summonable on the right.
            summonablePane = new JPanel();
            newSummonablePane = new JPanel();
            summonablePane.setBorder(BorderFactory
                .createTitledBorder("Summonables"));
            newSummonablePane.setBorder(BorderFactory
                .createTitledBorder("Summoned"));
            int i = 0;
            for (CritterSource summonable : summonableList)
            {
                Chit chit = new Chit(summonable.getChit());
                chit.addMouseListener(this);
                summonablePane.add(chit);
                critterSourceList.add(summonable);
                // add 1 summonable slot per decision up to slots available.
                i++;
                if (i < slots)
                {
                    Chit slot = new Chit(CRITTERSLOT);
                    newSummonablePane.add(slot);
                }
            }
            gridPane.add(summonablePane);
            gridPane.add(newSummonablePane);
        }

        if (legion.canRecruit() && !recruits.isEmpty())
        {
            recruitPane = new JPanel();
            for (CreatureType creature : recruits)
            {
                Chit chit = new Chit(CREATURE_SIZE, creature.getName());
                chit.addMouseListener(this);
                legionPane.add(chit);
                critterSourceList.add(new CritterSource(creature, CARETAKER));
            }
            gridPane.add(recruitPane);
            newRecruitPane = new JPanel();
            if (!client.isMyTurn()) // defender
            {
                Chit slot = new Chit(CRITTERSLOT);
                newRecruitPane.add(slot);
            }
            gridPane.add(newRecruitPane);
        }
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        Object source = e.getSource();
        if (source instanceof Chit)
        {
            Chit chit = (Chit)source;
            Container container = chit.getParent();
            CritterSource cs = findCritterSource(chit);
            assert cs != null : "Battle Aftermath click chit without CritterSrce";
            if (container.equals(newAcquirablePane)
                || container.equals(newSummonablePane)
                || container.equals(newRecruitPane)
                || container.equals(newLegionPane))
            {
                // unselect the critter - Slots are not Clickable. 
                // Must be a Selected Chit (has CritterSource).
                cs.unselect();
                selectedList.remove(cs);
                container.remove(chit);
                container.add(new Chit(CRITTERSLOT));
            }
            else
            {
                // select the chit: All Selectables have a Crittersource
                // ignore if already selected.
                if (!cs.isSelected())
                {
                    // assert all children with a mouse listener are 
                    // selectable chits. 
                    Chit[] chits = (Chit[])container.getComponents();
                    boolean selectionDone = false;
                    for (int i = 0; i < chits.length && !selectionDone; i++)
                    {
                        Chit child = chits[i];
                        if (child.getId().equals(CRITTERSLOT.getId()))
                        {
                            selectedList.add(cs);
                            container.remove(child);
                            container.add(cs.select());
                            selectionDone = true; // Only swap out the first.
                        }
                    }
                }
            }
        }
    }

    private CritterSource findCritterSource(Chit chit)
    {
        for (int i = 0; i < critterSourceList.size(); i++)
        {
            CritterSource cs = critterSourceList.get(i);
            if (chit.equals(cs.getChit()) || chit.equals(cs.getSelectedChit()))
            {
                return cs;
            }
        }
        return null;
    }

    @Override
    public void windowClosing(WindowEvent e)
    {

        // TODO - is reset Necessary?
        //        reset();
        dispose();
    }

    public void actionPerformed(ActionEvent e)
    {
        if (e.getActionCommand().equals("Cancel"))
        {
            //TODO change to a Reset.
            reset();
        }
        else if (e.getActionCommand().equals("Done"))
        {
            if (selectedList.size() > 1)
            {
                cleanup();
            }
            dispose();
        }
    }

    void reset()
    {
        for (CritterSource cs : selectedList)
        {
            Container container = cs.getSelectedChit().getParent();
            selectedList.remove(cs);
            container.remove(cs.getSelectedChit());
            cs.unselect();
            container.add(new Chit(CRITTERSLOT));
            dispose();
        }
    }

    void cleanup()
    {

        for (CritterSource cs : selectedList)
        {
            Container container = cs.getSelectedChit().getParent();
            if (container.equals(newAcquirablePane))
            {
                //client.acquireAngelCallback(legion, cs.getCreature().getName());
            }
            if (container.equals(newSummonablePane))
            {
                //                client.doSummon(legion, cs.getLegion(), cs.getCreature()
                //                    .getName());
            }
            if (container.equals(newRecruitPane))
            {
                //                client.doRecruit(legion, cs.getCreature().getName(), client
                //                    .findRecruiterName(legion, cs.getCreature().getName(),
                //                        legion.getCurrentHex().getDescription()));
            }
        }
        dispose();
    }
}
