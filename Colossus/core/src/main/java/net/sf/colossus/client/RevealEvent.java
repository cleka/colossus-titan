package net.sf.colossus.client;

import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.*;
import javax.swing.border.TitledBorder;

import net.sf.colossus.server.Constants;


public class RevealEvent
{
    private Client client;
    private int turnNumber;
    private int playerNr;
    private int eventType;
    private String markerId;
    private int height;
    private ArrayList knownCreatures;

    private int oldRoll;
    private int newRoll;
    
    private int scale;
    private JPanel p;
    

    public final static int eventSplit = 0;
    public final static int eventRecruit = 1;
    public final static int eventSummon = 2;
    public final static int eventTeleport = 3;
    public final static int eventAcquire = 4;
    public final static int eventWinner = 5;
    public final static int eventEliminated = 6;
    public final static int eventTurnChange = 7;
    public final static int eventPlayerChange = 8;
    public final static int eventMulligan = 9;

    private final static String eventSplitText = "Split";
    private final static String eventRecruitText = "Recruit";
    private final static String eventSummonText = "Summon";
    private final static String eventTeleportText = "Teleport";
    private final static String eventAcquireText = "Acquire";
    private final static String eventWinnerText = "Winner";
    private final static String eventEliminatedText = "Eliminated";
    private final static String eventTurnChangeText = "TurnChange";
    private final static String eventPlayerChangeText = "PlayerChange";
    private final static String eventMulliganText = "Mulligan";

    private static String[] eventTypeToString = {
        eventSplitText, eventRecruitText, eventSummonText, eventTeleportText, 
        eventAcquireText, eventWinnerText, eventEliminatedText, 
        eventTurnChangeText, eventPlayerChangeText, eventMulliganText
    };
    
    
    
    // child legion or summoner:
    private String markerId2;
    private int height2;

    
    public RevealEvent(Client client, int turnNumber, int playerNr, int eventType, 
            String markerId, int height, ArrayList knownCreatures,
            String markerId2, int height2)
    {
        if (markerId == null && eventType != eventPlayerChange && eventType != eventTurnChange)
        {
            System.out.println("ERROR: null marker for event "+getEventTypeText(eventType));
            return;
        }
        this.client = client;
        this.turnNumber = turnNumber;
        this.playerNr = playerNr;
        this.eventType = eventType;
        // affected legion; split: parent; summon: donor
        this.markerId = markerId;
        this.height = height;
        this.knownCreatures = knownCreatures;
        // next 2: child legion or summoner
        this.markerId2 = markerId2;
        this.height2 = height2;
        
       // System.out.println("NEW RevealEvent: "+this.toString());
    }

    // mulligan
    public RevealEvent(Client client, int turnNumber, int playerNr, 
            int eventType, int oldRoll, int newRoll)
    {
        this.client = client;
        this.turnNumber = turnNumber;
        this.playerNr = playerNr;
        this.eventType = eventType;

        this.oldRoll = oldRoll;
        this.newRoll = newRoll;
    }

    
    public int getEventType()
    {
        return eventType;
    }

    public String getEventTypeText()
    {
        return eventTypeToString[eventType];
    }

    public String getEventTypeText(int type)
    {
        return eventTypeToString[type];
    }

    public String getMarkerId()
    {
        return markerId;
    }

    public String getMarkerId2()
    {
        return markerId2;
    }

    public int getTurn()
    {
        return turnNumber;
    }
    
    public String getPlayer()
    {
        return client.getPlayerInfo(playerNr).getName();
    }
    
    public int getPlayerNr()
    {
        return playerNr;
    }

    public String toString()
    {
        String msg = "<unknown event?>";
        if (eventType == eventSplit)
        {
            msg = "Revealing event: \"" + getEventTypeText() + "\" (turn "+turnNumber+"):\n" +
                "- Legion with marker: "+ markerId  + " (now height: " + height+"\n" +
                "- Splitoff to marker: "+ markerId2 + " (now height: " + height2+"\n";
        }
        else if (eventType == eventSummon)
        {
            RevealedCreature rc = (RevealedCreature) this.knownCreatures.get(0);
            String summoned = rc.getName();
            
            msg = "Revealing event: \"" + getEventTypeText() + "\":\n" +
                 "  Summonable \"" + summoned + "\" from " + markerId + 
                 "(" + height + ") to " + markerId2 + "("+height2+")";
        }

        else if (eventType == eventWinner)
        {
            msg = "Revealing event: Winner = "+markerId;
        }

        else if (eventType == eventEliminated)
        {
            msg = "Revealing event: Eliminated = "+markerId;
        }

        else if (eventType == eventTurnChange)
        {
            msg = "Revealing event: Turn change, now player "+getPlayerNr()+
            " ("+getPlayer()+"), Turn "+getTurn();
        }
        else if (eventType == eventPlayerChange)
        {
            msg = "Revealing event: Player change, now player "+getPlayerNr()+
            " ("+getPlayer()+"), Turn "+getTurn();
        }
        else if (eventType == eventMulligan)
        {
            msg = "Revealing event: Player "+getPlayerNr()+
            " ("+getPlayer()+"), Turn "+getTurn() + " took mulligan;" +
            " old="+ oldRoll + ", new=" + newRoll;
        }
        
        else
        {
            StringBuffer msgBuf = new StringBuffer(1000);

            msgBuf.append("Revealing event: \"" + getEventTypeText() + "\" for marker " + markerId + "\n");
            Iterator it = knownCreatures.iterator();
        
            int i = 0;
            while (it.hasNext())
            {   
                i++;
                RevealedCreature rc = (RevealedCreature)it.next();
                msgBuf.append(i + ". " + rc.toString()+"\n");
                // System.out.println(i + ". " + rc.toDetails());
            }
            msgBuf.append(" => legion " + markerId + " now " + height + " creatures.");
            msg = msgBuf.toString();
        }

        return msg;
    }
    
