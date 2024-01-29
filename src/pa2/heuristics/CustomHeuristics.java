package src.pa2.heuristics;

import edu.bu.chess.search.DFSTreeNode;

// SYSTEM IMPORTS
import src.pa2.heuristics.DefaultHeuristics;
import edu.bu.chess.game.piece.Piece;
import edu.bu.chess.game.piece.PieceType;
import edu.bu.chess.game.move.Move;
import edu.bu.chess.game.move.MoveType;
import edu.bu.chess.game.player.Player;
import edu.bu.chess.utils.Coordinate;
import java.util.List;
import java.util.Set;


// JAVA PROJECT IMPORTS
import src.pa2.heuristics.DefaultHeuristics;


public class CustomHeuristics extends Object {

	/**
	 * TODO: implement me! The heuristics that I wrote are useful, but not very good for a good chessbot.
	 * Please use this class to add your heuristics here! I recommend taking a look at the ones I provided for you
	 * in DefaultHeuristics.java (which is in the same directory as this file)
	 */
	public static double getMaxPlayerHeuristicValue(DFSTreeNode node) {
        double materialScore = getMaterialScore(node);
        double positionalScore = getPositionalScore(node);
        double mobilityScore = getMobilityScore(node);
        double kingSafetyScore = getKingSafetyScore(node);

        // Combine these heuristics in a way that reflects their importance
        return materialScore + positionalScore + mobilityScore + kingSafetyScore;
		// return DefaultHeuristics.getMaxPlayerHeuristicValue(node);
	}

	private static double getMaterialScore(DFSTreeNode node) {
		double score = 0.0;
		Set<Piece> pieces = node.getGame().getBoard().getPieces(node.getMaxPlayer());
		for (Piece piece : pieces) {
			score += getPieceValue(piece.getType());
		}
		return score;
	}

	private static double getPieceValue(PieceType pieceType) {
        // Implement logic to assign values to each piece type
        switch (pieceType) {
            case PAWN: return 1.0;
            case KNIGHT: return 3.0;
            case BISHOP: return 3.0;
            case ROOK: return 5.0;
            case QUEEN: return 9.0;
            case KING: return 0.0; // King's value is not typically counted in material evaluation
            default: return 0.0;
        }
    }

    private static double getPositionalScore(DFSTreeNode node) {
        double score = 0.0;
        for (Piece piece : node.getGame().getBoard().getPieces(node.getMaxPlayer())) {
            Coordinate position = node.getGame().getBoard().getPiecePosition(piece);
            score += getPositionalValue(piece.getType(), position);
        }
        return score;
    }

	private static double getPositionalValue(PieceType type, Coordinate position) {
		// Positional values can be more subjective and complex.
		// Here, we'll just give central pawns some additional value.
		if (type == PieceType.PAWN && (position.getXPosition() >= 2 && position.getXPosition() <= 5) && (position.getYPosition() >= 2 && position.getYPosition() <= 5)) {
			return 0.5; // Central pawn bonus
		}
		return 0.0;
	}

    private static double getMobilityScore(DFSTreeNode node) {
		// Assume node provides a way to get all legal moves for the player
		List<Move> moves = node.getGame().getAllMoves(node.getMaxPlayer());
		return moves.size(); // More moves might imply better mobility
	}

	private static boolean isKingThreatened(DFSTreeNode node, Piece king) {
		// Get all possible capture moves for the opponent
		Player opponent = node.getGame().getOtherPlayer(node.getMaxPlayer());
		List<Move> opponentCaptureMoves = node.getGame().getAllCaptureMoves(opponent);
	
		// Check if any capture move targets the king
		for (Move move : opponentCaptureMoves) {
			if (move.getType() == MoveType.CAPTUREMOVE) {
				// Assuming there's a way to get the target piece of the capture move
				// You'll need to replace getTargetPiece() with the actual method to get the target piece from a capture move
				Piece targetPiece = getTargetPiece(move); 
				if (targetPiece == king) {
					return true; // King is directly targeted by a capture move
				}
			}
		}
		return false; // The king is safe for now
	}
	
	// This is a placeholder method and needs to be replaced with the actual implementation
	private static Piece getTargetPiece(Move move) {
		// Implement logic to retrieve the target piece of a capture move
		// This depends on how your Move class and its subclasses (if any) are structured
		return null; // Placeholder return
	}
	
	private static double getKingSafetyScore(DFSTreeNode node) {
		double score = 0.0;
		Set<Piece> pieces = node.getGame().getBoard().getPieces(node.getMaxPlayer(), PieceType.KING);
		if (pieces.size() == 1) {
			Piece king = pieces.iterator().next();
			Coordinate kingPos = node.getGame().getBoard().getPiecePosition(king);
			if (!isKingThreatened(node, king)) {
				score += 1.0; // Not threatened, so increase score for safety
			}
		}
		return score;
	}
}