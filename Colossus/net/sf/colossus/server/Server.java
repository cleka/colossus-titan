package net.sf.colossus.server;


import java.util.*;
import java.net.*;
import javax.swing.*;

import net.sf.colossus.client.Client;
// XXX temp
import net.sf.colossus.client.Proposal;


/**
 *  Class Server lives on the server side and handles all communcation with
 *  the clients.  It talks to the server classes locally, and to the Clients
 *  via the network protocol.
 *  @version $Id$
 *  @author David Ripton
 */

public final class Server
{
    private Game game;
    private Critter striker;

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
    void addClient(String playerName)
    {
        clients.add(new Client(this, playerName));
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

    void allSetCarries(int carryDamage, Set carryTargets)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.setCarries(carryDamage, carryTargets);
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


    boolean getClientOption(String playerName, String optname)
    {
        Client client = getClient(playerName);
        if (client != null)
        {
            return client.getOption(optname);
        }
        return false;
    }

    /** Return the name of the first human-controlled client, or null if
     *  all clients are AI-controlled. */
    private String getFirstHumanClientName()
    {
        for (int i = 0; i < game.getNumPlayers(); i++)
        {
            Player player = game.getPlayer(i);
            if (player.isHuman())
            {
                return player.getName();
            }
        }
        return null;
    }

    /** Get the option from the first human-controlled client.  If there 
     *  are none, get the option from the first AI-controlled client. */
    boolean getClientOption(String optname)
    {
        String clientName = getFirstHumanClientName();
        if (clientName == null)
        {
            clientName = game.getPlayer(0).getName();
        }
        return getClientOption(clientName, optname);
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

    /** Get the option from the first human-controlled client.  If there 
     *  are none, get the option from the first AI-controlled client. */
    int getClientIntOption(String optname)
    {
        String clientName = getFirstHumanClientName();
        if (clientName == null)
        {
            clientName = game.getPlayer(0).getName();
        }
        return getClientIntOption(clientName, optname);
    }

    void setClientOption(String playerName, String optname,
        boolean value)
    {
        Client client = getClient(playerName);
        if (client != null)
        {
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


    /** Find out if the player wants to acquire an angel or archangel. */
    void askAcquireAngel(String playerName, String markerId, List recruits)
    {
        Client client = getClient(playerName);
        if (client != null)
        {
            client.askAcquireAngel(markerId, recruits);
        }
    }

    public void acquireAngel(String markerId, String angelType)
    {
        Legion legion = game.getLegionByMarkerId(markerId);
        if (legion != null)
        {
            legion.addAngel(angelType);
        }
    }


    void createSummonAngel(Legion legion)
    {
        Client client = getClient(legion.getPlayerName());
        client.createSummonAngel(legion.getMarkerId(), 
            legion.getLongMarkerName());
    }

    void reinforce(Legion legion)
    {
        if (getClientOption(legion.getPlayerName(), Options.autoRecruit))
        {
            legion.getPlayer().aiReinforce(legion);
        }
        else
        {
            Client client = getClient(legion.getPlayerName());
            client.doReinforce(legion.getMarkerId());
        }
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


    /** Return true if the legion has moved and can recruit. */
    public boolean canRecruit(String markerId)
    {
        Legion legion = game.getLegionByMarkerId(markerId);
        return legion != null && legion.hasMoved() && legion.canRecruit();
    }

    /** Return a list of creature name strings. */
    public java.util.List findEligibleRecruiters(String markerId,
        String recruitName)
    {
        java.util.List creatures = 
            game.findEligibleRecruiters(markerId, recruitName);
        java.util.List strings = new ArrayList();
        Iterator it = creatures.iterator();
        while (it.hasNext())
        {
            Creature creature = (Creature)it.next();
            strings.add(creature.getName());
        }
        return strings;
    }

    /** Handle mustering for legion. */ 
    public void doMuster(String markerId, String recruitName,
        String recruiterName)
    {
        Legion legion = game.getLegionByMarkerId(markerId);
        if (legion != null && (legion.hasMoved() || game.getPhase() ==
            Constants.FIGHT) && legion.canRecruit())
        {
            legion.sortCritters();
            if (recruitName != null)
            {
                Creature recruit = Creature.getCreatureByName(recruitName);
                Creature recruiter = Creature.getCreatureByName(recruiterName);
                if (recruit != null)
                {
                    game.doRecruit(legion, recruit, recruiter);
                }
            }

            if (!legion.canRecruit())
            {
                didMuster(legion);
            }
        }
        // Need to always call this to keep game from hanging.
        if (game.getPhase() == Constants.FIGHT)
        {
            if (game.getBattle() != null)
            {
                game.getBattle().doneReinforcing();
            }
        }
    }

    void didMuster(Legion legion)
    {
        allUpdateStatusScreen();
        allUnselectHexByLabel(legion.getCurrentHexLabel());
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.didMuster(legion.getMarkerId());
        }
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
        return game.getBattle().concede(markerId);
    }


    void twoNegotiate(Legion attacker, Legion defender)
    {
        Client client1 = getClient(defender.getPlayerName());
        client1.askNegotiate(attacker.getLongMarkerName(), 
            defender.getLongMarkerName(), attacker.getMarkerId(), 
            defender.getMarkerId(), attacker.getImageNames(true),
            defender.getImageNames(true), attacker.getCurrentHexLabel());

        Client client2 = getClient(attacker.getPlayerName());
        client2.askNegotiate(attacker.getLongMarkerName(), 
            defender.getLongMarkerName(), attacker.getMarkerId(), 
            defender.getMarkerId(), attacker.getImageNames(true),
            defender.getImageNames(true), attacker.getCurrentHexLabel());
    }

    // XXX Stringify the proposal.
    /** playerName makes a proposal. */
    public void makeProposal(String playerName, Proposal proposal)
    {
        game.makeProposal(playerName, proposal);
    }

    /** Tell playerName about proposal. */
    void tellProposal(String playerName, Proposal proposal)
    {
        Client client = getClient(playerName);
        client.tellProposal(proposal);
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
            battle.getCritter(hexLabel));
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


    // XXX Stringify the return value.
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


    void allSetBattleValues(String attackerName, String defenderName, 
        String attackerHexId, String defenderHexId, char terrain, 
        int strikeNumber, int damage, int carryDamage, int [] rolls, 
        Set carryTargets)
    {
        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.setBattleValues(attackerName, defenderName,
                attackerHexId, defenderHexId, terrain, strikeNumber,
                damage, carryDamage, rolls, carryTargets);
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

    /** Takes a Set of PenaltyOptions. */
    void askChooseStrikePenalty(SortedSet penaltyOptions)
    {
        String playerName = game.getActivePlayerName();
        Client client = getClient(playerName);
        ArrayList choices = new ArrayList();
        Iterator it = penaltyOptions.iterator();
        while (it.hasNext())
        {
            PenaltyOption po = (PenaltyOption)it.next();
            striker = po.getStriker();
            choices.add(po.toString());
        }
        client.askChooseStrikePenalty(choices);
    }

    public void assignStrikePenalty(String playerName, String prompt)
    {
        striker.assignStrikePenalty(prompt);
        striker = null;
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

    public List getLegionImageNames(String markerId, String playerName)
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
        game.advancePhase(Constants.SPLIT, playerName);
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
            game.advancePhase(Constants.MOVE, playerName);
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
            game.advancePhase(Constants.FIGHT, playerName);
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
        game.advancePhase(Constants.MUSTER, playerName);
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
        game.advancePhase(game.getPhase(), playerName);
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


    // XXX Stringify the return value.
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


    // XXX Stringify.
    /** Return the available legion markers for playerName. */
    public Set getMarkersAvailable(String playerName)
    {
        Player player = game.getPlayer(playerName);
        return player.getMarkersAvailable();
    }

    public void doSplit(String parentId, String childId, String results)
    {
        game.doSplit(parentId, childId, results);
    }

    /** Callback from game after this legion was split off. */
    void didSplit(String hexLabel, String parentId, String childId, int height)
    {
        allUpdateStatusScreen();
        allUnselectHexByLabel(hexLabel);
        allAlignLegions(hexLabel);

        Iterator it = clients.iterator();
        while (it.hasNext())
        {
            Client client = (Client)it.next();
            client.didSplit(childId);
        }
    }

    public void doMove(String markerId, String hexLabel, String entrySide,
        boolean teleport, String teleportingLord)
    {
        boolean moved = game.doMove(markerId, hexLabel, entrySide, teleport,
            teleportingLord);

        if (moved)
        {
            Iterator it = clients.iterator();
            while (it.hasNext())
            {
                Client client = (Client)it.next();
                client.didMove(markerId);
            }
        }
    }

    /** Return a list of Creatures. */
    public List findEligibleRecruits(String markerId, String hexLabel)
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

    /** Return a list of creature name strings. */
    public List listTeleportingLords(String markerId, String hexLabel)
    {
        Legion legion = game.getLegionByMarkerId(markerId);
        return legion.listTeleportingLords(hexLabel);
    }


    /** Return an int which is all possible entry sides (1, 3, 5)
     *  added together. */
    public Set getPossibleEntrySides(String markerId, String hexLabel,
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


    void setPlayerName(String playerName, String name)
    {
        Client client = getClient(playerName);
        client.setPlayerName(name);
    }

    void askPickColor(String playerName, Set colorsLeft)
    {
        Client client = getClient(playerName);
        client.askPickColor(colorsLeft);
    }

    public void assignColor(String playerName, String color)
    {
        game.assignColor(playerName, color);
    }

    public String getHexForLegion(String markerId)
    {
        Legion legion = game.getLegionByMarkerId(markerId);
        if (legion != null)
        {
            return legion.getCurrentHexLabel();
        }
        return null;
    }
}
