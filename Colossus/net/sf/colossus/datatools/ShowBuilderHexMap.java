package net.sf.colossus.datatools;

import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;


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

    private AbstractAction plain;
    private AbstractAction tower;
    private AbstractAction bramble;
    private AbstractAction sand;
    private AbstractAction tree;
    private AbstractAction bog;
    private AbstractAction volcano;
    private AbstractAction drift;
    private AbstractAction lake;

    private AbstractAction dune;
    private AbstractAction cliff;
    private AbstractAction slope;
    private AbstractAction wall;
    private AbstractAction nothing;

    private AbstractAction alt0;
    private AbstractAction alt1;
    private AbstractAction alt2;

    private AbstractAction saveBattlelandAction;
    private AbstractAction saveBattlelandAsAction;
    private AbstractAction quitAction;

    JMenuBar menuBar;

    ShowBuilderHexMap(String masterHexLabel,
                  char t,
                  String f)
    {
        super(masterHexLabel, t, f);

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

        mi = fileMenu.add(saveBattlelandAction);
        mi.setMnemonic(KeyEvent.VK_S);
        mi = fileMenu.add(saveBattlelandAsAction);
        mi.setMnemonic(KeyEvent.VK_A);
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

        alt0 = new AbstractAction("Set Elevation to: 0") {
                public void actionPerformed(ActionEvent e) {
                    GUIBuilderHex h = getHexContainingPoint(lastPoint);
                    h.setElevation(0);
                    h.repaint(lastComponent);
                }
            };

        alt1 = new AbstractAction("Set Elevation to: 1") {
                public void actionPerformed(ActionEvent e) {
                    GUIBuilderHex h = getHexContainingPoint(lastPoint);
                    h.setElevation(1);
                    h.repaint(lastComponent);
                }
            };

        alt2 = new AbstractAction("Set Elevation to: 2") {
                public void actionPerformed(ActionEvent e) {
                    GUIBuilderHex h = getHexContainingPoint(lastPoint);
                    h.setElevation(2);
                    h.repaint(lastComponent);
                }
            };

        dune = new AbstractAction("dune") {
                public void actionPerformed(ActionEvent e) {
                    GUIBuilderHex h = getHexContainingPoint(lastPoint);
                    h.setHexside(lastSide, 'd');
                    h.repaint(lastComponent);
                    ((GUIBuilderHex)h.getNeighbor(lastSide)).repaint(lastComponent);
                }
            };

        cliff = new AbstractAction("cliff") {
                public void actionPerformed(ActionEvent e) {
                    GUIBuilderHex h = getHexContainingPoint(lastPoint);
                    h.setHexside(lastSide, 'c');
                    h.repaint(lastComponent);
                    ((GUIBuilderHex)h.getNeighbor(lastSide)).repaint(lastComponent);
                }
            };

        slope = new AbstractAction("slope") {
                public void actionPerformed(ActionEvent e) {
                    GUIBuilderHex h = getHexContainingPoint(lastPoint);
                    h.setHexside(lastSide, 's');
                    h.repaint(lastComponent);
                    ((GUIBuilderHex)h.getNeighbor(lastSide)).repaint(lastComponent);
                }
            };

        wall = new AbstractAction("wall") {
                public void actionPerformed(ActionEvent e) {
                    GUIBuilderHex h = getHexContainingPoint(lastPoint);
                    h.setHexside(lastSide, 'w');
                    h.repaint(lastComponent);
                    ((GUIBuilderHex)h.getNeighbor(lastSide)).repaint(lastComponent);
                }
            };

        nothing = new AbstractAction("nothing") {
                public void actionPerformed(ActionEvent e) {
                    GUIBuilderHex h = getHexContainingPoint(lastPoint);
                    h.setHexside(lastSide, ' ');
                    h.repaint(lastComponent);
                    ((GUIBuilderHex)h.getNeighbor(lastSide)).repaint(lastComponent);
                }
            };

        plain = new AbstractAction("plain") {
                public void actionPerformed(ActionEvent e) {
                    GUIBuilderHex h = getHexContainingPoint(lastPoint);
                    h.setTerrain('p');
                    h.repaint(lastComponent);
                }
            };

        tower = new AbstractAction("tower") {
                public void actionPerformed(ActionEvent e) {
                    GUIBuilderHex h = getHexContainingPoint(lastPoint);
                    h.setTerrain('w');
                    h.repaint(lastComponent);
                }
            };

        bramble = new AbstractAction("bramble") {
                public void actionPerformed(ActionEvent e) {
                    GUIBuilderHex h = getHexContainingPoint(lastPoint);
                    h.setTerrain('r');
                    h.repaint(lastComponent);
                }
            };

        sand = new AbstractAction("sand") {
                public void actionPerformed(ActionEvent e) {
                    GUIBuilderHex h = getHexContainingPoint(lastPoint);
                    h.setTerrain('s');
                    h.repaint(lastComponent);
                }
            };

        tree = new AbstractAction("tree") {
                public void actionPerformed(ActionEvent e) {
                    GUIBuilderHex h = getHexContainingPoint(lastPoint);
                    h.setTerrain('t');
                    h.repaint(lastComponent);
                }
            };

        bog = new AbstractAction("bog") {
                public void actionPerformed(ActionEvent e) {
                    GUIBuilderHex h = getHexContainingPoint(lastPoint);
                    h.setTerrain('o');
                    h.repaint(lastComponent);
                }
            };

        volcano = new AbstractAction("volcano") {
                public void actionPerformed(ActionEvent e) {
                    GUIBuilderHex h = getHexContainingPoint(lastPoint);
                    h.setTerrain('v');
                    h.repaint(lastComponent);
                }
            };

        drift = new AbstractAction("drift") {
                public void actionPerformed(ActionEvent e) {
                    GUIBuilderHex h = getHexContainingPoint(lastPoint);
                    h.setTerrain('d');
                    h.repaint(lastComponent);
                }
            };

        lake = new AbstractAction("lake") {
                public void actionPerformed(ActionEvent e) {
                    GUIBuilderHex h = getHexContainingPoint(lastPoint);
                    h.setTerrain('l');
                    h.repaint(lastComponent);
                }
            };

        popupMenuTerrain = new JPopupMenu("Choose Terrain");
        contentPane.add(popupMenuTerrain);
        mi = popupMenuTerrain.add(plain);
        mi = popupMenuTerrain.add(tower);
        mi = popupMenuTerrain.add(bramble);
        mi = popupMenuTerrain.add(sand);
        mi = popupMenuTerrain.add(tree);
        mi = popupMenuTerrain.add(bog);
        mi = popupMenuTerrain.add(volcano);
        mi = popupMenuTerrain.add(drift);
        mi = popupMenuTerrain.add(lake);
        popupMenuTerrain.addSeparator();
        mi = popupMenuTerrain.add(alt0);
        mi = popupMenuTerrain.add(alt1);
        mi = popupMenuTerrain.add(alt2);

        popupMenuBorder = new JPopupMenu("Choose Border");
        contentPane.add(popupMenuBorder);
        mi = popupMenuBorder.add(dune);
        mi = popupMenuBorder.add(cliff);
        mi = popupMenuBorder.add(slope);
        mi = popupMenuBorder.add(wall);
        mi = popupMenuBorder.add(nothing);

        lastPoint = new Point(0,0);
        lastComponent = contentPane;
        lastSide = 0;
    }

    public void mousePressed(MouseEvent e)
    {
        lastPoint = e.getPoint();
        lastComponent = e.getComponent();
        GUIBuilderHex h = getHexContainingPoint(lastPoint);
        if (h != null)
        {
            if (h.innerContains(lastPoint))
            { // change content
                popupMenuTerrain.show(e.getComponent(), lastPoint.x, lastPoint.y);
            }
            else
            { // change border
                Point c = h.findCenter();
                if (c.y >= lastPoint.y)
                { // uppper half
                    if (lastPoint.x >=((c.x) + (h.getBounds().x + h.getBounds().width))/2)
                        lastSide = 1;
                    else if (lastPoint.x <= ((c.x) + (h.getBounds().x))/2)
                        lastSide = 5;
                    else
                        lastSide = 0;
                }
                else
                { // lower half
                    if (lastPoint.x >=((c.x) + (h.getBounds().x + h.getBounds().width))/2)
                        lastSide = 2;
                    else if (lastPoint.x <= ((c.x) + (h.getBounds().x))/2)
                        lastSide = 4;
                    else
                        lastSide = 3;
                }

                popupMenuBorder.show(e.getComponent(), lastPoint.x, lastPoint.y); 
            }
        }
    }

    public void windowClosing(WindowEvent e)
    {
        dialog.dispose();
    }
}
