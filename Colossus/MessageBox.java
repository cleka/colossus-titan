import java.awt.*;
import java.awt.event.*;

/**
 * Class MessageBox creates a message dialog with an OK button.
 * @version $Id$
 * author David Ripton
 */

class MessageBox extends Dialog implements ActionListener
{

    MessageBox(Frame parentFrame, String message)
    {
        super(parentFrame, "Message");

        int scale = 60;

        add(new Label(message));

        Button button = new Button("OK");
        add(button);
        button.addActionListener(this);

        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(new Point(d.width / 2 - getSize().width / 2,
            d.height / 2 - getSize().height / 2));
        
        pack();
        setVisible(true);
    }


    public void actionPerformed(ActionEvent e)
    {
        dispose();
    }
}
