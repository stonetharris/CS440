package src.lab14.agents;

// SYSTEM IMPORTS
import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.environment.model.history.History.HistoryView;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;
import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.util.Direction;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// JAVA PROJECT IMPORTS
import edu.bu.lab14.agents.StochasticAgent;
import edu.bu.lab14.agents.StochasticAgent.RewardFunction;
import edu.bu.lab14.agents.StochasticAgent.TransitionModel;
import edu.bu.lab14.utilities.Coordinate;
import edu.bu.lab14.utilities.Pair;

public class ValueIterationAgent extends StochasticAgent {
    public static final double GAMMA = 1; // feel free to change this around!
    public static final double EPSILON = 1e-6; // don't change this though
    private Map<Coordinate, Double> utilities;

	public ValueIterationAgent(int playerNum) {
		super(playerNum);
        this.utilities = null;
	}

    public Map<Coordinate, Double> getUtilities() { return this.utilities; }
    private void setUtilities(Map<Coordinate, Double> u) { this.utilities = u; }

    public boolean isTerminalState(Coordinate c) {
        return c.equals(StochasticAgent.POSITIVE_TERMINAL_STATE)
            || c.equals(StochasticAgent.NEGATIVE_TERMINAL_STATE);
    }

    /**
     * A method to get an initial utility map where every coordinate is mapped to the utility 0.0
     */
    public Map<Coordinate, Double> getZeroMap(StateView state) {
        Map<Coordinate, Double> m = new HashMap<Coordinate, Double>();
        for(int x = 0; x < state.getXExtent(); ++x) {
            for(int y = 0; y < state.getYExtent(); ++y) {
                if(!state.isResourceAt(x, y)) {
                    // we can go here
                    m.put(new Coordinate(x, y), 0.0);
                }
            }
        }
        return m;
    }

    public void valueIteration(StateView state) {
        Map<Coordinate, Double> utilities = getZeroMap(state);
        utilities.put(StochasticAgent.POSITIVE_TERMINAL_STATE, 1.0);
        utilities.put(StochasticAgent.NEGATIVE_TERMINAL_STATE, -1.0);
        double delta;
        do {
            delta = 0;
            Map<Coordinate, Double> newUtilities = new HashMap<>(utilities);
            for (Coordinate c : utilities.keySet()) {
                if (!isTerminalState(c)) {
                    double maxUtility = Double.NEGATIVE_INFINITY;
                    for (Direction d : TransitionModel.CARDINAL_DIRECTIONS) {
                        double actionUtility = 0.0;
                        for (Pair<Coordinate, Double> transition : TransitionModel.getTransitionProbs(state, c, d)) {
                            Coordinate nextState = transition.getFirst();
                            double prob = transition.getSecond();
                            double reward = RewardFunction.getReward(c); 
                            actionUtility += prob * (reward + GAMMA * utilities.get(nextState));
                        }
                        maxUtility = Math.max(maxUtility, actionUtility);
                    }                    
                    newUtilities.put(c, maxUtility);
                    delta = Math.max(delta, Math.abs(maxUtility - utilities.get(c)));
                }
            }
            utilities = newUtilities;
        } while (delta > EPSILON);

        setUtilities(utilities);

        // for DEBUG
        // System.out.println("Utilities after value iteration:");
        // for (int y = 0; y < state.getYExtent(); y++) {
        //     for (int x = 0; x < state.getXExtent(); x++) {
        //         Coordinate c = new Coordinate(x, y);
        //         Double utility = utilities.get(c);
        //         if (utility != null) {
        //             System.out.printf("%6.2f ", utility);
        //         } else {
        //             System.out.print("------ ");
        //         }
        //     }
        //     System.out.println();
        // }

    }

    @Override
    public void computePolicy(StateView state, HistoryView history) {
        // compute the utilities
        this.valueIteration(state);

        // compute the policy from the utilities
        Map<Coordinate, Direction> policy = new HashMap<Coordinate, Direction>();

        for(Coordinate c : this.getUtilities().keySet()) {
            // figure out what to do when in this state
            double maxActionUtility = Double.NEGATIVE_INFINITY;
            Direction bestDirection = null;

            // go over every action
            for(Direction d : TransitionModel.CARDINAL_DIRECTIONS) {

                // measure how good this action is as a weighted combination of future state's utilities
                double thisActionUtility = 0.0;
                for(Pair<Coordinate, Double> transition : TransitionModel.getTransitionProbs(state, c, d)) {
                    thisActionUtility += transition.getSecond() * this.getUtilities().get(transition.getFirst());
                }

                // keep the best one!
                if(thisActionUtility > maxActionUtility) {
                    maxActionUtility = thisActionUtility;
                    bestDirection = d;
                }
            }

            // policy recommends the best action for every state
            policy.put(c, bestDirection);
        }

        this.setPolicy(policy);
    }
}