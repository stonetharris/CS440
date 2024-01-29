package src.lab2.agents;

// SYSTEM IMPORTS
import edu.bu.lab2.agents.MazeAgent;
import edu.bu.lab2.graph.Vertex;
import edu.bu.lab2.graph.Path;

import edu.cwru.sepia.environment.model.state.State.StateView;

import java.util.HashSet;   // will need for dfs
import java.util.Stack;     // will need for dfs
import java.util.Set;       // will need for dfs


// JAVA PROJECT IMPORTS


public class DFSMazeAgent
    extends MazeAgent
{

    public DFSMazeAgent(int playerNum)
    {
        super(playerNum);
    }

    @Override
    public Path search(Vertex src,
                       Vertex goal,
                       StateView state)
    {
        Stack<Path> stack = new Stack<>();
        Set<Vertex> visited = new HashSet<>();
        Path initialPath = new Path(src);
        stack.push(initialPath);
        visited.add(src);

        int[][] movements = {
            {1, 1}, {1, 0}, {1, -1}, {0, 1}, {0, -1}, {-1, -1}, {-1, 0}, {-1, 1}
        };

        while (!stack.isEmpty()) {
            Path currentPath = stack.pop();
            Vertex currentVertex = currentPath.getDestination();
            
            if (Math.abs(currentVertex.getXCoordinate() - goal.getXCoordinate()) <= 1 &&
                Math.abs(currentVertex.getYCoordinate() - goal.getYCoordinate()) <= 1) {
                return currentPath;
            }

            for (int i = 0; i < 8; i++) {
                int newX = currentVertex.getXCoordinate() + movements[i][0];
                int newY = currentVertex.getYCoordinate() + movements[i][1];
                Vertex nextVertex = new Vertex(newX, newY);
                if (state.inBounds(newX, newY) && 
                    !state.isResourceAt(newX, newY) && 
                    !state.isUnitAt(newX, newY) && 
                    !visited.contains(nextVertex)) {
                    visited.add(nextVertex);
                    stack.push(new Path(nextVertex, 1, currentPath));
                }
            }
        }

        return new Path(src);
    }

    @Override
    public boolean shouldReplacePlan(StateView state)
    {
        return false;
    }

}
