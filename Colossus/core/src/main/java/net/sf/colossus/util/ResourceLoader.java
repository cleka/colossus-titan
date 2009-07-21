package net.sf.colossus.util;


import java.util.logging.Logger;


public class ResourceLoader
{
    private static final Logger LOGGER = Logger.getLogger(ResourceLoader.class
        .getName());

    private final String host;
    private final int port;

    public ResourceLoader(String hostArg, int portArg)
    {
        this.host = hostArg;
        this.port = portArg;

        LOGGER.finest("ResourceLoader created for host " + host + " port "
            + port);

        StaticResourceLoader.setDataServer(host, port);

    }

    @Override
    public String toString()
    {
        return "ResourceLoader with DataServer " + host + ":" + port;
    }
}
