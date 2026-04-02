package ds;

public class SegmentTree {
    private double[] tree;
    private int n;

    public SegmentTree(double[] arr) {
        n = arr.length;
        tree = new double[4 * n];
        build(arr, 0, 0, n - 1);
    }

    private void build(double[] arr, int node, int start, int end) {
        if (start == end) {
            tree[node] = arr[start];
        } else {
            int mid = (start + end) / 2;
            int leftChild = 2 * node + 1;
            int rightChild = 2 * node + 2;

            build(arr, leftChild, start, mid);
            build(arr, rightChild, mid + 1, end);

            tree[node] = tree[leftChild] + tree[rightChild];
        }
    }

    public void update(int index, double val) {
        updateRec(0, 0, n - 1, index, val);
    }

    private void updateRec(int node, int start, int end, int idx, double val) {
        if (start == end) {
            tree[node] = val; // Assuming we set the value directly
        } else {
            int mid = (start + end) / 2;
            int leftChild = 2 * node + 1;
            int rightChild = 2 * node + 2;

            if (start <= idx && idx <= mid) {
                updateRec(leftChild, start, mid, idx, val);
            } else {
                updateRec(rightChild, mid + 1, end, idx, val);
            }

            tree[node] = tree[leftChild] + tree[rightChild];
        }
    }

    public double query(int l, int r) {
        return queryRec(0, 0, n - 1, l, r);
    }

    private double queryRec(int node, int start, int end, int l, int r) {
        if (r < start || end < l) {
            return 0; // Out of range
        }
        if (l <= start && end <= r) {
            return tree[node]; // Completely in range
        }
        int mid = (start + end) / 2;
        int leftChild = 2 * node + 1;
        int rightChild = 2 * node + 2;

        double p1 = queryRec(leftChild, start, mid, l, r);
        double p2 = queryRec(rightChild, mid + 1, end, l, r);

        return p1 + p2;
    }
}
