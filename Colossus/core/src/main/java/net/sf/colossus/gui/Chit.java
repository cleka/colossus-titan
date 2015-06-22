package net.sf.colossus.gui;


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

import net.sf.colossus.client.Client;
import net.sf.colossus.common.IOptions;
import net.sf.colossus.common.Options;
import net.sf.colossus.game.PlayerColor;
import net.sf.colossus.server.VariantSupport;
import net.sf.colossus.util.StaticResourceLoader;
import net.sf.colossus.variant.CreatureType;


/**
 * Class Chit implements the GUI for a Titan chit representing
 * either a character or a legion.
 *
 * TODO offer constructors using the Legion instead of strings
 * TODO consider splitting into LegionChit and CreatureChit
 * TODO Important: All creature (and marker) related Chits should get an
 * option argument in order to be able to ask for options like
 * "marker in players original color or now-owning-player's-color
 * and "angel in traditionl blue or in actual players color".
 *
 * @author David Ripton
 * @author Romain Dolbeau
 */

class Chit extends JPanel
{
    private static final Logger LOGGER = Logger
        .getLogger(Chit.class.getName());

    private final Image bufferedImage;
    private Image bufferedInvertedImage;
    Rectangle rect;
    final Client client; // may be null; set for some subclasses
    final IOptions options;

    /** Flag to mark chit as dead and paint it with an "X" through it. */
    private boolean dead;

    /** Flag to paint a border around the chit. */
    private boolean border = true;
    private Color borderColor = Color.black;

    private final boolean playerColoredAngel;

    /** Flag to paint the chit upside-down. */
    protected final boolean inverted;

    // Initialize early to avoid NullPointerException with GTK L&F
    private final String id;

    final static BasicStroke oneWide = new BasicStroke(1);
    private final static BasicStroke threeWide = new BasicStroke(3);

    /**
     * Factory method for creatures, based on image names
     * TODO try to get rid of the string bases Creature chits
     * @param scale
     * @param id
     * @return The newly created CreatureChit
     */
    public static Chit newCreatureChit(int scale, String id)
    {
        return new Chit(scale, id);
    }

    /**
     * Factory method for creatures, based on CreatureType
     * @param scale
     * @param type
     * @return The newly created CreatureChit
     */
    public static Chit newCreatureChit(int scale, CreatureType type)
    {
        return new Chit(scale, type);
    }

    /**
     * Factory method for creatures, based on markerId
     * @param scale
     * @param markerId
     * @return The newly created MarkerChit
     */
    public static Chit newDiceChit(int scale, String markerId)
    {
        return new Chit(scale, markerId);
    }

    /**
     * Factory method for icons representing e.g. hazard effects
     * @param scale
     * @param id
     * @return the created Chit
     */
    public static Chit newSymbolChit(int scale, String id)
    {
        return new Chit(scale, id);
    }

    /**
     *           P l a i n   C o n s t r u c t o r s
     *
     **/
    // to be used mostly by the factory methods
    Chit(int scale, String id)
    {
        this(scale, id, false, false);
    }

    Chit(int scale, CreatureType creatureType)
    {
        this(scale, creatureType.getName());
    }

    // No client, no options: use only for TerrainEffect symbol or dice!
    Chit(int scale, String id, String[] overlays)
    {
        this(scale, id, false, false, false, overlays, null, null);
    }

    Chit(int scale, String id, boolean inverted, Client client)
    {
        this(scale, id, inverted, false, false, client);
    }

    Chit(int scale, String id, boolean inverted, boolean dubious)
    {
        this(scale, id, inverted, dubious, false, null);
    }

    Chit(int scale, String id, boolean inverted, boolean dubious,
        boolean dubiousAsBlank, Client client)
    {
        this(scale, id, inverted, dubious, dubiousAsBlank, null, client,
            client != null ? client.getOptions() : null);
    }

