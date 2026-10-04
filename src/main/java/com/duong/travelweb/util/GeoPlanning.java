package com.duong.travelweb.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Thuật toán hình học cho AI Planner (thuần, không phụ thuộc Spring): khoảng cách haversine,
 * gom điểm thành k cụm cân bằng (mỗi cụm = một ngày) và tìm thứ tự đi ngắn nhất xuất phát từ một điểm.
 * Toạ độ luôn là {lat, lng}.
 */
public final class GeoPlanning {
    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final int KMEANS_ITERATIONS = 25;
    private static final int KMEANS_RESTARTS = 6;
    /** Tối đa số điểm thử mọi hoán vị (7! = 5040); nhiều hơn thì láng giềng gần nhất + 2-opt. */
    private static final int BRUTE_FORCE_MAX = 7;

    private GeoPlanning() {
    }

    public static double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.min(1, Math.sqrt(a)));
    }

    public static double haversineKm(double[] a, double[] b) {
        return haversineKm(a[0], a[1], b[0], b[1]);
    }

    /** Trung bình toạ độ (đủ chính xác trong phạm vi một thành phố/tỉnh). */
    public static double[] centroid(List<double[]> points) {
        double lat = 0;
        double lng = 0;
        for (double[] p : points) {
            lat += p[0];
            lng += p[1];
        }
        return new double[]{lat / points.size(), lng / points.size()};
    }

    /**
     * Chia n điểm thành k cụm gần nhau, mỗi cụm có từ floor(n/k) đến ceil(n/k) điểm (k ≤ n).
     * k-means++ (Random theo seed → cùng seed cùng kết quả) với bước gán có sức chứa; chạy vài lần, lấy lần có
     * tổng bình phương khoảng cách nhỏ nhất. Trả nhãn cụm 0..k-1 theo thứ tự điểm.
     */
    public static int[] balancedClusters(List<double[]> points, int k, long seed) {
        int n = points.size();
        if (k <= 0 || k > n) {
            throw new IllegalArgumentException("k phải trong khoảng 1.." + n);
        }
        double[][] xy = project(points);
        if (k == 1) {
            return new int[n];
        }
        Random random = new Random(seed);
        int[] best = null;
        double bestCost = Double.MAX_VALUE;
        for (int restart = 0; restart < KMEANS_RESTARTS; restart++) {
            double[][] centers = initCenters(xy, k, random);
            int[] labels = null;
            for (int iter = 0; iter < KMEANS_ITERATIONS; iter++) {
                int[] next = assignWithCapacity(xy, centers);
                centers = updateCenters(xy, next, k);
                if (labels != null && Arrays.equals(labels, next)) {
                    break;
                }
                labels = next;
            }
            double cost = 0;
            for (int i = 0; i < n; i++) {
                cost += dist2(xy[i], centers[labels[i]]);
            }
            if (cost < bestCost) {
                bestCost = cost;
                best = labels;
            }
        }
        return best;
    }

    /**
     * Thứ tự thăm các điểm sao cho tổng quãng đường start → p1 → … → pn (đường mở, không quay về) ngắn nhất.
     * start null → được chọn điểm bắt đầu tuỳ ý. ≤ 7 điểm: thử mọi hoán vị (tối ưu); nhiều hơn: láng giềng gần nhất + 2-opt.
     */
    public static int[] bestOpenPath(double[] start, List<double[]> points) {
        int n = points.size();
        if (n == 0) {
            return new int[0];
        }
        double[][] d = new double[n][n];
        double[] fromStart = new double[n];
        for (int i = 0; i < n; i++) {
            fromStart[i] = start == null ? 0 : haversineKm(start, points.get(i));
            for (int j = 0; j < n; j++) {
                d[i][j] = haversineKm(points.get(i), points.get(j));
            }
        }
        int[] order = new int[n];
        for (int i = 0; i < n; i++) {
            order[i] = i;
        }
        if (n <= BRUTE_FORCE_MAX) {
            int[] best = order.clone();
            double[] bestLength = {pathLength(order, fromStart, d)};
            permute(order, 0, fromStart, d, best, bestLength);
            return best;
        }
        int[] path = nearestNeighbour(fromStart, d);
        twoOpt(path, fromStart, d);
        return path;
    }

    // ------------------------------------------------------------------ k-means

    /** Chiếu phẳng (km) quanh vĩ độ trung bình — đủ chính xác cho bán kính vài trăm km. */
    private static double[][] project(List<double[]> points) {
        double meanLat = centroid(points)[0];
        double kx = 111.32 * Math.cos(Math.toRadians(meanLat));
        double[][] xy = new double[points.size()][];
        for (int i = 0; i < points.size(); i++) {
            xy[i] = new double[]{points.get(i)[1] * kx, points.get(i)[0] * 110.57};
        }
        return xy;
    }

    private static double[][] initCenters(double[][] xy, int k, Random random) {
        int n = xy.length;
        double[][] centers = new double[k][];
        centers[0] = xy[random.nextInt(n)].clone();
        double[] nearest = new double[n];
        for (int c = 1; c < k; c++) {
            double total = 0;
            for (int i = 0; i < n; i++) {
                double m = Double.MAX_VALUE;
                for (int j = 0; j < c; j++) {
                    m = Math.min(m, dist2(xy[i], centers[j]));
                }
                nearest[i] = m;
                total += m;
            }
            int chosen = random.nextInt(n);
            if (total > 0) {
                double r = random.nextDouble() * total;
                for (int i = 0; i < n; i++) {
                    r -= nearest[i];
                    if (r <= 0) {
                        chosen = i;
                        break;
                    }
                }
            }
            centers[c] = xy[chosen].clone();
        }
        return centers;
    }

    /**
     * Gán điểm cho tâm gần nhất nhưng mỗi cụm tối đa ceil(n/k) điểm: xét các cặp (điểm, tâm) theo khoảng cách tăng dần.
     * Sau đó cụm nào còn thiếu (dưới floor(n/k)) thì lấy điểm gần nó nhất từ cụm đang dư.
     */
    private static int[] assignWithCapacity(double[][] xy, double[][] centers) {
        int n = xy.length;
        int k = centers.length;
        int cap = (n + k - 1) / k;
        int min = n / k;
        List<double[]> pairs = new ArrayList<>(n * k);
        for (int i = 0; i < n; i++) {
            for (int c = 0; c < k; c++) {
                pairs.add(new double[]{dist2(xy[i], centers[c]), i, c});
            }
        }
        pairs.sort(Comparator.comparingDouble(p -> p[0]));
        int[] labels = new int[n];
        Arrays.fill(labels, -1);
        int[] count = new int[k];
        for (double[] p : pairs) {
            int i = (int) p[1];
            int c = (int) p[2];
            if (labels[i] < 0 && count[c] < cap) {
                labels[i] = c;
                count[c]++;
            }
        }
        for (int c = 0; c < k; c++) {
            while (count[c] < min) {
                int bestPoint = -1;
                double bestDist = Double.MAX_VALUE;
                for (int i = 0; i < n; i++) {
                    if (count[labels[i]] > min) {
                        double dd = dist2(xy[i], centers[c]);
                        if (dd < bestDist) {
                            bestDist = dd;
                            bestPoint = i;
                        }
                    }
                }
                if (bestPoint < 0) {
                    break;
                }
                count[labels[bestPoint]]--;
                labels[bestPoint] = c;
                count[c]++;
            }
        }
        return labels;
    }

    private static double[][] updateCenters(double[][] xy, int[] labels, int k) {
        double[][] sum = new double[k][2];
        int[] count = new int[k];
        for (int i = 0; i < xy.length; i++) {
            sum[labels[i]][0] += xy[i][0];
            sum[labels[i]][1] += xy[i][1];
            count[labels[i]]++;
        }
        double[][] centers = new double[k][];
        for (int c = 0; c < k; c++) {
            centers[c] = count[c] == 0 ? xy[c % xy.length].clone() : new double[]{sum[c][0] / count[c], sum[c][1] / count[c]};
        }
        return centers;
    }

    private static double dist2(double[] a, double[] b) {
        double dx = a[0] - b[0];
        double dy = a[1] - b[1];
        return dx * dx + dy * dy;
    }

    // ------------------------------------------------------------------ routing

    private static double pathLength(int[] order, double[] fromStart, double[][] d) {
        double length = fromStart[order[0]];
        for (int i = 1; i < order.length; i++) {
            length += d[order[i - 1]][order[i]];
        }
        return length;
    }

    private static void permute(int[] a, int k, double[] fromStart, double[][] d, int[] best, double[] bestLength) {
        if (k == a.length) {
            double length = pathLength(a, fromStart, d);
            if (length < bestLength[0] - 1e-9) {
                bestLength[0] = length;
                System.arraycopy(a, 0, best, 0, a.length);
            }
            return;
        }
        for (int i = k; i < a.length; i++) {
            swap(a, k, i);
            permute(a, k + 1, fromStart, d, best, bestLength);
            swap(a, k, i);
        }
    }

    private static int[] nearestNeighbour(double[] fromStart, double[][] d) {
        int n = fromStart.length;
        boolean[] used = new boolean[n];
        int[] path = new int[n];
        int current = 0;
        for (int i = 1; i < n; i++) {
            if (fromStart[i] < fromStart[current]) {
                current = i;
            }
        }
        path[0] = current;
        used[current] = true;
        for (int step = 1; step < n; step++) {
            int next = -1;
            for (int j = 0; j < n; j++) {
                if (!used[j] && (next < 0 || d[current][j] < d[current][next])) {
                    next = j;
                }
            }
            path[step] = next;
            used[next] = true;
            current = next;
        }
        return path;
    }

    /** 2-opt cho đường mở: đảo đoạn [i..j] nếu làm ngắn quãng đường, lặp tới khi không cải thiện. */
    private static void twoOpt(int[] path, double[] fromStart, double[][] d) {
        boolean improved = true;
        int guard = 0;
        while (improved && guard++ < 100) {
            improved = false;
            double current = pathLength(path, fromStart, d);
            for (int i = 0; i < path.length - 1; i++) {
                for (int j = i + 1; j < path.length; j++) {
                    reverse(path, i, j);
                    double candidate = pathLength(path, fromStart, d);
                    if (candidate < current - 1e-9) {
                        current = candidate;
                        improved = true;
                    } else {
                        reverse(path, i, j);
                    }
                }
            }
        }
    }

    private static void reverse(int[] a, int i, int j) {
        while (i < j) {
            swap(a, i++, j--);
        }
    }

    private static void swap(int[] a, int i, int j) {
        int t = a[i];
        a[i] = a[j];
        a[j] = t;
    }
}
