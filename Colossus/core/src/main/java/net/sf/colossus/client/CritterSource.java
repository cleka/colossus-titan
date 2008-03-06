package net.sf.colossus.client;


import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;

import net.sf.colossus.game.Legion;
import net.sf.colossus.variant.CreatureType;


public class CritterSource
{
    // should be derived from Scale
    private static final int CREATURE_SIZE = 30;
    private static final int MARKER_SIZE = 15;

    private final CreatureType creature;
    private final Legion legion;
    private Chit chit;
    private Chit selectedChit;

    public CritterSource(CreatureType creature, Legion legion)
    {
        this(creature, legion, null);
    }

    public CritterSource(CreatureType creature, Legion legion, Chit chit)
    {
        this.creature = creature;
        this.legion = legion;
        this.chit = chit;
        if (chit == null)
        {
            makechit();
        }
    }

    public CreatureType getCreature()
    {
        return creature;
    }

    public Legion getLegion()
    {
        return legion;
    }

    public Chit select()
    {
        selectedChit = new Chit(chit);
        return selectedChit;
    }

    public void unselect()
    {
        selectedChit = null;
    }

    public boolean isSelected()
    {
        return selectedChit != null;
    }

    public Chit getSelectedChit()
    {
        return selectedChit;
    }

    public void setSelectedChit(Chit selectedChit)
    {
        this.selectedChit = selectedChit;
    }

    public Chit getChit()
    {
        return chit;
    }

    private void makechit()
    {
        chit = new Chit(CREATURE_SIZE, creature.getName());
        if (legion == null)
        {
            return;
        }
        if (legion instanceof LegionClientSide)

        {
            BufferedImage bi = new BufferedImage(CREATURE_SIZE, CREATURE_SIZE,
                BufferedImage.TYPE_INT_ARGB);
            Graphics2D biContext = bi.createGraphics();
            biContext.drawImage(chit.getBufferedImage(), 0, 0, CREATURE_SIZE,
                CREATURE_SIZE, null);
            //ResourceLoader.waitOnImage(bi);
            Image markerImage = ((LegionClientSide)legion).getMarker()
                .getBufferedImage();
            biContext.drawImage(markerImage, 0, 0, MARKER_SIZE, MARKER_SIZE,
                null);
            chit.setBufferedImage(bi);
        }
    }
} // class CritterSource