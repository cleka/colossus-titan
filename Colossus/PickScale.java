import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

/** Class SetScale allows picking a new GUI scale for a client.
 *  @version $Id$
 *  @author David Ripton
 */

public final class PickScale extends JDialog implements WindowListener,
    ChangeListener, ActionListener
{
    private static int newScale;
    // A JSpinner would be better, but is not supported until JDK 1.4.
    private JSlider slider;


    private PickScale(JFrame parentFrame, int oldScale)
    {
        super(parentFrame, "Set Scale", true);

        setBackground(Color.lightGray);

        addWindowListener(this);

        Container contentPane = getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));

        slider = new JSlider(JSlider.HORIZONTAL, 5, 25, oldScale);
        slider.setMajorTickSpacing(5);
        slider.setMinorTickSpacing(1);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        contentPane.add(slider);
        slider.addChangeListener(this);

        // Need another BoxLayout to place buttons horizontally.
        Box buttonBar = new Box(BoxLayout.X_AXIS);
        contentPane.add(buttonBar);

        JButton accept = new JButton("Accept");
        accept.addActionListener(this);
        buttonBar.add(accept);

        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(this);
        buttonBar.add(cancel);

        pack();

        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(new Point(d.width / 2 - getSize().width / 2,
            d.height / 2 - getSize().height / 2));

        setVisible(true);
        repaint();
    }

    /** Return the new scale if the user accepted it, or -1 if the
     *  user cancelled the dialog. */
    public static int pickScale(JFrame parentFrame, int oldScale)
    {
        new PickScale(parentFrame, oldScale);
        return newScale;
    }


    public void stateChanged(ChangeEvent e)
    {
        newScale = slider.getValue();
    }

    public void actionPerformed(ActionEvent e)
    {
        if (e.getActionCommand().equals("Accept"))
        {
            dispose();
        }
        else if (e.getActionCommand().equals("Cancel"))
        {
            newScale = -1;
            dispose();
        }
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


    public static void main(String [] args)
    {
        JFrame frame = new JFrame();
        int scale = PickScale.pickScale(frame, 15);
        System.out.println("new scale is " + scale);
        System.exit(0);
    }
}
