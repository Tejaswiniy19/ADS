package ds;

import models.Transaction;
import java.util.TreeMap;

/**
 * Red-Black Tree Implementation for Benchmarking
 * Uses Java's built-in TreeMap which is backed by a Red-Black Tree.
 */
public class RedBlackTree {
    public static long rotations = 0;
    private TreeMap<Long, Transaction> tree;

    public int getHeight() {
        if (tree.isEmpty()) return 0;
        return (int) (Math.log(tree.size()) / Math.log(2)) + 1;
    }

    public RedBlackTree() {
        tree = new TreeMap<>();
    }

    public void insert(Transaction tx) {
        tree.put(tx.transactionId, tx);
    }

    public void delete(long transactionId) {
        tree.remove(transactionId);
    }

    public Transaction search(long transactionId) {
        return tree.get(transactionId);
    }
}
