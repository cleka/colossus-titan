package net.sf.colossus.ai.helper;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.colossus.client.CritterMove;
import net.sf.colossus.util.Glob;


/** LegionMove has a List of one CritterMove per mobile critter
 *  in the legion.
 * Originally in SimpleAI, which at the time had the following authors.
 * @author Bruce Sherrod, David Ripton, Romain Dolbeau
*/
public class LegionMove implements Comparable<LegionMove>
{
    private final List<CritterMove> critterMoves = new ArrayList<CritterMove>();
    private Map<CritterMove, String> evaluation = null;
    private String lmeval = null;
    private int value;

    public void add(CritterMove cm)
    {
        critterMoves.add(cm);
    }

    public List<CritterMove> getCritterMoves()
    {
        return Collections.unmodifiableList(critterMoves);
    }

    public void resetEvaluate()
    {
        evaluation = null;
        lmeval = null;
        value = Integer.MIN_VALUE;
    }

    public int getValue()
    {
        return value;
    }

    public void setValue(int v)
    {
        value = v;
    }

    public void setEvaluate(CritterMove cm, String val)
    {
        if (evaluation == null)
            evaluation = new HashMap<CritterMove, String>();
        evaluation.put(cm, val);
    }

    public void setEvaluate(String val)
    {
        lmeval = val;
    }

    @Override
    public String toString()
    {
        List<String> cmStrings = new ArrayList<String>();
        for (CritterMove cm : critterMoves)
        {
            cmStrings.add(cm.toString());
        }
        return Glob.glob(", ", cmStrings);
    }

    public String getStringWithEvaluation()
    {
        List<String> cmStrings = new ArrayList<String>();
        for (CritterMove cm : critterMoves)
        {
            StringBuffer buf = new StringBuffer();
            buf.append(cm.toString());
            if (evaluation != null)
            {
                if (evaluation.containsKey(cm))
                {
                    buf.append(" [");
                    buf.append(evaluation.get(cm));
                    buf.append("]");
                }
            }
            cmStrings.add(buf.toString());
        }
        if (lmeval != null)
            cmStrings.add(" {" + lmeval + "}");
        return Glob.glob(", \n", cmStrings);
    }

    @Override
    public boolean equals(Object ob)
    {
        if (!(ob instanceof LegionMove))
        {
            return false;
        }
        LegionMove lm = (LegionMove)ob;
        return toString().equals(lm.toString());
    }

    public int compareTo(LegionMove m)
    {
        if (this.equals(m))
            return 0; // we're trying to guarantee consistency with equals

        if (this.getValue() < m.getValue())
            return -1;
        if (this.getValue() > m.getValue())
            return 1;

        return this.toString().compareTo(m.toString());
    }

    @Override
    public int hashCode()
    {
        return toString().hashCode();
    }
}