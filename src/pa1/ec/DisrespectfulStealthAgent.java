package src.pa1.ec;


// SYSTEM IMPORTS
import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionFeedback;
import edu.cwru.sepia.action.ActionResult;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.environment.model.history.DamageLog;
import edu.cwru.sepia.environment.model.history.DeathLog;
import edu.cwru.sepia.environment.model.history.History.HistoryView;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.ResourceNode.ResourceView;
import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.environment.model.state.Unit;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;
import edu.cwru.sepia.util.Direction;


import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;
import java.util.Stack;


// JAVA PROJECT IMPORTS
import edu.bu.pa1.distance.DistanceMetric;
import edu.bu.pa1.graph.Vertex;
import edu.bu.pa1.graph.Path;

public class DisrespectfulStealthAgent extends Agent {
    // Fields of this class
    // TODO: add your fields here! For instance, it might be a good idea to
    // know when you've killed the enemy townhall so you know when to escape!
    // TODO: implement the state machine for following a path once we calculate it
    //       this will for sure adding your own fields.
    private int myUnitID;
    private int enemyTownhallUnitID;
    private int enemyGoldUnitID;
    private Set<Integer> otherEnemyUnitIDs;

    private final int enemyUnitSightRadius;

    //added fields:
    private boolean enemyTownhallDestroyed;
    private Vertex startingPosition;
    private Path currentPlan;
    private int playerNum;
    private int currentPathIndex;
    private boolean isReturning;
    private Vertex goalPosition;
    private boolean enemyGoldTaken;

    public DisrespectfulStealthAgent(int playerNum, String[] args)
    {
        super(playerNum);

        // set these fields to some invalid state and populate them in initialStep()
        this.myUnitID = -1;
        this.enemyTownhallUnitID = -1;
        this.enemyGoldUnitID = -1;
        this.otherEnemyUnitIDs = null;
        // TODO: make sure to initialize your fields (to some invalid state) here!
        this.enemyTownhallDestroyed = false;
        this.startingPosition = null;
        this.currentPlan = null;
        this.playerNum = playerNum;
        this.currentPathIndex = 0;
        this.isReturning = false;
        this.goalPosition = null;
        int enemyUnitSightRadius = -1;
        this.enemyGoldTaken = false;

        if(args.length == 2)
        {
            try
            {
                enemyUnitSightRadius = Integer.parseInt(args[1]);

                if(enemyUnitSightRadius <= 0)
                {
                    throw new Exception("ERROR");
                }
            } catch(Exception e)
            {
                System.err.println("ERROR: [StealthAgent.StealthAgent]: error parsing second arg=" + args[1]
                    + " which should be a positive integer");
            }
        } else
        {
            System.err.println("ERROR [StealthAgent.StealthAgent]: need to provide a second arg <enemyUnitSightRadius>");
            System.exit(-1);
        }

        this.enemyUnitSightRadius = enemyUnitSightRadius;
    }

    // TODO: add some getter methods for your fields! Thats the java way to do things!
    public int getMyUnitID() { return this.myUnitID; }
    public int getEnemyTownhallUnitID() { return this.enemyTownhallUnitID; }
    public int getEnemyGoldUnitID() { return this.enemyGoldUnitID; }
    public final Set<Integer> getOtherEnemyUnitIDs() { return this.otherEnemyUnitIDs; }
    public final int getEnemyUnitSightRadius() { return this.enemyUnitSightRadius; }

    // new ones:
    public boolean getEnemyTownhallDestroyed() {return this.enemyTownhallDestroyed;}
    public Vertex getStartingPosition() {return this.startingPosition;}
    public Path getCurrentPlan() {return this.currentPlan;}
    public int getCurrentPathIndex() { return this.currentPathIndex; }
    public boolean getIsReturning() {return this.isReturning;}
    public Vertex getGoalPosition() {return this.goalPosition;}
    public boolean getEnemyGoldTaken() {return this.enemyGoldTaken;}


