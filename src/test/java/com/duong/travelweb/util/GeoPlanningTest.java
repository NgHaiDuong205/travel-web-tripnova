package com.duong.travelweb.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeoPlanningTest {
    @Test
    void haversineMatchesKnownDistance() {
        // Hồ Gươm → Lăng Bác ~ 2.1 km đường chim bay
        double km = GeoPlanning.haversineKm(21.0287, 105.8524, 21.0368, 105.8346);
        assertEquals(2.1, km, 0.3);
        // Hà Nội → TP.HCM ~ 1140 km
        assertEquals(1140, GeoPlanning.haversineKm(21.0285, 105.8542, 10.7769, 106.7009), 20);
    }

    @Test
    void clustersSeparateDistantGroups() {
        Random random = new Random(1);
        List<double[]> points = new ArrayList<>();
        double[][] centers = {{16.06, 108.22}, {15.88, 108.33}, {16.00, 108.05}};
        for (double[] c : centers) {
            for (int i = 0; i < 5; i++) {
                points.add(new double[]{c[0] + random.nextGaussian() * 0.005, c[1] + random.nextGaussian() * 0.005});
            }
        }
        int[] labels = GeoPlanning.balancedClusters(points, 3, 42);
        for (int g = 0; g < 3; g++) {
            Set<Integer> groupLabels = new HashSet<>();
            for (int i = 0; i < 5; i++) {
                groupLabels.add(labels[g * 5 + i]);
            }
            assertEquals(1, groupLabels.size(), "mỗi nhóm điểm gần nhau phải chung một ngày");
        }
        assertEquals(3, new HashSet<>(toList(labels)).size());
        assertArrayEquals(labels, GeoPlanning.balancedClusters(points, 3, 42), "cùng seed phải cùng kết quả");
    }

    @Test
    void clustersAreBalancedAndNeverEmpty() {
        Random random = new Random(7);
        List<double[]> points = new ArrayList<>();
        // 9 điểm dồn một chỗ + 1 điểm lẻ: k-means thường để trống cụm, bản cân bằng thì không.
        for (int i = 0; i < 9; i++) {
            points.add(new double[]{10.77 + random.nextDouble() * 0.01, 106.70 + random.nextDouble() * 0.01});
        }
        points.add(new double[]{10.30, 107.08});
        for (int k = 1; k <= 7; k++) {
            int[] labels = GeoPlanning.balancedClusters(points, k, 3);
            int[] count = new int[k];
            for (int label : labels) {
                count[label]++;
            }
            for (int c = 0; c < k; c++) {
                assertTrue(count[c] >= points.size() / k && count[c] <= (points.size() + k - 1) / k,
                        "k=" + k + " cụm " + c + " có " + count[c] + " điểm");
            }
        }
    }

    @Test
    void bestOpenPathFollowsLine() {
        // Các điểm trên một đường thẳng, xáo trộn; xuất phát ở đầu dãy → phải đi lần lượt.
        List<double[]> points = new ArrayList<>();
        int[] shuffled = {4, 0, 6, 2, 5, 1, 3};
        for (int i : shuffled) {
            points.add(new double[]{16.0, 108.0 + i * 0.01});
        }
        int[] order = GeoPlanning.bestOpenPath(new double[]{16.0, 107.99}, points);
        for (int i = 0; i < order.length; i++) {
            assertEquals(i, shuffled[order[i]]);
        }
    }

    @Test
    void bestOpenPathHandlesManyPointsWithTwoOpt() {
        List<double[]> points = new ArrayList<>();
        int[] shuffled = {9, 3, 0, 11, 6, 2, 8, 1, 10, 5, 7, 4};
        for (int i : shuffled) {
            points.add(new double[]{16.0 + i * 0.01, 108.0});
        }
        int[] order = GeoPlanning.bestOpenPath(new double[]{15.99, 108.0}, points);
        for (int i = 0; i < order.length; i++) {
            assertEquals(i, shuffled[order[i]]);
        }
        assertEquals(0, GeoPlanning.bestOpenPath(null, new ArrayList<>()).length);
    }

    private static List<Integer> toList(int[] values) {
        List<Integer> list = new ArrayList<>();
        for (int v : values) {
            list.add(v);
        }
        return list;
    }
}
