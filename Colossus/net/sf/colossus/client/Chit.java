package net.sf.colossus.client;


import java.awt.*;
import java.io.*;
import javax.swing.*;
import java.util.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.net.*;
import net.sf.colossus.util.ResourceLoader;
import net.sf.colossus.util.Log;
import net.sf.colossus.util.Options;
import net.sf.colossus.server.Creature;
import net.sf.colossus.server.VariantSupport;


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
    Rectangle rect;
    private Container container;
    Client client;  // may be null; set for some subclasses

    /** Flag to mark chit as dead and paint it with an "X" through it. */
    private boolean dead;

    /** Flag to paint a border around the chit. */
    boolean border = true;
    Color borderColor = Color.black;

    /** Flag to paint the chit upside-down. */
    private boolean inverted = false;

    private String id;

    static BasicStroke oneWide = new BasicStroke(1);
    static BasicStroke threeWide = new BasicStroke(3);

    Chit(int scale, String id, Container container)
    {
        this(scale, id, container, false, false);
    }

    Chit(int scale, String id, Container container, boolean inverted)
    {
        this(scale, id, container, inverted, false);
    }

    Chit(int scale, String id, Container container, boolean inverted,
            boolean dubious)
    {
        super();

        this.inverted = inverted;

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
                icon = getImageIcon(id, scale);
            }
            else
            { // special case : the Titan.
                String[] filenames = new String[4 + (dubious ? 1 : 0)];
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
                int skill = Creature.getCreatureByName("Titan").getSkill();
                filenames[3] = "Skill-" + skill + "" + color;

                if (dubious)
                {
                    filenames[4] = "QuestionMarkMask" +
                            (color.equals("BlackColossus") ? "Red" : "");
                }

                icon = getImageIcon(filenames, scale);
            }
        }
        else
        {
            Creature cre = Creature.getCreatureByName(id);
            String[] names = cre.getImageNames();

            if (dubious)
            {
                String[] names2 = new String[names.length + 1];
                for (int i = 0; i < names.length; i++)
                {
                    names2[i] = names[i];
                }
                names2[names.length] = "QuestionMarkMask" +
                        (cre.getBaseColor().equals("black") ? "Red" : "");
                names = names2;
            }

            icon = getImageIcon(names, scale);
        }
    }

    // XXX Duplicate code.
    int getTitanPower()
    {
        if (!id.startsWith("Titan-"))
        {
            return -1;
        }
        int index = 6;
        int index2 = index;
        char c = id.charAt(index2);
        while ((c >= '0') && (c <= '9'))
        {
            index2++;
            c = id.charAt(index2);
        }
        int power = Integer.parseInt(id.substring(index, index2));
        return power;
    }

    static ImageIcon getImageIcon(String imageFilename, int scale)
    {
        ImageIcon tempIcon = null;
        java.util.List directories = VariantSupport.getImagesDirectoriesList();
        tempIcon = ResourceLoader.getImageIcon(imageFilename, directories,
                scale, scale);
        if (tempIcon == null)
        {
            Log.error("Couldn't get image :" + imageFilename);
            System.exit(1);
        }

        return tempIcon;
    }

    static ImageIcon getImageIcon(String[] imageFilenames, int scale)
    {
        java.util.List directories = VariantSupport.getImagesDirectoriesList();
        Image composite = ResourceLoader.getCompositeImage(imageFilenames,
                directories,
                scale, scale);
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
        if (container == null)
        {
            return;
        }

        Graphics2D g2 = (Graphics2D)g;
        super.paintComponent(g2);
        Image image = icon.getImage();

        if (inverted &&
                (client == null ||
                !client.getOption(Options.doNotInvertDefender)))
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
            g2.setStroke(oneWide);
        }

        if (border)
        {
            g2.setColor(borderColor);
            Rectangle rect = getBounds();
            g.drawRect(rect.x, rect.y, rect.width - 1, rect.height - 1);
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

    void setBorderColor(Color borderColor)
    {
        this.borderColor = borderColor;
    }

    boolean isInverted()
    {
        return inverted;
    }
}
