import java.awt.*;

/**
 * Class BattleChit implements the GUI for a Titan chit representing
 * a creature on a BattleMap.
 * @version $Id$
 * @author David Ripton
 */

class BattleChit extends Chit
{
    private Legion legion;
    private Critter critter;
    private BattleMap map;
    private boolean moved = false;
    private boolean struck = false;

    private BattleHex currentHex;
    private BattleHex startingHex;

    // Damage taken
    private int hits = 0;

    // Mark whether this chit is a legal carry target.
    private boolean carryFlag = false;


    BattleChit(int cx, int cy, int scale, String imageFilename,
        Container container, Critter critter, BattleHex hex,
        Legion legion, BattleMap map)
    {
        super(cx, cy, scale, imageFilename, container);
        this.critter = critter;
        this.currentHex = hex;
        this.startingHex = hex;
        this.map = map;
        this.legion = legion;
    }


    public Critter getCritter()
    {
        return critter;
    }


    public int getHits()
    {
        return hits;
    }


    public void setHits(int damage)
    {
        hits = damage;
    }


    public int getPower()
    {
        return critter.getPower();
    }


    public void checkForDeath()
    {
        if (hits >= getPower())
        {
            setDead(true);
            currentHex.repaint();
        }
    }


    public Legion getLegion()
    {
        return legion;
    }


    public Player getPlayer()
    {
        return legion.getPlayer();
    }


    public boolean hasMoved()
    {
        return moved;
    }


    public void commitMove()
    {
        startingHex = currentHex;
        moved = false;
    }


    public boolean hasStruck()
    {
        return struck;
    }


    public void commitStrike()
    {
        struck = false;
    }


    public BattleHex getCurrentHex()
    {
        return currentHex;
    }


    public BattleHex getStartingHex()
    {
        return startingHex;
    }


    // Dead chits count as chits in contact only if countDead is true.
    public int numInContact(boolean countDead)
    {
        // Offboard chits are not in contact.
        if (currentHex.isEntrance())
        {
            return 0;
        }

        int count = 0;
        for (int i = 0; i < 6; i++)
        {
            // Adjacent creatures separated by a cliff are not in contact.
            if (currentHex.getHexside(i) != 'c' &&
                currentHex.getOppositeHexside(i) != 'c')
            {
                BattleHex hex = currentHex.getNeighbor(i);
                if (hex != null)
                {
                    if (hex.isOccupied())
                    {
                        BattleChit chit = hex.getChit();
                        if (chit.getPlayer() != getPlayer() &&
                            (countDead || !chit.isDead()))
                        {
                            count++;
                        }
                    }
                }
            }
        }

        return count;
    }

    // Dead chits count as chits in contact only if countDead is true.
    public boolean inContact(boolean countDead)
    {
        return (numInContact(countDead) > 0);
    }


    public void moveToHex(BattleHex hex)
    {
        currentHex.removeChit(this);
        currentHex = hex;
        currentHex.addChit(this);
        moved = true;
        map.markLastChitMoved(this);
        map.repaint();
    }


    public void undoMove()
    {
        currentHex.removeChit(this);
        currentHex = startingHex;
        currentHex.addChit(this);
        moved = false;
        map.clearLastChitMoved();
        map.repaint();
    }


    // Return the number of dice that will be rolled when striking this
    // target, including modifications for terrain.
    public int getDice(BattleChit target)
    {
        BattleHex targetHex = target.getCurrentHex();

        int dice = getPower();

        boolean rangestrike = !inContact(true);
        if (rangestrike)
        {
            dice /= 2;

            // Dragon rangestriking from volcano: +2
            if (critter.getName().equals("Dragon") && 
                currentHex.getTerrain() == 'v')
            {
                dice += 2;
            }
        }
        else
        {
            // Dice can be modified by terrain.
            // Dragon striking from volcano: +2
            if (critter.getName().equals("Dragon") && 
                currentHex.getTerrain() == 'v')
            {
                dice += 2;
            }

            // Adjacent hex, so only one possible direction.
            int direction = map.getDirection(currentHex, targetHex, false);
            char hexside = currentHex.getHexside(direction);
            char oppHexside = currentHex.getOppositeHexside(direction);

            // Native striking down a dune hexside: +2
            if (hexside == 'd' && critter.isNativeSandDune())
            {
                dice += 2;
            }
            // Native striking down a slope hexside: +1
            else if (hexside == 's' && critter.isNativeSlope())
            {
                dice++;
            }
            // Non-native striking up a dune hexside: -1
            else if (oppHexside == 'd' && !critter.isNativeSandDune())
            {
                dice--;
            }
        }

        return dice;
    }


