package net.sf.colossus.webserver;


import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *  WebServer specific version of the Options / cf file handling.
 *
 *  TODO: why did I make a copy, instead of using the normal Options class?
 *
 *  @author Clemens Katzer
 */
public class WebServerOptions
{
    private static final Logger LOGGER = Logger
        .getLogger(WebServerOptions.class.getName());

    private final Properties props = new Properties();
    private final String filename;

    public WebServerOptions(String filename)
    {
        this.filename = filename;
    }

    public void loadOptions()
    {
        try
        {
            FileInputStream in = new FileInputStream(filename);
            props.load(in);
        }
        catch (IOException e)
        {
            LOGGER.log(Level.SEVERE, "Couldn't read options from " + filename,
                e);
            return;
        }
    }

    public void setOption(String optname, String value)
    {
        props.setProperty(optname, value);
    }

    public void setOption(String optname, boolean value)
    {
        setOption(optname, String.valueOf(value));
    }

    public void setOption(String optname, int value)
    {
        setOption(optname, String.valueOf(value));
    }

    public String getStringOption(String optname)
    {
        String value = props.getProperty(optname);
        return value;
    }

    public boolean getOption(String optname)
    {
        String value = getStringOption(optname);
        return (value != null && value.equals("true"));
    }

    /** Return -1 if the option's value has not been set. */
    public int getIntOption(String optname)
    {
        String buf = getStringOption(optname);
        int value = -1;
        try
        {
            value = Integer.parseInt(buf);
        }
        catch (NumberFormatException ex)
        {
            value = -1;
        }
        return value;
    }

    public int getIntOptionNoUndef(String optname)
    {
        int val = getIntOption(optname);
        if (val == -1)
        {
            LOGGER.log(Level.SEVERE, "Invalid or not set value for " + optname
                + " from WebServer config file " + filename);
            System.exit(1);
        }
        return val;
    }

    public void removeOption(String optname)
    {
        props.remove(optname);
    }
}
