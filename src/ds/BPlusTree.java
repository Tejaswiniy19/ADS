package ds;

import models.Transaction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Simplified B+ Tree Implementation for Benchmarking
 */
public class BPlusTree {
    private int m;
    public static long nodeSplits = 0;
    private Node root;

    public int getHeight() {
        int height = 0;
        Node curr = root;
        while (!(curr instanceof LeafNode)) {
            height++;
            curr = ((InternalNode) curr).children.get(0);
        }
        return height + 1;
    }

    public BPlusTree(int degree) {
        this.m = degree;
        this.root = new LeafNode();
    }

    private class SplitResult {
        long key;
        Node sibling;
        SplitResult(long k, Node s) { key = k; sibling = s; }
    }

    private abstract class Node {
        List<Long> keys;
        Node() {
            keys = new ArrayList<>();
        }
        abstract boolean isOverflow();
        abstract SplitResult split();
    }

    private class InternalNode extends Node {
        List<Node> children;

        InternalNode() {
            super();
            children = new ArrayList<>();
        }

        @Override
        boolean isOverflow() {
            return children.size() > m;
        }

        @Override
        SplitResult split() {
            int mid = keys.size() / 2;
            long upKey = keys.get(mid);
            InternalNode sibling = new InternalNode();
            sibling.keys.addAll(keys.subList(mid + 1, keys.size()));
            sibling.children.addAll(children.subList(mid + 1, children.size()));
            keys.subList(mid, keys.size()).clear();
            children.subList(mid + 1, children.size()).clear();
            nodeSplits++;
            return new SplitResult(upKey, sibling);
        }
    }

    private class LeafNode extends Node {
        List<Transaction> values;
        LeafNode next;

        LeafNode() {
            super();
            values = new ArrayList<>();
        }

        @Override
        boolean isOverflow() {
            return values.size() >= m;
        }

        @Override
        SplitResult split() {
            int mid = keys.size() / 2;
            LeafNode sibling = new LeafNode();
            sibling.keys.addAll(keys.subList(mid, keys.size()));
            sibling.values.addAll(values.subList(mid, values.size()));
            keys.subList(mid, keys.size()).clear();
            values.subList(mid, values.size()).clear();
            sibling.next = this.next;
            this.next = sibling;
            nodeSplits++;
            return new SplitResult(sibling.keys.get(0), sibling);
        }
    }

    public void insert(Transaction tx) {
        long key = tx.transactionId;
        SplitResult res = insertRec(root, key, tx);
        if (res != null) {
            InternalNode newRoot = new InternalNode();
            newRoot.keys.add(res.key);
            newRoot.children.add(root);
            newRoot.children.add(res.sibling);
            root = newRoot;
        }
    }

    private SplitResult insertRec(Node node, long key, Transaction tx) {
        if (node instanceof LeafNode) {
            LeafNode leaf = (LeafNode) node;
            int idx = Collections.binarySearch(leaf.keys, key);
            if (idx < 0) {
                idx = -idx - 1;
                leaf.keys.add(idx, key);
                leaf.values.add(idx, tx);
            }
            if (leaf.isOverflow()) return leaf.split();
            return null;
        } else {
            InternalNode internal = (InternalNode) node;
            int idx = Collections.binarySearch(internal.keys, key);
            if (idx < 0) idx = -idx - 1;
            else idx++;
            if (idx >= internal.children.size()) idx = internal.children.size() - 1;
            SplitResult res = insertRec(internal.children.get(idx), key, tx);
            if (res != null) {
                internal.keys.add(idx, res.key);
                internal.children.add(idx + 1, res.sibling);
                if (internal.isOverflow()) return internal.split();
            }
            return null;
        }
    }

    public Transaction search(long key) {
        Node curr = root;
        while (curr instanceof InternalNode) {
            InternalNode internal = (InternalNode) curr;
            int idx = Collections.binarySearch(internal.keys, key);
            if (idx < 0) idx = -idx - 1;
            else idx++;
            if (idx >= internal.children.size()) idx = internal.children.size() - 1;
            curr = internal.children.get(idx);
        }
        LeafNode leaf = (LeafNode) curr;
        int idx = Collections.binarySearch(leaf.keys, key);
        if (idx >= 0) return leaf.values.get(idx);
        return null;
    }
}
