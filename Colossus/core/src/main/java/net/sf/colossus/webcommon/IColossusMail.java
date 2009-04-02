package net.sf.colossus.webcommon;


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
