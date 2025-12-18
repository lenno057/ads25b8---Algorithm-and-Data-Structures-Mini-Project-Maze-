import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

public class Maze extends JPanel {

    // --- Konfigurasi Logika ---
    protected final int COLS = 40;
    protected final int ROWS = 30;

    // Variabel Rendering
    protected int cellSize;
    protected int startX, startY;
    protected boolean drawScan = true;

    // Callback untuk update statistik ke UI
    protected Consumer<String> statsCallback;

    // Kecepatan Animasi
    protected final int SOLVE_DELAY = 10;
    protected final int PATH_DELAY = 25;
    protected final int GEN_BATCH = 15;

    // Warna & Style
    protected final Color COLOR_BG = new Color(30, 30, 30);
    protected final Color COLOR_WALL = Color.WHITE;
    protected final Color COLOR_START = new Color(50, 205, 50);
    protected final Color COLOR_END = new Color(220, 20, 60);
    protected final Color COLOR_SOLUTION = new Color(255, 215, 0);
    protected final Color COLOR_SEARCH = new Color(0, 255, 255, 120);

    protected final Stroke STROKE_WALL = new BasicStroke(3);
    protected final Stroke STROKE_PATH = new BasicStroke(6);

    // --- Struktur Data ---
    protected Cell[][] grid;
    protected Cell startCell, endCell;
    protected List<Cell> finalPath;

    protected boolean isGenerating = false;
    protected boolean isSolving = false;

    public Maze() {
        setBackground(COLOR_BG);
        setupGrid();
    }

    public void setStatsCallback(Consumer<String> callback) {
        this.statsCallback = callback;
    }

    protected static class Cell {
        int r, c;
        boolean[] walls = {true, true, true, true};
        boolean visited = false;
        boolean searchVisited = false;
        Cell parent = null;

        public Cell(int r, int c) {
            this.r = r;
            this.c = c;
        }
    }

