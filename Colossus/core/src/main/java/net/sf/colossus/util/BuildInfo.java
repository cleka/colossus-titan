package net.sf.colossus.util;


import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Helper class to retrieve the Build information from build.properties file
 */
public class BuildInfo
{
    private static final Logger LOGGER = Logger.getLogger(BuildInfo.class
        .getName());

    public static String getBuildInfo(boolean full)
    {
        try
        {
            Properties buildInfo = new Properties();
            ClassLoader cl = BuildInfo.class.getClassLoader();
            InputStream is = cl
                .getResourceAsStream("META-INF/build.properties");
            if (is == null)
            {
                LOGGER.log(Level.INFO, "No build information available.");
                return "UNKNOWN";
            }
            buildInfo.load(is);
            String revInfo = buildInfo
                .getProperty("svn.revision.max-with-flags");
            String timeStamp = buildInfo.getProperty("build.timestamp");
            String byUser = buildInfo.getProperty("username");

            String buildInfoString;
            if (full)
            {
                buildInfoString = "Built " + timeStamp + " by " + byUser
                    + " from revision " + revInfo;
            }
            else
            {
                buildInfoString = revInfo;
            }
            return buildInfoString;
        }
        catch (IOException ex)
        {
            LOGGER.log(Level.WARNING, "Problem reading build info file", ex);
        }
        return "UNKNOWN";
    }

    /**
     *  Get an info string describing the current build:
     *  Build time stamp, username and revision number
     *  (revision number may contain a charactor indicating that the sources
     *  were modified before compilation)
     *  @return The long/full build info string
     */
    public static String getFullBuildInfoString()
    {
        return getBuildInfo(true);
    }

    /**
     *  Get the string describing the pure revision info
     *  (revision number, plus perhaps a character indicating sources
     *  were modified before compilation).
     *  @return The revision information string
     */
    public static String getRevisionInfoString()
    {
        return getBuildInfo(false);
    }

}