    // TODO: add some setter methods for your fields if they need them! Thats the java way to do things!
    private void setMyUnitID(int id) { this.myUnitID = id; }
    private void setEnemyTownhallUnitID(int id) { this.enemyTownhallUnitID = id; }
    private void setOtherEnemyUnitIDs(Set<Integer> s) { this.otherEnemyUnitIDs = s; }


    private void setEnemyTownhallDestroyed(boolean b) {this.enemyTownhallDestroyed = b;}
    private void setIsReturning(boolean b) {this.isReturning = b;}
    // public void setStartingPosition(Vertex v) {this.startingPosition = v;}
    public void setGoalPosition(Vertex v) {this.goalPosition = v;}
    public void setEnemyGoldTaken(boolean b) {this.enemyGoldTaken = b;}
    public void setCurrentPlan(Path p) {this.currentPlan = p;}
    public void setEnemyGoldUnitID(int id) {this.enemyGoldUnitID = id; }

    /**
        TODO: if you add any fields to this class it might be a good idea to initialize them here
              if they need sepia information!
     */
    @Override
    public Map<Integer, Action> initialStep(StateView state,
                                            HistoryView history)
    {
        // this method is typically used to discover the units in the game.
        // any units we want to pay attention to we probably want to store in some fields

        // first find out which units are mine and which units aren't
        Set<Integer> myUnitIDs = new HashSet<Integer>();
        for(Integer unitID : state.getUnitIds(this.getPlayerNumber()))
        {
            myUnitIDs.add(unitID);
        }

        // should only be one unit controlled by me
        if(myUnitIDs.size() != 1)
        {
            System.err.println("ERROR: should only be 1 unit controlled by player=" +
                this.getPlayerNumber() + " but found " + myUnitIDs.size() + " units");
            System.exit(-1);
        } else
        {
            this.setMyUnitID(myUnitIDs.iterator().next()); // get the one unit id
        }


        // there can be as many other players as we want, and they can controll as many units as they want,
        // but there should be only ONE enemy townhall unit
        Set<Integer> enemyTownhallUnitIDs = new HashSet<Integer>();
        Set<Integer> enemyGoldUnitIDs = new HashSet<Integer>();
        Set<Integer> otherEnemyUnitIDs = new HashSet<Integer>();
        for(Integer playerNum : state.getPlayerNumbers())
        {
            if(playerNum != this.getPlayerNumber())
            {
                for(Integer unitID : state.getUnitIds(playerNum))
                {
                    if(state.getUnit(unitID).getTemplateView().getName().toLowerCase().equals("townhall"))
                    {
                        enemyTownhallUnitIDs.add(unitID);
                    } else if(state.getUnit(unitID).getTemplateView().getName().toLowerCase().equals("gold")) {
                        enemyGoldUnitIDs.add(unitID);
                    } else {
                        otherEnemyUnitIDs.add(unitID);
                    }
                }
            }
        }

        // should only be one unit controlled by me
        if(enemyTownhallUnitIDs.size() != 1)
        {
            System.err.println("ERROR: should only be 1 enemy townhall unit present on the map but found "
                + enemyTownhallUnitIDs.size() + " such units");
            System.exit(-1);
        } else
        {
            this.setEnemyTownhallUnitID(enemyTownhallUnitIDs.iterator().next()); // get the one unit id
            this.setOtherEnemyUnitIDs(otherEnemyUnitIDs);
        }

        return this.middleStep(state, history);
    }

