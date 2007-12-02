/**
 * 
 */
package net.sf.colossus.client;


import java.awt.Color;
import java.awt.Dimension;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.JPanel;
import javax.swing.JLabel;

import net.sf.colossus.util.HTMLColor;
import net.sf.colossus.util.Options;


public final class LegionInfoPanel extends JPanel
{
    private String valueText = "";

    public LegionInfoPanel(LegionInfo legion, int scale, int margin,
        int padding, boolean usePlayerColor, int viewMode,
        String playerName, boolean dubiousAsBlanks)
    {
        boolean contentCertain = false;
        boolean hideAll = false;

        if ( viewMode == Options.viewableAllNum )
        {
            contentCertain = true;
            viewAll(legion, usePlayerColor, scale, margin, padding,
                dubiousAsBlanks, hideAll);
        }
        else if ( viewMode == Options.viewableOwnNum )
        {
            String legionOwner = legion.getPlayerName();
            if ( playerName.equals(legionOwner) )
            {
                contentCertain = true;
                viewAll(legion, usePlayerColor, scale, margin, padding,
                    dubiousAsBlanks, hideAll);
            }
            else
            {
                hideAll = true;
                viewAll(legion, usePlayerColor, scale, margin, padding,
                    false, hideAll);
            }
        }
        else if ( viewMode == Options.viewableEverNum )
        {
            // for this mode, in Game/Server broadcasting of revealed info
            // is limited to those that are entitled to know;
            // thus we can use the splitPrediction to decide what is
            // "has ever been shown or can be concluded".
            viewAll(legion, usePlayerColor, scale, margin, padding,
                dubiousAsBlanks, hideAll);
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
                StringBuffer uncertainIndicator = new StringBuffer(8);
                uncertainIndicator.append("+");
                while (numUC > 0)
                {
                    uncertainIndicator.append("?");
                    numUC--;
                }
                // substring so that StringBuffer gets released.
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

    private void viewAll(LegionInfo legion, boolean usePlayerColor, int scale,
        int margin, int padding, boolean dubiousAsBlanks, boolean hideAll)
    {
        setLayout(null);

        if (usePlayerColor)
        {
            Color playerColor =
                HTMLColor.stringToColor(legion.getPlayerInfo().getColor() +
                "Colossus");
            setBackground(playerColor);
        }

        int i = 0;
        int effectiveChitSize = 0; // Chit treats scale as a hint, 
        // actual size might differ

        // We could add the marker, if we want:
        boolean showMarker = false;
        if (showMarker)
        {
            Chit marker = new Chit(scale, legion.getMarkerId(),
                false, true, false);
            if (effectiveChitSize == 0)
            {
                effectiveChitSize = marker.getWidth(); // they should be all the same size
            }
            add(marker);
            marker.setLocation(i * (effectiveChitSize + padding) + margin,
                margin);
            i++;
        }

        List imageNames = legion.getImageNames();
        List certain = legion.getCertainties();

        // if uncertain shall be shown ones only as blanks, then 
        // also sort the blanks all to the end:
        // (just unnecessary work if hideAll is set.)
        if (dubiousAsBlanks && !hideAll)
        {
            Iterator iIt = imageNames.iterator();
            Iterator cIt = certain.iterator();
            List cNames = new ArrayList();
            List cCertain = new ArrayList();
            List ucNames = new ArrayList();
            List ucCertain = new ArrayList();
            while (iIt.hasNext())
            {
                String imageName = (String)iIt.next();
                Boolean sure = (Boolean)cIt.next();
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

        Iterator it = imageNames.iterator();
        Iterator it2 = certain.iterator();

        // now add the chits one by one to the panel:
        while (it.hasNext())
        {
            String imageName = (String)it.next();
            Chit chit;
            boolean sure = ((Boolean)it2.next()).booleanValue();
            if (hideAll)
            {
                chit = new Chit(scale, "QuestionMarkMask", false, true, false);
            }
            else
            {
                chit = new Chit(scale, imageName, false, !sure, dubiousAsBlanks);
            }
            if (effectiveChitSize == 0)
            {
                // they should be all the same size
                effectiveChitSize = chit.getWidth();
            }
            add(chit);
            chit.setLocation(i * (effectiveChitSize + padding) + margin, margin);
            i++;
        }

        // This fixes a repaint bug under Linux.
        if (imageNames.size() == 1)
        {
            add(Box.createRigidArea(new Dimension(scale, scale)));
        }

        setSize((legion.getImageNames().size()+(showMarker?1:0)) *
            (effectiveChitSize + padding) -
                padding + 2 * margin,
                effectiveChitSize + 2 * margin);
        setMinimumSize(getSize());
        setPreferredSize(getSize());
        setMaximumSize(getSize());
    }
}
