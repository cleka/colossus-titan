package net.sf.colossus.server;


import java.util.*;
import java.net.*;
import javax.swing.*;

import net.sf.colossus.client.Client;


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

    // XXX Need to verify that various requests came from the correct
    // client for that player.

    /** For now we'll keep a list of client refs locally rather than using
     *  the network protocol.  We will eventually instead keep a list of the
     *  existing socket connections to use. Maybe also save things like
     *  the originating IP, in case a connection breaks and we need to
     *  authenticate reconnects.  Do not share these references. */
    private List clients = new ArrayList();


    Server(Game game)
    {
        this.game = game;
    }


    /** Temporary.  We will not use direct client refs later. */
    void addClient(Client client)
    {
        clients.add(client);
    }


    /** temp */
    void disposeAllClients()
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



    void allUpdateStatusScreen()
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.updateStatusScreen(getPlayerInfo());
        }
    }

    void allUpdateCaretakerDisplay()
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.updateCaretakerDisplay();
        }
    }

    void allShowMovementRoll(int roll)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.showMovementRoll(roll);
        }
    }

    void allClearCarries()
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
    void loadOptions()
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.loadOptions();
        }
    }

    void saveOptions()
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.saveOptions();
        }
    }

    private Client getClient(String playerName)
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

    private Client getClient(int playerNum)
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

    boolean getClientOption(int playerNum, String optname)
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
    private int getFirstHumanClientNum()
    {
        for (int i = 0; i < game.getNumPlayers(); i++)
        {
            Player player = game.getPlayer(i);
            if (player.isHuman())
            {
                return i;
            }
        }
        return -1;
    }

    /** Get the option from the first human-controlled client.  If there 
     *  are none, get the option from the first AI-controlled client. */
    boolean getClientOption(String optname)
    {
        int clientNum = getFirstHumanClientNum();
        if (clientNum == -1)
        {
            clientNum = 0;
        }
        return getClientOption(clientNum, optname);
    }

    String getClientStringOption(String playerName, String optname)
    {
        Client client = getClient(playerName);
        if (client != null)
        {
            return client.getStringOption(optname);
        }
        return "false";
    }

    int getClientIntOption(String playerName, String optname)
    {
        Client client = getClient(playerName);
        if (client != null)
        {
            return client.getIntOption(optname);
        }
        return -1;
    }

    int getClientIntOption(int playerNum, String optname)
    {
        Client client = getClient(playerNum);
        if (client != null)
        {
            return client.getIntOption(optname);
        }
        return -1;
    }

    /** Get the option from the first human-controlled client.  If there 
     *  are none, get the option from the first AI-controlled client. */
    int getClientIntOption(String optname)
    {
        int clientNum = getFirstHumanClientNum();
        if (clientNum == -1)
        {
            clientNum = 0;
        }
        return getClientIntOption(clientNum, optname);
    }

    /** XXX temp */
    void setClientOption(String playerName, String optname,
        boolean value)
    {
        Client client = getClient(playerName);
        if (client != null)
        {
            client.setOption(optname, value);
        }
    }

    void setClientOption(int playerNum, String optname,
        boolean value)
    {
        Client client = getClient(playerNum);
        if (client != null)
        {
            client.setOption(optname, value);
        }
    }

    void setAllClientsOption(String optname, boolean value)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.setOption(optname, value);
        }
    }

    /** XXX temp */
    void allInitBoard()
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.initBoard();
        }
    }

    void allLoadInitialMarkerImages()
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.loadInitialMarkerImages();
        }
    }

    void allSetupPlayerLabel()
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.setupPlayerLabel();
        }
    }

    void showMessageDialog(String playerName, String message)
    {
        Client client = getClient(playerName);
        client.showMessageDialog(message);
    }

    void allShowMessageDialog(String message)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.showMessageDialog(message);
        }
    }


    void highlightCarries(String playerName)
    {
        Client client = getClient(playerName);
        client.highlightCarries();
    }


    void allSetupSplitMenu()
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.setupSplitMenu();
        }
    }

    void allSetupMoveMenu()
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.setupMoveMenu();
        }
    }

    void allSetupFightMenu()
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.setupFightMenu();
        }
    }

    void allSetupMusterMenu()
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.setupMusterMenu();
        }
    }


    void allSetupBattleSummonMenu()
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.setupBattleSummonMenu();
        }
    }

    void allSetupBattleRecruitMenu()
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.setupBattleRecruitMenu();
        }
    }

    void allSetupBattleMoveMenu()
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.setupBattleMoveMenu();
        }
    }

    void allSetupBattleFightMenu()
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.setupBattleFightMenu();
        }
    }


    void allAlignLegions(String hexLabel)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.alignLegions(hexLabel);
        }
    }

    void allAlignLegions(Set hexLabels)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.alignLegions(hexLabels);
        }
    }


    void allAddMarker(String markerId)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.addMarker(markerId);
        }
    }

    void allRemoveMarker(String markerId)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.removeMarker(markerId);
        }
    }

    void allUnselectHexByLabel(String hexLabel)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.unselectHexByLabel(hexLabel);
        }
    }

    void allUnselectAllHexes()
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.unselectAllHexes();
        }
    }

    void allRepaintHex(String hexLabel)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.repaintMasterHex(hexLabel);
        }
    }


    void allUnselectBattleHexByLabel(String hexLabel)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.unselectBattleHexByLabel(hexLabel);
        }
    }

    void allUnselectAllBattleHexes()
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.unselectAllBattleHexes();
        }
    }

    void allRepaintBattleHex(String hexLabel)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.repaintBattleHex(hexLabel);
        }
    }


    void allAlignBattleChits(String hexLabel)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.alignBattleChits(hexLabel);
        }
    }

    void allAlignBattleChits(Set hexLabels)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.alignBattleChits(hexLabels);
        }
    }

    void allPlaceNewChit(Critter critter, boolean inverted)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.placeNewChit(critter.getImageName(inverted), 
                critter.getTag(), critter.getCurrentHexLabel());
        }
    }

    void allSetBattleChitDead(int tag)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.setBattleChitDead(tag);
        }
    }

    void allSetBattleChitHits(int tag, int hits)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.setBattleChitHits(tag, hits);
        }
    }

    void allRemoveBattleChit(int tag)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.removeBattleChit(tag);
        }
    }


    void allHighlightEngagements()
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.highlightEngagements();
        }
    }


    /** Find out if the player wants to acquire and angel or archangel. */
    String acquireAngel(String playerName, List recruits)
    {
        String angelType = null;
        Client client = getClient(playerName);
        if (client != null)
        {
            angelType = client.acquireAngel(recruits);
        }
        return angelType;
    }


    void createSummonAngel(Legion legion)
    {
        Client client = getClient(legion.getPlayerName());
        client.createSummonAngel(legion.getMarkerId(), 
            legion.getLongMarkerName());
    }

    public void doSummon(String markerId, String donorId, String angel)
    {
        Legion legion = game.getLegionByMarkerId(markerId);
        Legion donor = game.getLegionByMarkerId(donorId);
        Creature creature = null;
        if (angel != null)
        {
            creature = Creature.getCreatureByName(angel);
        }
        game.doSummon(legion, donor, creature);
    }


    /** Called from Game. */
    String pickRecruit(Legion legion)
    {
        legion.sortCritters();
        Client client = getClient(legion.getPlayerName());
        List recruits = game.findEligibleRecruits(
            legion.getMarkerId(), legion.getCurrentHexLabel());
        List imageNames = legion.getImageNames(true);
        String hexDescription = legion.getCurrentHex().getDescription();

        return client.pickRecruit(recruits, imageNames, hexDescription,
            legion.getMarkerId());
    }

    /** Handle mustering for legion.  Return true if the legion 
     *  mustered something. */
    public boolean doMuster(String markerId)
    {
        Legion legion = game.getLegionByMarkerId(markerId);
        if (legion != null && legion.hasMoved() && legion.canRecruit())
        {
            legion.sortCritters();
            String recruitName = pickRecruit(legion);
            if (recruitName != null)
            {
                Creature recruit = Creature.getCreatureByName(recruitName);
                if (recruit != null)
                {
                    game.doRecruit(recruit, legion);
                }
            }

            if (!legion.canRecruit())
            {
                allUpdateStatusScreen();
                allUnselectHexByLabel(legion.getCurrentHexLabel());
                return true;
            }
        }
        return false;
    }



    /** Called from Game. */
    String pickRecruiter(Legion legion, List recruiters)
    {
        Client client = getClient(legion.getPlayerName());

        List imageNames = legion.getImageNames(true);
        String hexDescription = legion.getCurrentHex().getDescription();
        return client.pickRecruiter(recruiters, imageNames, hexDescription,
            legion.getMarkerId());
    }


    /** Called from Game. */
    String splitLegion(Legion legion, String selectedMarkerId)
    {
        Client client = getClient(legion.getPlayerName());
        return client.splitLegion(legion.getMarkerId(), 
            legion.getLongMarkerName(), selectedMarkerId, 
            legion.getImageNames(true));
    }


    String pickLord(Legion legion)
    {
        Client client = getClient(legion.getPlayerName());
        return client.pickLord(legion.getUniqueLordImageNames());
    }


    public void engage(String hexLabel)
    {
        game.engage(hexLabel);
    }


    /** Ask ally's player whether he wants to concede with ally. */
    void askConcede(Legion ally, Legion enemy)
    {
        if (getClientOption(ally.getPlayerName(), Options.autoFlee))
        {
            if (ally.getPlayer().aiConcede(ally, enemy))
            {
                concede(ally.getMarkerId());
            }
            else
            {
                doNotConcede(ally.getMarkerId());
            }
        }
        else
        {
            Client client = getClient(ally.getPlayerName());
            client.askConcede(ally.getLongMarkerName(),
                ally.getCurrentHex().getDescription(), ally.getMarkerId(),
                ally.getImageNames(true), enemy.getMarkerId(), 
                enemy.getImageNames(true));
        }
    }

    public void concede(String markerId)
    {
        game.concede(markerId);
    }

    public void doNotConcede(String markerId)
    {
        game.doNotConcede(markerId);
    }

    /** Ask ally's player whether he wants to flee with ally. */
    void askFlee(Legion ally, Legion enemy)
    {
        if (getClientOption(ally.getPlayerName(), Options.autoFlee))
        {
            if (ally.getPlayer().aiFlee(ally, enemy))
            {
                flee(ally.getMarkerId());
            }
            else
            {
                doNotFlee(ally.getMarkerId());
            }
        }
        else
        {
            Client client = getClient(ally.getPlayerName());
            client.askFlee(ally.getLongMarkerName(),
                ally.getCurrentHex().getDescription(), ally.getMarkerId(),
                ally.getImageNames(true), enemy.getMarkerId(), 
                enemy.getImageNames(true));
        }
    }

    public void flee(String markerId)
    {
        game.flee(markerId);
    }

    public void doNotFlee(String markerId)
    {
        game.doNotFlee(markerId);
    }


    public boolean tryToConcede(String markerId)
    {
        Battle battle = game.getBattle();
        return game.getBattle().tryToConcede(markerId);
    }


