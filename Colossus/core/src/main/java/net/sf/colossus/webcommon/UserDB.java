package net.sf.colossus.webcommon;


import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.colossus.webserver.WebServerClient;


public class UserDB
{
    private static final Logger LOGGER = Logger.getLogger(UserDB.class
        .getName());

    private final int maxUsers;

    private final String usersFile;

    private final HashMap<String, User> userMap = new HashMap<String, User>();
    private final HashMap<String, User> loggedInUserMap = new HashMap<String, User>();

    private final HashMap<String, User> pendingRegistrations = new HashMap<String, User>();

    private long highestExistingId;

    public UserDB(String filename, int maxUsersVal)
    {
        maxUsers = maxUsersVal;
        usersFile = filename;
        if (usersFile != null)
        {
            readUsersFromFile();
        }
    }

    public int getUserCount()
    {
        synchronized (userMap)
        {
            return userMap.size();
        }
    }

    public void updateLoggedinStatus(User u, WebServerClient wsc)
    {
        String username = u.getName();
        synchronized (loggedInUserMap)
        {
            if (wsc == null)
            {
                if (loggedInUserMap.containsKey(username))
                {
                    loggedInUserMap.remove(username);
                }
            }
            else
            {
                loggedInUserMap.put(username, u);
            }
        }

    }

    public boolean isUserOnline(User u)
    {
        synchronized (loggedInUserMap)
        {
            return loggedInUserMap.containsKey(u.getName());
        }
    }

    public Collection<User> getLoggedInUsers()
    {
        synchronized (loggedInUserMap)
        {
            Collection<User> c = new LinkedList<User>();
            c.addAll(loggedInUserMap.values());
            return c;
        }
    }

    public User findUserByName(String name)
    {
        synchronized (userMap)
        {
            String nameAllLower = name.toLowerCase();
            return userMap.get(nameAllLower);
        }
    }

    public String getLoggedInNamesAsString(String useSeparator)
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

    // still dummy
    public int getDeadCount()
    {
        return 0;
    }

    // still dummy
    public int getEnrolledCount()
    {
        return 0;
    }

    // still dummy
    public int getPlayingCount()
    {
        return 0;
    }

    public int getLoggedInCount()
    {
        synchronized (loggedInUserMap)
        {
            return loggedInUserMap.size();
        }
    }

    public Collection<User> getAllUsers()
    {
        synchronized (userMap)
        {
            Collection<User> c = new LinkedList<User>();
            c.addAll(userMap.values());
            return c;
        }
    }

    /**
     * Given a username and password, verifies that the user
     * is allowed to login with that password.
     * @param username
     * @param password
     * @return reasonLoginFailed (String), null if login ok
     **/
    public String verifyLogin(String username, String password)
    {
        String reasonLoginFailed = null;

        User user = findUserByName(username);

        if (user == null)
        {
            reasonLoginFailed = "Invalid username";
        }
        else if (password != null && user.isCorrectPassword(password))
        {
            // ok, return null to indicate all is fine
        }
        else
        {
            reasonLoginFailed = "Invalid username/password";
        }

        return reasonLoginFailed;
    }

    public String registerUser(String username, String password, String email,
        IColossusMail mailObject)
    {
        boolean isAdmin = false;

        String usernameAllLc = username;
        usernameAllLc = usernameAllLc.toLowerCase();
        if (usernameAllLc.startsWith("guest")
            || usernameAllLc.startsWith("anonym"))
        {
            String problem = "Usernames starting with 'guest' or 'anonym' "
                + " are reserved for special purposes!";
            return problem;
        }
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
            highestExistingId++;
            User u = new User(highestExistingId, username, password, email,
                isAdmin, null, null, null, 0);
            String cCode = u.getLastConfirmationCode();
            User.LOGGER.fine("Confirmation code for user " + username
                + " is: " + cCode);

            String reason = sendConfirmationMail(username, email, cCode,
                mailObject);
            if (reason != null)
            {
                // mail sending failed, for some reason. Let user know it.
                return reason;
            }

            pendingRegistrations.put(username, u);
            // so far everything fine. Now client shall request the conf. code

            reason = User.PROVIDE_CONFCODE;
            return reason;
        }
    }

    public String sendConfirmationMail(String username, String email,
        String confCode, IColossusMail mailObject)
    {
        // this is in webcommon package:
        return mailObject.sendConfirmationMail(username, email, confCode);
    }

    public String confirmRegistration(String username, String confirmationCode)
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
        User.LOGGER.fine(msg);

        reason = confirmIfCorrectCode(username, confirmationCode);
        return reason;
    }

    private String confirmIfCorrectCode(String username,
        String tryConfirmationCode)
    {
        User u = pendingRegistrations.get(username);
        if (u == null)
        {
            return "No confirmation pending for this username";
        }

        if (!u.getLastConfirmationCode().equals(tryConfirmationCode))
        {
            return User.WRONG_CONFCODE;
        }

        User.LOGGER.info("Registration confirmed for user '" + username
            + "', email '" + u.getEmail() + "'.");

        pendingRegistrations.remove(username);
        storeUser(u);
        storeUsersToFile();

        return null;
    }

    public String changeProperties(String username, String oldPW,
        String newPW, String email, Boolean isAdmin)
    {
        String reason;

        User u = findUserByName(username);
        if (u == null)
        {
            reason = "User does not exist";
            return reason;
        }
        else if ((reason = verifyLogin(username, oldPW)) != null)
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

    private void readUsersFromFile()
    {
        long maxId = 0;
        try
        {
            BufferedReader users = new BufferedReader(new InputStreamReader(
                new FileInputStream(usersFile)));

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
                    User u = User.makeUserFromUserLine(line);
                    if (u != null)
                    {
                        storeUser(u);
                        if (u.getId() > maxId)
                        {
                            maxId = u.getId();
                        }
                    }
                }
            }
            this.highestExistingId = maxId;
            users.close();
        }
        catch (FileNotFoundException e)
        {
            LOGGER.log(Level.SEVERE,
                "Users file " + usersFile + " not found!", e);
            System.exit(1);
        }
        catch (IOException e)
        {
            LOGGER.log(Level.SEVERE, "IOException while reading users file "
                + usersFile + "!", e);
            System.exit(1);
        }
    }

    public void storeUsersToFile()
    {
        if (usersFile == null)
        {
            User.LOGGER.log(Level.SEVERE, "UsersFile name is null!");
            System.exit(1);
        }

        // LOGGER.log(Level.FINE, "Storing users back to file " + filename);

        PrintWriter out = null;
        try
        {
            out = new PrintWriter(new FileOutputStream(usersFile));

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
            User.LOGGER.log(Level.SEVERE, "Writing users file " + usersFile
                + "failed: FileNotFoundException: ", e);
            System.exit(1);
        }
    }

    private void storeUser(User u)
    {
        String name = u.getName();
        String nameAllLower = name.toLowerCase();
        synchronized (userMap)
        {
            userMap.put(nameAllLower, u);
        }
    }

    public void cleanup()
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
