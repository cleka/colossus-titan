
package net.sf.colossus.protocol;

public class LegionEvent extends GameEvent
{
	public final static int nLEGION_DIED = 1;
	public final static int nLEGION_LOST_CHARACTER = 2;
	public final static int nLEGION_GAINED_CHARACTER = 3;

	private int m_nEventType;

	public LegionEvent(Object oSource, int nEventType)
		{
			super(oSource);
			m_nEventType = nEventType;
		}

	public int getEventType()
		{
			return m_nEventType;
		}
	
// -----------------------------
// OVERRIDE OBJECT

	public String toString()
		{
			return "LegionEvent[" + getSource() + ", " + getEventType() + "]";
		}
}
