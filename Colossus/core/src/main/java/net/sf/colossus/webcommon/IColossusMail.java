package net.sf.colossus.webcommon;


/**
 *  Describes the interface how the Game Server sends a mail.
 *
 *  So far the only use case is to send a mail with confirmation code
 *  to complete a registration.
 *
 *  The interface is needed because class User (which does send the
 *  registration mail) is also needed in WebClient, but the actual
 *  implementation exists only on Game Server side ( = webserver package).
 *
 *  @author Clemens Katzer
 */
public interface IColossusMail
{
    /**
     * Request from the ColossusMail object to send the mail
     * (with the confirmationCode) to the given email address,
     * in order to complete the registration of user username
     * @param username Name of user of which registration is ongoing
     * @param email email address to where to send the mail
     * @param confirmationCode the code user has to provide in the gui field
     *        in order to complete the registration
     * @return The reason why it failed, or null if all is fine.
     */
    public String sendConfirmationMail(String username, String email,
        String confirmationCode);
}
