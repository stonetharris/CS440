package src.lab9.agents;

// SYSTEM IMPORTS

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// JAVA PROJECT IMPORTS
import edu.bu.lab9.agents.SurvivalAgent;
import edu.bu.lab9.dtree.quality.Entropy;
import edu.bu.lab9.linalg.Matrix;
import edu.bu.lab9.utils.Pair;

public class DecisionTreeAgent extends SurvivalAgent {
    public enum FeatureType {
        CONTINUOUS,
        DISCRETE
    }

    public static class DecisionTree extends Object {
        public static class Node extends Object {
            boolean isLeaf;
            int classification;
            int featureIndex;
            double threshold;
            Node leftChild;
            Node rightChild;
        }

        public Node root;
        public static final FeatureType[] FEATURE_HEADER = {FeatureType.CONTINUOUS, FeatureType.CONTINUOUS, FeatureType.DISCRETE, FeatureType.DISCRETE};
        public DecisionTree() {
            this.root = null;
        }

        public Node getRoot() { return this.root; }
        private void setRoot(Node n) { this.root = n; }

        public void fit(Matrix X, Matrix y_gt) {
            this.root = buildTree(X, y_gt, FEATURE_HEADER);
        }

        public int predict(Matrix x) {
            // TODO: complete me!
            // class 0 means Human (i.e. not a zombie), class 1 means zombie
            return 1;
        }

    }

    private DecisionTree tree;

    public DecisionTreeAgent(int playerNum, String[] args) {
        super(playerNum, args);
        this.tree = new DecisionTree();
    }

    public DecisionTree getTree() { 
        return this.tree; 
    }

    private Node buildTree(Matrix X, Matrix y, FeatureType[] features) {
        if (allSameClass(y)) {
            Node leaf = new Node();
            leaf.isLeaf = true;
            leaf.classification = y.get(0, 0); 
            return leaf;
        }
    
        int bestFeatureIndex = selectBestFeature(X, y, features);
        double[] bestFeatureValues = X.getColumn(bestFeatureIndex);
        double threshold = calculateThreshold(bestFeatureValues); 
    
        Node node = new Node();
        node.featureIndex = bestFeatureIndex;
        node.threshold = threshold; 
    
        Pair<Matrix, Matrix> splitData = splitData(X, y, bestFeatureIndex, threshold);
        node.leftChild = buildTree(splitData.first, splitData.second, updatedFeatures(features, bestFeatureIndex));
        node.rightChild = buildTree(splitData.third, splitData.fourth, updatedFeatures(features, bestFeatureIndex));
    
        return node;
    }

    @Override
    public void train(Matrix X, Matrix y_gt) {
        System.out.println(X.getShape() + " " + y_gt.getShape());
        this.getTree().fit(X, y_gt);
    }

    @Override
    public int predict(Matrix featureRowVector) {
        return this.getTree().predict(featureRowVector);
    }

}
