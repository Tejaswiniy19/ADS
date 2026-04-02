package ds;

import models.Transaction;

public class AvlTree {
    class Node {
        Transaction data;
        Node left, right;
        int height;

        Node(Transaction data) {
            this.data = data;
            height = 1;
        }
    }

    public static long rotations = 0;
    private Node root;

    public int getHeight() {
        return height(root);
    }

    private int height(Node N) {
        if (N == null) return 0;
        return N.height;
    }

    private int max(int a, int b) {
        return Math.max(a, b);
    }

    private Node rightRotate(Node y) {
        Node x = y.left;
        Node T2 = x.right;

        x.right = y;
        y.left = T2;

        y.height = max(height(y.left), height(y.right)) + 1;
        x.height = max(height(x.left), height(x.right)) + 1;

        rotations++;
        return x;
    }

    private Node leftRotate(Node x) {
        Node y = x.right;
        Node T2 = y.left;

        y.left = x;
        x.right = T2;

        x.height = max(height(x.left), height(x.right)) + 1;
        y.height = max(height(y.left), height(y.right)) + 1;

        rotations++;
        return y;
    }


    private int getBalance(Node N) {
        if (N == null) return 0;
        return height(N.left) - height(N.right);
    }

    public void insert(Transaction tx) {
        root = insertRec(root, tx);
    }

    private Node insertRec(Node node, Transaction data) {
        if (node == null) return new Node(data);

        if (data.transactionId < node.data.transactionId)
            node.left = insertRec(node.left, data);
        else if (data.transactionId > node.data.transactionId)
            node.right = insertRec(node.right, data);
        else return node; // Duplicate keys not allowed

        node.height = 1 + max(height(node.left), height(node.right));

        int balance = getBalance(node);

        if (balance > 1 && data.transactionId < node.left.data.transactionId)
            return rightRotate(node);

        if (balance < -1 && data.transactionId > node.right.data.transactionId)
            return leftRotate(node);

        if (balance > 1 && data.transactionId > node.left.data.transactionId) {
            node.left = leftRotate(node.left);
            return rightRotate(node);
        }

        if (balance < -1 && data.transactionId < node.right.data.transactionId) {
            node.right = rightRotate(node.right);
            return leftRotate(node);
        }

        return node;
    }

    public Transaction search(long transactionId) {
        Node res = searchRec(root, transactionId);
        return res != null ? res.data : null;
    }

    private Node searchRec(Node root, long id) {
        if (root == null || root.data.transactionId == id)
            return root;

        if (root.data.transactionId < id)
            return searchRec(root.right, id);

        return searchRec(root.left, id);
    }
}
