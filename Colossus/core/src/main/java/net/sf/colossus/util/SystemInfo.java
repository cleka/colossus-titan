package net.sf.colossus.util;



/**
 * Helper class to provide information about operating system
 * and Java version (and provider).
 */
public class SystemInfo
{

    private static String osName;
    private static String osVersion;

    private static String runtimeName;
    private static String vmName;
    private static String javaVersion;

    static
    {
        osName = System.getProperty("os.name", "unknown OS");
        osVersion = System.getProperty("os.version", "unknown version");

        runtimeName = System.getProperty("java.runtime.name", "unknown");
        vmName = System.getProperty("java.vm.name", "unknown");
        javaVersion = System.getProperty("java.version", "unknown");
    }

    public static String getOsInfo()
    {
        return osName + " " + osVersion;
    }

    public static String getFullJavaInfo()
    {
        return runtimeName + "/" + vmName + " " + javaVersion;
    }

    public static String getDisplayJavaInfo()
    {
        return runtimeName + " " + javaVersion;
    }

    public static boolean isOracleJava7()
    {
        if (javaVersion.startsWith("1.7.0")
            && (runtimeName.startsWith("Java(TM)") || vmName.startsWith("Java HotSpot(TM)")))
        {
            return true;
        }
        else
        {
            return false;
        }
    }
}
