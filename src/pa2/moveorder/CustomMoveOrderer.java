package src.pa2.moveorder;


// SYSTEM IMPORTS
import edu.bu.chess.search.DFSTreeNode;

import java.util.ArrayList;
import java.util.List;


// JAVA PROJECT IMPORTS
import src.pa2.moveorder.DefaultMoveOrderer;

public class CustomMoveOrderer
    extends Object
{

	/**
	 * TODO: implement me!
	 * This method should perform move ordering. Remember, move ordering is how alpha-beta pruning gets part of its power from.
	 * You want to see nodes which are beneficial FIRST so you can prune as much as possible during the search (i.e. be faster)
	 * @param nodes. The nodes to order (these are children of a DFSTreeNode) that we are about to consider in the search.
	 * @return The ordered nodes.
	 */
	public static List<DFSTreeNode> order(List<DFSTreeNode> nodes) {
        List<DFSTreeNode> captureMoves = new ArrayList<>();
        List<DFSTreeNode> promotePawnMoves = new ArrayList<>();
        List<DFSTreeNode> castleMoves = new ArrayList<>();
        List<DFSTreeNode> otherMoves = new ArrayList<>();

        for (DFSTreeNode node : nodes) {
            if (node.getMove() != null) {
                switch (node.getMove().getType()) {
                    case CAPTUREMOVE:
                        captureMoves.add(node);
                        break;
                    case PROMOTEPAWNMOVE:
                        promotePawnMoves.add(node);
                        break;
                    case CASTLEMOVE:
                        castleMoves.add(node);
                        break;
                    default:
                        otherMoves.add(node);
                        break;
                }
            } else {
                otherMoves.add(node);
            }
        }

        // Combine the lists, with capture moves first, then pawn promotions, then castling, then others
        List<DFSTreeNode> orderedMoves = new ArrayList<>();
        orderedMoves.addAll(captureMoves);
        orderedMoves.addAll(promotePawnMoves);
        orderedMoves.addAll(castleMoves);
        orderedMoves.addAll(otherMoves);

        return orderedMoves;
		// return DefaultMoveOrderer.order(nodes);
    }

}
