package net.sf.colossus.variant;

public class PowerSkill
{
    private final String name;
    private final int power_attack;
    private final int power_defend; // how many dice attackers lose
    private final int skill_attack;
    private final int skill_defend;
    private double hp; // how many hit points or power left
    private final double value;

    public PowerSkill(String nm, int p, int pa, int pd, int sa, int sd)
    {
        name = nm;
        power_attack = pa;
        power_defend = pd;
        skill_attack = sa;
        skill_defend = sd;
        hp = p; // may not be the same as power_attack!
        value = p * Math.min(sa, sd);
    }

    public PowerSkill(String nm, int pa, int sa)
    {
        this(nm, pa, pa, 0, sa, sa);
    }

    public int getPowerAttack()
    {
        return power_attack;
    }

    public int getPowerDefend()
    {
        return power_defend;
    }

    public int getSkillAttack()
    {
        return skill_attack;
    }

    public int getSkillDefend()
    {
        return skill_defend;
    }

    public double getHP()
    {
        return hp;
    }

    public void setHP(double h)
    {
        hp = h;
    }

    public void addDamage(double d)
    {
        hp -= d;
    }

    public double getPointValue()
    {
        return value;
    }

    public String getName()
    {
        return name;
    }

    @Override
    public String toString()
    {
        return name + "(" + hp + ")";
    }
}