import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;


/**
 * Class PickColor lets a player choose a color of legion markers.
 * @version $Id$
 * @author David Ripton
 */


public final class PickColor extends JDialog implements WindowListener,
    ActionListener
{
    private JLabel [] colorLabel = new JLabel[6];

    public static final String [] colorNames =
        {"Black", "Blue", "Brown", "Gold", "Green", "Red"};
    private static final Color [] background = { Color.black, Color.blue,
        HTMLColor.brown, Color.yellow, Color.green, Color.red };
    private static final Color [] foreground = { Color.white, Color.white,
        Color.white, Color.black, Color.black, Color.black };

    private static final int [] colorMnemonics =
        {KeyEvent.VK_B, KeyEvent.VK_L, KeyEvent.VK_O, KeyEvent.VK_G,
            KeyEvent.VK_E, KeyEvent.VK_R};

    private static String color;


    private PickColor(JFrame parentFrame, String playerName, 
        Set colorsLeft)
    {
        super(parentFrame, playerName + ", Pick a Color", true);

        color = null;

        setBackground(Color.lightGray);
        pack();

        Container contentPane = getContentPane();
        contentPane.setLayout(new FlowLayout());

        for (int i = 0; i < 6; i++)
        {
            if (colorsLeft.contains(colorNames[i]))
            {
                JButton button = new JButton();
                int scale = Scale.get();
                button.setPreferredSize(new Dimension(7 * scale, 4 * scale));
                button.setText(colorNames[i]);
                button.setMnemonic(colorMnemonics[i]);
                button.setBackground(background[i]);
                button.setForeground(foreground[i]);
                button.addActionListener(this);
                contentPane.add(button);
            }
        }

        pack();

        // Center dialog on screen.
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(new Point(d.width / 2 - getSize().width / 2, d.height / 2
                     - getSize().height / 2));

        addWindowListener(this);
        setVisible(true);
    }


    public static String pickColor(JFrame parentFrame, String playerName,
        Set colorsLeft)
    {
        new PickColor(parentFrame, playerName, colorsLeft);
        return color;
    }


    public static String getColorName(int i)
    {
        if (i >= 0 && i < colorNames.length)
        {
            return colorNames[i];
        }
        return null;
    }

    public static Color getForegroundColor(String colorName)
    {
        for (int i = 0; i < colorNames.length; i++)
        {
            if (colorName.equals(colorNames[i]))
            {
                return foreground[i];
            }
        }
        return null;
    }

    public static Color getBackgroundColor(String colorName)
    {
        for (int i = 0; i < colorNames.length; i++)
        {
            if (colorName.equals(colorNames[i]))
            {
                return background[i];
            }
        }
        return null;
    }


    private int colorNumber(String colorName)
    {
        for (int i = 0; i < 6; i++)
        {
            if (colorName.equals(colorNames[i]))
            {
                return i;
            }
        }

        return -1;
    }


    public void windowActivated(WindowEvent e)
    {
    }

    public void windowClosed(WindowEvent e)
    {
    }

    public void windowClosing(WindowEvent e)
    {
    }

    public void windowDeactivated(WindowEvent e)
    {
    }

    public void windowDeiconified(WindowEvent e)
    {
    }

    public void windowIconified(WindowEvent e)
    {
    }

    public void windowOpened(WindowEvent e)
    {
    }

    public void actionPerformed(ActionEvent e)
    {
        color = e.getActionCommand();
        dispose();
    }


    public static void main(String [] args)
    {
        Set colorsLeft = new HashSet();
        colorsLeft.add("Black");
        colorsLeft.add("Blue");
        colorsLeft.add("Brown");
        colorsLeft.add("Gold");
        colorsLeft.add("Green");
        colorsLeft.add("Red");
        PickColor.pickColor(new JFrame(), "Test", colorsLeft);
        System.out.println("Chose " + color);
        System.exit(0);
    }
}