    public int getAttackerSkill(BattleChit target)
    {
        BattleHex targetHex = target.getCurrentHex();

        int attackerSkill = critter.getSkill();

        boolean rangestrike = !inContact(true);

        // Skill can be modified by terrain.
        if (!rangestrike)
        {
            // Non-native striking out of bramble: -1
            if (currentHex.getTerrain() == 'r' && !critter.isNativeBramble())
            {
                attackerSkill--;
            }

            if (currentHex.getElevation() > targetHex.getElevation())
            {
                // Adjacent hex, so only one possible direction.
                int direction = map.getDirection(currentHex, targetHex, false);
                char hexside = currentHex.getHexside(direction);
                // Striking down across wall: +1
                if (hexside == 'w')
                {
                    attackerSkill++;
                }
            }
            else if (currentHex.getElevation() < targetHex.getElevation())
            {
                // Adjacent hex, so only one possible direction.
                int direction = map.getDirection(targetHex, currentHex, false);
                char hexside = targetHex.getHexside(direction);
                // Non-native striking up slope: -1
                // Striking up across wall: -1
                if ((hexside == 's' && !critter.isNativeSlope()) ||
                    hexside == 'w')
                {
                    attackerSkill--;
                }
            }

        }
        else if (!critter.getName().equals("Warlock"))
        {
            // Range penalty
            if (map.getRange(currentHex, targetHex) == 4)
            {
                attackerSkill--;
            }

            // Non-native rangestrikes: -1 per intervening bramble hex
            if (!critter.isNativeBramble())
            {
                attackerSkill -= map.countBrambleHexes(currentHex, targetHex);
            }

            // Rangestrike up across wall: -1 per wall
            boolean wall = false;
            for (int i = 0; i < 6; i++)
            {
                if (targetHex.getHexside(i) == 'w')
                {
                    wall = true;
                }
            }
            if (wall)
            {
                if (targetHex.getElevation() > currentHex.getElevation())
                {
                    attackerSkill -= (targetHex.getElevation() -
                        currentHex.getElevation());
                }
            }

            // Rangestrike into volcano: -1
            if (targetHex.getTerrain() == 'v')
            {
                attackerSkill--;
            }
        }

        return attackerSkill;
    }


    public int getStrikeNumber(BattleChit target)
    {
        BattleHex targetHex = target.getCurrentHex();
        boolean rangestrike = !inContact(true);

        int attackerSkill = getAttackerSkill(target);
        int defenderSkill = target.getCritter().getSkill();

        int strikeNumber = 4 - attackerSkill + defenderSkill;

        // Strike number can be modified directly by terrain.
        // Native defending in bramble, from strike by a non-native: +1
        // Native defending in bramble, from rangestrike by a non-native
        //     non-warlock: +1
        if (targetHex.getTerrain() == 'r' &&
            target.getCritter().isNativeBramble() &&
            !critter.isNativeBramble() &&
            !(rangestrike && critter.getName().equals("Warlock")))
        {
            strikeNumber++;
        }

        // Sixes always hit.
        if (strikeNumber > 6)
        {
            strikeNumber = 6;
        }

        return strikeNumber;
    }


