package net.sf.colossus.ai.objectives;


import net.sf.colossus.util.ValueRecorder;


/**
 * Interface for a tactical (i.e. when fighting on battlelands) objective.
 * The idea is to set 'objectives' for tha AI to attain when fighting,
 * and then try to bias the moves of the AIs toward achieving said objectives.
 * @author Romain Dolbeau
 */
public interface TacticalObjective
{
    /** Whether the objective is already achieved
     *
     * @return Whether the objective is already achieved
     */
    boolean objectiveAttained();

    /** How much does the 'current situation' contributes to the objective.
     * The actual value is currently added to the overall evaluation
     * of the whole legion move.
     * @return How much does he 'current situation' contributes to the objective
     */
    ValueRecorder situationContributeToTheObjective();

    /** Get the current priority of this objective.
     *
     * @return The current priority of this objective.
     */
    float getPriority();

    /** Get the description of this objective.
     *
     * @return The description of this objective.
     */
    String getDescription();

    /** Change the priority of this objective.
     *
     * @param newPriority The new priority.
     * @return The old priority.
     */
    float changePriority(float newPriority);
}