    protected void setupGrid() {
        grid = new Cell[ROWS][COLS];
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                grid[r][c] = new Cell(r, c);
            }
        }
        startCell = grid[0][0];
        endCell = grid[ROWS - 1][COLS - 1];
        finalPath = new ArrayList<>();
        repaint();
    }

    public void generatePrim() {
        if (isGenerating || isSolving) return;
        setupGrid();
        isGenerating = true;
        if (statsCallback != null) statsCallback.accept("Generating Maze...");

        new Thread(() -> {
            ArrayList<Cell> frontier = new ArrayList<>();
            Random rand = new Random();
            startCell.visited = true;
            addFrontier(startCell, frontier);

            int loop = 0;
            while (!frontier.isEmpty()) {
                Cell current = frontier.remove(rand.nextInt(frontier.size()));
                List<Cell> neighbors = getNeighbors(current);
                if (!neighbors.isEmpty()) {
                    Cell neighbor = neighbors.get(rand.nextInt(neighbors.size()));
                    removeWalls(current, neighbor);
                    current.visited = true;
                    addFrontier(current, frontier);
                    if(++loop % GEN_BATCH == 0) visualize(1);
                }
            }
            grid[0][0].walls[3] = false;
            grid[ROWS-1][COLS-1].walls[1] = false;
            isGenerating = false;
            if (statsCallback != null) statsCallback.accept("Maze Generated.\nReady to solve.");
            repaint();
        }).start();
    }

    public void solve(boolean useBFS) {
        if (isGenerating || isSolving) return;
        resetSolver();
        isSolving = true;
        String algoName = useBFS ? "BFS" : "DFS";
        if (statsCallback != null) statsCallback.accept("Running " + algoName + "...");

        new Thread(() -> {
            LinkedList<Cell> list = new LinkedList<>();
            list.add(startCell);
            startCell.searchVisited = true;
            boolean found = false;
            int nodesVisited = 0;

            while (!list.isEmpty()) {
                Cell current = useBFS ? list.poll() : list.removeLast();
                nodesVisited++;

                if (current == endCell) {
                    found = true;
                    break;
                }

                for (Cell next : getConnectedNeighbors(current)) {
                    if (!next.searchVisited) {
                        next.searchVisited = true;
                        next.parent = current;
                        list.add(next);
                    }
                }
                visualize(SOLVE_DELAY);
            }

            if (found) {
                reconstructPath(endCell);
                // Update Statistik ke Panel Samping (Bukan Popup)
                String result = String.format("""
                    Algorithm: %s
                    ----------------
                    Status: Finished
                    Nodes Visited: %d
                    Path Found: Yes
                    (Unweighted)
                    """, algoName, nodesVisited);

                if (statsCallback != null) statsCallback.accept(result);
            } else {
                if (statsCallback != null) statsCallback.accept(algoName + " Failed to find path.");
            }
            isSolving = false;
            repaint();
        }).start();
    }

    protected void addFrontier(Cell cell, ArrayList<Cell> frontier) {
        int[][] dirs = {{-1,0}, {1,0}, {0,-1}, {0,1}};
        for(int[] d : dirs) {
            int nr = cell.r + d[0], nc = cell.c + d[1];
            if(isValid(nr, nc) && !grid[nr][nc].visited && !frontier.contains(grid[nr][nc]))
                frontier.add(grid[nr][nc]);
        }
    }

    protected void removeWalls(Cell a, Cell b) {
        int dr = a.r - b.r, dc = a.c - b.c;
        if (dr == 1) { a.walls[0] = false; b.walls[2] = false; }
        if (dr == -1){ a.walls[2] = false; b.walls[0] = false; }
        if (dc == 1) { a.walls[3] = false; b.walls[1] = false; }
        if (dc == -1){ a.walls[1] = false; b.walls[3] = false; }
    }

    protected List<Cell> getNeighbors(Cell c) {
        List<Cell> list = new ArrayList<>();
        int[][] dirs = {{-1,0}, {1,0}, {0,-1}, {0,1}};
        for(int[] d : dirs) {
            int nr = c.r+d[0], nc = c.c+d[1];
            if(isValid(nr, nc) && grid[nr][nc].visited) list.add(grid[nr][nc]);
        }
        return list;
    }

    protected List<Cell> getConnectedNeighbors(Cell c) {
        List<Cell> list = new ArrayList<>();
        if (!c.walls[0] && isValid(c.r-1, c.c)) list.add(grid[c.r-1][c.c]);
        if (!c.walls[1] && isValid(c.r, c.c+1)) list.add(grid[c.r][c.c+1]);
        if (!c.walls[2] && isValid(c.r+1, c.c)) list.add(grid[c.r+1][c.c]);
        if (!c.walls[3] && isValid(c.r, c.c-1)) list.add(grid[c.r][c.c-1]);
        return list;
    }

    protected boolean isValid(int r, int c) { return r >= 0 && r < ROWS && c >= 0 && c < COLS; }

    protected void resetSolver() {
        finalPath.clear();
        for(int r=0; r<ROWS; r++) {
            for(int c=0; c<COLS; c++) {
                grid[r][c].searchVisited = false;
                grid[r][c].parent = null;
            }
        }
        repaint();
    }

    protected void reconstructPath(Cell end) {
        Cell temp = end;
        while (temp != null) {
            finalPath.add(temp);
            temp = temp.parent;
            visualize(PATH_DELAY);
        }
        Collections.reverse(finalPath);
    }

    protected void visualize(int delay) {
        try { SwingUtilities.invokeLater(this::repaint); if(delay > 0) Thread.sleep(delay); } catch(Exception _){}
    }

    protected void calculateDimensions() {
        int panelW = getWidth();
        int panelH = getHeight();
        int cellW = panelW / COLS;
        int cellH = panelH / ROWS;
        cellSize = Math.max(1, Math.min(cellW, cellH));
        startX = (panelW - (COLS * cellSize)) / 2;
        startY = (panelH - (ROWS * cellSize)) / 2;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        calculateDimensions();
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        drawMazeElements(g2);
    }

    protected void drawMazeElements(Graphics2D g2) {
        g2.setColor(COLOR_WALL);
        g2.setStroke(STROKE_WALL);

        for(int r=0; r<ROWS; r++) {
            for(int c=0; c<COLS; c++) {
                int x = startX + c * cellSize;
                int y = startY + r * cellSize;

                if(grid[r][c].walls[0]) g2.drawLine(x, y, x+cellSize, y);
                if(grid[r][c].walls[1]) g2.drawLine(x+cellSize, y, x+cellSize, y+cellSize);
                if(grid[r][c].walls[2]) g2.drawLine(x+cellSize, y+cellSize, x, y+cellSize);
                if(grid[r][c].walls[3]) g2.drawLine(x, y+cellSize, x, y);

                if(grid[r][c].searchVisited && drawScan) {
                    g2.setColor(COLOR_SEARCH);
                    g2.fillRect(x+2, y+2, cellSize-4, cellSize-4);
                    g2.setColor(COLOR_WALL);
                }
            }
        }

        if (startCell != null) {
            g2.setColor(COLOR_START);
            g2.fillRect(startX + startCell.c * cellSize + 5, startY + startCell.r * cellSize + 5, cellSize - 10, cellSize - 10);
        }
        if (endCell != null) {
            g2.setColor(COLOR_END);
            g2.fillRect(startX + endCell.c * cellSize + 5, startY + endCell.r * cellSize + 5, cellSize - 10, cellSize - 10);
        }

        if (!finalPath.isEmpty()) {
            g2.setColor(COLOR_SOLUTION);
            g2.setStroke(STROKE_PATH);
            for (int i = 0; i < finalPath.size() - 1; i++) {
                Cell a = finalPath.get(i);
                Cell b = finalPath.get(i + 1);
                g2.drawLine(startX + a.c*cellSize + cellSize/2, startY + a.r*cellSize + cellSize/2,
                        startX + b.c*cellSize + cellSize/2, startY + b.r*cellSize + cellSize/2);
            }
        }
    }
}