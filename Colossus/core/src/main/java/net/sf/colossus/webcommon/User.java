package net.sf.colossus.webcommon;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.util.Glob;


/**
 *  One user at the WebServer side.
 *  Also used on client side, because interface requires so, but
 *  basically only to store the username, everything else is unused.
 *
 *  @author Clemens Katzer
 */
public class User
{
    static final Logger LOGGER = Logger.getLogger(User.class.getName());

    private final static String TYPE_USER = "user";
    private final static String TYPE_ADMIN = "admin";

    private final static String USERLINE_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    private final static SimpleDateFormat userlineDateFormatter = new SimpleDateFormat(
        USERLINE_DATE_FORMAT);

    // We use the same separator as for Web Protocol also for UserLines in user file:
    private final static String SEP = IWebServer.WebProtocolSeparator;

    // At the moment conf-code is 3 numbers between 00 and 99:
    private final static int MAX_RANDOM = 99;

    public final static String PROVIDE_CONFCODE = "Provide the confirmation code";
    public final static String WRONG_CONFCODE = "Wrong confirmation code!";
    public final static String TEMPLATE_CONFCODE = "10 20 30";
    public final static String TEMPLATE_CONFCODE_REPLACEMENT = "11 21 31";

    private IWebClient webserverClient;

    private long id;
    private final String name;
    private String password;
    private String email;
    private boolean isAdmin;
    private String created;
    private String lastLogin;
    private String lastLogout;
    private long onlineSecs;
    private long sessionStarted = -1L;

    // Only needed during registration:
    private String lastSentConfirmationCode;

    private final List<String> ignoredUsers = new ArrayList<String>();

    public User(String name)
    {
        this.name = name;
    }

    public User(long id, String name, String password, String email,
        boolean isAdmin, String created, String lastLogin, String lastLogout,
        long onlineSecs)
    {
        this.id = id;
        this.name = name;
        this.password = password;
        this.email = email;
        this.isAdmin = isAdmin;

        if (created == null)
        {
            String now = makeUserlineDate(new Date().getTime());
            this.created = now;
            this.lastLogin = now;
            this.lastLogout = now;
            this.lastSentConfirmationCode = makeConfirmationCode();
            this.onlineSecs = 0;
        }
        else
        {
            this.created = created;
            this.lastLogin = lastLogin;
            this.lastLogout = lastLogout;
            this.lastSentConfirmationCode = "";
            this.onlineSecs = onlineSecs;
        }
    }

    public long getId()
    {
        return this.id;
    }

    public String getName()
    {
        return this.name;
    }

    public boolean isCorrectPassword(String providedPassword)
    {
        return providedPassword.equals(this.password);
    }

    public String getEmail()
    {
        return email;
    }

    public String getCreated()
    {
        return this.created;
    }

    public long getOnlineTime()
    {
        return this.onlineSecs;
    }

    // Make sure it is a 2 digit number, to avoid problems in
    // comparison between "56 5 12" and "56 05 12".
    private static long atLeast10(long original)
    {
        return (original < 10L ? original + 10L : original);
    }

    public static String makeConfirmationCode()
    {
        long n1 = atLeast10(Math.round((MAX_RANDOM * Math.random())));
        long n2 = atLeast10((new Date().getTime()) % MAX_RANDOM);
        long n3 = atLeast10(Math.round((MAX_RANDOM * Math.random())));

        String confCode = n1 + " " + n2 + " " + n3;
        // Do not let happen it to be exactly the template code
        // - the client GUI verifies that user has typed something different
        if (confCode.equals(TEMPLATE_CONFCODE))
        {
            confCode = TEMPLATE_CONFCODE_REPLACEMENT;
        }
        return confCode;
    }

    // Only used during while registration is pending.
    String getLastConfirmationCode()
    {
        return lastSentConfirmationCode;
    }

    public boolean isAdmin()
    {
        return isAdmin;
    }

    public void setIsAdmin(boolean val)
    {
        isAdmin = val;
    }

