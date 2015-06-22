package net.sf.colossus.gui;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.sf.colossus.client.LegionClientSide;
import net.sf.colossus.common.Options;
import net.sf.colossus.game.Legion;
import net.sf.colossus.guiutil.KDialog;
import net.sf.colossus.util.HTMLColor;
import net.sf.colossus.variant.CreatureType;


/**
 * Class EditLegion allows to edit the contents of a Legion
 * Based on ShowLegion
 *
 * @author Clemens Katzer
 */

final class EditLegion extends KDialog
{
    private final ClientGUI gui;

    private final LegionClientSide legion;

    private final JLabel infoLabel;

    EditLegion(ClientGUI gui, JFrame parentFrame, LegionClientSide legion,
        Point point, JScrollPane pane, int scale, int viewMode,
        boolean isMyLegion, boolean dubiousAsBlanks, boolean showMarker)
    {
        super(parentFrame, "EDIT: " + legion.getMarkerId(), false);

        this.gui = gui;
        this.legion = legion;

        this.infoLabel = new JLabel("--some info here--");

        if (legion.getImageNames().isEmpty())
        {
            dispose();
            return;
        }

        setBackground(Color.lightGray);
        addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                dispose();
            }
        });

        getContentPane().setLayout(new BorderLayout());

        setNormalText();
        getContentPane().add(infoLabel, BorderLayout.NORTH);

        LegionEditPanel liPanel = new LegionEditPanel(legion, scale, 5, 2,
            false, viewMode, isMyLegion, dubiousAsBlanks, false, showMarker);
        getContentPane().add(liPanel, BorderLayout.CENTER);

        String valueText = liPanel.getValueText();
        String ownerText = isMyLegion ? "" : " ["
            + legion.getPlayer().getName() + "]";

        setTitle("EDIT: " + legion.getMarkerId() + valueText + ownerText);
        liPanel = null;

        JButton doneButton = new JButton("Done");
        doneButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                beDone();
            }
        });
        getContentPane().add(doneButton, BorderLayout.SOUTH);

        placeRelative(parentFrame, point, pane);

        pack();
        addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                dispose();
            }
        });
        setVisible(true);
        repaint();
    }

    public void setNormalText()
    {
        infoLabel.setText("Click on a creature to remove it");
    }

    public void setRelocateText()
    {
        infoLabel.setText("Click on destination hex!");
    }

    public void selectedCreature(CreatureType type)
    {
        gui.getBoard().setEditOngoing(EditLegion.this);
        gui.getClient().editAddCreature(legion.getMarkerId(), type.getName());
        dispose();
    }

    public void addCreature()
    {
        new CreatureCollectionView(gui.getBoard().getFrame(), gui, this);
    }

    public Legion getLegion()
    {
        return legion;
    }

    public void beDone()
    {
        dispose();
    }

    public final class LegionEditPanel extends JPanel
    {
        private String valueText = "";

        public LegionEditPanel(LegionClientSide legion, int scale, int margin,
            int padding, boolean usePlayerColor, int viewMode,
            boolean isMyLegion, boolean dubiousAsBlanks,
            boolean showLegionValue, boolean showMarker)
        {
            boolean contentCertain = false;
            boolean hideAll = false;

            if (viewMode == Options.viewableAllNum)
            {
                contentCertain = true;
                viewAll(legion, usePlayerColor, scale, margin, padding,
                    dubiousAsBlanks, hideAll, showLegionValue, showMarker);
            }
            else if (viewMode == Options.viewableOwnNum)
            {
                if (isMyLegion)
                {
                    contentCertain = true;
                    viewAll(legion, usePlayerColor, scale, margin, padding,
                        dubiousAsBlanks, hideAll, showLegionValue, showMarker);
                }
                else
                {
                    hideAll = true;
                    viewAll(legion, usePlayerColor, scale, margin, padding,
                        false, hideAll, showLegionValue, showMarker);
                }
            }
            else if (viewMode == Options.viewableEverNum)
            {
                // for this mode, in Game/Server broadcasting of revealed info
                // is limited to those that are entitled to know;
                // thus we can use the splitPrediction to decide what is
                // "has ever been shown or can be concluded".
                viewAll(legion, usePlayerColor, scale, margin, padding,
                    dubiousAsBlanks, hideAll, showLegionValue, showMarker);
            }
            else
            {
                viewOtherText("not implemented...");
            }

            if (contentCertain)
            {
                int value = legion.getPointValue();
                valueText = " (" + value + " points)";
            }
            else
            {
                int value;
                int numUC;
                if (viewMode == Options.viewableOwnNum)
                {
                    value = 0;
                    numUC = legion.getHeight();
                }
                else
                {
                    value = legion.getCertainPointValue();
                    numUC = legion.numUncertainCreatures();
                }

                String ucString = "";
                if (numUC > 0)
                {
                    StringBuilder uncertainIndicator = new StringBuilder(8);
                    uncertainIndicator.append("+");
                    while (numUC > 0)
                    {
                        uncertainIndicator.append("?");
                        numUC--;
                    }
                    // substring so that StringBuilder gets released.
                    ucString = uncertainIndicator.substring(0);
                }

                valueText = " (" + value + ucString + " points)";
            }

        }

        public String getValueText()
        {
            return valueText;
        }

        private void viewOtherText(String text)
        {
            add(new JLabel(text));
        }

        private void viewAll(LegionClientSide legion, boolean usePlayerColor,
            int scale, int margin, int padding, boolean dubiousAsBlanks,
            boolean hideAll, boolean showLegionValue, boolean showMarker)
        {
            setLayout(null);

            if (usePlayerColor)
            {
                Color playerColor = HTMLColor.stringToColor(legion.getPlayer()
                    .getColor() + "Colossus");
                setBackground(playerColor);
            }

            int i = 0;
            int effectiveChitSize = 0; // Chit treats scale as a hint,
            // actual size might differ

            if (showMarker)
            {
                Marker marker = new Marker(legion, scale,
                    legion.getLongMarkerId());
                if (effectiveChitSize == 0)
                {
                    // they should be all the same size
                    effectiveChitSize = marker.getWidth();
                }
                add(marker);
                marker.setLocation(i * (effectiveChitSize + padding) + margin,
                    margin);
                i++;
            }

            List<String> imageNames = legion.getImageNames();
            List<Boolean> certain = legion.getCertainties();
            boolean allCertain = !hideAll;

            // if uncertain shall be shown ones only as blanks, then
            // also sort the blanks all to the end:
            // (just unnecessary work if hideAll is set.)
            if (dubiousAsBlanks && !hideAll)
            {
                Iterator<String> iIt = imageNames.iterator();
                Iterator<Boolean> cIt = certain.iterator();
                List<String> cNames = new ArrayList<String>();
                List<Boolean> cCertain = new ArrayList<Boolean>();
                List<String> ucNames = new ArrayList<String>();
                List<Boolean> ucCertain = new ArrayList<Boolean>();
                while (iIt.hasNext())
                {
                    String imageName = iIt.next();
                    Boolean sure = cIt.next();
                    if (sure.booleanValue())
                    {
                        cNames.add(imageName);
                        cCertain.add(sure);
                    }
                    else
                    {
                        ucNames.add(imageName);
                        ucCertain.add(sure);
                    }
                }

                imageNames.clear();
                imageNames.addAll(cNames);
                imageNames.addAll(ucNames);
                cNames.clear();
                ucNames.clear();

                certain.clear();
                certain.addAll(cCertain);
                certain.addAll(ucCertain);
                cCertain.clear();
                ucCertain.clear();
            }

            Iterator<String> it = imageNames.iterator();
            Iterator<Boolean> it2 = certain.iterator();

            // now add the chits one by one to the panel:
            while (it.hasNext())
            {
                String imageName = it.next();
                Chit chit;
                boolean sure = it2.next().booleanValue();
                if (!sure)
                {
                    allCertain = false;
                }
                if (hideAll)
                {
                    chit = new Chit(scale, "QuestionMarkMask", false, true,
                        false, null);
                }
                else
                {
                    chit = new Chit(scale, imageName, false, !sure,
                        dubiousAsBlanks, null);
                }
                if (effectiveChitSize == 0)
                {
                    // they should be all the same size
                    effectiveChitSize = chit.getWidth();
                }
                add(chit);
                chit.setLocation(i * (effectiveChitSize + padding) + margin,
                    margin);

                chit.addMouseListener(new MouseAdapter()
                {
                    @Override
                    public void mousePressed(MouseEvent e)
                    {
                        Chit chit = (Chit)e.getComponent();
                        String id = chit.getId();
                        gui.getBoard().setEditOngoing(EditLegion.this);
                        gui.getClient().editRemoveCreature(
                            EditLegion.this.legion.getMarkerId(), id);
                        dispose();
                    }
                });

                i++;
            }

            JPanel buttonPanel = new JPanel();
            JButton addButton = new JButton("Add");
            addButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    addCreature();
                }
            });
            buttonPanel.add(addButton);

            final JButton relocateButton = new JButton("Move");
            relocateButton.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    gui.getBoard().setRelocateOngoing(EditLegion.this);
                    setRelocateText();
                    relocateButton.setEnabled(false);
                }
            });
            buttonPanel.add(relocateButton);

            buttonPanel.setLocation(
                i * (effectiveChitSize + padding) + margin, margin);
            buttonPanel.setSize(new Dimension(effectiveChitSize,
                effectiveChitSize));
            i++;
            add(buttonPanel);

            if (showLegionValue && allCertain)
            {
                JLabel sizeLabel = new JLabel(String.valueOf(legion
                    .getPointValue()));
                sizeLabel.setForeground(Color.WHITE);
                add(sizeLabel);
                sizeLabel.setLocation(i * (effectiveChitSize + padding)
                    + margin, margin);
                sizeLabel.setSize(new Dimension(effectiveChitSize,
                    effectiveChitSize));
                i++;
            }

            // This fixes a repaint bug under Linux.
            if (imageNames.size() == 1)
            {
                add(Box.createRigidArea(new Dimension(scale, scale)));
            }

            setSize(
                (legion.getImageNames().size() + (showMarker ? 1 : 0) + 1 + (showLegionValue
                    && allCertain ? 1 : 0))
                    * (effectiveChitSize + padding) - padding + 2 * margin,
                effectiveChitSize + 2 * margin);
            setMinimumSize(getSize());
            setPreferredSize(getSize());
            setMaximumSize(getSize());
        }

    }

}
