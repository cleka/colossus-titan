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

    private static final Properties BUILD_PROPERTIES = new Properties();
    static
    {
        // first set some defaults, they get replaced if loading succeeds
        BUILD_PROPERTIES.setProperty("release.version", "unknown");
        BUILD_PROPERTIES.setProperty("git.commit", "unknown");
        BUILD_PROPERTIES.setProperty("build.timestamp", "unknown");
        BUILD_PROPERTIES.setProperty("username", "unknown");
        ClassLoader cl = BuildInfo.class.getClassLoader();
        InputStream is = cl.getResourceAsStream("META-INF/build.properties");
        if (is != null)
        {
            try
            {
                BUILD_PROPERTIES.load(is);
            }
            catch (IOException e)
            {
                LOGGER.log(Level.WARNING, "Failed to load build properties.",
                    e);
            }
        }
    }

    public static String getBuildInfo(boolean full)
    {
        String revInfo = BUILD_PROPERTIES.getProperty("git.commit");
        if (revInfo.length() > 10)
        {
            revInfo = revInfo.substring(0, 11) + "...";
        }
        String timeStamp = BUILD_PROPERTIES.getProperty("build.timestamp");
        String byUser = BUILD_PROPERTIES.getProperty("username");

        String buildInfoString;
        if (full)
        {
            buildInfoString = timeStamp + " by " + byUser + " from commit "
                + revInfo;
        }
        else
        {
            buildInfoString = "commit " + revInfo;
        }
        return buildInfoString;
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

    /**
     * Retrieves the version of Colossus we are running.
     *
     * This returns either a version number for an official release or
     * "SNAPSHOT" otherwise.
     *
     * @return The release version of the Colossus instance
     */
    public static String getReleaseVersion()
    {
        return BUILD_PROPERTIES.getProperty("release.version");
    }
}
