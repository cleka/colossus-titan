package net.sf.colossus.gui;


import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sf.colossus.guiutil.INonRecticleJComponent;


/**
 * The <code>TrueHexGridLayout</code> class is a layout manager that
 * lays out a container's components in a grid with alternate rows
 * offset by a partial grid component.  The vertical gap is automatically
 * sized so that the Hex components can draw the non base-rectangle portions
 * of the hexes in the gap. This results in a hexagonal tesselation.
 * <p>
 * When constructed, the layout can start with an indented row or not.
 * The sizing can be done Isometrically or not.
 *
 * TODO: implement the containers <code>ComponentOrientation</code> property
 *
 * If either the Number of Rows or the Number of Columns is set to Zero either
 * by constructor or the set rows or set columns method, then the grid will be
 * assumed to be square and layed out as such.  This class is used for battle
 * Hexes.
 *
 * @author  Edward Dranathi based loosely on Sun's GridLayout class.
 *
 */
public class TrueHexGridLayout implements LayoutManager
{
    int rows;
    int cols;
    boolean indentOddRows;
    private final boolean isometricShape;

    public TrueHexGridLayout(int pRows, int pColumns, boolean pIndentFirstRow,
        boolean pIsometricShape)
    {
        rows = pRows;
        cols = pColumns;
        indentOddRows = pIndentFirstRow;
        isometricShape = pIsometricShape;
    }

    public TrueHexGridLayout(int pRows, int pColumns, boolean pIndentFirstRow)
    {
        this(pRows, pColumns, pIndentFirstRow, true);
    }

    public void addLayoutComponent(String name, Component comp)
    {
        // Nothing to see here.
    }

    public void removeLayoutComponent(Component comp)
    {
        // TODO Auto-generated method stub

    }

    /**
     * Lays out the specified container using this layout.
     * <p>
     * This method resizes the components in the specified target
     * container in order to satisfy the constraints of the
     * <code>BattleHexGridLayout</code> object.
     * <p>
     * This layout manager determines the size of individual
     * components by dividing the free space in the container into
     * equal-sized portions according to the number of rows and columns
     * in the layout. The container's free space equals the container's
     * size minus any insets and vertical gap needed.
     * All components in the layout are given the same size.
     *
     * @param      parent   the container in which to do the layout
     * @see        java.awt.Container
     * @see        java.awt.Container#doLayout
     */
    public void layoutContainer(Container parent)
    {
        synchronized (parent.getTreeLock())
        {
            Insets insets = parent.getInsets();
            int ncomponents = parent.getComponentCount();
            int nrows = rows;
            int ncols = cols;

            if (ncomponents == 0)
            {
                return;
            }
            if (ncols > 0)
            {
                nrows = (ncomponents + ncols - 1) / ncols;
            }
            else
            {
                ncols = (ncomponents + nrows - 1) / nrows;
            }
            int containerWidth = parent.getWidth()
                - (insets.left + insets.right);
            int containerHeight = parent.getHeight()
                - (insets.top + insets.bottom);
            if (isometricShape)
            {
                if (containerHeight > containerWidth)
                {
                    containerHeight = containerWidth;
                }
                else
                {
                    containerWidth = containerHeight;
                }
            }
            int compWidth = (containerWidth * 2) / (ncols * 2 + 1);
            int rowIndent = compWidth / 2;
            int vgap = containerHeight / (nrows * 5 + 1);
            int compHeight = vgap * 4;
            if (isometricShape)
            {
                if (compWidth > compHeight)
                {
                    compWidth = compHeight;
                }
                else
                {
                    compHeight = compWidth;
                }
                vgap = compHeight / 4;
            }

            /* layout left to right one row at a time, top to bottom */
            int y = insets.top + vgap; // init y
            for (int row = 0; row < nrows; row++)
            {
                int x = indentOddRows ^ (row % 2 == 0) ? insets.left
                    : insets.left + rowIndent;
                for (int col = 0; col < ncols; col++)
                {
                    int i = row * ncols + col;
                    if (i < ncomponents)
                    {

                        Component c = parent.getComponent(i);
                        if (c != null)
                        {
                            if (c instanceof INonRecticleJComponent)
                            {
                                INonRecticleJComponent cNR = (INonRecticleJComponent)c;
                                cNR.resizeBaseRectangle(new Rectangle(x, y,
                                    compWidth, compHeight));
                            }
                            else
                            {
                                c.setBounds(x, y, compWidth, compHeight);
                            }
                        }
                    }
                    x += compWidth; // increment X coordinate.
                }
                y += compHeight + vgap; // increment Y coordinate.
            }
        }
    }

    public Dimension minimumLayoutSize(Container parent)
    {
        return null;
    }

    public Dimension preferredLayoutSize(Container parent)
    {
        return null;
    }

    /* (non-Javadoc)
     * @see java.awt.LayoutManager#removeLayoutComponent(java.awt.Component)
     */

    public static void main(String[] args)
    {
        JFrame f = new JFrame();
        JPanel map = new JPanel();
        map.setLayout(new TrueHexGridLayout(6, 6, true));
        for (int i = 0; i < 36; i++)
        {
            JLabel x = new JLabel();
            x.setBorder(BorderFactory.createLineBorder(Color.BLUE, 1));
            x.setText("" + i);
            map.add(x);
        }
        f.setTitle("Sample True Hex Grid Layout");
        f.setContentPane(map);
        f.setSize(408, 534);
        f.setVisible(true);
        f.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                System.exit(0);
            }
        });
    }
}
