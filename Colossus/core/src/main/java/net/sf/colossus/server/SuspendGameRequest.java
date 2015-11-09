package net.sf.colossus.server;


import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import net.sf.colossus.common.Constants;
import net.sf.colossus.util.Glob;


public class SuspendGameRequest
{
    private static final Logger LOGGER = Logger
        .getLogger(SuspendGameRequest.class.getName());

    private final Server server;

    private final HashSet<ClientHandler> suspendRequestApprovers = new HashSet<ClientHandler>();

    private final List<String> denyingClients = new LinkedList<String>();

    private ClientHandler currentRequestor;

    public SuspendGameRequest(Server server)
    {
        this.server = server;
    }

    public void requestToSuspendGame()
    {
        currentRequestor = server.processingCH;
        String playerName = currentRequestor.getPlayerName();

        LOGGER.info("Player " + playerName + " requests to suspend the game.");

        if (!suspendRequestApprovers.isEmpty())
        {
            LOGGER.warning("Player " + playerName + " requested to suspend "
                + "the game, but a previous request is still ongoing!");
            server.processingCH.nak("Another request is still in progress.",
                "Suspend request ignored");
            return;
        }

        denyingClients.clear();
        suspendRequestApprovers.clear();

        // collect all eligible ones to request approval; excludes the stub
        for (ClientHandler client : server.getRealClients())
        {
            if (client.equals(server.processingCH))
            {
                LOGGER.finest("Skipping requesting CH "
                    + client.getPlayerName());
            }
            else if (client.isSpectator())
            {
                LOGGER.finest("Skipping spectator CH "
                    + client.getPlayerName());
            }
            else if (client.canHandleSuspendRequests())
            {
                LOGGER.finest("A client to be asked: "
                    + client.getPlayerName());
                suspendRequestApprovers.add(client);
            }
            else
            {
                LOGGER.finest("Client can't handle the request: "
                    + client.getPlayerName());
            }
        }

        if (suspendRequestApprovers.size() == 0)
        {
            LOGGER.finest("No other clients can confirm suspend; asking the "
                + "requestor itself to have a client message to trigger the "
                + "suspend handling");
            suspendRequestApprovers.add(server.processingCH);
        }
        else
        {
            LOGGER.finest("There are " + suspendRequestApprovers.size()
                + " (other) clients that can handle that request");
        }

        List<ClientHandler> approvers = new LinkedList<ClientHandler>(
            (suspendRequestApprovers));
        for (ClientHandler client : approvers)
        {
            LOGGER.finest("Sending suspend request to player "
                + client.getPlayerName());
            client.askSuspendConfirmation(playerName,
                Constants.SUSPEND_APPROVE_TIMEOUT);
        }
    }

    public void suspendResponse(boolean approved)
    {
        if (suspendRequestApprovers.isEmpty())
        {
            // silently ignore replies that come "too late"
            return;
        }
        LOGGER.info("Player " + server.processingCH + " replies: " + approved);

        suspendRequestApprovers.remove(server.processingCH);
        if (!approved)
        {
            denyingClients.add(server.processingCH.getPlayerName());
        }

        if (suspendRequestApprovers.isEmpty())
        {
            LOGGER.info("approvers list now empty");
            if (denyingClients.size() == 0)
            {

                LOGGER.info("All approved, suspending game...");
                server.getGame().getNotifyWebServer().gameIsSuspended();
                server.initiateSuspendGame();
            }
            else
            {
                suspendDenied();
            }
            currentRequestor = null;
        }
        else
        {
            int count = suspendRequestApprovers.size();
            LOGGER.finest("Waiting for " + count + " more responses");
        }
    }

    public void suspendDenied()
    {
        int count = denyingClients.size();
        String playerList = Glob.glob(", ", denyingClients);
        String message = "Player " + currentRequestor.getPlayerName()
            + " had requested to suspend the game, but " + count + " player"
            + (count == 1 ? "" : "s") + " (" + playerList
            + ") denied it. Game will not be suspended.";
        LOGGER.info("Informing clients: " + message);
        server.messageFromServerToAll(message);
    }

}
