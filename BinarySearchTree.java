import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class BinarySearchTree<T extends Comparable<T>, R> implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private BSTNode<T, R> root;
    
    private static class BSTNode<T, R> implements Serializable {
        private static final long serialVersionUID = 1L;
        T key;
        R record;
        BSTNode<T, R> left;
        BSTNode<T, R> right;
        
        BSTNode(T key, R record) {
            this.key = key;
            this.record = record;
            this.left = null;
            this.right = null;
        }
    }
    
    public BinarySearchTree() {
        root = null;
    }
    
    public void insert(T key, R record) {
        root = insertRecursive(root, key, record);
    }
    
    private BSTNode<T, R> insertRecursive(BSTNode<T, R> node, T key, R record) {
        if (node == null) {
            return new BSTNode<>(key, record);
        }
        int cmp = key.compareTo(node.key);
        if (cmp < 0) {
            node.left = insertRecursive(node.left, key, record);
        } else if (cmp > 0) {
            node.right = insertRecursive(node.right, key, record);
        } else {
            System.out.println("Error: Duplicate key insertion attempted: " + key);
        }
        return node;
    }
    
    public R search(T key) {
        return searchRecursive(root, key);
    }
    
    private R searchRecursive(BSTNode<T, R> node, T key) {
        if (node == null) {
            return null;
        }
        int cmp = key.compareTo(node.key);
        if (cmp == 0) {
            return node.record;
        } else if (cmp < 0) {
            return searchRecursive(node.left, key);
        } else {
            return searchRecursive(node.right, key);
        }
    }
    
    public List<R> inOrderTraversal() {
        List<R> records = new ArrayList<>();
        inOrderRecursive(root, records);
        return records;
    }
    
    private void inOrderRecursive(BSTNode<T, R> node, List<R> records) {
        if (node != null) {
            inOrderRecursive(node.left, records);
            records.add(node.record);
            inOrderRecursive(node.right, records);
        }
    }
    
    public void delete(T key) {
        root = deleteRecursive(root, key);
    }
    
    private BSTNode<T, R> deleteRecursive(BSTNode<T, R> node, T key) {
        if (node == null) {
            return null;
        }
        int cmp = key.compareTo(node.key);
        if (cmp < 0) {
            node.left = deleteRecursive(node.left, key);
        } else if (cmp > 0) {
            node.right = deleteRecursive(node.right, key);
        } else {
            if (node.left == null) {
                return node.right;
            } else if (node.right == null) {
                return node.left;
            }
            BSTNode<T, R> minNode = minValueNode(node.right);
            node.key = minNode.key;
            node.record = minNode.record;
            node.right = deleteRecursive(node.right, minNode.key);
        }
        return node;
    }
    
    private BSTNode<T, R> minValueNode(BSTNode<T, R> node) {
        BSTNode<T, R> current = node;
        while (current.left != null) {
            current = current.left;
        }
        return current;
    }
}
