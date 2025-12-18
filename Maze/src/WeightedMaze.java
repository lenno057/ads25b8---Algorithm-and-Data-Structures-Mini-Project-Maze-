import java.awt.*;
import java.util.*;

public class WeightedMaze extends Maze {

    private static final int COST_GRASS = 1;
    private static final int COST_MUD = 5;
    private static final int COST_WATER = 10;

    private final Color C_GRASS = new Color(0, 100, 0);
    private final Color C_MUD = new Color(139, 69, 19);
    private final Color C_WATER = new Color(0, 0, 205);

    private final int[][] terrainGrid;
    private boolean useTerrainMode = false;

    public WeightedMaze() {
        super();
        terrainGrid = new int[ROWS][COLS];
    }

    @Override
    public void generatePrim() {
        useTerrainMode = false;
        super.generatePrim();
    }

    public void generateWeightedTerrain() {
        if (isGenerating || isSolving) return;
        useTerrainMode = true;

        // 1. Generate Struktur Dasar (Perfect Maze)
        super.generatePrim();

        new Thread(() -> {
            try { Thread.sleep(200); } catch(Exception _){} // Tunggu Prim selesai visualisasi kasar

            Random rand = new Random();

            // 2. Assign Weights (Terrain)
            for (int r = 0; r < ROWS; r++) {
                for (int c = 0; c < COLS; c++) {
                    double p = rand.nextDouble();
                    if (p < 0.60) terrainGrid[r][c] = COST_GRASS;
                    else if (p < 0.85) terrainGrid[r][c] = COST_MUD;
                    else terrainGrid[r][c] = COST_WATER;
                }
            }
            terrainGrid[0][0] = COST_GRASS;
            terrainGrid[ROWS-1][COLS-1] = COST_GRASS;

            // 3. [PENTING] Tambahkan Loops agar ada banyak jalur alternatif!
            // Kita acak menghapus dinding tambahan sebanyak 10% dari total sel
            addLoops(rand);

            if (statsCallback != null) statsCallback.accept("Terrain Generated with Loops.\nMultiple paths available for comparison.");
            repaint();
        }).start();
    }

    // Method Baru: Menghapus dinding secara acak untuk membuat jalur alternatif
    private void addLoops(Random rand) {
        int removed = 0;
        while (removed < 120) {
            int r = rand.nextInt(ROWS - 2) + 1; // Hindari pinggir
            int c = rand.nextInt(COLS - 2) + 1;

            Cell cell = grid[r][c];
            // Pilih dinding acak untuk dihapus (0=Top, 1=Right, 2=Bottom, 3=Left)
            int wallIdx = rand.nextInt(4);

            // Cek apakah dinding itu masih ada
            if (cell.walls[wallIdx]) {
                // Tentukan tetangga di seberang dinding
                int nr = r, nc = c;
                if (wallIdx == 0) nr--;
                else if (wallIdx == 1) nc++;
                else if (wallIdx == 2) nr++;
                else {
                    nc--;
                }

                // Pastikan koordinat tetangga valid
                if (isValid(nr, nc)) {
                    Cell neighbor = grid[nr][nc];
                    // Hapus dinding di kedua sisi (cell & neighbor)
                    removeWalls(cell, neighbor);
                    removed++;
                }
            }
        }
    }

    public void solveWeighted(boolean useAStar) {
        if (isGenerating || isSolving) return;

        isSolving = true;
        resetSolver();
        String algoName = useAStar ? "A* (A-Star)" : "Dijkstra";
        if (statsCallback != null) statsCallback.accept("Running " + algoName + "...");

        new Thread(() -> {
            Map<Cell, Integer> dist = new HashMap<>();
            Map<Cell, Cell> parent = new HashMap<>();
            PriorityQueue<double[]> pq = new PriorityQueue<>(Comparator.comparingDouble(a -> a[0]));

            for(int r=0; r<ROWS; r++)
                for(int c=0; c<COLS; c++) dist.put(grid[r][c], Integer.MAX_VALUE);

            dist.put(startCell, 0);
            pq.add(new double[]{0, startCell.r, startCell.c});

            boolean found = false;
            int visitedNodesCount = 0;

            while (!pq.isEmpty()) {
                double[] currData = pq.poll();
                Cell current = grid[(int)currData[1]][(int)currData[2]];

                if (current == endCell) {
                    found = true;
                    break;
                }

                // Optimization: Skip jika kita sudah menemukan rute lebih baik ke node ini
                if (useAStar) {
                    dist.get(current);
                    heuristic(current, endCell);
                } else {
                    dist.get(current);
                }// continue;

                if (!current.searchVisited) {
                    current.searchVisited = true;
                    visitedNodesCount++;
                    if (visitedNodesCount % 5 == 0) visualize(1);
                }

                for (Cell neighbor : getConnectedNeighbors(current)) {
                    int cost = useTerrainMode ? terrainGrid[neighbor.r][neighbor.c] : 1;
                    int newDist = dist.get(current) + cost;

                    if (newDist < dist.get(neighbor)) {
                        dist.put(neighbor, newDist);
                        parent.put(neighbor, current);

                        double priority = newDist;
                        if(useAStar) priority += heuristic(neighbor, endCell);

                        pq.add(new double[]{priority, neighbor.r, neighbor.c});
                    }
                }
            }

            if (found) {
                reconstructPath(parent.get(endCell), parent);

                int finalCost = dist.get(endCell);

                String result = String.format("""
                    Algorithm: %s
                    ----------------
                    Status: Finished
                    Total Cost: %d
                    Nodes Visited: %d
                    Efficiency: %.2f%%
                    (Map has Loops)
                    """, algoName, finalCost, visitedNodesCount, ((double) visitedNodesCount /(ROWS*COLS))*100);

                if (statsCallback != null) statsCallback.accept(result);
            }
            isSolving = false;
            repaint();
        }).start();
    }

    private double heuristic(Cell a, Cell b) {
        return (Math.abs(a.r - b.r) + Math.abs(a.c - b.c));
    }

    private void reconstructPath(Cell curr, Map<Cell, Cell> parent) {
        while (curr != null) {
            finalPath.add(curr);
            curr = parent.get(curr);
            visualize(15);
        }
        Collections.reverse(finalPath);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.calculateDimensions();

        if (!useTerrainMode) {
            this.drawScan = true;
            super.paintComponent(g);
            return;
        }

        this.drawScan = false;

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setFont(new Font("SansSerif", Font.BOLD, Math.max(10, cellSize / 2)));
        FontMetrics fm = g2.getFontMetrics();

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                int w = terrainGrid[r][c];

                if (w == COST_MUD) g2.setColor(C_MUD);
                else if (w == COST_WATER) g2.setColor(C_WATER);
                else g2.setColor(C_GRASS);
                g2.fillRect(startX + c * cellSize, startY + r * cellSize, cellSize, cellSize);

                // Gambar Angka Bobot
                if (cellSize > 15) {
                    g2.setColor(new Color(255, 255, 255, 180));
                    String text = String.valueOf(w);
                    int textX = startX + c * cellSize + (cellSize - fm.stringWidth(text)) / 2;
                    int textY = startY + r * cellSize + ((cellSize - fm.getHeight()) / 2) + fm.getAscent();
                    g2.drawString(text, textX, textY);
                }

                if (grid[r][c].searchVisited) {
                    g2.setColor(new Color(255, 255, 255, 100));
                    g2.fillRect(startX + c * cellSize, startY + r * cellSize, cellSize, cellSize);
                }
            }
        }

        super.drawMazeElements(g2);
    }
}