    /**
        TODO: implement me! This is the method that will be called every turn of the game.
              This method is responsible for assigning actions to all units that you control
              (which should only be a single footman in this game)
     */
    @Override
    public Map<Integer, Action> middleStep(StateView state, HistoryView history) {
        if (state.getUnit(this.getEnemyTownhallUnitID()) == null) {
            // System.out.println("Trying to reset destination...");
            this.setGoalPosition(this.getStartingPosition());
            this.setCurrentPlan(search(getCurrentPosition(state), this.getStartingPosition(), state));
        } else if (state.getUnit(this.getEnemyGoldUnitID()) == null) {
            this.setGoalPosition(getEnemyTownhallPosition(state));
            this.setCurrentPlan(search(getCurrentPosition(state), this.getEnemyTownhallPosition(state), state));
        }
        // System.out.println("Entered middleStep");
        Map<Integer, Action> actions = new HashMap<Integer, Action>();
        Unit.UnitView footmanUnit = state.getUnit(this.getMyUnitID());
        int footmanX = footmanUnit.getXPosition();
        int footmanY = footmanUnit.getYPosition();
        Vertex curLocation = new Vertex(footmanX, footmanY);
        
        if(this.shouldReplacePlan(state) 
        //|| (this.getCurrentPlan() == null && (!this.getNearGoalLocs(state).contains(this.getCurrentPosition(state))
        //|| state.getUnit(this.getEnemyTownhallUnitID()) == null))
        ) {
            this.setCurrentPlan(search(this.getCurrentPosition(state), this.getEnemyTownhallPosition(state), state));
        }

        if (this.getSecondToLastVertex(this.getCurrentPlan()) != null && (this.getCurrentPosition(state).getXCoordinate() != this.getSecondToLastVertex(this.getCurrentPlan()).getXCoordinate()
        || this.getCurrentPosition(state).getYCoordinate() != this.getSecondToLastVertex(this.getCurrentPlan()).getYCoordinate())) {
            int xDiff = this.getSecondToLastVertex(this.getCurrentPlan()).getXCoordinate() - this.getCurrentPosition(state).getXCoordinate();
            int yDiff = this.getSecondToLastVertex(this.getCurrentPlan()).getYCoordinate() - this.getCurrentPosition(state).getYCoordinate();
            Direction nexDirection = Direction.getDirection(xDiff, yDiff);
            System.out.println();
            System.out.println("about to add an action in direction" + nexDirection.toString());
            actions.put(this.getMyUnitID(), Action.createPrimitiveMove(this.getMyUnitID(), nexDirection));
            // Vertex newLoc = new Vertex(this.getSecondToLastVertex(this.getCurrentPlan()).getXCoordinate(), this.getSecondToLastVertex(this.getCurrentPlan()).getYCoordinate());
        }

        if (this.getNearGoalLocs(state).contains(this.getCurrentPosition(state))) {
            actions.put(this.getMyUnitID(), Action.createPrimitiveAttack(this.getMyUnitID(), this.getEnemyTownhallUnitID()));
        }

        // if (state.getUnit(this.getEnemyTownhallUnitID()) == null) {
        //     // System.out.println("Trying to reset destination...");
        //     this.setGoalPosition(this.getStartingPosition());
        //     this.setCurrentPlan(search(getCurrentPosition(state), this.getStartingPosition(), state));
        // }
        System.out.println("returning actions: " + actions.toString());
        System.out.println();
        System.out.println();
        return actions;
    }

    public Vertex getCurrentPosition(StateView state) {
        UnitView myUnit = state.getUnit(getMyUnitID());
        return new Vertex(myUnit.getXPosition(), myUnit.getYPosition());
    }

    public int getPathLength(Path path) {
        int length = 0;
        while (path != null) {
            length++;
            path = path.getParentPath();
        }
        return length;
    }

    public Vertex getPathVertexAt(Path path, int index) {
        int currentIndex = getPathLength(path) - 1;
        while (path != null) {
            if (currentIndex == index) {
                return path.getDestination();
            }
            currentIndex--;
            path = path.getParentPath();
        }
        return null;
    }

    public Vertex getSecondToLastVertex(Path path) {
        if (path == null || path.getParentPath() == null) return null;
        while (path.getParentPath().getParentPath() != null) {
            path = path.getParentPath();
        }
        return path.getDestination();
    }    