/*
    // XXX This needs to be multithreaded to work properly when
    // both clients are running in the same JVM.
    public void twoNegotiate(Legion attacker, Legion defender)
    {
        Client client1 = getClient(attacker.getPlayerName());
        client1.askNegotiate(attacker.getLongMarkerName(), 
            defender.getLongMarkerName(), attacker.getMarkerId(), 
            defender.getMarkerId(), attacker.getImageNames(true),
            defender.getImageNames(true), attacker.getCurrentHexLabel());
        Client client2 = getClient(defender.getPlayerName());
        client2.askNegotiate(attacker.getLongMarkerName(), 
            defender.getLongMarkerName(), attacker.getMarkerId(), 
            defender.getMarkerId(), attacker.getImageNames(true),
            defender.getImageNames(true), attacker.getCurrentHexLabel());
    }
*/


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


    public boolean undoBattleMove(String hexLabel)
    {
        Battle battle = game.getBattle();
        return battle.undoMove(hexLabel);
    }


    public int [] getCritterTags(String hexLabel)
    {
        Battle battle = game.getBattle();
        List critters = battle.getCritters(hexLabel);
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


    void allSetBattleDiceValues(String attackerName,
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

    void allSetBattleDiceCarries(int carries)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.setBattleDiceCarries(carries);
        }
    }

    void allSetBattleWaitCursor()
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.setBattleWaitCursor();
        }
    }

    void allSetBattleDefaultCursor()
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.setBattleDefaultCursor();
        }
    }

    boolean chooseStrikePenalty(String playerName, String prompt)
    {
        Client client = getClient(playerName);
        return client.chooseStrikePenalty(prompt);
    }


    void allInitBattleMap(String masterHexLabel)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.initBattleMap(masterHexLabel);
        }
    }


    void allDisposeBattleMap()
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.disposeBattleMap();
        }
    }


    void setupPlayerLabel(String playerName)
    {
        Client client = getClient(playerName);
        client.setupPlayerLabel();
    }

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

    public List getLegionImageNames(String markerId, 
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

    public void undoSplit(String playerName, String splitoffId)
    {
        if (playerName != getActivePlayerName())
        {
            return;
        }
        game.getPlayer(playerName).undoSplit(splitoffId);
    }

    public void undoMove(String playerName, String markerId)
    {
        if (playerName != getActivePlayerName())
        {
            return;
        }
        game.getPlayer(playerName).undoMove(markerId);
    }

    public void undoRecruit(String playerName, String markerId)
    {
        if (playerName != getActivePlayerName())
        {
            return;
        }
        game.getPlayer(playerName).undoRecruit(markerId);
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
        game.advancePhase(Constants.SPLIT);
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
            player.recombineIllegalSplits();
            game.advancePhase(Constants.MOVE);
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
            game.advancePhase(Constants.FIGHT);
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
        game.advancePhase(Constants.MUSTER);
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


    public int getMulligansLeft(String playerName)
    {
        Player player = game.getPlayer(playerName);
        return player.getMulligansLeft();
    }

    public void setDonor(String hexLabel)
    {
        Player player = game.getActivePlayer();
        Legion donor = game.getFirstFriendlyLegion(hexLabel, player);
        if (donor != null)
        {
            player.setDonor(donor);
        }
    }

    public String getDonorId(String playerName)
    {
        Player player = game.getPlayer(playerName);
        Legion donor = player.getDonor();
        if (donor != null)
        {
            return donor.getMarkerId();
        }
        else
        {
            return null;
        }
    }

    public boolean donorHasAngel(String playerName)
    {
        Player player = game.getPlayer(playerName);
        Legion donor = player.getDonor();
        if (donor != null)
        {
            return (donor.numCreature(Creature.getCreatureByName("Angel")) 
                >= 1);
        }
        else
        {
            return false;
        }
    }

    public boolean donorHasArchangel(String playerName)
    {
        Player player = game.getPlayer(playerName);
        Legion donor = player.getDonor();
        if (donor != null)
        {
            return (donor.numCreature(Creature.getCreatureByName("Archangel"))
                >= 1);
        }
        else
        {
            return false;
        }
    }


    public String [] getPlayerInfo()
    {
        String [] info = new String[game.getNumPlayers()];
        Iterator it = game.getPlayers().iterator();
        int i = 0;
        while (it.hasNext())
        {
            Player player = (Player)it.next();
            info[i++] = player.getStatusInfo(); 
        }
        return info;
    }


    public int getLegionHeight(String markerId)
    {
        Legion legion = game.getLegionByMarkerId(markerId);
        if (legion != null)
        {
            return legion.getHeight();
        }
        return 0;
    }

    public int getCreatureCount(String creatureName)
    {
        return game.getCaretaker().getCount(creatureName);
    }


    public String getPlayerColor(String playerName)
    {
        Player player = game.getPlayer(playerName);
        if (player != null)
        {
            return player.getColor();
        }
        return "unknown";
    }

    public String getBattleActivePlayerName()
    {
        return game.getBattle().getActivePlayerName();
    }

    public int getBattlePhase()
    {
        return game.getBattle().getPhase();
    }

    public int getBattleTurnNumber()
    {
        return game.getBattle().getTurnNumber();
    }

    public int getPhase()
    {
        return game.getPhase();
    }

    public int getTurnNumber()
    {
        return game.getTurnNumber();
    }

    /** Return the splitoffId. */
    public String doSplit(String markerId)
    {
        return game.doSplit(markerId);
    }

    public boolean doMove(String markerId, String hexLabel, int entrySide,
        boolean teleport)
    {
        return game.doMove(markerId, hexLabel, entrySide, teleport);
    }

    /** Return a list of Creatures. */
    public List findEligibleRecruits(String markerId, 
        String hexLabel)
    {
        return game.findEligibleRecruits(markerId, hexLabel);
    }

    /** Return a set of hexLabels. */
    public Set findAllEligibleRecruitHexes()
    {
        return game.findAllEligibleRecruitHexes();
    }

    /** Return a set of hexLabels. */
    public Set findSummonableAngels(String markerId)
    {
        return game.findSummonableAngels(markerId);
    }

    /** Return a set of hexLabels. */
    public Set listTeleportMoves(String markerId)
    {
        Legion legion = game.getLegionByMarkerId(markerId);
        return game.listTeleportMoves(legion, legion.getCurrentHex(),
            legion.getPlayer().getMovementRoll(), false);
    }

    /** Return a set of hexLabels. */
    public Set listNormalMoves(String markerId)
    {
        Legion legion = game.getLegionByMarkerId(markerId);
        return game.listNormalMoves(legion, legion.getCurrentHex(),
            legion.getPlayer().getMovementRoll(), false);
    }


    /** Return an int which is all possible entry sides (1, 3, 5)
     *  added together. */
    public int getPossibleEntrySides(String markerId, String hexLabel,
        boolean teleport)
    {
        return game.getPossibleEntrySides(markerId, hexLabel, teleport);
    }


    public List getAllLegionIds()
    {
        return game.getAllLegionIds();
    }

    public int getActivePlayerNum()
    {
        return game.getActivePlayerNum();
    }

    public List getLegionMarkerIds(String hexLabel)
    {
        return game.getLegionMarkerIds(hexLabel);
    }

    public Set findAllUnmovedLegionHexes()
    {
        return game.findAllUnmovedLegionHexes();
    }

    public Set findTallLegionHexes()
    {
        return game.findTallLegionHexes();
    }

    public Set findEngagements()
    {
        return game.findEngagements();
    }

    public void newGame()
    {
        game.newGame();
    }

    public void loadGame(String filename)
    {
        game.loadGame(filename);
    }

    public void saveGame()
    {
        game.saveGame();
    }

    public void saveGame(String filename)
    {
        game.saveGame(filename);
    }

    // XXX Stringify the collection.
    String pickMarker(String playerName, Collection markersAvailable)
    {
        Client client = getClient(playerName);
        return client.pickMarker(markersAvailable);
    }


    void setPlayerName(int i, String name)
    {
        Client client = getClient(i);
        client.setPlayerName(name);
    }

    String pickColor(int i, Set colorsLeft)
    {
        Client client = getClient(i);
        return client.pickColor(colorsLeft);
    }
}
