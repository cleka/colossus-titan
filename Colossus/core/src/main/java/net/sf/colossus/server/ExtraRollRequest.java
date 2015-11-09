package net.sf.colossus.server;


import java.util.LinkedList;
import java.util.logging.Logger;

public class ExtraRollRequest
{
    private static final Logger LOGGER = Logger
        .getLogger(ExtraRollRequest.class.getName());

    private LinkedList<ClientHandler> eligibleClients = null;
    int currentRequestId = 0;
    ClientHandler currentRequestor = null;
    int approvals = 0;
    int denials = 0;

    private final Server server;

    public ExtraRollRequest(Server server)
    {
        this.server = server;
    }

    synchronized public void handleExtraRollRequest(ClientHandler processingCH)
    {
        approvals = 0;
        denials = 0;
        currentRequestor = processingCH;
        currentRequestId++;
        eligibleClients = new LinkedList<ClientHandler>();

        String playerName = processingCH.getPlayerName();
        LOGGER.finer("Player " + playerName + " requests extra roll.");

        // realClients does not include the stub (local spectator)
        for (ClientHandler client : server.getRealClients())
        {
            if (client.equals(processingCH))
            {
                LOGGER.finest("Skipping requesting CH "
                    + client.getPlayerName());
            }
            else if (client.isSpectator())
            {
                LOGGER.finest("Skipping spectator CH "
                    + client.getPlayerName());
            }
            else if (client.canHandleExtraRollRequest())
            {
                LOGGER.finest("A client to be asked: "
                    + client.getPlayerName());
                eligibleClients.add(client);
            }
            else if (client.isTemporarilyInTrouble()
                || client.isTemporarilyDisconnected())
            {
                LOGGER.finest("Skipping in-trouble-CH "
                    + client.getPlayerName());
            }
            else
            {
                LOGGER.finest("Client can't handle the request: "
                    + client.getPlayerName());
            }
        }

        if (eligibleClients.size() == 0)
        {

            LOGGER.finest("No other clients can confirm; asking requestor "
                + " itself to have a client message to trigger the suspend "
                + "handling");
            eligibleClients.add(processingCH);
        }
        else
        {
            LOGGER.finest("There are " + eligibleClients.size()
                + " (other) clients that support that request");
        }

        for (ClientHandler client : eligibleClients)
        {
            LOGGER.finest("Sending extra-roll request to player "
                + client.getPlayerName());
            client.requestExtraRollApproval(playerName, currentRequestId);
        }
    }

    synchronized public void handleExtraRollResponse(int requestId,
        ClientHandler processingCH, boolean approved)
    {
        String playerName = processingCH.getPlayerName();
        if (requestId == currentRequestId)
        {
            if (approved)
            {
                approvals++;
                LOGGER.finest("Client " + playerName
                    + " approved the extra roll request");
            }
            else
            {
                denials++;
                LOGGER.finest("Client " + playerName
                    + " denied the extra roll request");
            }

            if (approvals + denials == eligibleClients.size())
            {
                LOGGER.finer("Got extraRollRequestResponses from all "
                    + eligibleClients.size() + " clients - ok!");
                if (denials == 0)
                {
                    int roll = server.getGame().makeExtraRoll();
                    String message = "Informing clients: Player "
                        + currentRequestor.getPlayerName()
                        + " requested extra roll and no player denied - player got new roll "
                        + roll + ".";
                    LOGGER.finer(message);
                    server.messageFromServerToAll(message);
                }
                else
                {
                    String message = "Informing clients: Player "
                        + currentRequestor.getPlayerName()
                        + " requested extra roll but " + denials
                        + " players denied it. Roll remains the same.";
                    LOGGER.finer(message);
                    server.messageFromServerToAll(message);
                }
                currentRequestor = null;
            }
        }
        else
        {
            LOGGER.warning("Got extraRollResponse for requestId " + requestId
                + ", but current extraRequestId is already "
                + currentRequestId + " - ignored.");
        }
    }
}