    public List<Vertex> getNearGoalLocs(StateView state) {
        Vertex townHallVertex = this.getEnemyTownhallPosition(state); // Assuming this method gives you the town hall's position.
        int x = townHallVertex.getXCoordinate();
        int y = townHallVertex.getYCoordinate();
    
        // Create a list for the surrounding vertices.
        List<Vertex> surroundingVertices = new ArrayList<>();

        surroundingVertices.add(new Vertex(x - 1, y - 1)); // top-left
        surroundingVertices.add(new Vertex(x, y - 1));     // top-center
        surroundingVertices.add(new Vertex(x + 1, y - 1)); // top-right
        surroundingVertices.add(new Vertex(x - 1, y));     // middle-left
        surroundingVertices.add(new Vertex(x + 1, y));     // middle-right
        surroundingVertices.add(new Vertex(x - 1, y + 1)); // bottom-left
        surroundingVertices.add(new Vertex(x, y + 1));     // bottom-center
        surroundingVertices.add(new Vertex(x + 1, y + 1)); // bottom-right
    
        return surroundingVertices;
    }

    public Set<Vertex> getEnemyPositions(StateView state) {
        Set<Vertex> enemyPositions = new HashSet<>();
        int myPlayerNumber = this.getPlayerNumber(); 
        for (Integer unitID : state.getAllUnitIds()) { // Get all visible units to the player.
            Unit.UnitView unitView = state.getUnit(unitID);
            // Use the getTemplateView() method to get the UnitTemplateView and then fetch the player.
            int unitPlayerNumber = unitView.getTemplateView().getPlayer();
            if (unitPlayerNumber != myPlayerNumber) {
                enemyPositions.add(new Vertex(unitView.getXPosition(), unitView.getYPosition()));
            }
        }
        return enemyPositions;
    }   

    public Vertex getEnemyTownhallPosition(StateView state) {
        if (!getEnemyTownhallDestroyed()) {
            UnitView enemyTownhall = state.getUnit(getEnemyTownhallUnitID());
            if(enemyTownhall != null) {
                return new Vertex(enemyTownhall.getXPosition(), enemyTownhall.getYPosition());
            }
            return startingPosition;
        } else {
            System.out.println("Why are you trying to get enemy townhall position, it is dead");
            return startingPosition;
        }
    }

    public Vertex bestDirection(Vertex src, Vertex goal, StateView state) {
        Set<Vertex> neighbors = getOutgoingNeighbors(src, state);
        Iterator<Vertex> iterateNeighbors = neighbors.iterator();
        Vertex bestNeighbor = null;
        float bestNeighborScore = 10000f;
        while(iterateNeighbors.hasNext()){
            Vertex cur = iterateNeighbors.next();
            float curscore = getEdgeWeight(state, src, cur) + heuristic(cur, goal);
            if(Float.compare(curscore, bestNeighborScore) < 0){
                bestNeighbor = cur;
                bestNeighborScore = curscore;
            }
        }
        return bestNeighbor;
    }

    // Please don't mess with this
    @Override
    public void terminalStep(StateView state,
                             HistoryView history)
    {
        boolean isMyUnitDead = state.getUnit(this.getMyUnitID()) == null;
        boolean isEnemyTownhallDead = state.getUnit(this.getEnemyTownhallUnitID()) == null;

        if(isMyUnitDead)
        {
            System.out.println("mission failed");
        } else if(isEnemyTownhallDead)
        {
            System.out.println("mission success");
        } else
        {
            System.out.println("how did we get here? Both my unit and the enemy townhall are both alive?");
        }
    }

    // You probably dont need to mess with this: we dont need to save our agent
    @Override
    public void savePlayerData(OutputStream os) {}

    // You probably dont need to mess with this: we dont need to load our agent from disk
    @Override
    public void loadPlayerData(InputStream is) {}


