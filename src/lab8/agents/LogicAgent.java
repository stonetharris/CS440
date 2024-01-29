package src.lab8.agents;


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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


// JAVA PROJECT IMPORTS
import edu.bu.minesweeper.Synchronizer;
import edu.bu.minesweeper.utilities.Coordinate;
import edu.bu.minesweeper.logic.Sentence;



import java.util.Iterator;


public class LogicAgent
    extends Agent
{

    public enum SquareStatus
    {
        SAFE,
        MINE,
        UNKNOWN;
    }

    public static class KnowledgeBase
        extends Object
    {

        // sentences are organized by their constraints
        // this helps us quickly look at the "complexity" of a sentence
        private Map<Integer, Set<Sentence> >    numMines2Sentences;

        // a set of coordinates that are known to be safe
        private Set<Coordinate>                 safeCoordinates;

        // a set of coordinates that are known to be mines
        private Set<Coordinate>                 mineCoordinates;

        public KnowledgeBase()
        {
            this.numMines2Sentences = new HashMap<Integer, Set<Sentence> >();
            this.safeCoordinates = new HashSet<Coordinate>();
            this.mineCoordinates = new HashSet<Coordinate>();
        }

        public Map<Integer, Set<Sentence> > getNumMines2Sentences() { return this.numMines2Sentences; }
        public Set<Coordinate> getSafeCoordinates() { return this.safeCoordinates; }
        public Set<Coordinate> getMineCoordinates() { return this.mineCoordinates; }

        public void setNumMines2Sentences(Map<Integer, Set<Sentence> > m) { this.numMines2Sentences = m; }

        public void add(Sentence s)
        {
            if(!this.getNumMines2Sentences().containsKey(s.getConstraint()))
            {
                this.getNumMines2Sentences().put(s.getConstraint(), new HashSet<Sentence>());
            }
            this.getNumMines2Sentences().get(s.getConstraint()).add(s);
        }

        public boolean simplifySentences() {
            boolean didAnyWork = false;
            System.out.println("Simplifying sentences...");
        
            Map<Integer, Set<Sentence>> newNumMines2Sentences = new HashMap<>();
        
            for (Map.Entry<Integer, Set<Sentence>> entry : numMines2Sentences.entrySet()) {
                for (Sentence s : entry.getValue()) {
                    Set<Coordinate> newCoordinates = new HashSet<>(s.getCoordinates());
                    int newConstraint = s.getConstraint();
        
                    // Remove safe coordinates and mines, adjust constraint for mines
                    for (Coordinate safe : safeCoordinates) {
                        if (newCoordinates.remove(safe)) {
                            didAnyWork = true;
                        }
                    }
                    for (Coordinate mine : mineCoordinates) {
                        if (newCoordinates.remove(mine)) {
                            newConstraint--;
                            didAnyWork = true;
                        }
                    }
        
                    // Add the modified sentence to the new map
                    if (!newNumMines2Sentences.containsKey(newConstraint)) {
                        newNumMines2Sentences.put(newConstraint, new HashSet<>());
                    }
                    System.out.println("Simplified sentence: " + newCoordinates + "=" + newConstraint);
                    newNumMines2Sentences.get(newConstraint).add(new Sentence(newCoordinates, newConstraint));
                }
            }
        
            setNumMines2Sentences(newNumMines2Sentences); // Update the map
            System.out.println("newNumMines2Sentences = " + newNumMines2Sentences);
            return didAnyWork;
        }

        public void markSafe(Coordinate c) {
            if (!safeCoordinates.contains(c)) {
                safeCoordinates.add(c);
            }
        }
        
        public void markMine(Coordinate c) {
            if (!mineCoordinates.contains(c)) {
                mineCoordinates.add(c);
            }
        }

        public boolean inferAllSafeCoordinates() {
            boolean didAnyWork = false;
            System.out.println("Inferring all safe coordinates...");
        
            // Iterate over each set of Sentences in the numMines2Sentences Map
            for (Set<Sentence> sentenceSet : numMines2Sentences.values()) {
                for (Sentence s : sentenceSet) {
                    if (s.getConstraint() == 0) {
                        // All coordinates in this sentence are safe
                        for (Coordinate c : s.getCoordinates()) {
                            markSafe(c);
                            System.out.println("Inferred safe coordinate: " + c);
                            didAnyWork = true;
                        }
                    }
                }
            }
        
            return didAnyWork;
        }
        

        public boolean inferAllMineCoordinates() {
            boolean didAnyWork = false;        
            // Iterate over each set of Sentences in the numMines2Sentences Map
            for (Set<Sentence> sentenceSet : numMines2Sentences.values()) {
                Iterator<Sentence> iterator = sentenceSet.iterator();
                while (iterator.hasNext()) {
                    Sentence s = iterator.next();
                    if (s.getCoordinates().size() == s.getConstraint()) {
                        // All coordinates in this sentence are mines
                        for (Coordinate c : s.getCoordinates()) {
                            markMine(c);
                            didAnyWork = true;
                        }
                        iterator.remove(); // remove the sentence as it's no longer needed
                    }
                }
            }
        
            return didAnyWork;
        }
        

        /**
        //EXTRA CREDIT
        public boolean mergeSentences()
        {
            // TODO: implement me!
        }
        */

        public void makeInferences()
        {
            boolean madeInferences = false;

            do
            {
                madeInferences = false;
                madeInferences = this.simplifySentences() || madeInferences;
                madeInferences = this.inferAllSafeCoordinates() || madeInferences;
                madeInferences = this.inferAllMineCoordinates() || madeInferences;
                // madeInferences = this.mergeSentences() || madeInferences; // extra credit!
            } while(madeInferences);
        }

    }

	private int                             myUnitID;
    private Set<Coordinate>                 gameCoordinates;
    private Set<Coordinate>                 unexploredCoordinates;
    private Coordinate                      coordinateIJustAttacked;
    private KnowledgeBase                   kb;

	public LogicAgent(int playerNum)
	{
		super(playerNum);
        this.gameCoordinates = new HashSet<Coordinate>();
        this.unexploredCoordinates = new HashSet<Coordinate>();
        this.coordinateIJustAttacked = null;
        this.kb = new KnowledgeBase();
	}
	
	public final int getMyUnitID() { return this.myUnitID; }
    public Set<Coordinate> getGameCoordinates() { return this.gameCoordinates; }
    public Set<Coordinate> getUnexploredCoordinates() { return this.unexploredCoordinates; }
    public Coordinate getCoordinateIJustAttacked() { return this.coordinateIJustAttacked; }
    public KnowledgeBase getKB() { return this.kb; }

    private void setMyUnitID(int i) { this.myUnitID = i; }
    private void setCoordinateIJustAttacked(Coordinate c) { this.coordinateIJustAttacked = c; }

	@Override
	public Map<Integer, Action> initialStep(StateView state,
                                            HistoryView history)
    {
		// locate enemy and friendly units
		List<Integer> myUnitIDs = state.getUnitIds(this.getPlayerNumber());
		if(myUnitIDs.size() != 1)
		{
			System.err.println("ERROR [LogicAgent.initialStep]: LogicAgent should control one unit");
			System.exit(-1);
		}

		if(!state.getUnit(myUnitIDs.get(0)).getTemplateView().getName().toLowerCase().equals("archer"))
		{
			System.err.println("ERROR [LogicAgent.initialStep]: LogicAgent should control one Archer unit");
			    System.exit(-1);
		}
		this.setMyUnitID(myUnitIDs.get(0));

		// get the other player
		Integer[] playerNumbers = state.getPlayerNumbers();
		if(playerNumbers.length != 2)
		{
			System.err.println("ERROR: Should only be two players in the game");
			System.exit(-1);
		}
		Integer enemyPlayerNumber = null;
		if(playerNumbers[0] != this.getPlayerNumber())
		{
			enemyPlayerNumber = playerNumbers[0];
		} else
		{
			enemyPlayerNumber = playerNumbers[1];
		}

        // check enemy units
        Set<Integer> enemyUnitIDs = new HashSet<Integer>();
        for(Integer unitID : state.getUnitIds(enemyPlayerNumber))
        {
            if(!state.getUnit(unitID).getTemplateView().getName().toLowerCase().equals("hiddensquare"))
		    {
			    System.err.println("ERROR [LogicAgent.initialStep]: Enemy should start off with HiddenSquare units!");
			        System.exit(-1);
		    }
            enemyUnitIDs.add(unitID);
        }


        // initially everything is unknown
        Coordinate coord = null;
        for(Integer unitID : enemyUnitIDs)
        {
            coord = new Coordinate(state.getUnit(unitID).getXPosition(),
                                   state.getUnit(unitID).getYPosition());
            this.getUnexploredCoordinates().add(coord);
            this.getGameCoordinates().add(coord);
        }

        // System.out.println("LogicAgent.initialStep: game coordinates=" + this.getGameCoordinates());

		return this.middleStep(state, history);
	}

    public Coordinate getUnexploredSafeCoordinate()
    {
        for(Coordinate unexploredCoordinate : this.getUnexploredCoordinates())
        {
            if(this.getKB().getSafeCoordinates().contains(unexploredCoordinate))
            {
                return unexploredCoordinate;
            }
        }
        return null;
    }

    public Coordinate getUnexploredNonMineCoordinate()
    {
        for(Coordinate unexploredCoordinate : this.getUnexploredCoordinates())
        {
            if(!this.getKB().getSafeCoordinates().contains(unexploredCoordinate) &&
               !this.getKB().getMineCoordinates().contains(unexploredCoordinate))
            {
                return unexploredCoordinate;
            }
        }
        return null;
    }

    public static int getNumNeighborsFromTemplateName(String lowerCaseTemplateName)
    {
        int numNeighbors = -1;
        if(lowerCaseTemplateName.equals("zerosquare"))
        {
            numNeighbors = 0;
        } else if(lowerCaseTemplateName.equals("onesquare"))
        {
            numNeighbors = 1;
        } else if(lowerCaseTemplateName.equals("twosquare"))
        {
            numNeighbors = 2;
        } else if(lowerCaseTemplateName.equals("threesquare"))
        {
            numNeighbors = 3;
        } else if(lowerCaseTemplateName.equals("foursquare"))
        {
            numNeighbors = 4;
        } else if(lowerCaseTemplateName.equals("fivesquare"))
        {
            numNeighbors = 5;
        } else if(lowerCaseTemplateName.equals("sixsquare"))
        {
            numNeighbors = 6;
        } else if(lowerCaseTemplateName.equals("sevensquare"))
        {
            numNeighbors = 7;
        } else if(lowerCaseTemplateName.equals("eightsquare"))
        {
            numNeighbors = 8;
        } else if(lowerCaseTemplateName.equals("hiddensquare") || lowerCaseTemplateName.equals("mine"))
        {
            ;
        } else
        {
            System.err.println("ERROR: LogicAgent.getNumNeighborsFromTemplateName: invalid template name [" +
                lowerCaseTemplateName + "]");
            System.exit(-1);
        }


        return numNeighbors;
    }

    public Set<Coordinate> getNeighbors(Coordinate c)
    {
        Set<Coordinate> neighbors = new HashSet<Coordinate>();
        for(int dX = -1; dX <= 1; ++dX)
        {
            for(int dY = -1; dY <= 1; ++dY)
            {
                Coordinate neighbor = new Coordinate(c.getXCoordinate() + dX, c.getYCoordinate() + dY);
                if(!(dX == 0 && dY == 0) && this.getGameCoordinates().contains(neighbor))
                {
                    neighbors.add(new Coordinate(c.getXCoordinate() + dX, c.getYCoordinate() + dY));
                }
            }
        }
        return neighbors;
    }

    public Sentence makeObservationSentence(StateView state)
    {
        Sentence s = null;

        Coordinate coordinateIJustAttacked = this.getCoordinateIJustAttacked();
        if(coordinateIJustAttacked != null)
        {
            // see what it became
            Integer newUnitID = state.unitAt(coordinateIJustAttacked.getXCoordinate(),
                                             coordinateIJustAttacked.getYCoordinate());

            if(newUnitID != null)
            {

                UnitView newUnitView = state.getUnit(newUnitID);
                int numNeighbors = LogicAgent.getNumNeighborsFromTemplateName(newUnitView.getTemplateView()
                    .getName().toLowerCase());

                if(numNeighbors < 0)
                {
                    // uh oh...mine
                } else
                {
                    // build the sentence
                    Set<Coordinate> neighbors = this.getNeighbors(coordinateIJustAttacked);
                    s = new Sentence(neighbors, numNeighbors);
                }
            }
        }

        return s;
    }

	@Override
	public Map<Integer, Action> middleStep(StateView state,
                                           HistoryView history)
    {
		Map<Integer, Action> actions = new HashMap<Integer, Action>();

        if(Synchronizer.isMyTurn(this.getPlayerNumber(), state))
        {

            // get the observation from the past
            if(state.getTurnNumber() > 0)
            {
                Sentence observationSentence = this.makeObservationSentence(state);
                System.out.println("INFO: LogicAgent.middleStep: observed sentence=" + observationSentence);

                if(observationSentence != null)
                {
                    this.getKB().add(observationSentence);
                    this.getKB().makeInferences();
                } else
                {
                    System.out.println("INFO: LogicAgent.middleStep: clicked on mine, about to lose!");
                }
            }

            System.out.println("INFO: LogicAgent.middleStep: safe coordinates=" + this.getKB().getSafeCoordinates());
            System.out.println("INFO: LogicAgent.middleStep: mine coordinates=" + this.getKB().getMineCoordinates());

            Coordinate coordinateOfUnitToAttack = this.getUnexploredSafeCoordinate();
            if(coordinateOfUnitToAttack == null)
            {
                // pick a random unknown one
                coordinateOfUnitToAttack = this.getUnexploredNonMineCoordinate();
            }

            // could have won the game (and waiting for enemy units to die)
            // or we have a coordinate to attack
            // we need to check that the unit at that coordinate is a hidden square (not allowed to attack other units)
            if(coordinateOfUnitToAttack != null)
            {
                Integer unitID = state.unitAt(coordinateOfUnitToAttack.getXCoordinate(),
                                              coordinateOfUnitToAttack.getYCoordinate());
                if(unitID == null)
                {
                    System.err.println("ERROR: LogicAgent.middleStep: deciding to attack unit at " +
                        coordinateOfUnitToAttack + " but no unit was found there!");
                    System.exit(-1);
                }

                String unitTemplateName = state.getUnit(unitID).getTemplateView().getName();
                if(!unitTemplateName.toLowerCase().equals("hiddensquare"))
                {
                    // can't attack non hidden-squares!
                    System.err.println("ERROR: LogicAgent.middleStep: deciding to attack unit at " +
                        coordinateOfUnitToAttack + " but unit at that square is [" + unitTemplateName + "] " +
                        "and should be a HiddenSquare unit!");
                    System.exit(-1);
                }
                this.setCoordinateIJustAttacked(coordinateOfUnitToAttack);

                actions.put(
                    this.getMyUnitID(),
                    Action.createPrimitiveAttack(
                        this.getMyUnitID(),
                        unitID)
                );
                this.getUnexploredCoordinates().remove(coordinateOfUnitToAttack);
            }


            // special case:
            // we know that the first square chosen is always safe...just a rule of minesweeper
            this.getKB().getSafeCoordinates().add(coordinateOfUnitToAttack);

        }

		return actions;
	}

    @Override
	public void terminalStep(StateView state, HistoryView history) {}

    @Override
	public void loadPlayerData(InputStream arg0) {}

	@Override
	public void savePlayerData(OutputStream arg0) {}

}
