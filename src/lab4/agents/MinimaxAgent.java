package src.lab4.agents;


// SYSTEM IMPORTS
import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.environment.model.history.History.HistoryView;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;
import edu.cwru.sepia.environment.model.state.ResourceNode.ResourceView;
import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.util.Direction;


import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;


// JAVA PROJECT IMPORTS
import edu.bu.lab4.game.tree.Node;
import edu.bu.lab4.utilities.Coordinate;
import edu.bu.lab4.utilities.DistanceMetric;


import src.lab4.heuristics.Heuristic;

public class MinimaxAgent
    extends Agent
{

	private Integer enemyPlayerNumber;
	private final int maxDepth;

	public MinimaxAgent(int playerNum, String[] args)
	{
		super(playerNum);
		if(args.length < 2)
		{
			System.err.println("ERROR: need to give MinimaxAgent a max search depth in the xml file");
			System.exit(1);
		}
        this.enemyPlayerNumber = -1;
		this.maxDepth = Integer.parseInt(args[1]);
		System.out.println("INFO [MinimaxAgent.MinimaxAgent] depth limit=" + this.getMaxDepth());
	}
	
	public final int getEnemyPlayerNumber() { return this.enemyPlayerNumber; }
	public final int getMaxDepth() { return this.maxDepth; }

    private void setEnemyPlayerNumber(int i) { this.enemyPlayerNumber = i; }

	@Override
	public Map<Integer, Action> initialStep(StateView state,
                                            HistoryView history)
    {
		// locate enemy and friendly units
		for(Integer unitID : state.getUnitIds(this.getPlayerNumber()))
        {
		    if(!state.getUnit(unitID).getTemplateView().getName().toLowerCase().equals("footman"))
		    {
			    System.err.println("ERROR [MinimaxAgent.initialStep]: MinimaxAgent should control Footman units");
			    System.exit(1);
		    }
        }

		// get the other player
		Integer[] playerNumbers = state.getPlayerNumbers();
		if(playerNumbers.length != 2)
		{
			System.err.println("ERROR: Should only be two players in the game");
			System.exit(1);
		}
		Integer enemyPlayerNumber = null;
		if(playerNumbers[0] != this.getPlayerNumber())
		{
			enemyPlayerNumber = playerNumbers[0];
		} else
		{
			enemyPlayerNumber = playerNumbers[1];
		}

		// get the footman controlled by the other player
		for(Integer unitID : state.getUnitIds(enemyPlayerNumber))
        {
		    if(!state.getUnit(unitID).getTemplateView().getName().toLowerCase().equals("archer"))
		    {
			    System.err.println("ERROR [MinimaxAgent.initialStep]: enemy agent should control Archer units");
			    System.exit(1);
		    }
        }
		this.setEnemyPlayerNumber(enemyPlayerNumber);
		return this.middleStep(state, history);
	}

	@Override
	public Map<Integer, Action> middleStep(StateView state,
                                           HistoryView history)
    {
		Map<Integer, Action> actions = null;

        if(state.getTurnNumber() % 2 != 0)
        {
		    actions = this.getMinimaxAction(state, history);
        }

		return actions;
	}

    @Override
	public void terminalStep(StateView state, HistoryView history) {}

    @Override
	public void loadPlayerData(InputStream arg0) {}

	@Override
	public void savePlayerData(OutputStream arg0) {}

	//TODO
	//Input: Takes parentNode and depth (int) parameter as input
	//Output: child node containing the action that should be taken
	//Additional Tips:
	// 1. Function is meant to be recursive, depth parameter controls recursion
	// 2. Recursion will terminate upon terminal state, or depth = this.getMaxDepth()
	public Node minimaxSearch(Node node, int depth) {
		// Base Case: If we are at a terminal state or have reached the max depth
		if (depth == this.getMaxDepth() || node.isTerminalState()) {
			return node;
		}
		
		List<Node> children = node.getChildren();
		
		// If the node is for the maximizing player
		if (node.getCurrentPlayer() == Node.Player.FOOTMAN) { 
			double maxUtility = Double.NEGATIVE_INFINITY;
			Node bestChild = null;
			for (Node child : children) {
				Node currentChild = minimaxSearch(child, depth + 1);
				if (currentChild.getFootmanUtilityValue() > maxUtility) {
					maxUtility = currentChild.getFootmanUtilityValue();
					bestChild = currentChild;
				}
			}
			return bestChild;
			
		} else { // Minimizing player
			double minUtility = Double.POSITIVE_INFINITY;
			Node bestChild = null;
			for (Node child : children) {
				Node currentChild = minimaxSearch(child, depth + 1);
				if (currentChild.getFootmanUtilityValue() < minUtility) {
					minUtility = currentChild.getFootmanUtilityValue();
					bestChild = currentChild;
				}
			}
			return bestChild;
		}
	}
	

	public Map<Integer, Action> getMinimaxAction(StateView state, HistoryView history)
	{
        // System.out.println("");
        // System.out.println("");
		return this.minimaxSearch(
            new Node(state,
                this.getPlayerNumber(),
                this.getEnemyPlayerNumber(),
                Node.Player.FOOTMAN
            ),
            0
        ).getActions();
	}

}