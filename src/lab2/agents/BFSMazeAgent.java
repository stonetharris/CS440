package src.lab2.agents;
// SYSTEM IMPORTS
import edu.bu.lab2.agents.MazeAgent;
import edu.bu.lab2.graph.Vertex;
import edu.bu.lab2.graph.Path;

import edu.cwru.sepia.environment.model.state.State.StateView;

import java.util.HashSet;       // will need for bfs
import java.util.Queue;         // will need for bfs
import java.util.LinkedList;    // will need for bfs
import java.util.Set;           // will need for bfs


// JAVA PROJECT IMPORTS

public class BFSMazeAgent
    extends MazeAgent
{
    private StateView stateView;

    public BFSMazeAgent(int playerNum)
    {
        super(playerNum);
        // System.out.println("BFSMazeAgent constructor called with playerNum: " + playerNum);
    }

@Override
public Path search(Vertex src, Vertex goal, StateView state) {

    Queue<Path> queue = new LinkedList<>();
    Set<Vertex> visited = new HashSet<>();
    Path srcPath = new Path(src);
    queue.add(srcPath);
    visited.add(src);
    int[][] possibleMovements = {{1, 1}, {1, 0}, {1, -1}, {0, 1}, {0, -1}, {-1, -1}, {-1, 0}, {-1, 1}};

    while (!queue.isEmpty()) {
        Path currentPath = queue.poll();
        Vertex currentCoordinates = currentPath.getDestination();

        for (int i = 0; i < 8; i++) {
            int newX = currentCoordinates.getXCoordinate() + possibleMovements[i][0];
            int newY = currentCoordinates.getYCoordinate() + possibleMovements[i][1];
            if (Math.abs(newX - goal.getXCoordinate()) <= 1 && 
                Math.abs(newY - goal.getYCoordinate()) <= 1) {
                return new Path(new Vertex(newX, newY), 1, currentPath);
            }

            if (state.inBounds(newX, newY) && !state.isResourceAt(newX, newY) && !state.isUnitAt(newX, newY)) {
                Vertex temporary = new Vertex(newX, newY);
                if (visited.add(temporary)) {
                    Path newPath = new Path(temporary, 1, currentPath);
                    queue.add(newPath);
                }
            }
        }
    }

    return new Path(src);
}

    @Override
    public boolean shouldReplacePlan(StateView state) {
        return false;
}
}