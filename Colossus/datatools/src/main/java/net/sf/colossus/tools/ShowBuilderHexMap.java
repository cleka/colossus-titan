package net.sf.colossus.tools;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.print.Book;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.swing.*;

import net.sf.colossus.variant.BattleHex;
import net.sf.colossus.client.GUIBattleHex;
import net.sf.colossus.variant.HazardTerrain;
import net.sf.colossus.xmlparser.BattlelandLoader;
import net.sf.colossus.parser.BattlelandRandomizerLoader;
import net.sf.colossus.server.VariantSupport;
import net.sf.colossus.util.ResourceLoader;

/**
 * Class ShowBuilderHexMap displays a battle map.
 * @version $Id$
 * @author David Ripton
 * @author Romain Dolbeau
 */
final class ShowBuilderHexMap extends BuilderHexMap implements WindowListener,
        MouseListener, Printable
{

    private JFrame frame;
    private JPopupMenu popupMenuTerrain;
    private JPopupMenu popupMenuBorder;
    private Point lastPoint;
    private Component lastComponent;
    private int lastSide;
    private JCheckBoxMenuItem towerItem;
    private AbstractAction towerAction;
    private AbstractAction clearStartListAction;
    private String mapName = null;

    class rndFileFilter extends javax.swing.filechooser.FileFilter
    {

        public boolean accept(java.io.File f)
        {
            if (f.isDirectory())
            {
                return (true);
            }
            if (f.getName().endsWith(".rnd"))
            {
                return (true);
            }
            return (false);
        }

        public String getDescription()
        {
            return ("Colossus RaNDom generator files");
        }
    }

    class xmlFileFilter extends javax.swing.filechooser.FileFilter
    {

        public boolean accept(java.io.File f)
        {
            if (f.isDirectory())
            {
                return (true);
            }
            if (f.getName().endsWith(".xml"))
            {
                return (true);
            }
            return (false);
        }

        public String getDescription()
        {
            return ("XML files");
        }
    }

    private void doRandomization(BattleHex[][] h, InputStream inputFile)
    {
        BattlelandRandomizerLoader parser =
                new BattlelandRandomizerLoader(inputFile);
        try
        {
            while (parser.oneArea(h) >= 0)
            {
            }
            parser.resolveAllHexsides(h);
            towerItem.setState(parser.isTower());
            List<String> startList = parser.getStartList();
            if (startList != null)
            {
                selectHexesByLabels(new java.util.HashSet<String>(startList));
            }
            basicName = parser.getTitle();
            displayName = parser.getTitle();
            subtitle = parser.getSubtitle();
            setMapName(basicName);
            
        } catch (Exception e)
        {
            System.err.println(e);
        }
    }
    
    private void doLoadRandom(BattleHex[][] h)
    {
        javax.swing.JFileChooser loadFileChooser = new JFileChooser(".");
        loadFileChooser.setFileFilter(new rndFileFilter());
        loadFileChooser.setDialogTitle(
                "Choose the RaNDom file to open (or cancel for nothing)");
        int returnVal = loadFileChooser.showOpenDialog(loadFileChooser);
        if (returnVal == javax.swing.JFileChooser.APPROVE_OPTION)
        {
            java.io.File rndFile = loadFileChooser.getSelectedFile();
            String tempRndName = rndFile.getName();
            String tempRndDirectory = rndFile.getParentFile().getAbsolutePath();
            List<String> directories = new java.util.ArrayList<String>();
            directories.add(tempRndDirectory);
            InputStream inputFile = ResourceLoader.getInputStream(tempRndName,
                    directories);
            if (inputFile != null)
            {
                doRandomization(h, inputFile);
            }
        }
    }
     
    private void doLoadFile(BattleHex[][] h)
    {
        JFileChooser loadFileChooser = new JFileChooser();
        loadFileChooser.setFileFilter(new xmlFileFilter());
        loadFileChooser.setDialogTitle(
                "Choose the battleland file to open (or cancel for nothing)");
        int returnVal = loadFileChooser.showOpenDialog(loadFileChooser);
        if (returnVal == javax.swing.JFileChooser.APPROVE_OPTION)
        {
            File loadFileFile = loadFileChooser.getSelectedFile();
            String temploadFileName = loadFileFile.getName();
            String temploadFileDirectory = loadFileFile.getParentFile().getAbsolutePath();
            List<String> directories = new ArrayList<String>();
            directories.add(temploadFileDirectory);
            InputStream inputFile =
                    net.sf.colossus.util.ResourceLoader.getInputStream(
                    temploadFileName,
                    directories);
            if (inputFile != null)
            {
                BattlelandLoader parser =
                        new BattlelandLoader(inputFile, h);
                try
                {
                    towerItem.setState(parser.isTower());
                    isTower = parser.isTower();
                    List<String> startList = parser.getStartList();
                    if (startList != null)
                    {
                        selectHexesByLabels(new HashSet<String>(startList));
                    }
                    subtitle = parser.getSubtitle();
                    String mapName = temploadFileName.replaceAll(".xml", ""); 
                    displayName = mapName;
                    basicName = mapName;
                    setMapName(displayName);
                    super.repaint();
                } catch (Exception e)
                {
                    System.err.println(e);
                }
            }
        }
    }

    private void doSaveFile()
    {
        javax.swing.JFileChooser saveFileChooser = new JFileChooser();
        saveFileChooser.setDialogTitle(
                "Choose the battleland file to save (or cancel for nothing)");
        int returnVal = saveFileChooser.showSaveDialog(saveFileChooser);
        if (returnVal == javax.swing.JFileChooser.APPROVE_OPTION)
        {
            java.io.File saveFileFile = saveFileChooser.getSelectedFile();
            String tempsaveFileName = saveFileFile.getName();
            String tempsaveFileDirectory = saveFileFile.getParentFile().getAbsolutePath();
            List<String> directories = new ArrayList<String>();
            directories.add(tempsaveFileDirectory);
            OutputStream outputFile =
                    net.sf.colossus.util.ResourceLoader.getOutputStream(
                    tempsaveFileName,
                    directories);
            if (outputFile != null)
            {
                String outStr = dumpAsString();
                try
                {
                    outputFile.write(outStr.getBytes());
                    outputFile.flush();
                    outputFile.close();
                    String mapName = tempsaveFileName.replaceAll(".xml", ""); 
                    displayName = mapName;
                    basicName = mapName;
                    setMapName(displayName);
                    super.repaint();
                } catch (Exception e)
                {
                    System.err.println(e);
                }
            }
        }
    }

    private void doPrintBattleland()
    {
        PrinterJob printerJob = PrinterJob.getPrinterJob();
        Book book = new Book();
        book.append(this, new PageFormat());
        printerJob.setPageable(book);
        boolean doPrint = printerJob.printDialog();
        if (doPrint)
        {
            try
            {
                printerJob.print();
            } catch (PrinterException exception)
            {
                System.err.println("Printing error: " + exception);
            }
        }
    }

    /* Printable */
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex)
    {
        if (pageIndex >= 1)
        {
            return Printable.NO_SUCH_PAGE;
        }

        Graphics2D g2 = (Graphics2D) graphics;
        g2.scale(.5, .5);

        g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
        paint(g2);

        return Printable.PAGE_EXISTS;
    }

    private void doFillDune(BattleHex[][] h)
    {
        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[i].length; j++)
            {
                if (h[i][j] != null)
                {
                    for (int k = 0; k < 6; k++)
                    {
                        if ((h[i][j].getHexside(k) == ' ') &&
                                (h[i][j].getOppositeHexside(k) == ' '))
                        {
                            BattleHex n = h[i][j].getNeighbor(k);
                            if (n != null)
                            {
                                if (h[i][j].getTerrain().getName().equals("Sand")
                                    &&
                                    n.getTerrain().getName().equals("Plains"))
                                {
                                    h[i][j].setHexside(k, 'd');
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void doFillCliff(BattleHex[][] h)
    {
        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[i].length; j++)
            {
                if (h[i][j] != null)
                {
                    for (int k = 0; k < 6; k++)
                    {
                        if ((h[i][j].getHexside(k) == ' ') &&
                                (h[i][j].getOppositeHexside(k) == ' '))
                        {
                            BattleHex n = h[i][j].getNeighbor(k);
                            if (n != null)
                            {
                                if (h[i][j].getElevation() >
                                        (n.getElevation()+1))
                                {
                                    h[i][j].setHexside(k, 'c');
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void doFillSlope(BattleHex[][] h)
    {
        for (int i = 0; i < h.length; i++)
        {
            for (int j = 0; j < h[i].length; j++)
            {
                if (h[i][j] != null)
                {
                    for (int k = 0; k < 6; k++)
                    {
                        if ((h[i][j].getHexside(k) == ' ') &&
                                (h[i][j].getOppositeHexside(k) == ' '))
                        {
                            BattleHex n = h[i][j].getNeighbor(k);
                            if (n != null)
                            {
                                if (h[i][j].getElevation() ==
                                        (n.getElevation()+1))
                                {
                                    h[i][j].setHexside(k, 's');
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    class TerrainAction extends AbstractAction
    {

        HazardTerrain te;

        TerrainAction(String t, HazardTerrain te)
        {
            super(t);
            this.te = te;
        }

        public void actionPerformed(ActionEvent e)
        {
            GUIBattleHex h = getHexContainingPoint(lastPoint);
            h.getHexModel().setTerrain(te);
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

        public void actionPerformed(ActionEvent e)
        {
            GUIBattleHex h = getHexContainingPoint(lastPoint);
            h.getHexModel().setElevation(el);
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

        public void actionPerformed(ActionEvent e)
        {
            GUIBattleHex h = getHexContainingPoint(lastPoint);
            h.getHexModel().setHexside(lastSide, c);
            h.repaint();
            ((GUIBattleHex) h.getNeighbor(lastSide)).repaint();
        }
    }
    private AbstractAction showBattlelandAction;
    private AbstractAction saveBattlelandAsAction;
    private AbstractAction printBattlelandAction;
    private AbstractAction quitAction;
    private AbstractAction eraseAction;
    private AbstractAction randomizeAction;
    private AbstractAction fillWithSlopeAction;
    private AbstractAction fillWithCliffAction;
    private AbstractAction fillWithDuneAction;
    private AbstractAction loadFileAction;
    JMenuBar menuBar;

    private class randomizeActionPredef extends AbstractAction
    {

        private final String filename;

        randomizeActionPredef(String name, String filename)
        {
            super(name);
            this.filename = filename;
        }

        public void actionPerformed(ActionEvent e)
        {
            InputStream inputFile = ResourceLoader.getInputStream(filename,
                    VariantSupport.getVarDirectoriesList());
            doRandomization(getBattleHexArray(), inputFile);
            repaint();
        }
    }

    ShowBuilderHexMap()
    {
        super();

        menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        menuBar.add(fileMenu);
        JMenuItem mi;

        showBattlelandAction = new AbstractAction("Show Battleland")
        {

            public void actionPerformed(ActionEvent e)
            {
                System.out.print(dumpAsString());
            }
        };

        saveBattlelandAsAction = new AbstractAction("Save Battleland as file...")
        {

            public void actionPerformed(ActionEvent e)
            {
                doSaveFile();
            }
        };

        printBattlelandAction = new AbstractAction("Print Battleland...")
        {

            public void actionPerformed(ActionEvent e)
            {
                doPrintBattleland();
            }
        };

        quitAction = new AbstractAction("Quit")
        {

            public void actionPerformed(ActionEvent e)
            {
                System.exit(0);
            }
        };

        eraseAction = new AbstractAction("Erase all map hexes & hexsides")
        {

            public void actionPerformed(ActionEvent e)
            {
                eraseMap();
                repaint();
            }
        };
        
        randomizeAction = new AbstractAction("Randomize Map from file...")
        {

            public void actionPerformed(ActionEvent e)
            {
                doLoadRandom(getBattleHexArray());
                repaint();
            }
        };

        loadFileAction = new AbstractAction("Load Map from file...")
        {

            public void actionPerformed(ActionEvent e)
            {
                doLoadFile(getBattleHexArray());
                repaint();
            }
        };

        mi = fileMenu.add(loadFileAction);
        mi.setMnemonic(KeyEvent.VK_O);
        mi = fileMenu.add(saveBattlelandAsAction);
        mi.setMnemonic(KeyEvent.VK_S);
        mi = fileMenu.add(printBattlelandAction);
        mi = fileMenu.add(showBattlelandAction);
        mi = fileMenu.add(quitAction);
        mi.setMnemonic(KeyEvent.VK_Q);

        JMenu specialMenu = new JMenu("Special");
        menuBar.add(specialMenu);
        
        mi = specialMenu.add(eraseAction);

        specialMenu.addSeparator();

        towerAction = new AbstractAction("Terrain is a Tower")
        {

            public void actionPerformed(ActionEvent e)
            {
                JCheckBoxMenuItem mi = (JCheckBoxMenuItem) e.getSource();
                isTower = !isTower;
                mi.setState(isTower);
            }
        };
        towerItem = (JCheckBoxMenuItem) specialMenu.add(new JCheckBoxMenuItem(
                towerAction));
        clearStartListAction = new AbstractAction("Remove StartList")
        {

            public void actionPerformed(ActionEvent e)
            {
                for (int i = 0; i < h.length; i++)
                {
                    for (int j = 0; j < h[0].length; j++)
                    {
                        if ((h[i][j] != null) &&
                                (h[i][j].isSelected()))
                        {
                            h[i][j].unselect();
                            h[i][j].repaint();
                        }
                    }
                }
            }
        };
        
        mi = specialMenu.add(clearStartListAction);
        specialMenu.addSeparator();

        fillWithSlopeAction = new AbstractAction("Fill Low Edge with Slope")
        {

            public void actionPerformed(ActionEvent e)
            {
                doFillSlope(getBattleHexArray());
                repaint();
            }
        };
        fillWithCliffAction = new AbstractAction("Fill High Edge with Cliff")
        {

            public void actionPerformed(ActionEvent e)
            {
                doFillCliff(getBattleHexArray());
                repaint();
            }
        };
        fillWithDuneAction = new AbstractAction("Fill Sand Edge with Dune")
        {

            public void actionPerformed(ActionEvent e)
            {
                doFillDune(getBattleHexArray());
                repaint();
            }
        };

        mi = specialMenu.add(fillWithSlopeAction);
        mi = specialMenu.add(fillWithCliffAction);
        mi = specialMenu.add(fillWithDuneAction);

        JMenu randomMenu = new JMenu("Randomize as ");

        final Map<String,String> defaultRandom = new TreeMap<String,String>();

        defaultRandom.put("Brush", "Battlelands/Brush.rnd");
        defaultRandom.put("Desert", "Battlelands/Desert.rnd");
        defaultRandom.put("Hills", "Battlelands/Hills.rnd");
        defaultRandom.put("Jungle", "Battlelands/Jungle.rnd");
        defaultRandom.put("Marsh", "Battlelands/Marsh.rnd");
        defaultRandom.put("Mountains", "Battlelands/Mountains.rnd");
        defaultRandom.put("Plains", "Battlelands/Plains.rnd");
        defaultRandom.put("Swamp", "Battlelands/Swamp.rnd");
        defaultRandom.put("Tower", "Battlelands/Tower.rnd");
        defaultRandom.put("Tundra", "Battlelands/Tundra.rnd");
        defaultRandom.put("Woods", "Battlelands/Woods.rnd");
        for (String name : defaultRandom.keySet()) {
            mi = randomMenu.add(new randomizeActionPredef(name, defaultRandom.get(name)));
        }
        randomMenu.addSeparator();
        defaultRandom.clear();
        defaultRandom.put("Beach", "Battlelands/Custom_Beach.rnd");
        for (String name : defaultRandom.keySet()) {
            mi = randomMenu.add(new randomizeActionPredef(name, defaultRandom.get(name)));
        }
        randomMenu.addSeparator();


        mi = randomMenu.add(randomizeAction);

        specialMenu.addSeparator();
        specialMenu.add(randomMenu);
               
        frame = new JFrame("BattlelandBuilder");

        Container contentPane = frame.getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(menuBar, BorderLayout.NORTH);

        addMouseListener(this);
        frame.addWindowListener(this);

        contentPane.add(this, BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);

        popupMenuTerrain = new JPopupMenu("Choose Terrain");
        contentPane.add(popupMenuTerrain);
        Collection<HazardTerrain> terrains =
                HazardTerrain.getAllHazardTerrains();
        GUIBattleHex tempH = new GUIBattleHex(0, 0, 1, this, 0, 0);

        for (HazardTerrain terrain : terrains)
        {
            tempH.getHexModel().setTerrain(terrain);
            mi = popupMenuTerrain.add(new TerrainAction(terrain.getName(),
                    terrain));
        }
        popupMenuTerrain.addSeparator();
        for (int i = 0; i < 4; i++)
        {
            mi = popupMenuTerrain.add(new ElevationAction(
                    "Set Elevation to: " + i,
                    i));
        }
        popupMenuTerrain.addSeparator();
        AbstractAction select = new AbstractAction("Select/Unselect (StartList)")
        {

            public void actionPerformed(ActionEvent e)
            {
                GUIBattleHex h = getHexContainingPoint(lastPoint);
                if (h.isSelected())
                {
                    h.unselect();
                }
                else
                {
                    h.select();
                }
                h.repaint();
            }
        };
        mi = popupMenuTerrain.add(select);

        popupMenuBorder = new JPopupMenu("Choose Border");
        contentPane.add(popupMenuBorder);
        char[] hexsides = BattleHex.getHexsides();

        for (int i = 0; i < hexsides.length; i++)
        {
            tempH.getHexModel().setHexside(0, hexsides[i]);
            mi = popupMenuBorder.add(new HexsideAction(tempH.getHexModel().
                    getHexsideName(0),
                    hexsides[i]));
        }

        lastPoint = new Point(0, 0);
        lastComponent = contentPane;
        lastSide = 0;
    }

    private void setMapName(String name)
    {
        mapName = name;
        frame.setTitle("BattlelandBuilder" + 
            (mapName == null ? "" : ": " + mapName)); 
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
                        ((c.x) + (h.getBounds().x + h.getBounds().width)) / 2)
                {
                    lastSide = 1;
                }
                else if (lastPoint.x <= ((c.x) + (h.getBounds().x)) / 2)
                {
                    lastSide = 5;
                }
                else
                {
                    lastSide = 0;
                }
            }
            else
            { // lower half
                if (lastPoint.x >=
                        ((c.x) + (h.getBounds().x + h.getBounds().width)) / 2)
                {
                    lastSide = 2;
                }
                else if (lastPoint.x <= ((c.x) + (h.getBounds().x)) / 2)
                {
                    lastSide = 4;
                }
                else
                {
                    lastSide = 3;
                }
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
        System.exit(0);
    }
}