    // Allow the player to choose whether to take a penalty
    // (fewer dice or higher strike number) in order to be
    // allowed to carry.  Return true if the penalty is taken,
    // or false if it is not.
    private boolean chooseStrikePenalty(BattleChit [] carryTargets)
    {
        StringBuffer prompt = new StringBuffer(
            "Take strike penalty to allow carrying to ");

        for (int i = 0; i < carryTargets.length; i++)
        {
            prompt.append(carryTargets[i].getCritter().getName() + " in " +
                carryTargets[i].getCurrentHex().getTerrainName().toLowerCase());
            if (i < carryTargets.length - 1)
            {
                prompt.append(", ");
            }
        }
        prompt.append("?");

        new OptionDialog(map, "Take Strike Penalty?", prompt.toString(), 
            "Take Penalty", "Do Not Take Penalty");

        return (OptionDialog.getLastAnswer() == OptionDialog.YES_OPTION);
    }


    // Sort penalty options by number of dice (ascending), then by
    //    strike number (descending).
    private void sortPenaltyOptions(PenaltyOption [] penaltyOptions,
        int numPenaltyOptions)
    {
        for (int i = 0; i < numPenaltyOptions - 1; i++)
        {
            for (int j = i + 1; j < numPenaltyOptions; j++)
            {
                if (penaltyOptions[i].compareTo(penaltyOptions[j]) > 0)
                {
                    PenaltyOption temp = penaltyOptions[i];
                    penaltyOptions[i] = penaltyOptions[j];
                    penaltyOptions[j] = temp;
                }
            }
        }
    }


    // Calculate number of dice and strike number needed to hit target,
    // and whether any carries are possible.  Roll the dice and apply
    // damage.  Highlight legal carry targets.
    public void strike(BattleChit target)
    {
        // sanity check
        if (target.getLegion().getPlayer() == legion.getPlayer())
        {
            System.out.println("tried to strike own creature!");
            return;
        }

        BattleHex targetHex = target.getCurrentHex();

        boolean carryPossible = true;
        if (numInContact(false) < 2)
        {
            carryPossible = false;
        }

        int dice = getDice(target);

        // Carries are only possible if the striker is rolling more dice than
        // the target has hits remaining.
        if (dice <= target.getPower() - target.getHits())
        {
            carryPossible = false;
        }

        int strikeNumber = getStrikeNumber(target);

        // Figure whether number of dice or strike number needs to be
        // penalized in order to carry.
        if (carryPossible)
        {
            // Count legal carry targets.
            int numCarryTargets = 0;

            // Count strike penalty options
            int numPenaltyOptions = 0;

            PenaltyOption [] penaltyOptions = new PenaltyOption[5];


            for (int i = 0; i < 6; i++)
            {
                // Adjacent creatures separated by a cliff are not in contact.
                if (currentHex.getHexside(i) != 'c' &&
                    currentHex.getOppositeHexside(i) != 'c')
                {
                    BattleHex hex = currentHex.getNeighbor(i);
                    if (hex != null && hex != targetHex && hex.isOccupied())
                    {
                        BattleChit chit = hex.getChit();
                        if (chit.getPlayer() != getPlayer() && !chit.isDead())
                        {
                            int tmpDice = getDice(chit);
                            int tmpStrikeNumber = getStrikeNumber(chit);

                            // Strikes not up across dune hexsides cannot
                            // carry up across dune hexsides.
                            if (currentHex.getOppositeHexside(i) == 'd')
                            {
                                int direction = map.getDirection(targetHex,
                                    currentHex, false);
                                if (targetHex.getHexside(direction) != 'd')
                                {
                                    chit.setCarryFlag(false);
                                }
                            }

                            else if (tmpStrikeNumber > strikeNumber ||
                                tmpDice < dice)
                            {
                                // Add this scenario to the list. 
                                penaltyOptions[numPenaltyOptions] = new 
                                    PenaltyOption(chit, tmpDice, 
                                    tmpStrikeNumber);
                                numPenaltyOptions++;
                            }

                            else
                            {
                                chit.setCarryFlag(true);
                                numCarryTargets++;
                            }
                        }
                    }
                }
            }


            // Sort penalty options by number of dice (ascending), then by
            //    strike number (descending).
            sortPenaltyOptions(penaltyOptions, numPenaltyOptions);


            // Find the group of PenaltyOptions with identical dice and
            //    strike numbers.

            int last = -1;

            while (++last < numPenaltyOptions)
            {
                int first = last; 
                while (last + 1 < numPenaltyOptions && 
                    penaltyOptions[last + 1].getDice() == 
                        penaltyOptions[first].getDice() &&
                    penaltyOptions[last + 1].getStrikeNumber() ==
                        penaltyOptions[first].getStrikeNumber())
                {
                    last++;
                }

                int tmpDice = penaltyOptions[first].getDice();
                int tmpStrikeNumber = penaltyOptions[first].getStrikeNumber();

                // Make sure the penalty is still relevant.
                if (tmpStrikeNumber > strikeNumber || tmpDice < dice) 
                {
                    BattleChit [] chits = new BattleChit[last - first + 1];
                    for (int i = first; i <= last; i++)
                    {
                        chits[i - first] = penaltyOptions[i].getChit();
                    }
                                    
                    if (chooseStrikePenalty(chits))
                    {
                        if (tmpStrikeNumber > strikeNumber)
                        {
                            strikeNumber = tmpStrikeNumber;
                        }
                        if (tmpDice < dice)
                        {
                            dice = tmpDice;
                        }
                        for (int i = 0; i < chits.length; i++)
                        {
                            chits[i].setCarryFlag(true);
                            numCarryTargets++;
                        }
                    }
                }
            }


            if (numCarryTargets == 0)
            {
                carryPossible = false;
            }
        }


        // Roll the dice.
        int damage = 0;

        int [] rolls = new int[dice];
        for (int i = 0; i < dice; i++)
        {
            rolls[i] = (int) Math.ceil(6 * Math.random());

            if (rolls[i] >= strikeNumber)
            {
                damage++;
            }
        }

        int totalDamage = target.getHits();
        totalDamage += damage;
        int carryDamage = 0;
        int power = target.getPower();
        if (totalDamage > power)
        {
            if (carryPossible)
            {
                carryDamage = totalDamage - power;
            }
            totalDamage = power;
        }
        target.setHits(totalDamage);
        target.checkForDeath();
        target.repaint();

        // Let the attacker choose whether to carry, if applicable.
        if (carryDamage > 0)
        {
            map.highlightCarries(carryDamage);
        }

        // Record that this attacker has struck.
        struck = true;

        // Display the rolls in the showDice dialog.
        ShowDice showDice = map.getShowDice();
        showDice.setAttacker(this);
        showDice.setDefender(target);
        showDice.setTargetNumber(strikeNumber);
        showDice.setRolls(rolls);
        showDice.setHits(damage);
        showDice.setCarries(carryDamage);
        showDice.setup();
    }