    /**
        TODO: implement me! This method should return "true" WHEN the current plan is bad,
              and return "false" when the path is still valid. I would recommend including
              figuring out when:
                    - the path you created is not blocked by another unit on the map (that has moved)
                    - you are getting too close to an enemy unit that is NOT the townhall
                        Remember, if you get too close to the enemy units they will kill you!
                        An enemy will see you if you get within a chebyshev distance of this.getEnemyUnitSightRadius()
                        squares away
     */
    public boolean shouldReplacePlan(StateView state) {
        // System.out.println("Entered shouldReplacePlan");
        // If there's no plan, we need a new one
        if (getCurrentPlan() == null) {
            // System.out.println("shouldReplacePlan: getCurrentPlan() == null, returning true");
            return true;
        }
        // if the town hall is destroyed and we aren't already at the starting position, we need a new plan?
        if (enemyTownhallDestroyed && !getCurrentPosition(state).equals(startingPosition)) {
            return true;
        }
    
        // Check if the path is blocked by another unit
        int pathLength = getPathLength(getCurrentPlan());
        for (int i = 0; i < pathLength; i++) {
            Vertex position = getPathVertexAt(getCurrentPlan(), i);
            Vertex currentPosition = getCurrentPosition(state);
            if (
                (!position.equals(currentPosition)) && 
                (state.isResourceAt(position.getXCoordinate(), position.getYCoordinate()) || 
                state.isUnitAt(position.getXCoordinate(), position.getYCoordinate()))
            ) {
                System.out.println("shouldReplacePlan: path is blocked at (" + position.getXCoordinate() + ", " + position.getYCoordinate() + "), return true ");
                return true; // Path is blocked
            }
        }
    
        // Check if we are getting too close to an enemy unit
        List<Integer> allUnitIDs = state.getAllUnitIds();
        for (int unitID : allUnitIDs) {
            UnitView unit = state.getUnit(unitID);
            if (!getOtherEnemyUnitIDs().contains(unitID) && unitID != getEnemyTownhallUnitID()) {
                continue; // Skip if it's not an enemy
            }
            for (int i = 0; i < pathLength; i++) {
                Vertex position = getPathVertexAt(getCurrentPlan(), i);
                int distance = Math.max(Math.abs(unit.getXPosition() - position.getXCoordinate()), 
                                        Math.abs(unit.getYPosition() - position.getYCoordinate()));
                
                if (distance <= getEnemyUnitSightRadius()) {
                    // System.out.println("shouldReplacePlan: in enemy radius, return true ");
                    return true; // Too close to an enemy
                }
            }
        }
        // System.out.println("shouldReplacePlan: valid plan, return false ");
        return false; // Plan is still valid
    }

