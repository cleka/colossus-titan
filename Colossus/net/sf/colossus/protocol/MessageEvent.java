
package net.sf.colossus.protocol;

/**

SAY <message>
    Received message to all players and observers.

TELL <player> <message>
    Received message to one player.

KIBBITZ <message>
    Received chat message to all observers.

 */
public class MessageEvent extends GameEvent
{
    MessageEvent(Object oSource)
	{
	    super(oSource);
	}
}
