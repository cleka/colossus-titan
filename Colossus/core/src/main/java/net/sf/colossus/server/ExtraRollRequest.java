package net.sf.colossus.server;


import java.util.LinkedList;
import java.util.logging.Logger;

public class ExtraRollRequest
{
    private static final Logger LOGGER = Logger
        .getLogger(ExtraRollRequest.class.getName());

    private LinkedList<ClientHandler> eligibleClients = null;
    int currentRequestId = 0;
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
        currentRequestId++;
        eligibleClients = new LinkedList<ClientHandler>();

        String playerName = processingCH.getPlayerName();
        LOGGER.info(playerName + " requests extra roll.");

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
            else
            {
                LOGGER.finest("Client can't handle the request: "
                    + client.getPlayerName());
            }
        }

        if (eligibleClients.size() == 0)
        {
            LOGGER.finest("No other clients can confirm...");
            eligibleClients.add(processingCH);
            processingCH
                .requestExtraRollApproval(playerName, currentRequestId);
        }
        else
        {
            LOGGER.finest("There are " + eligibleClients.size()
                + " clients that support that request");
        }

        for (ClientHandler client : eligibleClients)
        {
            LOGGER.finest("SENDING REQUEST TO " + client.getPlayerName());
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
                LOGGER.info("Got extraRollRequestResponses from all "
                    + eligibleClients.size() + " clients - ok!");
                if (denials == 0)
                {
                    int roll = server.getGame().makeExtraRoll();
                    LOGGER.info(playerName
                        + " takes a mulligan and rolls " + roll);
                    String message = "Player "
                        + playerName
                        + " requested extra roll and no player denied - player got new roll "
                        + roll + ".";
                    server.messageFromServerToAll(message);
                }
                else
                {
                    String message = "Player " + playerName
                        + " requested extra roll but " + denials
                        + " players denied it. Roll remains the same.";
                    server.messageFromServerToAll(message);
                }
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
