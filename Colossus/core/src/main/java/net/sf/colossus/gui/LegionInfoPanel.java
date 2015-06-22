package net.sf.colossus.gui;


import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sf.colossus.client.LegionClientSide;
import net.sf.colossus.common.Options;
import net.sf.colossus.util.HTMLColor;


/**
 * Creates a JPanel displaying one legion,
 * used by AutoInspector and ShowLegion (right-click on legion)
 */
public final class LegionInfoPanel extends JPanel
{
    private String valueText = "";

    public LegionInfoPanel(LegionClientSide legion, int scale, int margin,
        int padding, boolean usePlayerColor, int viewMode, boolean isMyLegion,
        boolean dubiousAsBlanks, boolean showLegionValue, boolean showMarker)
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
                viewAll(legion, usePlayerColor, scale, margin, padding, false,
                    hideAll, showLegionValue, showMarker);
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
            Marker marker = new Marker(legion, scale, legion.getLongMarkerId());
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
                chit = new Chit(scale, "QuestionMarkMask", false, true, false,
                    null);
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
            i++;
        }

        if (showLegionValue && allCertain)
        {
            JLabel sizeLabel = new JLabel(String.valueOf(legion
                .getPointValue()));
            sizeLabel.setForeground(Color.WHITE);
            add(sizeLabel);
            sizeLabel.setLocation(i * (effectiveChitSize + padding) + margin,
                margin);
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
            (legion.getImageNames().size() + (showMarker ? 1 : 0) + (showLegionValue
                && allCertain ? 1 : 0))
                * (effectiveChitSize + padding) - padding + 2 * margin,
            effectiveChitSize + 2 * margin);
        setMinimumSize(getSize());
        setPreferredSize(getSize());
        setMaximumSize(getSize());
    }
}