    private void addLabel(String text)
    {
        JLabel label = new JLabel(text);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(label);
    }
    
    // Todo: paint height on top of marker possible?
    private void addMarker(String markerId)
    {
        if (markerId == null)
        {
            System.out.println("ERROR: markerId null, event type "+getEventTypeText()+" turn" +getTurn());
        }
        try
        {
            Chit marker = new Chit(scale, markerId);
            marker.setAlignmentX(Component.LEFT_ALIGNMENT);
            p.add(marker);
        }
        catch(Exception e)
        {
            System.out.println("\n\nCATCH: ERROR: markerId null, event type "+
              getEventTypeText()+" turn" +getTurn());
        };
        
    }

    // TODO duplicated from LegionInfo.java:
    /** Return the full basename for a titan in legion markerId,
     *  first finding that legion's player, player color, and titan size.
     *  Default to Constants.titan if the info is not there. */
    String getTitanBasename()
    {
        try
        {
            PlayerInfo info = client.getPlayerInfo(playerNr);
            String color = info.getColor();
            int power = info.getTitanPower();
            return "Titan-" + power + "-" + color;
        }
        catch (Exception ex)
        {
            return Constants.titan;
        }
    }
 
    private Chit getSolidMarker()
    {
        Chit solidMarker;
        // I would have liked to paint a solid marker with color of that
        // player, instead of the Titan picture (or any individual
        // marker), because this is for the "player as such",
        // not related to any single marker or the Titan creature.
        // But even if I had created BrSolid.gif (or even copied
        // Br01.gif to that name), did compileVariants, and the gif image
        // was listed in jar tfv usage, still I got "Couldn't get image"
        // error and everything was hanging.
        // So, for now we go with the Titan icon.
/*
        try
        {
            String color = client.getShortColor(playerNr);
            solidMarker = new Chit(scale, color+"Solid");
        }
        catch(Exception e)
        {
            System.out.println("exception...");
            // if solid marker does not exist for this color,
            // use as fallback the Titan chit.
            solidMarker = new Chit(scale, getTitanBasename());
        }
*/
        solidMarker = new Chit(scale, getTitanBasename());
        solidMarker.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        return solidMarker;
    }
    
    private void addCreature(String creatureName)
    {
        String name = new String(creatureName);
        if (name.equals(Constants.titan))
        {
            name = getTitanBasename();
        }
        if (name == null)
        {
            System.out.println("ERROR: creature name null, event type "+getEventTypeText()+" turn" +getTurn());
        }

        Chit creature = new Chit(scale, name);
        creature.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(creature);
    }

    private JPanel infoEvent(String text)
    {
        JPanel p = new JPanel();

        p.setBorder(new TitledBorder(""));
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel label = new JLabel(text);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(label);
        // p.add(Box.createRigidArea(new Dimension(5,0)));

        return p;
    }

    public JPanel toPanel()
    {
        this.scale = 2 * Scale.get();

        if (eventType == eventTurnChange)
        {
            return infoEvent("Turn "+turnNumber+" starts");
        }
        if (eventType == eventPlayerChange)
        {
            return infoEvent("Turn "+turnNumber+", player "+getPlayer());
        }

        JPanel p = new JPanel();
        this.p = p;
        
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);

        if (eventType == eventMulligan)
        {
            Chit solidMarker = getSolidMarker();
            p.add(solidMarker);

            p.add(Box.createRigidArea(new Dimension(5,0)));
            addLabel(getEventTypeText()+": ");
            
            Chit oldDie = new MovementDie(this.scale,
                    MovementDie.getDieImageName(oldRoll));
            oldDie.setAlignmentX(Component.LEFT_ALIGNMENT);
            p.add(oldDie);

            addLabel(" => ");

            Chit newDie = new MovementDie(this.scale,
                    MovementDie.getDieImageName(newRoll));
            newDie.setAlignmentX(Component.LEFT_ALIGNMENT);
            p.add(newDie);
            
            return p;
        }
        
        addMarker(markerId);
        p.add(Box.createRigidArea(new Dimension(5,0)));
        
        if (eventType == eventWinner || eventType == eventEliminated)
        {
            addLabel(getEventTypeText());
            return p;
        }
       
        
        addLabel(getEventTypeText()+": ");
        
        if (eventType == eventSplit)
        {
            int oldHeight = height+height2;
            addLabel("("+oldHeight+") ==> ");
            addMarker(markerId);
            addLabel("("+height+")");
            addMarker(markerId2);
            addLabel("("+height2+")");
        }
        else if (eventType == eventRecruit)
        {
            Iterator it = knownCreatures.iterator();
            while (it.hasNext())
            {   
                RevealedCreature rc = (RevealedCreature)it.next();
                if (rc.wasRecruited())
                {
                    addLabel( " => ");
                }
                String creature = rc.getName();
                addCreature(creature);
            }
        }
        else if (eventType == eventSummon)
        {
            Iterator it = knownCreatures.iterator();
            while (it.hasNext())
            {   
                RevealedCreature rc = (RevealedCreature)it.next();
                String creature = rc.getName();
                addCreature(creature);
            }
            addLabel(" to ");
            addMarker(markerId2);
        }

        else if ( knownCreatures != null )
        {
            Iterator it = knownCreatures.iterator();
            while (it.hasNext())
            {   
                RevealedCreature rc = (RevealedCreature)it.next();
                String creature = rc.getName();
                addCreature(creature);
            }
        }
        
        return p;
    }
}