    public boolean getCarryFlag()
    {
        return carryFlag;
    }


    public void setCarryFlag(boolean flag)
    {
        carryFlag = flag;
    }


    public void paint(Graphics g)
    {
        super.paint(g);

        if (hits > 0 && !isDead())
        {
            String hitString = Integer.toString(hits);
            Rectangle rect = getBounds();

            // Construct a font 3 times the size of the current font.
            Font oldFont = g.getFont();
            String name = oldFont.getName();
            int size = oldFont.getSize();
            int style = oldFont.getStyle();
            Font font = new Font(name, style, 3 * size);
            g.setFont(font);

            FontMetrics fontMetrics = g.getFontMetrics();
            int fontHeight = fontMetrics.getAscent();
            int fontWidth = fontMetrics.stringWidth(hitString);

            // Provide a high-contrast background for the number.
            g.setColor(Color.white);
            g.fillRect(rect.x + (rect.width - fontWidth) / 2,
                rect.y + (rect.height - fontHeight) / 2,
                fontWidth, fontHeight);

            // Show number of hits taken in red.
            g.setColor(Color.red);

            g.drawString(hitString, rect.x + (rect.width - fontWidth) / 2,
                rect.y + (rect.height + fontHeight) / 2);

            // Restore the font.
            g.setFont(oldFont);
        }
    }
}
