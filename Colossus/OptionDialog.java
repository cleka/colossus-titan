import java.awt.*;
import java.awt.event.*;

/** 
 *  Class OptionDialog presents a configurable modal yes/no dialog box.
 *  @version $Id$
 *  @author David Ripton
 */

public class OptionDialog extends Dialog implements ActionListener
{
    public static final int YES_OPTION = 1;
    public static final int NO_OPTION = 0;

    // Will be set to YES_OPTION or NO_OPTION
    private static int lastAnswer = NO_OPTION;

    // XXX Ensure that yesString and noString are different.
    private String yesString;
    private String noString;


    public OptionDialog(Frame parentFrame, String title, String question,
        String yesString, String noString)
    {
        super(parentFrame, title, true);

        this.yesString = yesString;
        this.noString = noString;

        setBackground(Color.lightGray);

        setLayout(new BorderLayout());

        setResizable(false);

        add(new Label(question), BorderLayout.NORTH);

        Button button1 = new Button(yesString);
        add(button1, BorderLayout.WEST);
        button1.addActionListener(this);

        Button button2 = new Button(noString);
        add(button2, BorderLayout.EAST);
        button2.addActionListener(this);

        pack();

        // Center dialog on screen.
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(new Point(d.width / 2 - getSize().width / 2, d.height / 2
                     - getSize().height / 2));

        setVisible(true);
    }


    public static int getLastAnswer()
    {
        return lastAnswer;
    }


    public void actionPerformed(ActionEvent e)
    {
        if (e.getActionCommand().equals(yesString))
        {
            lastAnswer = YES_OPTION;
        }
        else
        {
            lastAnswer = NO_OPTION;
        }
        
        dispose();
    }


    // Unit test.
    public static void main(String [] args)
    {
        new OptionDialog(new Frame(), "Title", "Yes or No?", "Yes", "No");

        System.out.println("Answer is " + OptionDialog.getLastAnswer());

        System.exit(0);
    }
}
