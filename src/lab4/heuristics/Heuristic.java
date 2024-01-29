package src.lab4.heuristics;

// SYSTEM IMPORTS
import edu.cwru.sepia.action.ActionType;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;
import edu.cwru.sepia.environment.model.state.ResourceNode.ResourceView;
import edu.cwru.sepia.environment.model.state.State.StateView;


import java.util.Collections;
import java.util.Comparator;
import java.util.List;


// JAVA PROJECT IMPORTS
import edu.bu.lab4.game.tree.Node;
import edu.bu.lab4.game.tree.Node.GameState;
import edu.bu.lab4.game.tree.Node.GameUnit;
import edu.bu.lab4.utilities.Coordinate;
import edu.bu.lab4.utilities.DistanceMetric;


public class Heuristic
    extends Object
{

	public static double getFootmanHeuristicValue(Node node)
	{
		return Heuristic.calculateFootmanDistanceHeuristic(node)
		    + Heuristic.calculateFootmanHealthHeuristic(node)
		    + Heuristic.calculateFootmanDamageHeuristic(node);
	}

	/**
	 * The job of this method is to calcualte the "utility value" of a node based on a the distances between
     * a footman and the closest archer. We want our dudes to rush the enemy,
     * so we'll calculate the smallest archer to each footman and encourage (i.e. assign higher numbers)
     * the footmen to be closer.
	 * @param node
	 */
	public static double calculateFootmanDistanceHeuristic(Node node)
	{
        double totalValue = 0.0;
        GameState state = node.getGameState();

        for(Integer footmanUnitID : state.getFootmanUnits().keySet())
        {
            totalValue += 1.0 / Heuristic.getEuclideanDistanceToClosestArcher(node, footmanUnitID);
        }

        return totalValue;
	}

    public static double getEuclideanDistanceToClosestArcher(Node node,
                                                             int footmanUnitID)
    {
        GameState state = node.getGameState();

        double closestEnemyDistance = Double.POSITIVE_INFINITY;
        Coordinate footmanPosition = state.getUnit(footmanUnitID).getPosition();

        // System.out.println("INFO [Heuristic.getEuclideanDistanceToClosestArcher]: footman=" + footmanPosition);

        for(GameUnit enemyUnit : state.getArcherUnits().values())
        {
            if(DistanceMetric.euclideanDistance(footmanPosition, enemyUnit.getPosition()) < closestEnemyDistance)
            {
                closestEnemyDistance = DistanceMetric.euclideanDistance(footmanPosition, enemyUnit.getPosition());
            }
        }

        return closestEnemyDistance;
    }

	/**
	 * The job of this method is to calculate the "utility value" of a node based health of the units.
     * We want our dudes to hurt the enemy, so we'll measure the percent of health that each unit has remaining
     * and calculate the overall difference in health
	 * @param node
	 */
	public static double calculateFootmanHealthHeuristic(Node node)
	{
        GameState state = node.getGameState();

        double totalFootmanRemainingHP = 0.0;
        double totalFootmanBaseHealth = 0.0;
        for(GameUnit unit : state.getFootmanUnits().values())
        {
            totalFootmanRemainingHP += unit.getCurrentHealth();
            totalFootmanBaseHealth += unit.getBaseHealth();
        }

        double totalArcherRemainingHP = 0.0;
        double totalArcherBaseHealth = 0.0;
        for(GameUnit unit : state.getArcherUnits().values())
        {
            totalArcherRemainingHP += unit.getCurrentHealth();
            totalArcherBaseHealth += unit.getBaseHealth();
        }

		return (totalFootmanRemainingHP / totalFootmanBaseHealth) -
               (totalArcherRemainingHP / totalArcherBaseHealth) + 1; // add 1 to keep utility values always positive
                                                                     // this is b/c worse utility is worth 0.0
                                                                     // and that happens when we lose
                                                                     // so we don't want anything to be worse!

	}

    /**
	 * The job of this method is to calculate the "utility value" of a node based health of what kind of moves
     * are in this state. We want our dudes to hurt the enemy, so I will increase the utility value
     * of every node where I am currently attacking
	 * @param node
	 */
	public static double calculateFootmanDamageHeuristic(Node node)
	{
        GameState state = node.getGameState();

        double totalDamageBonus = 0.0;

        for(Integer footmanUnitID : state.getFootmanUnits().keySet())
        {
            if(node.getActions().containsKey(footmanUnitID) &&
               node.getActions().get(footmanUnitID).getType().equals(ActionType.PRIMITIVEATTACK))
            {
                // increase damage bonus by the amount the unit can attack by
                totalDamageBonus += state.getUnit(footmanUnitID).getBasicAttack();
            }
        }

        return totalDamageBonus;
	}

}