    public void setProperties(String pw, String email, Boolean isAdminObj)
    {
        if (pw != null)
        {
            password = pw;
        }

        if (email != null)
        {
            this.email = email;
        }

        if (isAdminObj != null)
        {
            isAdmin = isAdminObj.booleanValue();
        }
    }

    public IWebClient getWebserverClient()
    {
        return this.webserverClient;
    }

    public void setWebClient(IWebClient wsc)
    {
        this.webserverClient = wsc;
    }

    private String makeUserlineDate(long when)
    {
        Date whenDate = new Date(when);
        String whenString = userlineDateFormatter.format(whenDate);
        return whenString;
    }

    public void updateLastLogin()
    {
        sessionStarted = new Date().getTime();
        lastLogin = makeUserlineDate(sessionStarted);
    }

    public void updateLastLogout()
    {
        if (sessionStarted == -1)
        {
            LOGGER.info("sessionStarted already -1, skip updateLogout");
            return;
        }
        long sessionEnded = new Date().getTime();
        long duration = (sessionEnded - sessionStarted) / 1000;
        if (duration >= 0 && sessionStarted > 0)
        {
            onlineSecs += duration;
            LOGGER.info("User " + name + " was " + duration
                + " seconds online, total now " + onlineSecs);
        }
        else
        {
            LOGGER.warning("SessionDuration or sessionStarted for user "
                + name + " negative? Started=" + sessionStarted + ", Ended="
                + sessionEnded + ", duration=" + duration
                + ", previously onlineSecs=" + onlineSecs);
        }
        lastLogout = makeUserlineDate(sessionEnded);
        sessionStarted = -1;
    }

    public static User makeUserFromUserLine(String line)
    {
        List<String> tokens = new LinkedList<String>();
        tokens.addAll(Arrays.asList(line.split(SEP)));
        if (tokens.size() != 9)
        {
            User.LOGGER.log(Level.WARNING, "invalid line '" + line
                + "' in user file!");
            return null;
        }

        long id = Long.parseLong(tokens.remove(0));
        String name = tokens.remove(0).trim();
        String password = tokens.remove(0).trim();
        String email = tokens.remove(0).trim();
        String type = tokens.remove(0).trim();
        String created = tokens.remove(0).trim();
        String lastLogin = tokens.remove(0).trim();
        String lastLogout = tokens.remove(0).trim();
        long onlineSecs = Long.parseLong(tokens.remove(0));

        boolean isAdmin = false;
        if (type.equals(User.TYPE_ADMIN))
        {
            isAdmin = true;
        }
        else if (type.equals(User.TYPE_USER))
        {
            isAdmin = false;
        }
        else
        {
            User.LOGGER.log(Level.WARNING, "invalid type '" + type
                + "' in user file line '" + line + "'");
        }
        User u = new User(id, name, password, email, isAdmin, created,
            lastLogin, lastLogout, onlineSecs);
        return u;
    }

    public String makeLine()
    {
        String type = (isAdmin ? TYPE_ADMIN : TYPE_USER);

        String line = this.id + SEP + this.name + SEP + password + SEP + email
            + SEP + type + SEP + created + SEP + lastLogin + SEP + lastLogout
            + SEP + onlineSecs;
        return line;
    }

    public List<String> getListOfIgnoredUsers()
    {
        List<String> list = new ArrayList<String>(ignoredUsers);
        return list;
    }

    public boolean isUserInIgnoredList(String username)
    {
        return ignoredUsers.contains(username);
    }

    public void addToIgnoredUsers(String username)
    {
        if (!isUserInIgnoredList(username))
        {
            ignoredUsers.add(username);
        }
        LOGGER.finest("addToIgnore: "
            + Glob.glob(":", getListOfIgnoredUsers()));
    }

    public void removeFromIgnoredUsers(String username)
    {
        ignoredUsers.remove(username);
        LOGGER.finest("removeFromToIgnore: "
            + Glob.glob(":", getListOfIgnoredUsers()));
    }

}
