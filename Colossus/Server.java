import java.util.*;
import java.net.*;
import javax.swing.*;

/**
 *  Class Server lives on the server side and handles all communcation with
 *  the clients.  It talks to the server classes locally, and to the Clients
 *  via the network protocol.
 *  @version $Id$
 *  @author David Ripton
 */

public final class Server
{
    public static final int port = 1969;
    private Game game;

    // XXX How do we keep track of which client goes with which player?
    // Sort by tower number when the game starts?

    /** For now we'll keep a list of client refs locally rather than using
     *  the network protocol.  We will eventually instead keep a list of the
     *  existing socket connections to use. Maybe also save things like
     *  the originating IP, in case a connection breaks and we need to
     *  authenticate reconnects.  Do not share these references. */
    private ArrayList clients = new ArrayList();


    public Server(Game game)
    {
        this.game = game;
    }


    /** Temporary.  We will not use direct client refs later. */
    public void addClient(Client client)
    {
        clients.add(client);
    }


    /** temp */
    public void disposeAllClients()
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.dispose();
        }
        clients.clear();
    }

    // TODO Set up one thread per client?

    // TODO Need a scheme to broadcast a message to all clients, versus
    // sending one to one client.  Use a list of client numbers for each
    // message, so that we can send a message to two players, not just
    // one or all?

    // TODO Event handling scheme to catch messages from clients.  Parsing
    // scheme to turn them into method calls.

    /** Temporary convenience method until all client-side classes no longer
     *  need game refs. */
    public Game getGame()
    {
        return game;
    }


    public void allUpdateStatusScreen()
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.updateStatusScreen();
        }
    }

    public void allUpdateCaretakerDisplay()
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.updateCaretakerDisplay();
        }
    }

    public void allShowMovementRoll(int roll)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.showMovementRoll(roll);
        }
    }

    public void allClearAllCarries()
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.clearAllCarries();
        }
    }

    // XXX temp
    public void loadOptions()
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.loadOptions();
        }
    }

    /** XXX do not use */
    public Client getClient(String playerName)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            if (playerName.equals(client.getPlayerName()))
            {
                return client;
            }
        }
        return null;
    }

    /** Don't use this. */
    public Client getClient(int playerNum)
    {
        return (Client)clients.get(playerNum);
    }


    public boolean getClientOption(String playerName, String optname)
    {
        Client client = getClient(playerName);
        if (client != null)
        {
            return client.getOption(optname);
        }
        return false;
    }

    public boolean getClientOption(int playerNum, String optname)
    {
        Client client = getClient(playerNum);
        if (client != null)
        {
            return client.getOption(optname);
        }
        return false;
    }

    public boolean getClientOption(String optname)
    {
        return getClientOption(0, optname);
    }

    public String getClientStringOption(String playerName, String optname)
    {
        Client client = getClient(playerName);
        if (client != null)
        {
            return client.getStringOption(optname);
        }
        return "false";
    }

    /** XXX temp */
    public void setClientOption(String playerName, String optname,
        boolean value)
    {
        Client client = getClient(playerName);
        if (client != null)
        {
            client.setOption(optname, value);
        }
    }

    public void setClientOption(int playerNum, String optname,
        boolean value)
    {
        Client client = getClient(playerNum);
        if (client != null)
        {
            client.setOption(optname, value);
        }
    }

    /** XXX temp */
    public void allInitBoard()
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.initBoard();
        }
    }

    public void allLoadInitialMarkerImages()
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.loadInitialMarkerImages();
        }
    }

    public void showMessageDialog(String playerName, String message)
    {
        Client client = getClient(playerName);
        client.showMessageDialog(message);
    }

    public void allShowMessageDialog(String message)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.showMessageDialog(message);
        }
    }


    public void highlightCarries(String playerName)
    {
        Client client = getClient(playerName);
        client.highlightCarries();
    }


    public void allSetupSplitMenu()
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.setupSplitMenu();
        }
    }

    public void allSetupMoveMenu()
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.setupMoveMenu();
        }
    }

    public void allSetupFightMenu()
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.setupFightMenu();
        }
    }

    public void allSetupMusterMenu()
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.setupMusterMenu();
        }
    }


    public void allSetupBattleSummonMenu()
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.setupBattleSummonMenu();
        }
    }

    public void allSetupBattleRecruitMenu()
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.setupBattleRecruitMenu();
        }
    }

    public void allSetupBattleMoveMenu()
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.setupBattleMoveMenu();
        }
    }

    public void allSetupBattleFightMenu()
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.setupBattleFightMenu();
        }
    }


    public void allAlignLegions(String hexLabel)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.alignLegions(hexLabel);
        }
    }

    public void allAlignLegions(Set hexLabels)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.alignLegions(hexLabels);
        }
    }


    public void allDeiconifyBoard()
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.deiconifyBoard();
        }
    }

    public void allAddMarker(String markerId)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.addMarker(markerId);
        }
    }

    public void allRemoveMarker(String markerId)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.removeMarker(markerId);
        }
    }

    public void allUnselectHexByLabel(String hexLabel)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.unselectHexByLabel(hexLabel);
        }
    }

    public void allUnselectAllHexes()
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.unselectAllHexes();
        }
    }

    public void allRepaintHex(String hexLabel)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.repaintMasterHex(hexLabel);
        }
    }


    public void allUnselectBattleHexByLabel(String hexLabel)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.unselectBattleHexByLabel(hexLabel);
        }
    }

    public void allUnselectAllBattleHexes()
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.unselectAllBattleHexes();
        }
    }

    public void allRepaintBattleHex(String hexLabel)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.repaintBattleHex(hexLabel);
        }
    }


    public void allAlignBattleChits(String hexLabel)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.alignBattleChits(hexLabel);
        }
    }

    public void allAlignBattleChits(Set hexLabels)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.alignBattleChits(hexLabels);
        }
    }

    public void allPlaceNewChit(Critter critter, boolean inverted)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.placeNewChit(critter, inverted);
        }
    }

    public void allSetBattleChitDead(int tag)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.setBattleChitDead(tag);
        }
    }

    public void allRemoveBattleChit(int tag)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.removeBattleChit(tag);
        }
    }


    public void allHighlightEngagements()
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.highlightEngagements();
        }
    }


    /** Find out if the player wants to acquire and angel or archangel. */
    public String acquireAngel(String playerName, ArrayList recruits)
    {
        String angelType = null;
        Client client = getClient(playerName);
        if (client != null)
        {
            angelType = client.acquireAngel(recruits);
        }
        return angelType;
    }


    public void createSummonAngel(Legion legion)
    {
        Client client = getClient(legion.getPlayerName());
        client.createSummonAngel(legion);
    }


    public String pickRecruit(Legion legion)
    {
        Client client = getClient(legion.getPlayerName());
        return client.pickRecruit(legion);
    }


    /** Need to do error checking. */
    public void doRecruit(Creature recruit, Legion legion)
    {
        game.doRecruit(recruit, legion);
    }


    public String pickRecruiter(Legion legion, ArrayList recruiters)
    {
        Client client = getClient(legion.getPlayerName());
        return client.pickRecruiter(legion, recruiters);
    }


    public String splitLegion(Legion legion, String selectedMarkerId)
    {
        Client client = getClient(legion.getPlayerName());
        return client.splitLegion(legion, selectedMarkerId);
    }


    public int pickEntrySide(String hexLabel, Legion legion)
    {
        Client client = getClient(legion.getPlayerName());
        return client.pickEntrySide(hexLabel, legion);
    }


    public String pickLord(Legion legion)
    {
        Client client = getClient(legion.getPlayerName());
        return client.pickLord(legion);
    }


    public void engage(String hexLabel)
    {
        game.engage(hexLabel);
    }


    public boolean askFlee(Legion defender, Legion attacker)
    {
        Client client = getClient(defender.getPlayerName());
        return client.askFlee(defender, attacker);
    }


    public boolean askConcede(Legion ally, Legion enemy)
    {
        Client client = getClient(ally.getPlayerName());
        return client.askConcede(ally, enemy);
    }


    public boolean tryToConcede(String markerId)
    {
        return game.getBattle().tryToConcede(markerId);
    }


    public void twoNegotiate(Legion attacker, Legion defender)
    {
        Client client1 = getClient(attacker.getPlayerName());
        client1.askNegotiate(attacker, defender);
        Client client2 = getClient(defender.getPlayerName());
        client2.askNegotiate(attacker, defender);
    }


    // XXX temp
    public void negotiate(String playerName, NegotiationResults results)
    {
        game.negotiate(playerName, results);
    }


    public void fight(String hexLabel)
    {
        game.fight(hexLabel);
    }


    public void allSetBattleDiceValues(String attackerName,
        String defenderName, String attackerHexId, String defenderHexId,
        char terrain, int strikeNumber, int damage, int carryDamage,
        int [] rolls)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.setBattleDiceValues(attackerName, defenderName,
                attackerHexId, defenderHexId, terrain, strikeNumber,
                damage, carryDamage, rolls);
        }
    }

    public void allSetBattleDiceCarries(int carries)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.setBattleDiceCarries(carries);
        }
    }

    public boolean chooseStrikePenalty(String playerName, String prompt)
    {
        Client client = getClient(playerName);
        return client.chooseStrikePenalty(prompt);
    }


    public void allInitBattleMap(String masterHexLabel, Battle battle)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.initBattleMap(masterHexLabel, battle);
        }
    }


    public void allShowBattleMap()
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.showBattleMap();
        }
    }


    public void allDisposeBattleMap()
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.disposeBattleMap();
        }
    }


    public void setupPlayerLabel(String playerName)
    {
        Client client = getClient(playerName);
        client.setupPlayerLabel();
    }


    public static void main(String [] args)
    {
        System.out.println("Not implemented yet");
    }
}