    /**
        TODO: implement me! a helper function to get the outgoing neighbors of a vertex.
     */
    public Set<Vertex> getOutgoingNeighbors(Vertex src,
    StateView state) {
        Set<Vertex> outgoingNeighbors = new HashSet<Vertex>();
        int maxX = state.getXExtent();
        int maxY = state.getYExtent();
        if (src.getYCoordinate() - 1 >= 0 && !state.isResourceAt(src.getXCoordinate(), src.getYCoordinate() - 1)) { // Top
                                                                                                                // neighbor
            outgoingNeighbors.add(new Vertex(src.getXCoordinate(), src.getYCoordinate() - 1));
        }
        if (src.getYCoordinate() + 1 < maxY && !state.isResourceAt(src.getXCoordinate(), src.getYCoordinate() + 1)) { // Bottom
                                                                                                                // neighbor
            outgoingNeighbors.add(new Vertex(src.getXCoordinate(), src.getYCoordinate() + 1));
        }
        if (src.getXCoordinate() - 1 >= 0 && !state.isResourceAt(src.getXCoordinate() - 1, src.getYCoordinate())) { // Left
                                                                                                                // neighbor
            outgoingNeighbors.add(new Vertex(src.getXCoordinate() - 1, src.getYCoordinate()));
        }
        if (src.getXCoordinate() + 1 < maxX && !state.isResourceAt(src.getXCoordinate() + 1, src.getYCoordinate())) { // Right
                                                                                                                // neighbor
            outgoingNeighbors.add(new Vertex(src.getXCoordinate() + 1, src.getYCoordinate()));
        }

        if (src.getXCoordinate() - 1 >= 0 && src.getYCoordinate() - 1 >= 0
                && !state.isResourceAt(src.getXCoordinate() - 1, src.getYCoordinate() - 1)) { // Top Left neighbor
            outgoingNeighbors.add(new Vertex(src.getXCoordinate() - 1, src.getYCoordinate() - 1));
        }
        if (src.getXCoordinate() + 1 < maxX && src.getYCoordinate() + 1 < maxY
                && !state.isResourceAt(src.getXCoordinate() + 1, src.getYCoordinate() + 1)) { // Bottom Right neighbor
            outgoingNeighbors.add(new Vertex(src.getXCoordinate() + 1, src.getYCoordinate() + 1));
        }
        if (src.getXCoordinate() + 1 < maxX && src.getYCoordinate() - 1 >= 0
                && !state.isResourceAt(src.getXCoordinate() + 1, src.getYCoordinate() - 1)) { // Top Right neighbor
            outgoingNeighbors.add(new Vertex(src.getXCoordinate() + 1, src.getYCoordinate() - 1));
        }
        if (src.getXCoordinate() - 1 >= 0 && src.getYCoordinate() + 1 < maxY
                && !state.isResourceAt(src.getXCoordinate() - 1, src.getYCoordinate() + 1)) { // Bottom Left neighbor
            outgoingNeighbors.add(new Vertex(src.getXCoordinate() - 1, src.getYCoordinate() + 1));
        }
        System.out.println(outgoingNeighbors);
        return outgoingNeighbors;
        }
    /**
        TODO: implement me! a helper function to get the edge weight of going from "src" to "dst"
              I would recommend discouraging your agent from getting near an enemy by producing
              really large edge costs for going to a vertex that is within the sight of an enemy
     */
    public float getEdgeWeight(StateView state, Vertex src, Vertex dst) {
        float edgeCost = 1f;
        final float ENEMY_SIGHT_PENALTY = 1000f; // Large penalty value
        final float ENEMY_NEAR_SIGHT_PENALTY = 50f; // Large penalty for being near the enemy sight
        final float ENEMY_FAR_SIGHT_PENALTY = 10f;
        final float ENEMY_VERYFAR_SIGHT_PENALTY = 1f;
        int enemyRange = getEnemyUnitSightRadius();

        // Need method to get a list of all enemy positions.
        Set<Vertex> enemyPositions = getEnemyPositions(state);

        for (Vertex enemyPos : enemyPositions) {
            int deltaX = Math.abs(enemyPos.getXCoordinate() - dst.getXCoordinate());
            int deltaY = Math.abs(enemyPos.getYCoordinate() - dst.getYCoordinate());

            // Check if dst is within the enemy's sight range (using Manhattan distance)
            if (deltaX <= enemyRange && deltaY <= enemyRange) {
                edgeCost += ENEMY_SIGHT_PENALTY; // Increase edge cost
            } else if (deltaX <= enemyRange + 1 && deltaY <= enemyRange + 1) {
                edgeCost += ENEMY_NEAR_SIGHT_PENALTY;
            } else if (deltaX <= enemyRange + 3 && deltaY <= enemyRange + 3) {
                edgeCost += ENEMY_FAR_SIGHT_PENALTY;
            } else if (deltaX <= enemyRange + 10 && deltaY <= enemyRange + 10) {
                edgeCost += ENEMY_VERYFAR_SIGHT_PENALTY;
            } 
        }
        return edgeCost;
    }

