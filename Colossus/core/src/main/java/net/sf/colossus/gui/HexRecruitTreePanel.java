package net.sf.colossus.gui;


import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.JLabel;

import net.sf.colossus.common.Constants;
import net.sf.colossus.game.RecruitGraph;
import net.sf.colossus.server.VariantSupport;
import net.sf.colossus.variant.CreatureType;
import net.sf.colossus.variant.IVariant;
import net.sf.colossus.variant.MasterBoardTerrain;
import net.sf.colossus.variant.MasterHex;
import net.sf.colossus.variant.Variant;


/**
 * Arranges the recruit tree for one hex in a panel, which can be
 * displayed e.g. in Autoinspector or right-click popup.
 */
public class HexRecruitTreePanel extends Box
{
    private final Map<Chit, CreatureType> chitToCreatureMap = new HashMap<Chit, CreatureType>();

    private final List<ShowCreatureDetails> creatureWindows = new ArrayList<ShowCreatureDetails>();

    private final JFrame parentFrame;
    private final Variant variant;
    private final IVariant ivariant;
    private final ClientGUI gui;

    public HexRecruitTreePanel(int direction, MasterBoardTerrain terrain,
        MasterHex hex, JFrame parent, boolean clickable, Variant variant,
        ClientGUI clientGui)
    {
        super(direction);
        this.parentFrame = parent;
        this.variant = variant;
        this.gui = clientGui;
        this.ivariant = clientGui.getClient();

        setAlignmentY(0);
        setBorder(BorderFactory.createLineBorder(Color.black));
        setBackground(terrain.getColor());

        StringBuffer theLabel = new StringBuffer();
        Set<String> doneNames = new TreeSet<String>();
        String displayName = terrain.getDisplayName();
        theLabel.append(displayName);
        doneNames.add(displayName);
        Set<MasterBoardTerrain> aliases = terrain.getAliases();
        if (!aliases.isEmpty())
        {
            boolean prefixDone = false;
            Iterator<MasterBoardTerrain> it = aliases.iterator();
            while (it.hasNext())
            {
                MasterBoardTerrain alias = it.next();
                if (!doneNames.contains(alias.getDisplayName()))
                {
                    if (!prefixDone)
                    {
                        prefixDone = true;
                        theLabel.append("(also: ");
                    }
                    else
                    {
                        theLabel.append(", ");
                    }
                    theLabel.append(alias.getDisplayName());
                    doneNames.add(alias.getDisplayName());
                }
            }
            if (prefixDone)
            {
                theLabel.append(")");
            }
        }
        JLabel terrainLabel = new JLabel(theLabel.toString());
        terrainLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(terrainLabel);

        List<CreatureType> creatures = ivariant.getPossibleRecruits(terrain,
            hex);
        Iterator<CreatureType> it = creatures.iterator();
        boolean firstTime = true;
        int scale = 4 * Scale.get();
        CreatureType prevCreature = VariantSupport.getCurrentVariant()
            .getCreatureByName(Constants.titan);
        while (it.hasNext())
        {
            CreatureType creature = it.next();

            int numToRecruit;
            if (firstTime)
            {
                numToRecruit = 0;
                firstTime = false;
            }
            else
            {
                numToRecruit = ivariant.numberOfRecruiterNeeded(prevCreature,
                    creature, terrain, hex);
            }

            JLabel numToRecruitLabel = new JLabel("");
            if (numToRecruit > 0 && numToRecruit < RecruitGraph.BIGNUM)
            {
                numToRecruitLabel.setText(Integer.toString(numToRecruit));
                numToRecruitLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            }

            add(numToRecruitLabel);
            Chit chit = Chit.newCreatureChit(scale, creature);
            add(chit);
            chitToCreatureMap.put(chit, creature);

            // ShowAllRecruits windows uses this, Inspector right now not
            if (clickable)
            {
                MouseListener creListener = new MouseAdapter()
                {
                    @Override
                    public void mouseClicked(MouseEvent e)
                    {
                        if (e.getButton() == MouseEvent.BUTTON1)
                        {
                            showCreatureInfo(e);
                        }
                    }
                };

                chit.addMouseListener(creListener);
            }
            chit.repaint();

            prevCreature = creature;
        }
    }

    public void showCreatureInfo(MouseEvent e)
    {
        Object source = e.getSource();
        if (source instanceof Chit)
        {
            CreatureType type = chitToCreatureMap.get(source);
            ShowCreatureDetails creatureWindow = new ShowCreatureDetails(
                this.parentFrame, type, null, null, this.variant, gui);
            creatureWindows.add(creatureWindow);
        }
        else
        {
            // showCreaturrInfo called for something which is not a chit?
        }
    }

    // ShowAllRecruits windows uses this, Inspector right now not
    public void closeCreatureWindows()
    {
        for (ShowCreatureDetails window : creatureWindows)
        {
            window.dispose();
        }
    }
}
