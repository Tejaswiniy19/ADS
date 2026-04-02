package benchmark;

import ds.*;
import models.Transaction;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SystemEvaluation {
    static class Query {
        long startId, endId;
        Query(long s, long e) { startId = s; endId = e; }
    }
    
    static class Update {
        long targetId;
        double newAmount;
        Update(long t, double d) { targetId = t; newAmount = d; }
    }

    public static void main(String[] args) {
        String[] datasets = {
            "csv/transactions_500000.csv",
            "csv/test_sorted_transactions.csv",
            "csv/test_reverse_transactions.csv",
            "csv/test_skewed_transactions.csv"
        };
        String[] datasetNames = {"random", "sorted", "reverse", "skewed"};

        StringBuilder json = new StringBuilder();
        json.append("{\n  \"datasets\": {\n");

        for (int i = 0; i < datasets.length; i++) {
            try {
                System.out.println("\n--- Evaluating Dataset: " + datasetNames[i] + " ---");
                DatasetMetrics metrics = evaluateDataset(datasets[i]);
                appendDatasetJson(json, datasetNames[i], metrics, i == datasets.length - 1);
            } catch (Exception e) {
                System.err.println("Fatal error evaluating dataset " + datasetNames[i]);
                e.printStackTrace();
            }
        }

        json.append("  }\n}");

        try (java.io.PrintWriter out = new java.io.PrintWriter("metrics.json")) {
            out.println(json.toString());
            System.out.println("\nSuccessfully exported metrics to metrics.json");
        } catch (IOException e) {
            System.err.println("Failed to write metrics.json: " + e.getMessage());
        }
    }

    static class DatasetMetrics {
        long avlInsert, avlSearch, rbInsert, rbSearch, splayInsert, splaySearch, bTreeInsert, bTreeSearch, bPlusInsert, bPlusSearch;
        long avlRotations, rbRotations, splayRotations;
        long bTreeSplits, bPlusSplits;
        int avlHeight, rbHeight, splayHeight, bTreeHeight, bPlusHeight;
        long segmentBuild, fenwickBuild, hiatBuild, hiatRange, hiatUpdate;

        // Validation samples
        long vSearchId; boolean vSearchFound; double vSearchTime;
        long vRangeStart, vRangeEnd; double vRangeSum, vRangeTime;
        long vUpdateId; double vUpdateOld, vUpdateNew, vUpdateTime;
    }

    private static DatasetMetrics evaluateDataset(String filename) {
        DatasetMetrics m = new DatasetMetrics();
        List<Transaction> transactions = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line; br.readLine();
            while ((line = br.readLine()) != null) {
                String[] p = line.split(",");
                transactions.add(new Transaction(Long.parseLong(p[0]), Long.parseLong(p[1]), Long.parseLong(p[2]), Double.parseDouble(p[3]), Double.parseDouble(p[4])));
            }
        } catch (IOException e) { return m; }

        int n = transactions.size();
        double[] amounts = new double[n];
        for (int i = 0; i < n; i++) amounts[i] = transactions.get(i).amount;

        // AVL
        AvlTree.rotations = 0;
        AvlTree avl = new AvlTree();
        long start = System.nanoTime();
        for (Transaction t : transactions) avl.insert(t);
        m.avlInsert = (System.nanoTime() - start) / 1000000;
        m.avlRotations = AvlTree.rotations;
        m.avlHeight = avl.getHeight();

        // RB
        RedBlackTree.rotations = 0;
        RedBlackTree rb = new RedBlackTree();
        start = System.nanoTime();
        for (Transaction t : transactions) rb.insert(t);
        m.rbInsert = (System.nanoTime() - start) / 1000000;
        m.rbRotations = RedBlackTree.rotations;
        m.rbHeight = rb.getHeight();

        // Splay
        SplayTree.rotations = 0;
        SplayTree splay = new SplayTree();
        start = System.nanoTime();
        for (Transaction t : transactions) splay.insert(t);
        m.splayInsert = (System.nanoTime() - start) / 1000000;
        m.splayRotations = SplayTree.rotations;
        m.splayHeight = splay.getHeight();

        // B-Tree
        BTree.nodeSplits = 0;
        BTree btree = new BTree(16);
        start = System.nanoTime();
        for (Transaction t : transactions) btree.insert(t);
        m.bTreeInsert = (System.nanoTime() - start) / 1000000;
        m.bTreeSplits = BTree.nodeSplits;
        m.bTreeHeight = btree.getHeight();

        // B+ Tree
        BPlusTree.nodeSplits = 0;
        BPlusTree bplus = new BPlusTree(16);
        start = System.nanoTime();
        for (Transaction t : transactions) bplus.insert(t);
        m.bPlusInsert = (System.nanoTime() - start) / 1000000;
        m.bPlusSplits = BPlusTree.nodeSplits;
        m.bPlusHeight = bplus.getHeight();

        // Search (10k random keys)
        Random rand = new Random(42);
        List<Long> keys = new ArrayList<>();
        for(int i=0; i<10000; i++) keys.add(transactions.get(rand.nextInt(n)).transactionId);

        start = System.nanoTime(); for(Long k : keys) avl.search(k); m.avlSearch = (System.nanoTime() - start) / 1000000;
        start = System.nanoTime(); for(Long k : keys) rb.search(k); m.rbSearch = (System.nanoTime() - start) / 1000000;
        start = System.nanoTime(); for(Long k : keys) splay.search(k); m.splaySearch = (System.nanoTime() - start) / 1000000;
        start = System.nanoTime(); for(Long k : keys) btree.search(k); m.bTreeSearch = (System.nanoTime() - start) / 1000000;
        start = System.nanoTime(); for(Long k : keys) bplus.search(k); m.bPlusSearch = (System.nanoTime() - start) / 1000000;

        // Analytics
        start = System.nanoTime(); new SegmentTree(amounts); m.segmentBuild = (System.nanoTime() - start) / 1000000;
        start = System.nanoTime(); FenwickTree ft = new FenwickTree(n); for(int i=0; i<n; i++) ft.update(i, amounts[i]); m.fenwickBuild = (System.nanoTime() - start) / 1000000;
        start = System.nanoTime(); HIAT hiat = new HIAT(n); for(Transaction t : transactions) hiat.insert(t); m.hiatBuild = (System.nanoTime() - start) / 1000000;

        // HIAT Range / Update
        long rangeStart = 1000000L + 5000;
        long rangeEnd = rangeStart + 1000;
        start = System.nanoTime(); double sum = hiat.getRangeSum(rangeStart, rangeEnd); long rangeNanos = System.nanoTime() - start;
        m.hiatRange = rangeNanos / 1000;
        
        long updateId = transactions.get(0).transactionId;
        start = System.nanoTime(); hiat.update(updateId, 999.99); long updateNanos = System.nanoTime() - start;
        m.hiatUpdate = updateNanos / 1000;

        // Validation Samples
        m.vSearchId = keys.get(0);
        start = System.nanoTime(); m.vSearchFound = (avl.search(m.vSearchId) != null); m.vSearchTime = (System.nanoTime() - start) / 1e9;
        
        m.vRangeStart = rangeStart; m.vRangeEnd = rangeEnd;
        m.vRangeSum = sum; m.vRangeTime = (rangeNanos) / 1e9;
        
        m.vUpdateId = updateId; m.vUpdateOld = transactions.get(0).amount; m.vUpdateNew = 999.99;
        m.vUpdateTime = (updateNanos) / 1e9;

        return m;
    }

    private static void appendDatasetJson(StringBuilder sb, String name, DatasetMetrics m, boolean isLast) {
        sb.append("    \"").append(name).append("\": {\n");
        sb.append("      \"avl\": {\"insert_ms\": ").append(m.avlInsert).append(", \"search_ms\": ").append(m.avlSearch).append(", \"rotations\": ").append(m.avlRotations).append(", \"height\": ").append(m.avlHeight).append("},\n");
        sb.append("      \"rb\": {\"insert_ms\": ").append(m.rbInsert).append(", \"search_ms\": ").append(m.rbSearch).append(", \"rotations\": ").append(m.rbRotations).append(", \"height\": ").append(m.rbHeight).append("},\n");
        sb.append("      \"splay\": {\"insert_ms\": ").append(m.splayInsert).append(", \"search_ms\": ").append(m.splaySearch).append(", \"rotations\": ").append(m.splayRotations).append(", \"height\": ").append(m.splayHeight).append("},\n");
        sb.append("      \"btree\": {\"insert_ms\": ").append(m.bTreeInsert).append(", \"search_ms\": ").append(m.bTreeSearch).append(", \"splits\": ").append(m.bTreeSplits).append(", \"height\": ").append(m.bTreeHeight).append("},\n");
        sb.append("      \"bplus\": {\"insert_ms\": ").append(m.bPlusInsert).append(", \"search_ms\": ").append(m.bPlusSearch).append(", \"splits\": ").append(m.bPlusSplits).append(", \"height\": ").append(m.bPlusHeight).append("},\n");
        sb.append("      \"analytics\": {\"segment_ms\": ").append(m.segmentBuild).append(", \"fenwick_ms\": ").append(m.fenwickBuild).append(", \"hiat_ms\": ").append(m.hiatBuild).append(", \"range_ms\": ").append(m.hiatRange).append(", \"update_ms\": ").append(m.hiatUpdate).append("},\n");
        sb.append("      \"validation\": {\n");
        sb.append("        \"search\": {\"id\": ").append(m.vSearchId).append(", \"found\": ").append(m.vSearchFound).append(", \"time\": ").append(m.vSearchTime).append("},\n");
        sb.append("        \"range\": {\"start\": ").append(m.vRangeStart).append(", \"end\": ").append(m.vRangeEnd).append(", \"sum\": ").append(m.vRangeSum).append(", \"time\": ").append(m.vRangeTime).append("},\n");
        sb.append("        \"update\": {\"id\": ").append(m.vUpdateId).append(", \"old\": ").append(m.vUpdateOld).append(", \"new\": ").append(m.vUpdateNew).append(", \"time\": ").append(m.vUpdateTime).append("}\n");
        sb.append("      }\n");
        sb.append("    }").append(isLast ? "" : ",").append("\n");
    }
}
