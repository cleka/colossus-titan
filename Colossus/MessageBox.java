import java.awt.*;
import java.awt.event.*;

/**
 * Class MessageBox creates a message dialog with an OK button.
 * @version $Id$
 * author David Ripton
 */

class MessageBox extends Dialog implements ActionListener
{
    private static boolean standalone = false;


    MessageBox(Frame parentFrame, String message)
    {
        super(parentFrame, "Message");

        setLayout(new GridLayout(0, 1));
        setBackground(java.awt.Color.lightGray);

        add(new Label(message));

        Button button = new Button("OK");
        add(button);
        button.addActionListener(this);

        pack();

        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(new Point(d.width / 2 - getSize().width / 2,
            d.height / 2 - getSize().height / 2));
        
        setVisible(true);
    }


    public void actionPerformed(ActionEvent e)
    {
        dispose();
        if (standalone)
        {
            System.exit(0);
        }
    }


    public static void main(String [] args)
    {
        standalone = true;
        if (args.length < 1)
        {
            new MessageBox(new Frame(), "Your message here");
        }
        else
        {
            new MessageBox(new Frame(), args[0]);
        }
    }
}
