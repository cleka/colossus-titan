Colossus alpha     Copyright 2002 David Ripton
$Id$

Colossus is an attempt at a Java clone of Avalon Hill's Titan(tm) boardgame.

It's not done yet.  Right now it allows hotseat play, and play against a
mostly working but not-quite-ready-for-prime-time AI.  Client/server 
networking is in development.  (Really, we're getting closer.)

This program is freeware, distributed under the GNU public license, which
is included in the file COPYING.GPL.  This means that you have the right to
make and distribute changes, as long as you always include the source code
so that others can do the same.  If you fix any bugs or add any features,
please send me a copy so that I can fold them into the master code.


Running the game requires: 

A 1.2 (aka "Java 2") or later version of a JRE (Java runtime environment) 
or JDK (Java development kit).  

(Colossus will not run under JRE 1.0.x or 1.1.x  It won't currently run 
as an applet in a web browser.  The obsolete Microsoft JVM that's bundled
with IE and Windows won't work.  Sorry.)

Windows, Solaris, and x86 Linux versions of the JRE and JDK are freely 
downloadable from http://java.sun.com.

Another Linux port is available from http://www.blackdown.org.  

The Mac version is at http://devworld.apple.com/java/

Info on other ports (AIX, OS/2, etc.) is at 
http://java.sun.com/cgi-bin/java-ports.cgi

The current recommended version is Sun JRE 1.3.1.

If in doubt, you probably want:
http://java.sun.com/j2se/1.3/jre/download-windows.html

You also need a computer with a mouse and a color display.  I'm not 
sure exactly what the minimum system spec is anymore.  A Pentium
133 with 64MB is slow but works.  If you try the game on something 
slower or with less memory, please let me know your system specs
and how well it worked.



GETTING THE PROGRAM TO RUN

The easiest way to run the game, if you already have Java Web Start (or
an equivalent JNLP launcher) installed on your computer, is to click on 
the Java Web Start link on the web page.  This will download the game, 
upgrade your JRE if necessary, make a shortcut icon if you want, etc.
Really slick, when it works.  If it doesn't work, you have other options.

Another option is to run the executable jar file. If you downloaded 
Colossus.jar by itself, you're set.  If you downloaded a zip file, you'll 
need to unzip it using your favorite unzip tool and find the jar file 
inside.  Try double-clicking on Colossus.jar in your GUI file manager.  
If that doesn't work, pop up a command prompt, cd to the directory where 
you unzipped the zip file, and try typing "java -jar Colossus.jar"

Yet another option, in case the jar file is temporarily broken for some 
reason, is to expand the whole game tree and run the Start class manually.  
Unzip the zip file and try "java net.sf.colossus.server.Start"



GAME PLAY INSTRUCTIONS

I assume you already know how to play Titan.  The full rules are copyrighted 
by Avalon Hill, so I can't provide them.  Titan is an excellent boardgame, 
and I recommend that everyone buy a copy while you still can.  (Avalon Hill 
was bought by Hasbro, which may or may not decide to reprint Titan in 
something like its original form.)

Once you get things running, a dialog should pop up, allowing you to
choose up to six player types (human, AI, or not present) and their names.
When you're done, click "OK"  Another dialog will pop up, telling you which 
tower each player gets and letting players pick colors in increasing order 
of tower number.  Pick colors for each player.  If you misclick, click
"Restart"  When every player has a color, click "Done"

Now a window will pop up for each player, letting him pick his initial legion
marker.  Pick one.  (If you really don't care, use the "Auto pick markers"
option on the Player menu.)

After each player has picked his initial legion marker, the MasterBoard window
will pop up.  You'll see each player's initial legion marker sitting in a
tower hex.  You'll also see a small window in the lower right corner of the
screen.  This is the Game Status window, which tracks each player's score,
number of legion markers remaining, etc.  And there's a Caretaker window which 
tracks how many of each creature remain in the stacks.  (These are optional: 
turn them on or off from the Graphics menu.)

You can right-click (or control-click if you have a one-button mouse) on a 
legion to see its contents. (Unless you've selected "All stacks visible" on 
the Game menu, the contents of other players' legions are hidden, except for 
those creatures which have recently been revealed via fighting, recruiting, 
or teleporting).  You can right-click on a hex to call up a menu, which lets 
you either see what you can recruit in that hex, or its battle map.

The active player first needs to split his initial 8-high legion.  You'll
notice that the hex containing the active player's legion is lit up as a
reminder; in future turns this will happen for all 7-high legions.  (It's
also legal to split legions with 4-6 characters in them, even though they
are not highlighted.)  Click on the legion.  A dialog will come up to let
you pick the new legion marker to use.  Then another dialog will come up,
allowing you to move characters between the two legions.  The game will
not let you leave the split phase on the first turn until each of your
legions contains three Creatures and one Lord.  When you're ready, select
Done from the Phase menu.

Next comes the movement phase.  The game will tell you your movement roll.
Click on a legion, and the places it can move will light up.  Little pictures
of the creatures the legion can recruit in hex will also appear.  Click on 
one of those places, and the legion will move there.  (In some cases, you 
will need to choose whether to teleport or move normally, or which lord 
teleports if there is more than once choice.)  The "Undo Last Move" and
"Undo All Moves" actions are there in case you change your mind.  During the
first turn only, and once only, there will be a "Take Mulligan" action which
you can use to re-roll your movement.  When you're done moving everything you
want to move, select Done.

If you moved any legions onto enemy legions, then next comes the engagement
phase.  Each hex with an engagement will light up.  Click on the one you want
to resolve first, and a window will pop up showing both legions and giving the
defender a chance to flee, if applicable.  If the defender doesn't flee, then
the attacker is given a chance to concede.  If both legions stick around, then
a negotiation window pops up, where creatures can be clicked on to "X" them
away.  If the combatants can come to an agreement where all the creatures on
at least one side die, then there's no need to fight.  Otherwise, it's time
for battle.  

(NOTE: Negotiation is currently broken.  It'll be back in soon.)

During a battle, the appropriate BattleMap pops up, with each legion on the
appropriate entry side.  (You have to choose an entry side during movement
when more than one is possible.)  The defender goes first.  Click on each
character, and the places it can move light up.  Click on one of those
places, and the character moves there.  Repeat until all characters are
on-board, unless you'd like to leave some off-board to die for some reason.
The "Undo Last Move" and "Undo All Moves" menu options are available.  When
done moving, click Done.  The attacker repeats the process, except that 
after he finishes moving, it's striking time.

Any creatures adjacent to an enemy must strike; rangestrikers with an enemy in
range and line of sight may strike.  (If you turn on the "Auto forced strike"
option, then creatures that are forced to strike and have only one legal
target will strike first without any intervention on your part, which speeds
things up a bit.)  Click the striker, and all his legal targets light up.  
Pick one, and he tries to strike it.  (If it's legal to take a strike penalty 
in order to carry, then a dialog will pop up to ask if you want to do so.)  
The number of hits are displayed on the target.  If the target is dead, it 
will have a big "X" displayed over it.  If there is excess damage that can 
legally carry over, then the legal carry target(s) will light up, and the
cursor should change to a number, and the striking player needs to pick 
which one to carry to, or click somewhere else to decline the carry.  This 
carry process can repeat if the strike blows through more than one creature.  
There's no way to undo strikes.  (That would be cheating.)  When done 
striking, choose Done. 

After the strike phase, the other player gets a strikeback phase.  It's
identical to the strike phase, except that rangestrikes are not allowed.
Dead creatures do get to strike back before being removed.

The first turn after he kills an opposing character, the attacker may be
allowed to summon an angel or archangel, if there is one available in an
unengaged legion, and he hasn't yet summoned an angel this turn, and the
legion doesn't already have seven creatures.  If so, a dialog will appear
and all MasterBoard hexes with summonable angels will light up.  The
attacker must click on one of those hexes, then select the angel or
archangel as appropriate in the dialog.

During turn 4 of the battle, the defender may be allowed to muster a recruit.
If so, a dialog will pop up showing the legal recruits.  If desired, pick one.
If no recruit is desired, dismiss the recruit dialog. (Click on the X in the
top right corner, or double-click the top left corner, depending on how you
normally dismiss dialogs in your OS.)

When the battle finishes, the winner gets some points and maybe the option of
acquiring one or more angels or archangels.  If the winner didn't summon
an angel or recruit a reinforcement earlier, he will get another choice if
eligible.

After all engagements are resolved, choose Done to proceed to the mustering 
phase.  Legions that moved and can recruit will light up.  Click on each one 
and choose a recruit.  If more than one type of creature is capable of 
summoning that recruit, you'll have to choose the recruiter(s) to be 
revealed, unless the "Autopick recruiter" option has been selected.
When done, click Done and pass the mouse to the next player.

The game ends when zero or one Titans remain.  The last player standing is
the winner; if the game ends with a mutual elimination, it's a draw.



IMPROVEMENTS:

If you find any bugs that you think we can fix, please let us know, in
as much detail as possible.  (In particular, include the OS and JVM 
version.)  The best way to report bugs is via the bug tracker at 
SourceForge -- go to http://colossus.sf.net, click on the SourceForge 
icon, and click on Bugs.  (If that's too hard you can just send email.)

We've tried to get the rules right, though a few areas (concession timing,
in particular) are still off.  Bruno Wolff's Titan Errata and Clarifications
at http://wolff.to/titan/errata.shtml is a good place to check for
rules issues.



FREQUENTLY ASKED QUESTIONS:

Q.  Why does the game sometimes prompt me twice for recruits?

A.  Look closer.  It's asking for the recruit and then for the recruiter(s).
    Do you want to recruit that cyclops with your other cyclops, or with
    your two gargoyles?  If like most people you don't care, then turn on 
    "auto pick recruiter" and the computer will just pick one for you.

Q.  How to I make a legion spin around in a circle back to the original
    hex when I roll a 6? 

A.  The second click needs to be inside the hex, but outside the legion 
    marker.  If you click inside the legion marker then you cancel the
    pending move and try to move the legion again.  I know this requires
    a lot of coordination for a turn-based boardgame, but I can't think
    of a better way to do this interface.

Q.  What does the Antialias option do?

A.  It makes the graphics a bit smoother (look closely at the hexside edges
    while you turn it on and off), but this takes some CPU cycles.  I
    recommend turning it on if you have a fast computer and leaving it off
    if you have a slow computer. 

Q.  When is network play going to be done?

A.  Probably about April 2002.  No guarantees -- real life sometimes
    gets in the way of fun programming projects.

    It's a lot of work, because I want to do it the right away (a 
    language-neutral, simple socket protocol with a server that enforces
    the rules rather than trusting the clients) rather than just slapping
    an quick RMI layer on the existing game.

Q.  Why is the AI so dumb?

A.  Nobody's doing much with the AI right now.  I'll start working
    on it seriously again after the network game is finished.

Q.  What's the difference between SimpleAI and MinimaxAI and RandomAI?

A.  SimpleAI does a straightforward one-ply lookahead for MasterBoard
    moves.  It works fairly well.  MinimaxAI uses a more general minimax 
    algorithm that in theory should be stronger (but slower), but it's 
    still very buggy so it's not recommended for non-developer use.  
    RandomAI just chooses one of the available AIs at random -- this 
    will be a more interesting option when there are multiple viable 
    alternatives.

Q.  What's the Load Variant button do? 

A.  The maps and recruit trees and stuff used to be hardcoded.  Romain
    has recently pulled them out into data files, which means that you
    can customize parts of the game by making new data files instead of
    changing code.  If that interests you, see FileFormat.txt and
    Variant-HOWTO.txt.  The ExtTitan, Badlands, and TitanPlus variants 
    are now included. 

Q.  How can I help?

A.  Bug reports are great.  Detailed bug reports delivered via the 
    SourceForge bug tracker are even better.  
    
    If you want to contribute code, make sure that you're starting 
    from the latest source (so pull from CVS).  Please read and follow
    CodingStandards.txt so your code is easier to merge.  Join and
    send mail to the dev mailing list at SF so we know what you're
    up to.  Beyond that, just code whatever you want and send 
    patches when it works.


Credits:

Programming:  David Ripton  dripton@wizard.net
              Bruce Sherrod (AI)  bruce@thematrix.com
	      Romain Dolbeau (variant support, overlay display)
              Tom Fruchterman (caretaker display)
              David Barr (applet conversion)

Counter art:  Jerry Reiger, David Lum, Tchula Ripton

Overlay art:  Chris Howe (Masterboard), D. U. Thibault (Battlelands)

Network protocol: Falk Hueffner

GUI ideas: Kris Giesing, David Lum

Bug reports:  Anthony Kam, Augustin Ku, Sean McCulloch, Luca Ferraro, 
              Jonathan Woodward, Aneel Nazareth, Paul Macgowan,
              Magnus Berglund, Don Woods, Dean Gaudet, Peter Becker

Web and CVS space, bug tracker, mailing lists, etc.: SourceForge
