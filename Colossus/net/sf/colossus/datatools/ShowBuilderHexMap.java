package net.sf.colossus.datatools;

import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

import net.sf.colossus.client.BattleHex;
import net.sf.colossus.client.GUIBattleHex;

/**
 * Class ShowBuilderHexMap displays a battle map.
 * @version $Id$
 * @author David Ripton
 * @author Romain Dolbeau
 */

final class ShowBuilderHexMap extends BuilderHexMap implements WindowListener,
    MouseListener
{
    private JDialog dialog;
    private JPopupMenu popupMenuTerrain;
    private JPopupMenu popupMenuBorder;
    private Point lastPoint;
    private Component lastComponent;
    private int lastSide;

    class rndFileFilter extends javax.swing.filechooser.FileFilter 
    {
        public boolean accept(java.io.File f) 
        {
            if (f.isDirectory()) 
            {
                return(true);
            }
            if (f.getName().endsWith(".rnd")) 
            {
                return(true);
            }
            return(false);
        }
        public String getDescription() 
        {
            return("Colossus RaNDom generator file");
        }
    }

    private void doLoadRandom(BattleHex[][] h)
    {
        javax.swing.JFileChooser rndChooser = new JFileChooser(".");
        rndChooser.setFileFilter(new rndFileFilter());
        rndChooser.setDialogTitle(
                   "Choose the RaNDom file (or cancel for nothing)");
        int returnVal = rndChooser.showOpenDialog(rndChooser);
        if (returnVal == javax.swing.JFileChooser.APPROVE_OPTION)
        {
            java.io.File rndFile = rndChooser.getSelectedFile();
            String tempRndName = rndFile.getName();
            String tempRndDirectory = rndFile.getParentFile().getAbsolutePath();
            java.util.List directories = new java.util.ArrayList();
            directories.add(tempRndDirectory);
            java.io.InputStream inputFile =
                net.sf.colossus.util.ResourceLoader.getInputStream(tempRndName, directories);
            if (inputFile != null)
            {
                net.sf.colossus.parser.BattlelandRandomizerLoader parser = 
                    new net.sf.colossus.parser.BattlelandRandomizerLoader(inputFile);
                try {
                    while (parser.oneArea(h) >= 0) {}
                } catch (Exception e) { System.err.println(e); }
            }
        }
    }

    class TerrainAction extends AbstractAction
    {
        char c;
        TerrainAction(String t, char c)
        {
            super(t);
            this.c = c;
        }
        public void actionPerformed(ActionEvent e) {
            GUIBattleHex h = getHexContainingPoint(lastPoint);
            h.setTerrain(c);
            h.repaint();
        }
    }

    class ElevationAction extends AbstractAction
    {
        int el;
        ElevationAction(String t, int el)
        {
            super(t);
            this.el = el;
        }
        public void actionPerformed(ActionEvent e) {
            GUIBattleHex h = getHexContainingPoint(lastPoint);
            h.setElevation(el);
            h.repaint();
        }
    }

    class HexsideAction extends AbstractAction        
    {
        char c;
        HexsideAction(String t, char c)
        {
            super(t);
            this.c = c;
        }
        public void actionPerformed(ActionEvent e) {
            GUIBattleHex h = getHexContainingPoint(lastPoint);
            h.setHexside(lastSide, c);
            h.repaint();
            ((GUIBattleHex)h.getNeighbor(lastSide)).repaint();
        }
    }

    private AbstractAction saveBattlelandAction;
    private AbstractAction saveBattlelandAsAction;
    private AbstractAction quitAction;
    private AbstractAction eraseAction;
    private AbstractAction randomizeAction;

    JMenuBar menuBar;

    ShowBuilderHexMap(String f)
    {
        super(f);

        menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        menuBar.add(fileMenu);
        JMenuItem mi;

        saveBattlelandAction = new AbstractAction("Save Battleland") {
                public void actionPerformed(ActionEvent e) {
                    System.out.print(dumpAsString());
                    if (filename != null)
                    {
                        
                    }
                }
            };

        saveBattlelandAsAction = new AbstractAction("Save Battleland As...") {
                public void actionPerformed(ActionEvent e) {
                    System.out.print(dumpAsString());
                }
            };

        quitAction = new AbstractAction("Quit") {
                public void actionPerformed(ActionEvent e) {
                    System.exit(0);
                }
            };

        eraseAction = new AbstractAction("Erase Map") {
                public void actionPerformed(ActionEvent e) {
                    eraseMap();
                    repaint();
                }
            };

        randomizeAction = new AbstractAction("Randomize Map (from file)") {
                public void actionPerformed(ActionEvent e) {
                    doLoadRandom(getBattleHexArray());
                    repaint();
                }
            };

        mi = fileMenu.add(saveBattlelandAction);
        mi.setMnemonic(KeyEvent.VK_S);
        mi = fileMenu.add(saveBattlelandAsAction);
        mi.setMnemonic(KeyEvent.VK_A);
        mi = fileMenu.add(eraseAction);
        mi = fileMenu.add(randomizeAction);
        mi = fileMenu.add(quitAction);
        mi.setMnemonic(KeyEvent.VK_Q);

        dialog = new JDialog();

        Container contentPane = dialog.getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(menuBar, BorderLayout.NORTH);

        addMouseListener(this);
        dialog.addWindowListener(this);

        contentPane.add(this, BorderLayout.CENTER);
        dialog.pack();
        dialog.setVisible(true);

        popupMenuTerrain = new JPopupMenu("Choose Terrain");
        contentPane.add(popupMenuTerrain);
        char[] terrains = BattleHex.getTerrains();
        GUIBattleHex tempH = new GUIBattleHex(0,0,1,this,0,0);

        for (int i = 0 ; i < terrains.length ; i++)
        {
            tempH.setTerrain(terrains[i]);
            mi = popupMenuTerrain.add(new TerrainAction(tempH.getTerrainName(),
                                                        terrains[i]));
        }
        popupMenuTerrain.addSeparator();
        for (int i = 0 ; i < 3 ; i++)
        {
            mi = popupMenuTerrain.add(new ElevationAction(
                                      "Set Elevation to: " + i,
                                      i));
        }

        popupMenuBorder = new JPopupMenu("Choose Border");
        contentPane.add(popupMenuBorder);
        char[] hexsides = BattleHex.getHexsides();

        for (int i = 0 ; i < hexsides.length ; i++)
        {
            tempH.setHexside(0,hexsides[i]);
            mi = popupMenuBorder.add(new HexsideAction(tempH.getHexsideName(0),
                                                       hexsides[i]));
        }
        
        lastPoint = new Point(0,0);
        lastComponent = contentPane;
        lastSide = 0;
    }

    public void mousePressed(MouseEvent e)
    {
        lastPoint = e.getPoint();
        lastComponent = e.getComponent();
        GUIBattleHex h = getHexContainingPoint(lastPoint);
        if (h != null)
        {
            Point c = h.findCenter();
            if (c.y >= lastPoint.y)
            { // uppper half
                if (lastPoint.x >=
                    ((c.x) + (h.getBounds().x + h.getBounds().width))/2)
                    lastSide = 1;
                else if (lastPoint.x <= ((c.x) + (h.getBounds().x))/2)
                    lastSide = 5;
                else
                    lastSide = 0;
            }
            else
            { // lower half
                if (lastPoint.x >=
                    ((c.x) + (h.getBounds().x + h.getBounds().width))/2)
                    lastSide = 2;
                else if (lastPoint.x <= ((c.x) + (h.getBounds().x))/2)
                    lastSide = 4;
                else
                    lastSide = 3;
            }
            if (h.innerContains(lastPoint) ||
                (h.getNeighbor(lastSide) == null))
            { // change content
                popupMenuTerrain.show(e.getComponent(),
                                      lastPoint.x,
                                      lastPoint.y);
            }
            else
            { // change border
                popupMenuBorder.show(e.getComponent(),
                                     lastPoint.x,
                                     lastPoint.y); 
            }
        }
    }

    public void windowClosing(WindowEvent e)
    {
        dialog.dispose();
    }
}
