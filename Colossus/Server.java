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

    public void allClearCarries()
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.clearCarries();
        }
    }


    public void leaveCarryMode()
    {
        Battle battle = game.getBattle();
        battle.leaveCarryMode();
    }


    public void doneWithBattleMoves()
    {
        Battle battle = game.getBattle();
        battle.doneWithMoves();
    }

    public boolean anyOffboardCreatures()
    {
        Battle battle = game.getBattle();
        return battle.anyOffboardCreatures();
    }

    public boolean doneWithStrikes()
    {
        Battle battle = game.getBattle();
        return battle.doneWithStrikes();
    }


    public void makeForcedStrikes(boolean rangestrike)
    {
        Battle battle = game.getBattle();
        battle.makeForcedStrikes(rangestrike);
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

    public void saveOptions()
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.saveOptions();
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

    /** Return the number of the first human-controlled client, or -1 if
     *  all clients are AI-controlled. */
    public int getFirstHumanClientNum()
    {
        for (int i = 0; i < game.getNumPlayers(); i++)
        {
            Player player = game.getPlayer(i);
            if (player.getType().equals("Human"))
            {
                return i;
            }
        }
        return -1;
    }

    /** Get the option from the first human-controlled client.  If there are none,
     *  get the option from the first AI-controlled client. */
    public boolean getClientOption(String optname)
    {
        int clientNum = getFirstHumanClientNum();
        if (clientNum == -1)
        {
            clientNum = 0;
        }
        return getClientOption(clientNum, optname);
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

    public void setAllClientsOption(String optname, boolean value)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
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

    public void allSetupPlayerLabel()
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.setupPlayerLabel();
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
            client.placeNewChit(critter.getImageName(inverted), 
                critter.getTag(), critter.getCurrentHexLabel());
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

    public void allSetBattleChitHits(int tag, int hits)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.setBattleChitHits(tag, hits);
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


    /** Called from Game. */
    String pickRecruit(Legion legion)
    {
        legion.sortCritters();
        Client client = getClient(legion.getPlayerName());
        ArrayList recruits = game.findEligibleRecruits(legion.getMarkerId(),
            legion.getCurrentHexLabel());
        java.util.List imageNames = legion.getImageNames(true);
        String hexDescription = legion.getCurrentHex().getDescription();

        return client.pickRecruit(recruits, imageNames, hexDescription,
            legion.getMarkerId());
    }

    /** Handle mustering for legion. */
    public void doMuster(String markerId)
    {
        Legion legion = game.getLegionByMarkerId(markerId);
        if (legion != null && legion.hasMoved() && legion.canRecruit())
        {
            legion.sortCritters();
            String recruitName = pickRecruit(legion);
            Creature recruit = Creature.getCreatureByName(recruitName);
            if (recruit != null)
            {
                game.doRecruit(recruit, legion);
            }

            if (!legion.canRecruit())
            {
                allUpdateStatusScreen();
                allUnselectHexByLabel(legion.getCurrentHexLabel());
            }
        }
    }



    /** Called from Game. */
    String pickRecruiter(Legion legion, ArrayList recruiters)
    {
        Client client = getClient(legion.getPlayerName());

        java.util.List imageNames = legion.getImageNames(true);
        String hexDescription = legion.getCurrentHex().getDescription();
        return client.pickRecruiter(recruiters, imageNames, hexDescription,
            legion.getMarkerId());
    }


    /** Called from Game. */
    String splitLegion(Legion legion, String selectedMarkerId)
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
        Battle battle = game.getBattle();
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


    public boolean doBattleMove(int tag, String hexLabel)
    {
        return game.getBattle().doMove(tag, hexLabel);
    }


    public void strike(int tag, String hexLabel)
    {
        Battle battle = game.getBattle();
        battle.getActiveLegion().getCritterByTag(tag).strike(
            battle.getCritter(hexLabel), false);
    }


    public void applyCarries(String hexLabel)
    {
        Battle battle = game.getBattle();
        battle.applyCarries(hexLabel);
    }

    public int getCarryDamage()
    {
        Battle battle = game.getBattle();
        return battle.getCarryDamage();
    }

    public Set getCarryTargets()
    {
        Battle battle = game.getBattle();
        return battle.getCarryTargets();
    }


    public void undoLastBattleMove()
    {
        Battle battle = game.getBattle();
        battle.undoLastMove();
    }

    public void undoAllBattleMoves()
    {
        Battle battle = game.getBattle();
        battle.undoAllMoves();
    }


    public int [] getCritterTags(String hexLabel)
    {
        Battle battle = game.getBattle();
        ArrayList critters = battle.getCritters(hexLabel);
        int [] tags = new int[critters.size()];
        int i = 0;
        Iterator it = critters.iterator();
        while (it.hasNext())
        {
            Critter critter = (Critter)it.next();
            tags[i++] = critter.getTag();
        }
        return tags;
    }


    /** Return a set of hexLabels. */
    public Set findMobileCritters()
    {
        Battle battle = game.getBattle();
        return battle.findMobileCritters();
    }

    /** Return a set of hexLabels. */
    public Set showBattleMoves(int tag)
    {
        Battle battle = game.getBattle();
        return battle.showMoves(tag);
    }

    /** Return a set of hexLabels. */
    public Set findStrikes(int tag)
    {
        Battle battle = game.getBattle();
        return battle.findStrikes(tag);
    }

    /** Return a set of hexLabels. */
    public Set findCrittersWithTargets()
    {
        Battle battle = game.getBattle();
        return battle.findCrittersWithTargets();
    }


    /** Return the player name for the critter tag.  Only works in battle. */
    public String getPlayerNameByTag(int tag)
    {
        Battle battle = game.getBattle();
        Legion legion = battle.getActiveLegion();
        if (legion.getCritterByTag(tag) != null)
        {
            return legion.getPlayerName();
        }
        legion = battle.getInactiveLegion();
        if (legion.getCritterByTag(tag) != null)
        {
            return legion.getPlayerName();
        }
        return "";
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

    public void allSetBattleWaitCursor()
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.setBattleWaitCursor();
        }
    }

    public void allSetBattleDefaultCursor()
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.setBattleDefaultCursor();
        }
    }

    public boolean chooseStrikePenalty(String playerName, String prompt)
    {
        Client client = getClient(playerName);
        return client.chooseStrikePenalty(prompt);
    }


    public void allInitBattleMap(String masterHexLabel)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.initBattleMap(masterHexLabel);
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

    // XXX Need to verify that the request came from the correct
    // client, rather than trusting the passed playerName.
    /** Return the new die roll, or -1 on error. */
    public int mulligan(String playerName)
    {
        if (!playerName.equals(getActivePlayerName()))
        {
            return -1;
        }
        return game.mulligan();
    }


    public String getLongMarkerName(String markerId)
    {
        Legion legion = game.getLegionByMarkerId(markerId);
        if (legion != null)
        {
            return legion.getLongMarkerName();
        }
        else
        {
            return "Bogus legion";
        }
    }

    public java.util.List getLegionImageNames(String markerId, 
        String playerName)
    {
        Legion legion = game.getLegionByMarkerId(markerId);
        if (legion == null)
        {
            return new ArrayList();
        }
        boolean showAll = false;
        if (playerName.equals(legion.getPlayerName()) ||
            getClientOption(Options.allStacksVisible))
        {
            showAll = true;
        }
        return legion.getImageNames(showAll);
    }

    public void undoLastSplit(String playerName)
    {
        if (playerName != getActivePlayerName())
        {
            return;
        }
        game.getPlayer(playerName).undoLastSplit();
    }

    public void undoLastMove(String playerName)
    {
        if (playerName != getActivePlayerName())
        {
            return;
        }
        game.getPlayer(playerName).undoLastMove();
    }

    public void undoLastRecruit(String playerName)
    {
        if (playerName != getActivePlayerName())
        {
            return;
        }
        game.getPlayer(playerName).undoLastRecruit();
    }

    public void undoAllSplits(String playerName)
    {
        if (playerName != getActivePlayerName())
        {
            return;
        }
        game.getPlayer(playerName).undoAllSplits();
    }

    public void undoAllMoves(String playerName)
    {
        if (playerName != getActivePlayerName())
        {
            return;
        }
        game.getPlayer(playerName).undoAllMoves();
    }

    public void undoAllRecruits(String playerName)
    {
        if (playerName != getActivePlayerName())
        {
            return;
        }
        game.getPlayer(playerName).undoAllRecruits();
    }

    /** Return true if it's okay to advance to the next phase. */
    public boolean doneWithSplits(String playerName)
    {
        if (playerName != getActivePlayerName())
        {
            return false;
        }
        if (game.getTurnNumber() == 1 && 
            game.getPlayer(playerName).getNumLegions() == 1)
        {
            return false;
        }
        game.advancePhase(Game.SPLIT);
        return true;
    }

    /** Return an error message, or an empty string if okay. */
    public String doneWithMoves(String playerName)
    {
        if (playerName != getActivePlayerName())
        {
            return "Not this player's turn";
        }

        Player player = game.getPlayer(playerName);

        // If any legion has a legal non-teleport move, then
        // the player must move at least one legion.
        if (player.legionsMoved() == 0 &&
            player.countMobileLegions() > 0)
        {
            return "At least one legion must move.";
        }
        // If legions share a hex and have a legal
        // non-teleport move, force one of them to take it.
        else if (player.splitLegionHasForcedMove())
        {
            return "Split legions must be separated.";
        }
        // Otherwise, recombine all split legions still in
        // the same hex, and move on to the next phase.
        else
        {
            player.undoAllSplits();
            game.advancePhase(Game.MOVE);
            return "";
        }
    }

    /** Return true if it's okay to advance to the next phase. */
    public boolean doneWithEngagements(String playerName)
    {
        if (playerName != getActivePlayerName())
        {
            return false;
        }
        // Advance only if there are no unresolved engagements.
        if (game.findEngagements().size() == 0)
        {
            game.advancePhase(Game.FIGHT);
            return true;
        }
        else
        {
            return false;
        }
    }

    /** Return true if it's okay to advance to the next phase. */
    public boolean doneWithRecruits(String playerName)
    {
        if (playerName != getActivePlayerName())
        {
            return false;
        }
        Player player = game.getPlayer(playerName);
        player.commitMoves();
        // Mulligans are only allowed on turn 1.
        player.setMulligansLeft(0);
        game.advancePhase(Game.MUSTER);
        return true;
    }

    public String getActivePlayerName()
    {
        return game.getActivePlayerName();
    }

    public boolean withdrawFromGame(String playerName)
    {
        // XXX Need to support inactive players quitting.
        if (!playerName.equals(getActivePlayerName()))
        {
            return false;
        }
        // XXX If player quits while engaged, might need to set slayer.
        game.getPlayer(playerName).die(null, true);
        game.advancePhase(game.getPhase());
        return true;
    }


    public String getPlayerNameByMarkerId(String markerId)
    {
        return game.getPlayerByMarkerId(markerId).getName();
    }


    public static void main(String [] args)
    {
        System.out.println("Not implemented yet");
    }
}
