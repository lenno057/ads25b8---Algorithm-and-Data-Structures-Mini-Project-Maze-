import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class MazeApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("All-in-One Maze Solver");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            frame.setResizable(true);

            WeightedMaze mazePanel = new WeightedMaze();

            // --- Panel Statistik (Kanan) ---
            JPanel statsPanel = new JPanel(new BorderLayout());
            statsPanel.setPreferredSize(new Dimension(250, 0));
            statsPanel.setBackground(new Color(35, 35, 35));
            statsPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

            JLabel lblTitle = new JLabel("Statistics");
            lblTitle.setFont(new Font("SansSerif", Font.BOLD, 22));
            lblTitle.setForeground(Color.WHITE);
            lblTitle.setBorder(new EmptyBorder(0, 0, 15, 0));

            JTextArea statsText = new JTextArea();
            statsText.setEditable(false);
            statsText.setFocusable(false);
            statsText.setFont(new Font("Monospaced", Font.PLAIN, 14));
            statsText.setBackground(new Color(45, 45, 45));
            statsText.setForeground(new Color(220, 220, 220));
            statsText.setBorder(new EmptyBorder(10, 10, 10, 10));
            statsText.setText("Ready.\nSelect generator...");

            statsPanel.add(lblTitle, BorderLayout.NORTH);
            statsPanel.add(statsText, BorderLayout.CENTER);

            // Sambungkan Callback dari Maze ke Text Area ini
            mazePanel.setStatsCallback(text -> SwingUtilities.invokeLater(() -> statsText.setText(text)));

            // --- Panel Kontrol (Bawah) ---
            JPanel controlPanel = new JPanel(new GridLayout(2, 1, 5, 5));
            controlPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            controlPanel.setBackground(new Color(45, 45, 45));

            JPanel genPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
            genPanel.setOpaque(false);
            JButton btnStandard = createButton("1. Standard Maze", new Color(80, 80, 80));
            JButton btnTerrain = createButton("2. Terrain Map (Weighted)", new Color(34, 139, 34));
            genPanel.add(btnStandard);
            genPanel.add(btnTerrain);

            JPanel solvePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
            solvePanel.setOpaque(false);
            JButton btnBFS = createButton("BFS", new Color(0, 100, 200));
            JButton btnDFS = createButton("DFS", new Color(128, 0, 128));
            JButton btnDijkstra = createButton("Dijkstra", new Color(200, 60, 0));
            JButton btnAStar = createButton("A*", new Color(210, 180, 0));
            btnAStar.setForeground(Color.BLACK);

            solvePanel.add(btnBFS);
            solvePanel.add(btnDFS);
            solvePanel.add(btnDijkstra);
            solvePanel.add(btnAStar);

            controlPanel.add(genPanel);
            controlPanel.add(solvePanel);

            // Listeners
            btnStandard.addActionListener(_ -> mazePanel.generatePrim());
            btnTerrain.addActionListener(_ -> mazePanel.generateWeightedTerrain());
            btnBFS.addActionListener(_ -> mazePanel.solve(true));
            btnDFS.addActionListener(_ -> mazePanel.solve(false));
            btnDijkstra.addActionListener(_ -> mazePanel.solveWeighted(false));
            btnAStar.addActionListener(_ -> mazePanel.solveWeighted(true));

            // Layout Utama
            frame.setLayout(new BorderLayout());
            frame.add(mazePanel, BorderLayout.CENTER);
            frame.add(statsPanel, BorderLayout.EAST); // Panel stats di kanan
            frame.add(controlPanel, BorderLayout.SOUTH);

            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    private static JButton createButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 14));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setOpaque(true);
        btn.setContentAreaFilled(true);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(200, 45));
        return btn;
    }
}