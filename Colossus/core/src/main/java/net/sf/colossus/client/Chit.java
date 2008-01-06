package net.sf.colossus.client;


import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JPanel;

import net.sf.colossus.server.Creature;
import net.sf.colossus.server.VariantSupport;
import net.sf.colossus.util.Options;
import net.sf.colossus.util.ResourceLoader;


/**
 * Class Chit implements the GUI for a Titan chit representing
 * either a character or a legion.
 * @version $Id$
 * @author David Ripton
 * @author Romain Dolbeau
 */

class Chit extends JPanel
{
    private static final Logger LOGGER = Logger
        .getLogger(Chit.class.getName());

    private Image bufferedImage;
    private Image bufferedInvertedImage;
    Rectangle rect;
    Client client; // may be null; set for some subclasses

    /** Flag to mark chit as dead and paint it with an "X" through it. */
    private boolean dead;

    /** Flag to paint a border around the chit. */
    boolean border = true;
    Color borderColor = Color.black;

    /** Flag to paint the chit upside-down. */
    protected boolean inverted = false;

    // Initialize early to avoid NullPointerException with GTK L&F
    private String id = "";

    static BasicStroke oneWide = new BasicStroke(1);
    static BasicStroke threeWide = new BasicStroke(3);

    Chit(int scale, String id)
    {
        this(scale, id, false, false);
    }

    Chit(int scale, String id, boolean inverted)
    {
        this(scale, id, inverted, false, false);
    }

    Chit(int scale, String id, boolean inverted, boolean dubious)
    {
        this(scale, id, inverted, dubious, false);
    }

    Chit(int scale, String id, boolean inverted, boolean dubious,
        boolean dubiousAsBlank)
    {
        // LayoutManager null - we want to place things ourselves
        super((LayoutManager)null);

        this.id = id;
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

        setBackground(Color.lightGray);

        if (dubious && dubiousAsBlank)
        {
            String[] names = new String[1];
            names[0] = "QuestionMarkMask";
            bufferedImage = getImage(names, scale);
        }
        else if (Creature.isCreature(id))
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
                names2[names.length] = "QuestionMarkMask"
                    + (cre.getBaseColor().equals("black") ? "Red" : "");
                names = names2;
            }
            bufferedImage = getImage(names, scale);
        }
        else
        {
            if (id.startsWith("Titan-"))
            {
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
                    filenames[4] = "QuestionMarkMask"
                        + (color.equals("BlackColossus") ? "Red" : "");
                }

                bufferedImage = getImage(filenames, scale);
            }
            else
            {
                bufferedImage = getImage(id, scale);
            }
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

    static Image getImage(String imageFilename, int scale)
    {
        ImageIcon tempIcon = null;
        List directories = VariantSupport.getImagesDirectoriesList();
        tempIcon = ResourceLoader.getImageIcon(imageFilename, directories,
            scale, scale);
        if (tempIcon == null)
        {
            LOGGER.log(Level.SEVERE, "Couldn't get image :" + imageFilename);
            throw new RuntimeException(
                "Unable to retrieve image for filename '" + imageFilename
                    + "'");
        }

        return tempIcon.getImage();
    }

    static Image getImage(String[] imageFilenames, int scale)
    {
        List directories = VariantSupport.getImagesDirectoriesList();
        Image composite = ResourceLoader.getCompositeImage(imageFilenames,
            directories, scale, scale);
        return composite;
    }

    String getId()
    {
        if (id == null)
        {
            // this should never happen, since id is initialized
            // already from beginning on, still someone gets a NPE
            // just for this id; perhaps due to using GTK L&F ? 
            LOGGER.log(Level.SEVERE, "Chit id is still null ?");
            id = "<notdefined?>";
        }
        return id;
    }

    @Override
    public String toString()
    {
        return getId();
    }

    void rescale(int scale)
    {
        rect.width = scale;
        rect.height = scale;
        setBounds(rect);
    }

    @Override
    public void paintComponent(Graphics g)
    {
        Graphics2D g2 = (Graphics2D)g;
        super.paintComponent(g2);
        Image image = bufferedImage;

        if (inverted
            && (client == null || !client
                .getOption(Options.doNotInvertDefender)))
        {
            if (bufferedInvertedImage == null)
            {
                int width = bufferedImage.getWidth(this);
                int height = bufferedImage.getHeight(this);
                BufferedImage bi = new BufferedImage(width, height,
                    BufferedImage.TYPE_INT_RGB);
                Graphics2D biContext = bi.createGraphics();
                biContext.drawImage(image, 0, 0, null);
                double theta = Math.PI;
                AffineTransform at = AffineTransform.getRotateInstance(theta,
                    width / 2, height / 2);
                AffineTransformOp ato = new AffineTransformOp(at,
                    AffineTransformOp.TYPE_BILINEAR);
                BufferedImage bi2 = ato.createCompatibleDestImage(bi, null);
                bi2 = ato.filter(bi, bi2);
                bufferedInvertedImage = bi2;
            }
            image = bufferedInvertedImage;
        }
        g2.drawImage(image, rect.x, rect.y, rect.width, rect.height, this);
        if (isDead())
        {
            // Draw a triple-wide red X.
            g2.setStroke(threeWide);
            g2.setColor(Color.red);
            g2.drawLine(rect.x, rect.y, rect.x + rect.width, rect.y
                + rect.height);
            g2.drawLine(rect.x + rect.width, rect.y, rect.x, rect.y
                + rect.height);
            g2.setStroke(oneWide);
        }

        if (border)
        {
            g2.setColor(borderColor);
            Rectangle rect = getBounds();
            g.drawRect(rect.x, rect.y, rect.width - 1, rect.height - 1);
        }
    }

    @Override
    public void setLocation(Point point)
    {
        rect.setLocation(point);
        setBounds(rect);
    }

    @Override
    public boolean contains(Point point)
    {
        return rect.contains(point);
    }

    @Override
    public Rectangle getBounds()
    {
        return rect;
    }

    public Point getCenter()
    {
        return new Point(rect.x + rect.width / 2, rect.y + rect.height / 2);
    }

    @Override
    public Dimension getPreferredSize()
    {
        return new Dimension(rect.width, rect.height);
    }

    @Override
    public Dimension getMinimumSize()
    {
        return getPreferredSize();
    }

    @Override
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

    public boolean isInverted()
    {
        return inverted;
    }
}
