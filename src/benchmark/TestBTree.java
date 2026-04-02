package benchmark;

import ds.BTree;
import models.Transaction;

public class TestBTree {
    public static void main(String[] args) {
        BTree btree = new BTree(4);
        for(int i = 0; i < 1000; i++) {
            btree.insert(new Transaction(i, 2, 3, 4, 5));
        }
        System.out.println("BTree ok");
        System.out.println(btree.search(500) != null);
    }
}