    // TODO this is a bit confusing: the id parameter can be either the name of
    // a creature type or a markerId (maybe more? => Or a symbol for hazard effect,
    // or some dice...).
    // Good thing markerIds have no overlap with creature names
    //  (should perhaps use Marker instead plain Chit there?)
    /**
     * @param idPerhapsWithColor CreatureType id, markerId, or filename of
     * some picture denoting some symbol (for HazardEffects).
     * For Markers, Titans and Angels could be of form <id>-<color> and then
     * they will be painted in that color (e.g. captured markers)
     * @param options TODO
     */
    private Chit(int scale, String idPerhapsWithColor, boolean inverted,
        boolean dubious, boolean dubiousAsBlank, String[] overlays,
        Client client, IOptions options)
    {
        // LayoutManager null - we want to place things ourselves
        super((LayoutManager)null);

        assert idPerhapsWithColor != null : "Each chit must have an ID set";
        if (isMarkerId(idPerhapsWithColor))
        {
            this.id = idPerhapsWithColor.substring(0, 4);
        }
        else
        {
            this.id = idPerhapsWithColor;
        }
        this.inverted = inverted;
        this.options = options;
        // should not be needed any more
        this.client = client;

        // TODO don't get that yet. Refactor all the boolean options
        // to get them via an IClient or something?
        this.playerColoredAngel = true;

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
        else if (VariantSupport.getCurrentVariant().isCreature(
            idPerhapsWithColor))
        {
            CreatureType cre = VariantSupport.getCurrentVariant()
                .getCreatureByName(idPerhapsWithColor);
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
            if (idPerhapsWithColor.startsWith("Titan-"))
            {
                String[] filenames = new String[4 + (dubious ? 1 : 0)];
                int power = getTitanPower();
                String color = idPerhapsWithColor.split("-")[2] + "Colossus";
                filenames[0] = "Plain" + "-" + color;
                filenames[1] = "TitanMask";
                filenames[2] = "Power-" + power + "-" + color;
                int skill = (VariantSupport.getCurrentVariant()
                    .getCreatureByName("Titan")).getSkill();
                filenames[3] = "Skill-" + skill + "-" + color;

                if (dubious)
                {
                    filenames[4] = "QuestionMarkMask"
                        + (color.equals("BlackColossus") ? "Red" : "");
                }

                bufferedImage = getImage(filenames, scale);
            }
            else if (idPerhapsWithColor.startsWith("Angel-"))
            {
                String[] filenames = new String[5 + (dubious ? 1 : 0)];
                int power = (VariantSupport.getCurrentVariant()
                    .getCreatureByName("Angel")).getPower();
                String color = playerColoredAngel ? (idPerhapsWithColor
                    .split("-")[2] + "Colossus") : "giantBlue";

                filenames[0] = "Plain" + "-" + color;
                filenames[1] = "AngelMask";
                filenames[2] = "Power-" + power + "-" + color;
                int skill = (VariantSupport.getCurrentVariant()
                    .getCreatureByName("Angel")).getSkill();
                filenames[3] = "Skill-" + skill + "-" + color;
                filenames[4] = "Angel-Name-" + color;

                if (dubious)
                {
                    filenames[5] = "QuestionMarkMask"
                        + (color.equals("BlackColossus") ? "Red" : "");
                }

                bufferedImage = getImage(filenames, scale);
            }
            else if (isMarkerId(idPerhapsWithColor))
            {
                // Legion marker
                // May be of form "Bk03" or form "Bk03-Black"
                String[] filenames = new String[2];
                String colorName;
                if (idPerhapsWithColor.length() >= 5)
                {
                    colorName = idPerhapsWithColor.split("-")[1];
                }
                else
                {
                    String shortColor = idPerhapsWithColor.substring(0, 2);
                    PlayerColor playerColor = PlayerColor
                        .getByShortName(shortColor);
                    colorName = playerColor.getName();
                }
                filenames[0] = "Plain" + "-" + colorName + "Colossus";
                filenames[1] = idPerhapsWithColor.substring(0, 4);
                bufferedImage = getImage(filenames, scale);
            }
            else
            {
                if (overlays == null)
                {
                    bufferedImage = getImage(idPerhapsWithColor, scale);
                }
                else
                {
                    String[] filenames = new String[overlays.length + 1];
                    filenames[0] = idPerhapsWithColor;
                    for (int i = 0; i < overlays.length; i++)
                    {
                        filenames[i + 1] = overlays[i];
                    }
                    bufferedImage = getImage(filenames, scale);
                }
            }
        }
    }

    public static boolean isMarkerId(String id)
    {
        return (id.length() >= 4 && id.charAt(0) >= 'A' && id.charAt(0) <= 'Z'
            && id.charAt(1) >= 'a' && id.charAt(1) <= 'z'
            && id.charAt(2) >= '0' && id.charAt(2) <= '1'
            && id.charAt(3) >= '0' && id.charAt(3) <= '9');
    }

    public int getTitanPower()
    {
        if (!id.startsWith("Titan-"))
        {
            return -1;
        }
        String[] parts = id.split("-");
        int power = Integer.parseInt(parts[1]);
        return power;
    }

    private static Image getImage(String imageFilename, int scale)
    {
        ImageIcon tempIcon = null;
        List<String> directories = VariantSupport.getImagesDirectoriesList();
        tempIcon = StaticResourceLoader.getImageIcon(imageFilename,
            directories, scale, scale);
        if (tempIcon == null)
        {
            LOGGER.log(Level.SEVERE, "Couldn't get image :" + imageFilename);
            throw new RuntimeException(
                "Unable to retrieve image for filename '" + imageFilename
                    + "'");
        }

        return tempIcon.getImage();
    }

    private static Image getImage(String[] imageFilenames, int scale)
    {
        List<String> directories = VariantSupport.getImagesDirectoriesList();
        Image composite = StaticResourceLoader.getCompositeImage(
            imageFilenames, directories, scale, scale);
        return composite;
    }

    // TODO should become package private again one day.
    // see isDead()
    public String getId()
    {
        if (id == null)
        {
            // this should never happen, since id is initialized
            // already from beginning on, still someone gets a NPE
            // just for this id; perhaps due to using GTK L&F ?
            LOGGER.log(Level.SEVERE, "Chit id is still null ?");
            //            id = "<notdefined?>";
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

        // TODO options == null is just a quick hack to prevent NPEs in cases
        // there would not be an options object.
        if (inverted
            && (options == null || !options
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
                    width / 2.0, height / 2.0);
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

}
