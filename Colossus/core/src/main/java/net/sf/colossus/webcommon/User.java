package net.sf.colossus.webcommon;


import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *  One user at the WebServer side.
 *  Also used on client side, because interface requires so, but
 *  basically only to store the username, everything else is unused.
 *
 *  The class statically contains a list of all user registered at the
 *  Public Game Server; this list is read from a file (later a DB??)
 *  into a HashMap to quickly look up all users.
 *
 *  @author Clemens Katzer
 */
public class User
{
    private static final Logger LOGGER = Logger
        .getLogger(User.class.getName());

    private static HashMap<String, User> userMap = new HashMap<String, User>();
    private static HashMap<String, User> loggedInUserMap = new HashMap<String, User>();

    // Use the same sepatator as for Web Protocol also for UserLines in user file:
    private final static String ulSep = IWebServer.WebProtocolSeparator;

    private static String usersFile = null;

    private final static String typeUser = "user";
    private final static String typeAdmin = "admin";

    private static int maxUsers;

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

    private static final HashMap<String, User> pendingRegistrations = new HashMap<String, User>();

    private IWebClient webserverClient;
    private static final int MAX_RANDOM = 99;

    public final static String PROVIDE_CONFCODE = "Provide the confirmation code";
    public final static String WRONG_CONFCODE = "Wrong confirmation code!";
    public final static String TEMPLATE_CONFCODE = "10 20 30";
    public final static String TEMPLATE_CONFCODE_REPLACEMENT = "11 21 31";

    public User(String name)
    {
        this.name = name;
    }

    private User(String name, String password, String email, boolean isAdmin,
        String created, String lastLogin, String lastLogout, long onlineSecs)
    {
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

    public String getName()
    {
        return this.name;
    }

    public String getEmail()
    {
        return email;
    }

    // Only used during while registration is pending.
    private String getLastConfirmationCode()
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
        synchronized (loggedInUserMap)
        {
            if (wsc == null)
            {
                if (loggedInUserMap.containsKey(this.name))
                {
                    loggedInUserMap.remove(this.name);
                }
            }
            else
            {
                loggedInUserMap.put(this.name, this);
            }
            this.webserverClient = wsc;
        }
    }

    /**
     * Given a username and password, verifies that the user
     * is allowed to login with that password.
     * @param username
     * @param password
     * @return reasonLoginFailed (String), null if login ok
     **/
    public static String verifyLogin(String username, String password)
    {
        String reasonLoginFailed = null;

        User user = findUserByName(username);

        if (user == null)
        {
            reasonLoginFailed = "Invalid username";
        }
        else if (password != null && password.equals(user.password)
            && username.equals(user.name))
        {
            // ok, return null to indicate all is fine
        }
        else
        {
            reasonLoginFailed = "Invalid username/password";
        }

        return reasonLoginFailed;
    }

    public static void storeUser(User u)
    {
        String name = u.getName();
        String nameAllLower = name.toLowerCase();
        synchronized (userMap)
        {
            userMap.put(nameAllLower, u);
        }
    }

    public static Collection<User> getAllUsers()
    {
        synchronized (userMap)
        {
            Collection<User> c = new LinkedList<User>();
            c.addAll(userMap.values());
            return c;
        }
    }

    public static int getUserCount()
    {
        synchronized (userMap)
        {
            return userMap.size();
        }
    }

    public static User findUserByName(String name)
    {
        synchronized (userMap)
        {
            String nameAllLower = name.toLowerCase();
            return userMap.get(nameAllLower);
        }
    }

    public static boolean isUserOnline(User u)
    {
        synchronized (loggedInUserMap)
        {
            return loggedInUserMap.containsKey(u.getName());
        }
    }

    public static Collection<User> getLoggedInUsers()
    {
        synchronized (loggedInUserMap)
        {
            Collection<User> c = new LinkedList<User>();
            c.addAll(loggedInUserMap.values());
            return c;
        }
    }

    public static String getLoggedInNamesAsString(String useSeparator)
    {
        String names = "<none>";
        String separator = "";

        synchronized (loggedInUserMap)
        {
            if (!loggedInUserMap.isEmpty())
            {
                StringBuilder list = new StringBuilder("");
                for (String key : loggedInUserMap.keySet())
                {
                    list.append(separator);
                    list.append(key);
                    separator = useSeparator;
                }
                names = list.toString();
            }
        }
        return names;
    }

    public static int getLoggedInCount()
    {
        synchronized (loggedInUserMap)
        {
            return loggedInUserMap.size();
        }
    }

    // still dummy
    public static int getEnrolledCount()
    {
        return 0;
    }

    // still dummy
    public static int getPlayingCount()
    {
        return 0;
    }

    // still dummy
    public static int getDeadCount()
    {
        return 0;
    }

    public static String registerUser(String username, String password,
        String email, IColossusMail mailObject)
    {
        boolean isAdmin = false;
        User alreadyExisting = findUserByName(username);
        if (alreadyExisting != null)
        {
            String problem = "User " + username + " does already exist.";
            return problem;
        }
        else if (getUserCount() >= maxUsers)
        {
            String problem = "Maximum number of accounts )" + maxUsers
                + ") reached - no more registrations possible,"
                + " until some administrator checks the situation.";
            return problem;
        }
        else
        {
            User u = new User(username, password, email, isAdmin, null, null,
                null, 0);
            String cCode = u.getLastConfirmationCode();
            LOGGER.fine("Confirmation code for user " + username + " is: "
                + cCode);

            String reason = sendConfirmationMail(username, email, cCode,
                mailObject);
            if (reason != null)
            {
                // mail sending failed, for some reason. Let user know it.
                return reason;
            }

            pendingRegistrations.put(username, u);
            // so far everything fine. Now client shall request the conf. code

            reason = PROVIDE_CONFCODE;
            return reason;
        }
    }

