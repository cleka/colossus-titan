package net.sf.colossus.client;


import java.awt.*;
import java.io.*;
import javax.swing.*;
import java.util.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.net.*;
import net.sf.colossus.util.ResourceLoader;
import net.sf.colossus.server.Constants;
import net.sf.colossus.client.VariantSupport;
import net.sf.colossus.server.Creature;

/**
 * Class Chit implements the GUI for a Titan chit representing
 * either a character or a legion.
 * @version $Id$
 * @author David Ripton
 * @author Romain Dolbeau
 */

class Chit extends JPanel
{
    private ImageIcon icon;
    private ImageIcon invertedIcon;
    private Rectangle rect;
    private Container container;

    /** Flag to mark chit as dead and paint it with an "X" through it. */
    private boolean dead;

    /** Flag to paint a border around the chit. */
    private boolean border;

    /** Flag to paint the chit upside-down. */
    private boolean inverted = false;

    private String id;

    private static BasicStroke threeWide = new BasicStroke(3);

    Chit(int scale, String id, Container container)
    {
        super();
        Point point = getLocation();

        // Images are 60x60, so if scale is close to that, avoid
        // rescaling.
        if (scale > 50 && scale < 70)
        {
            scale = 60;
        }

        rect = new Rectangle(point.x, point.y, scale, scale);
        setBounds(rect);

        this.container = container;
        this.id = id;

        setBackground(Color.lightGray);

        
        if (!Creature.isCreature(id))
        {
            if (!(id.startsWith("Titan-")))
            {
                icon = getImageIcon(id);
            }
            else
            { // special case : the Titan.
System.err.println("TRYING: " + id);
                String[] filenames = new String[4];
                int index = 6;
                int index2 = index;
                char c = id.charAt(index2);
                while ((c >= '0') && (c <= '9'))
                {
                    index2++;
                    c = id.charAt(index2);
                }
                int power = Integer.parseInt(id.substring(index, index2));
                String color = id.substring(index2) + "Colossus";
                filenames[0] = "Plain" + color;
                filenames[1] = "TitanMask";
                filenames[2] = "Power-" + power + color;
                filenames[3] = "Skill-4" + color;
System.err.println("WILL TRY WITH: " +
                   filenames[0] + "," +
                   filenames[1] + "," +
                   filenames[2] + "," +
                   filenames[3]);
                   
                icon = getImageIcon(filenames);
            }
        }
        else
        {
            Creature cre = Creature.getCreatureByName(id);
            icon = getImageIcon(cre.getImagesNames());
        }
    }

    Chit(int scale, String id, Container container, boolean inverted)
    {
        this(scale, id, container);
        this.inverted = inverted;
    }

    static ImageIcon getImageIcon(String imageFilename)
    {
        ImageIcon tempIcon = null;
        java.util.List directories = VariantSupport.getImagesDirectoriesList();
        tempIcon = ResourceLoader.getImageIcon(imageFilename, directories);
        if (tempIcon == null)
        {
            System.out.println("Couldn't get image :" + imageFilename);
            System.exit(1);
        }
        
        return tempIcon;
    }

    static ImageIcon getImageIcon(String imageFilenames[])
    {
        java.util.List directories = VariantSupport.getImagesDirectoriesList();
        Image composite = ResourceLoader.getCompositeImage(imageFilenames,
                                                           directories);
        return new ImageIcon(composite);
    }

    String getId()
    {
        return id;
    }

    public String toString()
    {
        return id;
    }


    void rescale(int scale)
    {
        rect.width = scale;
        rect.height = scale;
        setBounds(rect);
    }


    public void paintComponent(Graphics g)
    {
        Graphics2D g2 = (Graphics2D)g;
        super.paintComponent(g2);
        Image image = icon.getImage();

        if (inverted)
        {
            if (invertedIcon == null)
            {
                int width = icon.getIconWidth();
                int height = icon.getIconHeight();
                BufferedImage bi = new BufferedImage(
                                           width, height,
                                           BufferedImage.TYPE_INT_RGB);
                Graphics2D biContext = bi.createGraphics();
                biContext.drawImage(image, 0, 0, null);
                double theta = Math.PI;
                AffineTransform at = AffineTransform.getRotateInstance(
                                         theta,
                                         width / 2,
                                         height / 2);
                AffineTransformOp ato = new AffineTransformOp(at,
                                            AffineTransformOp.TYPE_BILINEAR);
                BufferedImage bi2 = ato.createCompatibleDestImage(bi, null);
                bi2 = ato.filter(bi, bi2);
                invertedIcon = new ImageIcon(bi2);
            }
            image = invertedIcon.getImage();
        }
        g2.drawImage(image, rect.x, rect.y, rect.width,
                     rect.height, container);
        if (isDead())
        {
            // Draw a triple-wide red X.
            g2.setStroke(threeWide);
            g2.setColor(Color.red);
            g2.drawLine(rect.x, rect.y, rect.x + rect.width,
                rect.y + rect.height);
            g2.drawLine(rect.x + rect.width, rect.y, rect.x,
                rect.y + rect.height);
        }

        if (border)
        {
            g2.setColor(Color.black);
            Rectangle rect = getBounds();
            g.drawRect(rect.x, rect.y, rect.width, rect.height);
        }
    }


    public void setLocation(Point point)
    {
        rect.setLocation(point);
        setBounds(rect);
    }


    public boolean contains(Point point)
    {
        return rect.contains(point);
    }


    public Rectangle getBounds()
    {
        return rect;
    }

    public Point getCenter()
    {
        return new Point(rect.x + rect.width / 2, rect.y + rect.height / 2);
    }

    public Dimension getPreferredSize()
    {
        return new Dimension(rect.width, rect.height);
    }

    public Dimension getMinimumSize()
    {
        return getPreferredSize();
    }

    public Dimension getMaximumSize()
    {
        return getPreferredSize();
    }


    boolean isDead()
    {
        return dead;
    }

    void setDead(boolean dead)
    {
        this.dead = dead;
        repaint();
    }

    void toggleDead()
    {
        dead = !dead;
    }

    void setBorder(boolean border)
    {
        this.border = border;
    }
}