    /**
        TODO: implement me! This method should implement your A* search algorithm, which is very close
              to how dijkstra's algorithm works, but instead uses an estimated total cost to the goal
              to sort rather than the known cost
     */
    public Path search(Vertex src, Vertex goal, StateView state) {
        System.out.println("Entered search() with source: " + src + " and goal: " + goal);
        Set<Vertex> openSet = new HashSet<>();
        Set<Vertex> closedSet = new HashSet<>();
        Map<Vertex, Float> gScore = new HashMap<>();
        Map<Vertex, Float> fScore = new HashMap<>();
        Map<Vertex, Path> pathMap = new HashMap<>(); // To keep track of the path leading to each vertex
        openSet.add(src); // Initialization
        gScore.put(src, 0f);
        fScore.put(src, heuristic(src, goal));
        pathMap.put(src, new Path(src));

        while (!openSet.isEmpty()) {
            Vertex current = getLowestFScoreVertex(openSet, fScore);
            if (current.equals(goal)) {
                System.out.println("Path to goal found: " + pathMap.get(current));
                return pathMap.get(current); // This will return the path leading to the goal
            }

            openSet.remove(current);
            closedSet.add(current);
            for (Vertex neighbor : getOutgoingNeighbors(current, state)) {
                // System.out.println("Checking neighbor: " + neighbor + " from current: " +
                // current);
                if (closedSet.contains(neighbor)) {
                    continue;
                }

                float tentativeGScore = gScore.get(current) + getEdgeWeight(state, current, neighbor);

                if (!openSet.contains(neighbor) || tentativeGScore < gScore.get(neighbor)) {
                    Path currentPath = pathMap.get(current);
                    Path newPath = new Path(neighbor, tentativeGScore, currentPath);
                    newPath.setEstimatedPathCostToGoal(tentativeGScore + heuristic(neighbor, goal));
                    pathMap.put(neighbor, newPath);
                    gScore.put(neighbor, tentativeGScore);
                    fScore.put(neighbor, tentativeGScore + heuristic(neighbor, goal));
                    // System.out.println("Adding " + neighbor + " to openSet with fScore: " +
                    // fScore.get(neighbor));
                    openSet.add(neighbor);
                }
            }
        }
        return null; // Return null if no path is found
    }

    private float heuristic(Vertex a, Vertex b) {
        return Math.abs(a.getXCoordinate() - b.getXCoordinate()) + Math.abs(a.getYCoordinate() - b.getYCoordinate()); // Manhattan distance
    }
    
    private Vertex getLowestFScoreVertex(Set<Vertex> openSet, Map<Vertex, Float> fScore) {
        return openSet.stream().min(Comparator.comparing(fScore::get)).orElse(null);
    }

    /**
        A helper method to get the direction we will need to go in order to go from src to an adjacent
        vertex dst. Knowing this direction is necessary in order to create primitive moves in Sepia which uses
        the following factory method:
            Action.createPrimitiveMove(<unitIDToMove>, <directionToMove>);
     */
    protected Direction getDirection(Vertex src,
                                     Vertex dst)
    {
        int xDiff = dst.getXCoordinate() - src.getXCoordinate();
        int yDiff = dst.getYCoordinate() - src.getYCoordinate();

        Direction dirToGo = null;

        if(xDiff == 1 && yDiff == 1)
        {
            dirToGo = Direction.SOUTHEAST;
        }
        else if(xDiff == 1 && yDiff == 0)
        {
            dirToGo = Direction.EAST;
        }
        else if(xDiff == 1 && yDiff == -1)
        {
            dirToGo = Direction.NORTHEAST;
        }
        else if(xDiff == 0 && yDiff == 1)
        {
            dirToGo = Direction.SOUTH;
        }
        else if(xDiff == 0 && yDiff == -1)
        {
            dirToGo = Direction.NORTH;
        }
        else if(xDiff == -1 && yDiff == 1)
        {
            dirToGo = Direction.SOUTHWEST;
        }
        else if(xDiff == -1 && yDiff == 0)
        {
            dirToGo = Direction.WEST;
        }
        else if(xDiff == -1 && yDiff == -1)
        {
            dirToGo = Direction.NORTHWEST;
        } else
        {
            System.err.println("ERROR: src=" + src + " and dst=" + dst + " are not adjacent vertices");
        }

        return dirToGo;
    }

}