    public static String sendConfirmationMail(String username, String email,
        String confCode, IColossusMail mailObject)
    {
        // this is in webcommon package:
        return mailObject.sendConfirmationMail(username, email, confCode);
    }

    // Make sure it is a 2 digit number, to avoid problems in
    // comparison between "56 5 12" and "56 05 12".
    private static long atLeast10(long original)
    {
        return (original < 10L ? original + 10L : original);
    }

    private static String makeConfirmationCode()
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

    public static String confirmRegistration(String username,
        String confirmationCode)
    {
        String reason = "";
        if (confirmationCode == null || confirmationCode.equals("null")
            || confirmationCode.equals(""))
        {
            reason = "Missing confirmation code";
            return reason;
        }

        String msg = "User " + username + " attempts confirmation "
            + "with code '" + confirmationCode + "'.";
        LOGGER.fine(msg);

        reason = confirmIfCorrectCode(username, confirmationCode);
        return reason;
    }

    private static String confirmIfCorrectCode(String username,
        String tryConfirmationCode)
    {
        User u = pendingRegistrations.get(username);
        if (u == null)
        {
            return "No confirmation pending for this username";
        }

        if (!u.getLastConfirmationCode().equals(tryConfirmationCode))
        {
            return WRONG_CONFCODE;
        }

        LOGGER.info("Registration confirmed for user '" + username
            + "', email '" + u.getEmail() + "'.");

        pendingRegistrations.remove(username);
        storeUser(u);
        storeUsersToFile();

        return null;
    }

    public static String changeProperties(String username, String oldPW,
        String newPW, String email, Boolean isAdmin)
    {
        String reason;

        User u = findUserByName(username);
        if (u == null)
        {
            reason = "User does not exist";
            return reason;
        }
        else if ((reason = User.verifyLogin(username, oldPW)) != null)
        {
            return reason;
        }
        else
        {
            u.setProperties(newPW, email, isAdmin);
            storeUsersToFile();
            return null;
        }
    }

    public static final String USERLINE_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    private static SimpleDateFormat userlineDateFormatter = new SimpleDateFormat(
        USERLINE_DATE_FORMAT);

    private static String makeUserlineDate(long when)
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

    public static void parseUserLine(String line)
    {
        String[] tokens = line.split(ulSep);
        if (tokens.length == 6)
        {
            String newLine = line + ulSep + tokens[5] + ulSep + "0";
            tokens = newLine.split(ulSep);
        }
        if (tokens.length != 8)
        {
            LOGGER.log(Level.WARNING, "invalid line '" + line
                + "' in user file!");
            return;
        }
        String name = tokens[0].trim();
        String password = tokens[1].trim();
        String email = tokens[2].trim();
        String type = tokens[3].trim();
        String created = tokens[4].trim();
        String lastLogin = tokens[5].trim();
        String lastLogout = tokens[6].trim();
        long onlineSecs = Long.parseLong(tokens[7]);

        boolean isAdmin = false;
        if (type.equals(typeAdmin))
        {
            isAdmin = true;
        }
        else if (type.equals(typeUser))
        {
            isAdmin = false;
        }
        else
        {
            LOGGER.log(Level.WARNING, "invalid type '" + type
                + "' in user file line '" + line + "'");
        }
        User u = new User(name, password, email, isAdmin, created, lastLogin,
            lastLogout, onlineSecs);
        storeUser(u);
    }

    public static void readUsersFromFile(String filename, int maxUsersVal)
    {
        usersFile = filename;
        maxUsers = maxUsersVal;

        try
        {
            BufferedReader users = new BufferedReader(new InputStreamReader(
                new FileInputStream(filename)));

            String line = null;
            while ((line = users.readLine()) != null)
            {
                if (line.startsWith("#"))
                {
                    // ignore comment line
                }
                else if (line.matches("\\s*"))
                {
                    // ignore empty line
                }
                else
                {
                    parseUserLine(line);
                }
            }
            users.close();
        }
        catch (FileNotFoundException e)
        {
            LOGGER.log(Level.SEVERE, "Users file " + filename + " not found!",
                e);
            System.exit(1);
        }
        catch (IOException e)
        {
            LOGGER.log(Level.SEVERE, "IOException while reading users file "
                + filename + "!", e);
            System.exit(1);
        }
    }

    public String makeLine()
    {
        String type = (isAdmin ? typeAdmin : typeUser);

        String line = this.name + ulSep + password + ulSep + email + ulSep
            + type + ulSep + created + ulSep + lastLogin + ulSep + lastLogout
            + ulSep + onlineSecs;
        return line;
    }

    public static void storeUsersToFile()
    {
        String filename = usersFile;

        if (usersFile == null)
        {
            LOGGER.log(Level.SEVERE, "UsersFile name is null!");
            System.exit(1);
        }

        // LOGGER.log(Level.FINE, "Storing users back to file " + filename);

        PrintWriter out = null;
        try
        {
            out = new PrintWriter(new FileOutputStream(filename));

            Collection<User> users = new LinkedList<User>();
            users.addAll(getAllUsers());

            for (User user : users)
            {
                String line = user.makeLine();
                out.println(line);
            }
            out.close();
        }
        catch (FileNotFoundException e)
        {
            LOGGER.log(Level.SEVERE, "Writing users file " + filename
                + "failed: FileNotFoundException: ", e);
            System.exit(1);
        }
    }

    public static void cleanup()
    {
        synchronized (userMap)
        {
            userMap.clear();
        }
        synchronized (loggedInUserMap)
        {
            loggedInUserMap.clear();
        }
    }
}
