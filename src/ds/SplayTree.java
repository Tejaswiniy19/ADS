package ds;

import models.Transaction;
import java.util.LinkedList;
import java.util.Queue;

public class SplayTree {

    class Node {
        Transaction data;
        Node left, right, parent;
        public Node(Transaction data) {
            this.data = data;
        }
    }

    public static long rotations = 0;
    private Node root;

    public int getHeight() {
        return getMaxHeight(root);
    }

    private int getMaxHeight(Node node) {
        if (node == null) return 0;
        Queue<Node> queue = new LinkedList<>();
        queue.add(node);
        int height = 0;
        while (!queue.isEmpty()) {
            int size = queue.size();
            for (int i = 0; i < size; i++) {
                Node curr = queue.poll();
                if (curr.left != null) queue.add(curr.left);
                if (curr.right != null) queue.add(curr.right);
            }
            height++;
        }
        return height;
    }

    public void insert(Transaction key) {
        Node z = root;
        Node p = null;

        while (z != null) {
            p = z;
            if (key.transactionId < z.data.transactionId) z = z.left;
            else if (key.transactionId > z.data.transactionId) z = z.right;
            else return; // Duplicate
        }

        z = new Node(key);
        z.parent = p;
        if (p == null) {
            root = z;
        } else if (key.transactionId < p.data.transactionId) {
            p.left = z;
        } else {
            p.right = z;
        }
        splay(z);
    }

    private void leftRotate(Node x) {
        Node y = x.right;
        x.right = y.left;
        if (y.left != null) y.left.parent = x;
        y.parent = x.parent;
        if (x.parent == null) root = y;
        else if (x == x.parent.left) x.parent.left = y;
        else x.parent.right = y;
        y.left = x;
        rotations++;
    }

    private void rightRotate(Node x) {
        Node y = x.left;
        x.left = y.right;
        if (y.right != null) y.right.parent = x;
        y.parent = x.parent;
        if (x.parent == null) root = y;
        else if (x == x.parent.right) x.parent.right = y;
        else x.parent.left = y;
        y.right = x;
        rotations++;
    }

    private void splay(Node x) {
        while (x.parent != null) {
            if (x.parent.parent == null) {
                if (x == x.parent.left) rightRotate(x.parent);
                else leftRotate(x.parent);
            } else if (x == x.parent.left && x.parent == x.parent.parent.left) {
                rightRotate(x.parent.parent);
                rightRotate(x.parent);
            } else if (x == x.parent.right && x.parent == x.parent.parent.right) {
                leftRotate(x.parent.parent);
                leftRotate(x.parent);
            } else if (x == x.parent.right && x.parent == x.parent.parent.left) {
                leftRotate(x.parent);
                rightRotate(x.parent);
            } else {
                rightRotate(x.parent);
                leftRotate(x.parent);
            }
        }
    }

    public Transaction search(long transactionId) {
        Node current = root;
        while (current != null) {
            if (transactionId == current.data.transactionId) {
                splay(current);
                return current.data;
            } else if (transactionId < current.data.transactionId) {
                current = current.left;
            } else {
                current = current.right;
            }
        }
        return null;
    }
}
