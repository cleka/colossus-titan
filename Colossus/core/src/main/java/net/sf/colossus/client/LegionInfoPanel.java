/**
 * 
 */
package net.sf.colossus.client;

import java.awt.Color;
import java.awt.Dimension;
import java.util.Iterator;
import java.util.List;

import javax.swing.Box;
import javax.swing.JPanel;

import net.sf.colossus.util.HTMLColor;

public final class LegionInfoPanel extends JPanel {
	public LegionInfoPanel(LegionInfo legion, int scale, int margin, int padding, boolean usePlayerColor) {
		setLayout(null);
        
        if(usePlayerColor) {
        	Color playerColor =
        		HTMLColor.stringToColor(
        				legion.getPlayerInfo().getColor() + 
        		"Colossus");
        	setBackground(playerColor);
        }

        List imageNames = legion.getImageNames();
        List certain = legion.getCertainties();
        Iterator it = imageNames.iterator();
        Iterator it2 = certain.iterator();
        int i = 0;
        int effectiveChitSize = 0; // Chit treats scale as a hint, actual size might differ
        while (it.hasNext())
        {
            String imageName = (String)it.next();
            boolean sure = ((Boolean)it2.next()).booleanValue();
            Chit chit = new Chit(scale, imageName, this, false, !sure);
            if (effectiveChitSize == 0) {
				effectiveChitSize = chit.getWidth(); // they should be all the same size
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
        
        setSize(legion.getImageNames().size() * (effectiveChitSize + padding) - padding +
                2 * margin,
                effectiveChitSize + 2 * margin);
        setMinimumSize(getSize());
        setPreferredSize(getSize());
        setMaximumSize(getSize());
	}
}