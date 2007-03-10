
package net.sf.colossus.server;


import java.awt.*;
import java.awt.event.*;

import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JButton;

import net.sf.colossus.server.Server;


/** 
 *  Simple log window for Startup progress (waiting for clients)
 *  @version $Id: StartupProgress.java 0000 2007-02-14 00:00:00Z cleka $
 *  @author Clemens Katzer
 */
public final class StartupProgress implements ActionListener 
{
    private JFrame logFrame;
    private TextArea text;
    private Container pane;
    private Server server;
    private JButton b;
    private JCheckBox autoCloseCheckBox;
    
    public StartupProgress (Server server)
    {
        this.server = server;
        
        //Create and set up the window.
        JFrame logFrame = new JFrame("Server startup progress log");
        this.logFrame = logFrame;
        
        logFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        Container pane = logFrame.getContentPane();
        this.pane = pane;
                
        TextArea text = new TextArea("", 20, 80);
        this.text = text;
        pane.add(text, BorderLayout.CENTER);
         
        JButton b1 = new JButton("Abort");
        this.b = b1;
        b1.setVerticalTextPosition(JButton.CENTER);
        b1.setHorizontalTextPosition(JButton.LEADING); //aka LEFT, for left-to-right locales
        b1.setMnemonic(KeyEvent.VK_A);
        b1.setActionCommand("abort");
        b1.addActionListener(this);
        b1.setToolTipText("Click this button to abort the start process.");
        pane.add(b1, BorderLayout.SOUTH);
        
        this.autoCloseCheckBox = new JCheckBox("Automatically close when game starts");
        autoCloseCheckBox.setSelected(true);
        pane.add(autoCloseCheckBox, BorderLayout.NORTH);
        
        //Display the window.
        logFrame.pack();
        logFrame.setVisible(true);
        
    }
      
    public void append(String s)
    {
        this.text.append(s + "\n");
    }

    public void setCompleted()
    {
        if(this.autoCloseCheckBox.isSelected()) {
            this.dispose();
            return;
        }
        this.text.append("OK, all clients have come in. You can close this window now.");

        JButton b2 = new JButton("Close");
        b2.setMnemonic(KeyEvent.VK_C);
        b2.setActionCommand("close");
        b2.addActionListener(this);
        b2.setToolTipText("Click this button to close this window.");

        this.pane.remove(this.b);
        this.pane.add(b2, BorderLayout.SOUTH);
        this.logFrame.pack();
    }

    public void dispose()
    {
        if ( this.logFrame != null )
        {
            this.logFrame.dispose();
            this.logFrame = null;
        }
    }
    
    public void actionPerformed(ActionEvent e) 
    {
        if ("abort".equals(e.getActionCommand()))
        {
            this.text.append("\nAbort requested, please wait...\n");
            this.server.panicExit();
        }
        
        if ("close".equals(e.getActionCommand())) 
        {
            this.text.append("\nClosing...\n");
            this.dispose();
        }
    }
}