Colossus alpha     October 9, 2000    Copyright 2000 David Ripton

This is Colossus.  It's an attempt at a Java clone of Avalon Hill's
Titan(tm) boardgame.

It's not done yet.  Right now it only allows hotseat play.  I'll do client /
server network play next, after getting bored fixing bugs in the hotseat game.  
I do not intend to add internal computer players, but I'll publish the 
server socket interface so that anyone who wants to can write a client-side 
bot.

This program is freeware, distributed under the GNU public license, which
is included in the file COPYING.GPL.  This means that you have the right to
make and distribute changes, as long as you always include the source code
so that others can do the same.  If you fix any bugs or add any features, 
please send me a copy so that I can fold them into the master code.  (Please 
don't add anything that would be a copyright violation.)


Running the game requires the following:

1. A 1.2 or later version of a JRE (Java runtime environment) or JDK 
   (Java development kit).  A JRE is a smaller download; a JDK also 
   lets you compile Java programs, not just run them.

   (Colossus used to run under JDK 1.1, but I recently made some
   changes that break backwards compatibility.  If this affects you,
   let me know.)

   Colossus will now also run as an applet in a web browser, as long
   as your browser is new enough to support JDK 1.2 applets.  That
   probably means downloading a recent version of Sun's Java plug-in 
   (included with the JRE), since most browsers come with out-of-date 
   Java virtual machines.

   Win32, Solaris, and Linux versions of the JDK and JRE are freely 
   downloadable from java.sun.com.  Another Linux port is available 
   from www.blackdown.org.  Info on other ports is available here:
   http://java.sun.com/cgi-bin/java-ports.cgi

   Here's my impression of how well various Java environments run
   Colossus.  When I say that one is buggy, I mean that it has 
   obvious problems running Colossus, not that it's worthless in
   general.  All the Java environments have strong and weak points,
   but the AWT is usually a weak point.

   Windows: Sun's 1.3 is good, but there are memory leaks
      in the HotSpot garbage collector.  Turning off client 
      HotSpot will make it slower but fix the memory
      leaks.  1.2.2 is okay.  1.2.0 and 1.2.1 have very serious 
      memory leaks in the image handling code, so I don't 
      recommend them.

   Solaris Sparc: Recent Sun boxes come with a pre-installed 
      1.2.x JDK, which is okay.  1.3 is faster, but has 
      memory leaks unless you turn off HotSpot.

   Linux: The Sun and Blackdown 1.2.2 versions are okay.  Sun's
      1.3 beta refresh is buggier.  (I haven't tried 1.3 final 
      yet; it might be better.)  kaffe is buggy.  IBM's JDK 1.3 
      is buggy.

   If you have something else, you might find bugs that I haven't.  
   If you manage to get Colossus running on a system I haven't tried, 
   please email me.  (In particular, I'd like to know if it works on
   a Mac.)

2. A reasonably fast computer capable of displaying high resolution graphics,
   with a two-button mouse.
   
   What is reasonably fast?  It depends.  I think Colossus runs acceptably
   on a Pentium 133 with 64 MB and a JVM with a JIT.  Your opinion may vary.
   The client-side HotSpot JVM in JDK 1.3 seems to help, so if you have a 
   slower machine, you might want to upgrade your JRE.

   The game looks best in 1280x960 or higher, but I've added some scaling
   so you should be able to get by in 1024x768.  Less should work but will
   be really ugly.  The Java AWT requires at least a 256-color display; it 
   might work with less, but colors will be off.

   Colossus used to require Swing, but it doesn't anymore.  But it might
   again someday.  (Swing is included in JDK 1.2+, so there's no longer
   quite as much reason to avoid it.)

   Java handles mouse buttons beyond the first badly.  In a perfect world,
   option-click on a Macintosh and the second mouse button on a PC would
   fire the same Java event.  They don't.  Because I don't have a Mac,
   I don't know if Colossus works correctly on one.  If you have a Mac
   with a one-button mouse, please let me know how things work.


Directions:

I assume you already know how to play Titan.  The rules are copyrighted by 
Avalon Hill, so I can't provide them.  Titan is an excellent boardgame, and I 
recommend that everyone buy a copy while you still can.  (Avalon Hill was
recently bought by Hasbro, which may or may not decide to reprint Titan in
something like its current form.)

Probably the best way to run Colossus is to use the executable jar file.  
If you downloaded Colossus.jar by itself, you're set.  If you downloaded
a zip file, you'll need to unzip it and find the jar file inside.  Try
double-clicking on Colossus.jar in your GUI file manager.  If that
doesn't work, pop up a command prompt, cd to the directory where 
you unzipped the zip file, and try typing "java -jar Colossus.jar"

Another alternative is to run Colossus as an applet.  Point your
web browser to the included index.html file, and hope it works.  If
it doesn't, you probably need to install Sun's Java plug-in.

Once you get things running, a dialog should pop up, allowing you to 
type in up to six player names.  Put names in some of the boxes, then 
click "OK"  Another dialog will pop up, telling you which tower each 
player gets and letting players pick colors in increasing order of 
tower number.  Pick colors for each player.  If you misclick, click 
"Restart"  When every player has a color, click "Done"

Now a window will pop up for each player, letting him pick his initial legion 
marker.  (You'll notice that right now the legion markers are just colored 
squares with numbers from 1 to 12 in them.  Feel free to draw new ones.)  
Pick one.

After each player has picked his initial legion marker, the BattleMap window 
will pop up.  You'll see each player's initial legion marker sitting in a 
tower hex.  You'll also see two small windows near the top of the screen.  One 
is the Game Status window, which tracks each player's score, number of legion 
markers remaining, etc.  The other is the Turn dialog, which displays whose 
turn it is, which phase it is, and has some buttons that the active player can 
click.

You can right-click on a legion to see its contents (note that the contents
of other players' legions are hidden, except for those creatures which have
recently been revealed via fighting, recruiting, or teleporting).  You can 
right-click on a hex to call up a menu, which lets you either see what you
can recruit in that hex, or its battle map.

The active player first needs to split his initial 8-high legion.  You'll 
notice that the hex containing the active player's legion is lit up as a 
reminder; in future turns this will happen for all 7-high legions.  (It's 
also legal to split legions with 4-6 characters in them.)  Click on the 
legion.  A dialog will come up to let you pick the new legion marker to use.  
Then another dialog will come up, allowing you to move characters between the 
two legions.  The game will not let you leave the split phase until each of 
your legions contains three Creatures and one Lord.  When you're ready, click 
"Done with Splits" in the Turn window.

Next comes the movement phase.  The game will tell you your movement roll.  
Click on a legion, and the places it can move will light up.  Click on one of 
those places, and the legion will move there.  (In some cases, you will need 
to choose whether to teleport or move normally.)  The "Undo Last Move" and 
"Undo All Moves" buttons are there in case you change your mind.  During the 
first turn only, and once only, there will be a "Take Mulligan" button which 
you can use to re-roll your movement.  When you're done moving everything you 
want to move, click "Done with Moves" 

If you moved any legions onto enemy legions, then next comes the engagement 
phase.  Each hex with an engagement will light up.  Click on the one you want 
to resolve first, and a window will pop up showing both legions and giving the 
defender a chance to flee, if applicable.  If the defender doesn't flee, then 
the attacker is given a chance to concede.  If both legions stick around, then
a negotiation window pops up, where creatures can be clicked on to "X" them 
away.  If the combatants can come to an agreement where all the creatures on 
at least one side die, then there's no need to fight.  Otherwise, it's time 
for battle.

During a battle, the appropriate BattleMap pops up, with each legion on the 
appropriate entry side.  (You have to choose an entry side during movement 
when more than one is possible.)  The defender goes first.  Click on each 
character, and the places it can move light up.  Click on one of those 
places, and the character moves there.  Repeat until all characters are 
on-board, unless you'd like to leave some off-board to die for some reason.  
The "Undo Last Move" and "Undo All Moves" buttons are available.  When done, 
click "Done with Moves"  The attacker repeats the process, except that after 
he finishes moving, it's strike time.  

Any creatures adjacent to an enemy must strike; rangestrikers with an enemy in 
range and line of sight may strike.  Click the striker, and all his legal 
targets light up.  Pick one, and he tries to strike it.  (If it's legal to 
take a strike penalty in order to carry, then a dialog will pop up to ask if 
you want to do so.)  The number of hits are displayed on the target.  If the 
target is dead, it will have a big "X" displayed over it.  If there is excess 
damage that can legally carry over, then the legal carry target(s) will light 
up, and the striking player needs to pick which one to carry to, or click 
somewhere else to decline the carry.  This process can repeat.  There's no way 
to undo strikes.  When done striking, click "Done with Strikes"

After the strike phase, the other player gets a strikeback phase.  It's 
identical to the strike phase, except that rangestrikes are not allowed.

The first turn after he kills an opposing character, the attacker may be 
allowed to summon an angel or archangel.  If so, a dialog will appear and all 
MasterBoard hexes with summonable angels will light up.  The attacker must 
click on one of those hexes, then select the angel or archangel as 
appropriate in the dialog.

During turn 4 of the battle, the defender may be allowed to muster a recruit.  
If so, a dialog will pop up showing the legal recruits.  If desired, pick one.

When the battle finishes, the winner gets some points and maybe the option of 
acquiring one or more angels or archangels.  If the winner didn't summon 
an angel or recruit a reinforcement earlier, he will get another choice if
eligible.

After all engagements are resolved, click the "Done with Engagements" button 
to proceed to the mustering phase.  Legions that moved and can recruit will 
light up.  Click on each one and choose a recruit.  If more than one type of
creature is capable of summoning that recruit, you'll have to choose the
recruiter(s).  When done, click "Done with Turn" and pass the mouse to the 
next player.

The game ends when zero or one Titans remain.  The last player standing is
the winner; if the game ends with a mutual elimination, it's a draw.


Improvements:

If you find any bugs that you think I can fix, please let me know, in 
as much detail as possible.  The ones I know about should be in BUGLIST.txt

The features that I'm planning to add are in TODO.txt  Other people can
also make improvements.

Java is still evolving, and the JDK itself still has plenty of bugs, 
especially in the AWT.  You can vote for the ones that you find most 
annoying at http://developer.java.sun.com

I've tried to get the rules right, though a few areas (concession timing,
in particular) are still off.  Bruno Wolff's Titan Errata and Clarifications 
at http://www.uwm.edu/~bruno/titan/errata.shtml is a good place to check for
rules issues.

There is currently some very rough save / load game code in place.  The
game produces a numbered .sav file in the saves/ subdirectory every turn.  
If you start a new game with "java -jar Colossus.jar filename.sav" 
instead of just "java -jar Colossus" then the save file will be loaded.  
"java -jar Colossus.jar --latest" will load the most recent save game in 
the saves/ subdirectory.  You'll want to delete all those save files 
from time to time.  I'll add a better user interface for this eventually.


Credits:

Programming:  David Ripton  dripton@wizard.net
              David Barr
Counter art:  Tchula Ripton
Bug reports:  Anthony Kam, Bruce Sherrod, Augustin Ku, David Barr, 
              Sean McCulloch, Luca Ferraro

