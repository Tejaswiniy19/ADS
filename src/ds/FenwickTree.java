package ds;

public class FenwickTree {
    private double[] tree;
    private int size;

    public FenwickTree(int size) {
        this.size = size;
        tree = new double[size + 1];
    }

    public void update(int index, double delta) {
        index++; // 1-based indexing for Fenwick
        while (index <= size) {
            tree[index] += delta;
            index += index & (-index);
        }
    }

    public double query(int index) {
        index++;
        double sum = 0;
        while (index > 0) {
            sum += tree[index];
            index -= index & (-index);
        }
        return sum;
    }

    public double rangeQuery(int left, int right) {
        return query(right) - query(left - 1);
    }
}
