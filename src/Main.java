import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

//  OCEAN STRIKE — Complete Naval Battle Game
//  Data Structures Used:
//    1. Stack       → Undo ship placement
//    2. Queue       → Turn management (FIFO)
//    3. LinkedList  → Attack history log
//    4. ArrayList   → Store ship positions
//    5. HashMap     → Coordinate-to-status lookup
//    6. HashSet     → Prevent duplicate attacks
//    7. PriorityQueue → Smart AI targeting
//    8. 2D Array    → Ocean grid boards
public class Main extends JFrame {

    //   CONSTANTS
    private static final int GRID        = 10;
    private static final int MAX_TURNS   = 20;
    private static final int SHIPS_EACH  = 5;
    private static final int HIT_DAMAGE  = 10;
    private static final int START_HP    = 50;

    //  PLAYER DATA
    private String playerName = "Commander";
    private int    humanHP    = START_HP;
    private int    compHP     = START_HP;
    private int    humanHits  = 0;
    private int    compHits   = 0;
    private int    turnCount  = 0;

    //  OCEAN GRIDS (2D Arrays)
    private char[][] humanOcean    = new char[GRID][GRID];
    private char[][] computerOcean = new char[GRID][GRID];

    // DATA STRUCTURES

    // 1. STACK — undo last ship placement during setup
    private Stack<int[]> placementStack = new Stack<>();

    // 2. QUEUE — whose turn is it? (FIFO rotation)
    private Queue<String> turnQueue = new LinkedList<>();

    // 3. LINKED LIST — running attack history log
    private LinkedList<String> attackHistory = new LinkedList<>();

    // 4. ARRAY LIST — human ship coordinates list
    private ArrayList<int[]> humanShipList = new ArrayList<>();

    // 5. HASH MAP — fast coordinate → hit/miss status lookup
    private HashMap<String, String> coordStatusMap = new HashMap<>();

    // 6. HASH SET — prevent duplicate attacks (both sides)
    private HashSet<String> humanAttacked  = new HashSet<>();
    private HashSet<String> computerAttacked = new HashSet<>();

    // 7. PRIORITY QUEUE — AI smart targeting after a hit
    //    Higher value = higher priority target
    private PriorityQueue<int[]> aiSmartTargets = new PriorityQueue<>(
            (a, b) -> b[2] - a[2]
    );

    //  GAME STATE
    private boolean placementPhase = true;
    private int     placedCount    = 0;
    private boolean gameOver       = false;

    // MAZE STATE
    private int playerRow = 1, playerCol = 1;
    private static final int EXIT_ROW = 8, EXIT_COL = 8;
    private static final int[][] MAZE = {
            {1,1,1,1,1,1,1,1,1,1},
            {1,0,0,1,0,0,0,0,0,1},
            {1,1,0,1,0,1,1,1,0,1},
            {1,0,0,0,0,1,0,0,0,1},
            {1,0,1,1,1,1,0,1,1,1},
            {1,0,0,0,0,0,0,1,0,1},
            {1,1,1,1,0,1,1,1,0,1},
            {1,0,0,1,0,0,0,1,0,1},
            {1,0,1,1,1,1,0,0,0,1},
            {1,1,1,1,1,1,1,1,1,1}
    };
    private JButton[][] mazeBtns = new JButton[10][10];
    private JFrame mazeFrame;

    //  GUI COMPONENTS
    private JButton[][] humanBtns    = new JButton[GRID][GRID];
    private JButton[][] computerBtns = new JButton[GRID][GRID];
    private JLabel  lblTurn, lblStats, lblStatus;
    private JTextArea historyArea;
    private JButton btnUndo;

    //  ENTRY POINT
    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::new);
    }

    public Main() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        runIntroSequence();
    }

    //  STEP 1 — INTRO: Name → Door → Maze → Riddle → Rules
    private void runIntroSequence() {

        // Get Commander name
        String input = JOptionPane.showInputDialog(null,
                "🚪  A massive rusted iron door rises from the ocean fog...\n\n" +
                        "   Engraving reads:  'Enter your Commander name to awaken the fleet:'\n",
                "THE IRON GATEWAY", JOptionPane.QUESTION_MESSAGE);

        if (input != null && !input.trim().isEmpty())
            playerName = input.trim();

        //  Confirm entry
        int choice = JOptionPane.showConfirmDialog(null,
                "🚪  Commander  « " + playerName + " »  —\n\n" +
                        "   A voice echoes from the depths:\n" +
                        "   'Do you dare open this door and face the labyrinth within?'\n",
                "THE CHOSEN PATH", JOptionPane.YES_NO_OPTION);

        if (choice != JOptionPane.YES_OPTION) System.exit(0);

        //  Explain maze
        JOptionPane.showMessageDialog(null,
                "🏃  You push the door open!\n\n" +
                        "   The floor collapses beneath you...\n" +
                        "   You fall into a dark stone LABYRINTH!\n\n" +
                        "   Navigate  P  (you) to  E  (exit) using the arrow buttons.\n" +
                        "   Grey blocks  █  are walls — you cannot pass through them.\n",
                "INTO THE LABYRINTH", JOptionPane.WARNING_MESSAGE);

        openMaze();
    }

    //  STEP 2 — MAZE
    private void openMaze() {
        mazeFrame = new JFrame("⚔  NAVIGATE THE LABYRINTH  ⚔");
        mazeFrame.setSize(520, 580);
        mazeFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mazeFrame.setLocationRelativeTo(null);
        mazeFrame.setLayout(new BorderLayout(6, 6));
        mazeFrame.getContentPane().setBackground(Color.BLACK);

        // Grid
        JPanel grid = new JPanel(new GridLayout(10, 10, 2, 2));
        grid.setBackground(Color.BLACK);
        grid.setBorder(new EmptyBorder(10, 10, 4, 10));
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                mazeBtns[i][j] = new JButton();
                mazeBtns[i][j].setFont(new Font("Segoe UI Emoji", Font.BOLD, 14));
                mazeBtns[i][j].setFocusPainted(false);
                mazeBtns[i][j].setMargin(new Insets(0,0,0,0));
                grid.add(mazeBtns[i][j]);
            }
        }
        renderMaze();

        // D-pad
        JPanel dpad = new JPanel(new GridLayout(3, 3, 4, 4));
        dpad.setBackground(Color.BLACK);
        dpad.setBorder(new EmptyBorder(4, 140, 10, 140));
        JButton up    = mazeBtn("▲");
        JButton down  = mazeBtn("▼");
        JButton left  = mazeBtn("◀");
        JButton right = mazeBtn("▶");
        up.addActionListener(e    -> movePlayer(-1,  0));
        down.addActionListener(e  -> movePlayer( 1,  0));
        left.addActionListener(e  -> movePlayer( 0, -1));
        right.addActionListener(e -> movePlayer( 0,  1));

        dpad.add(new JLabel()); dpad.add(up);    dpad.add(new JLabel());
        dpad.add(left);         dpad.add(new JLabel()); dpad.add(right);
        dpad.add(new JLabel()); dpad.add(down);  dpad.add(new JLabel());

        JLabel hint = new JLabel("Move  P  to reach  E  — use the buttons above", SwingConstants.CENTER);
        hint.setForeground(new Color(150, 150, 150));
        hint.setFont(new Font("Arial", Font.ITALIC, 12));

        mazeFrame.add(grid,  BorderLayout.CENTER);
        mazeFrame.add(dpad,  BorderLayout.SOUTH);
        mazeFrame.add(hint,  BorderLayout.NORTH);
        mazeFrame.setVisible(true);
    }

    private JButton mazeBtn(String txt) {
        JButton b = new JButton(txt);
        b.setBackground(new Color(40, 40, 60));
        b.setForeground(Color.WHITE);
        b.setFont(new Font("Arial", Font.BOLD, 16));
        b.setFocusPainted(false);
        return b;
    }

    private void renderMaze() {
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                JButton b = mazeBtns[i][j];
                if (i == playerRow && j == playerCol) {
                    b.setText("P"); b.setBackground(new Color(0, 200, 220));
                    b.setForeground(Color.BLACK);
                } else if (i == EXIT_ROW && j == EXIT_COL) {
                    b.setText("E"); b.setBackground(new Color(180, 0, 220));
                    b.setForeground(Color.WHITE);
                } else if (MAZE[i][j] == 1) {
                    b.setText("█"); b.setBackground(new Color(50, 50, 60));
                    b.setForeground(new Color(80, 80, 90));
                } else {
                    b.setText("·"); b.setBackground(new Color(15, 15, 25));
                    b.setForeground(new Color(60, 80, 100));
                }
            }
        }
    }

    private void movePlayer(int dr, int dc) {
        int nr = playerRow + dr, nc = playerCol + dc;
        if (nr >= 0 && nr < 10 && nc >= 0 && nc < 10 && MAZE[nr][nc] == 0) {
            playerRow = nr; playerCol = nc;
            renderMaze();
            if (playerRow == EXIT_ROW && playerCol == EXIT_COL) {
                mazeFrame.dispose();
                showRiddle();
            }
        }
    }

    //  STEP 3 — RIDDLE
    private void showRiddle() {
        String ans = JOptionPane.showInputDialog(null,
                "🧩  Labyrinth cleared!  The exit door has a lock...\n\n" +
                        "   Riddle:\n" +
                        "   « I have cities, but no houses live in me.\n" +
                        "     I have mountains, but no trees grow on me.\n" +
                        "     I have oceans, but no fish swim in me.\n" +
                        "     What am I? »\n\n" +
                        "   Type your answer:\n",
                "SECURITY PASSCODE", JOptionPane.QUESTION_MESSAGE);

        if (ans != null && ans.trim().equalsIgnoreCase("map")) {
            JOptionPane.showMessageDialog(null,
                    "✅  'ACCESS GRANTED.'  Security locks disengage.\n\n" +
                            "   The ocean vault opens before you, Commander " + playerName + "!\n",
                    "CORRECT!", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(null,
                    "❌  'ACCESS DENIED.'  But you kick the gate open by force!\n\n" +
                            "  \n",
                    "WRONG!", JOptionPane.WARNING_MESSAGE);
        }
        showRules();
    }

    //  STEP 4 — RULES PAGE
    private void showRules() {
        JFrame rules = new JFrame("📋  BATTLE BRIEFING");
        rules.setSize(560, 500);
        rules.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        rules.setLocationRelativeTo(null);
        rules.setLayout(new BorderLayout(8, 8));
        rules.getContentPane().setBackground(Color.BLACK);

        JTextArea txt = new JTextArea();
        txt.setEditable(false);
        txt.setBackground(Color.BLACK);
        txt.setForeground(new Color(255, 80, 80));
        txt.setFont(new Font("Monospaced", Font.BOLD, 13));
        txt.setMargin(new Insets(14, 18, 14, 18));
        txt.setText(
                "╔══════════════════════════════════════════════════╗\n" +
                        "║          OCEAN STRIKE — BATTLE BRIEFING          ║\n" +
                        "╠══════════════════════════════════════════════════╣\n" +
                        "║                                                  ║\n" +
                        "║  SETUP PHASE:                                    ║\n" +
                        "║   • Click " + SHIPS_EACH + " cells on YOUR grid (left) to place  ║\n" +
                        "║     your fleet.  Use UNDO button to take back.   ║\n" +
                        "║                                                  ║\n" +
                        "║  BATTLE PHASE:                                   ║\n" +
                        "║   • Click cells on ENEMY grid (right) to fire.  ║\n" +
                        "║   • Each hit deals " + HIT_DAMAGE + " damage.                   ║\n" +
                        "║   • You start with " + START_HP + " HP.  Sink all ships!        ║\n" +
                        "║   • Cannot fire at the same spot twice.          ║\n" +
                        "║                                                  ║\n" +
                        "║  SYMBOLS:                                        ║\n" +
                        "║   🛡  = Your ship    💥 = Hit    X = Miss       ║\n" +
                        "║                                                  ║\n" +
                        "║  WIN CONDITIONS:                                 ║\n" +
                        "║   • Reduce enemy HP to 0, OR                    ║\n" +
                        "║   • Have more hits after " + MAX_TURNS + " total turns.         ║\n" +
                        "║                                                  ║\n" +
                        "║  DATA STRUCTURES USED:                           ║\n" +
                        "║   Stack · Queue · LinkedList · ArrayList         ║\n" +
                        "║   HashMap · HashSet · PriorityQueue · 2D Array   ║\n" +
                        "╚══════════════════════════════════════════════════╝\n"
        );

        JButton startBtn = new JButton("  🚨  ENGAGE BATTLE SYSTEMS  🚨  ");
        startBtn.setFont(new Font("Arial", Font.BOLD, 15));
        startBtn.setBackground(new Color(30, 30, 30));
        startBtn.setForeground(new Color(255, 80, 80));
        startBtn.setFocusPainted(false);
        startBtn.setBorder(new EmptyBorder(12, 20, 12, 20));
        startBtn.addActionListener(e -> { rules.dispose(); initGame(); });

        rules.add(new JScrollPane(txt), BorderLayout.CENTER);
        rules.add(startBtn, BorderLayout.SOUTH);
        rules.setVisible(true);
    }

    //  STEP 5 — INIT GAME DATA

    private void initGame() {

        // Reset all data structures
        placementStack.clear();
        turnQueue.clear();
        attackHistory.clear();
        humanShipList.clear();
        coordStatusMap.clear();
        humanAttacked.clear();
        computerAttacked.clear();
        aiSmartTargets.clear();

        humanHP = START_HP; compHP = START_HP;
        humanHits = 0; compHits = 0;
        turnCount = 0; placedCount = 0;
        placementPhase = true; gameOver = false;

        // QUEUE — add both players in turn order
        turnQueue.add(playerName);
        turnQueue.add("Enemy AI");

        // Init 2D arrays
        for (int i = 0; i < GRID; i++)
            for (int j = 0; j < GRID; j++) {
                humanOcean[i][j]    = '.';
                computerOcean[i][j] = '.';
            }

        // Place computer ships randomly
        Random rand = new Random();
        int placed = 0;
        while (placed < SHIPS_EACH) {
            int r = rand.nextInt(GRID), c = rand.nextInt(GRID);
            if (computerOcean[r][c] != 'B') {
                computerOcean[r][c] = 'B';
                // HASH MAP — store computer ship locations
                coordStatusMap.put("comp_" + r + "," + c, "SHIP");
                placed++;
            }
        }

        buildGameGUI();
    }

    //  STEP 6 — BUILD GAME GUI
    private void buildGameGUI() {
        getContentPane().removeAll();
        setTitle("⚓  OCEAN STRIKE  —  Commander: " + playerName);
        setSize(1200, 720);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));
        getContentPane().setBackground(new Color(8, 18, 35));

        // TOP BAR
        JPanel top = new JPanel(new GridLayout(2, 1));
        top.setBackground(new Color(8, 18, 35));
        top.setBorder(new EmptyBorder(8, 12, 4, 12));

        lblTurn = new JLabel("PHASE 1:  Place " + SHIPS_EACH + " ships on YOUR grid (left)", SwingConstants.CENTER);
        lblTurn.setFont(new Font("Impact", Font.PLAIN, 26));
        lblTurn.setForeground(new Color(255, 165, 0));

        lblStats = new JLabel(getStatsText(), SwingConstants.CENTER);
        lblStats.setFont(new Font("Arial", Font.BOLD, 13));
        lblStats.setForeground(new Color(180, 200, 220));

        top.add(lblTurn);
        top.add(lblStats);
        add(top, BorderLayout.NORTH);

        //  CENTER: two grids
        JPanel center = new JPanel(new GridLayout(1, 2, 16, 0));
        center.setBackground(new Color(8, 18, 35));
        center.setBorder(new EmptyBorder(4, 12, 4, 12));

        center.add(buildGridPanel("⚓  YOUR OCEAN  (click to place ships)", humanBtns,    false));
        center.add(buildGridPanel("🚨  ENEMY WATERS  (click to fire)",      computerBtns, true ));
        add(center, BorderLayout.CENTER);

        //  BOTTOM BAR
        JPanel bottom = new JPanel(new BorderLayout(8, 0));
        bottom.setBackground(new Color(8, 18, 35));
        bottom.setBorder(new EmptyBorder(4, 12, 8, 12));

        // Status label
        lblStatus = new JLabel("  Setup: Click YOUR grid to place " + SHIPS_EACH + " ships.", SwingConstants.LEFT);
        lblStatus.setOpaque(true);
        lblStatus.setBackground(new Color(20, 30, 50));
        lblStatus.setForeground(Color.WHITE);
        lblStatus.setFont(new Font("Arial", Font.PLAIN, 13));
        lblStatus.setBorder(new EmptyBorder(6, 10, 6, 10));

        // Undo button (STACK)
        btnUndo = new JButton("↩  UNDO Last Placement  (Stack)");
        btnUndo.setFont(new Font("Arial", Font.BOLD, 12));
        btnUndo.setBackground(new Color(60, 30, 10));
        btnUndo.setForeground(new Color(255, 180, 80));
        btnUndo.setFocusPainted(false);
        btnUndo.setBorder(new EmptyBorder(8, 14, 8, 14));
        btnUndo.addActionListener(e -> undoLastPlacement());

        bottom.add(lblStatus, BorderLayout.CENTER);
        bottom.add(btnUndo,   BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        // ── EAST: Attack History (LinkedList) ─────────────
        JPanel east = new JPanel(new BorderLayout(0, 4));
        east.setBackground(new Color(12, 22, 40));
        east.setBorder(new CompoundBorder(
                new MatteBorder(0, 1, 0, 0, new Color(40, 70, 110)),
                new EmptyBorder(8, 10, 8, 10)));
        east.setPreferredSize(new Dimension(220, 0));

        JLabel histTitle = new JLabel("📜  Attack Log  (LinkedList)", SwingConstants.CENTER);
        histTitle.setForeground(new Color(100, 160, 220));
        histTitle.setFont(new Font("Arial", Font.BOLD, 11));

        historyArea = new JTextArea();
        historyArea.setEditable(false);
        historyArea.setBackground(new Color(8, 14, 28));
        historyArea.setForeground(new Color(140, 180, 200));
        historyArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        historyArea.setMargin(new Insets(4, 6, 4, 6));

        east.add(histTitle, BorderLayout.NORTH);
        east.add(new JScrollPane(historyArea), BorderLayout.CENTER);
        add(east, BorderLayout.EAST);

        setVisible(true);
        revalidate();
        repaint();
    }

    // Build one ocean grid panel
    private JPanel buildGridPanel(String title, JButton[][] btns, boolean isEnemy) {
        JPanel wrap = new JPanel(new BorderLayout(0, 6));
        wrap.setBackground(new Color(12, 25, 48));
        wrap.setBorder(new CompoundBorder(
                new LineBorder(new Color(40, 80, 130), 1, true),
                new EmptyBorder(8, 8, 8, 8)));

        JLabel lbl = new JLabel(title, SwingConstants.CENTER);
        lbl.setFont(new Font("Arial", Font.BOLD, 13));
        lbl.setForeground(isEnemy ? new Color(255, 80, 80) : new Color(80, 200, 255));
        wrap.add(lbl, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(GRID, GRID, 2, 2));
        grid.setBackground(new Color(8, 18, 35));

        for (int i = 0; i < GRID; i++) {
            for (int j = 0; j < GRID; j++) {
                JButton b = new JButton(isEnemy ? "?" : "~");
                b.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
                b.setBackground(isEnemy ? new Color(60, 12, 12) : new Color(10, 30, 65));
                b.setForeground(Color.WHITE);
                b.setFocusPainted(false);
                b.setMargin(new Insets(1, 1, 1, 1));
                b.setBorder(BorderFactory.createLineBorder(new Color(30, 55, 90), 1));
                btns[i][j] = b;
                final int r = i, c = j;
                if (isEnemy)
                    b.addActionListener(e -> humanFiresAt(r, c));
                else
                    b.addActionListener(e -> placeShip(r, c));
                grid.add(b);
            }
        }
        wrap.add(grid, BorderLayout.CENTER);
        return wrap;
    }

    //  PLACEMENT PHASE — uses Stack + ArrayList + HashMap
    private void placeShip(int r, int c) {
        if (!placementPhase || gameOver) return;
        if (humanOcean[r][c] == 'B') {
            setStatus("⚠  Already placed a ship there! Choose another cell.");
            return;
        }

        // Place ship
        humanOcean[r][c] = 'B';

        // ARRAY LIST — remember ship position
        humanShipList.add(new int[]{r, c});

        // HASH MAP — store coordinate status
        coordStatusMap.put("human_" + r + "," + c, "SHIP");

        // STACK — push so we can undo
        placementStack.push(new int[]{r, c});

        // Update button
        humanBtns[r][c].setText("🛡");
        humanBtns[r][c].setBackground(new Color(0, 100, 40));

        placedCount++;

        if (placedCount >= SHIPS_EACH) {
            // Done placing — start battle
            placementPhase = false;
            btnUndo.setEnabled(false);
            lblTurn.setText("YOUR TURN  —  Fire at the Enemy grid (right)!");
            lblTurn.setForeground(new Color(80, 220, 120));
            setStatus("  All ships placed!  Now click the ENEMY grid to attack.");
        } else {
            int left = SHIPS_EACH - placedCount;
            setStatus("  Ship placed at (" + r + "," + c + ").  Place " + left + " more.");
        }
    }

    // STACK — undo last placement
    private void undoLastPlacement() {
        if (placementStack.isEmpty()) {
            setStatus("⚠  Nothing to undo!  Stack is empty.");
            return;
        }
        // STACK pop (LIFO)
        int[] last = placementStack.pop();
        int r = last[0], c = last[1];

        humanOcean[r][c] = '.';
        coordStatusMap.remove("human_" + r + "," + c);

        // Remove from ArrayList too
        for (int i = humanShipList.size() - 1; i >= 0; i--) {
            int[] pos = humanShipList.get(i);
            if (pos[0] == r && pos[1] == c) { humanShipList.remove(i); break; }
        }

        humanBtns[r][c].setText("~");
        humanBtns[r][c].setBackground(new Color(10, 30, 65));
        placedCount--;
        setStatus("  ↩  Undo successful!  Ship removed from (" + r + "," + c + ").  Place " + (SHIPS_EACH - placedCount) + " more.");
    }

    //  BATTLE PHASE — Human fires
    private void humanFiresAt(int r, int c) {
        if (placementPhase || gameOver) return;

        String key = r + "," + c;

        // HASH SET — prevent duplicate attacks
        if (humanAttacked.contains(key)) {
            setStatus("⚠  Already fired there!  (HashSet blocked duplicate attack)");
            return;
        }
        humanAttacked.add(key);

        // QUEUE — peek whose turn it is
        String currentTurn = turnQueue.peek();

        boolean hit = (computerOcean[r][c] == 'B');
        if (hit) {
            computerOcean[r][c] = 'H';
            compHP   -= HIT_DAMAGE;
            humanHits++;
            computerBtns[r][c].setText("💥");
            computerBtns[r][c].setBackground(new Color(180, 30, 30));

            // LINKED LIST — log attack
            attackHistory.addFirst("T" + (turnCount+1) + " YOU hit (" + r + "," + c + ")");
            setStatus("  💥  DIRECT HIT at (" + r + "," + c + ")!  Enemy takes " + HIT_DAMAGE + " damage!");
        } else {
            computerOcean[r][c] = 'X';
            computerBtns[r][c].setText("X");
            computerBtns[r][c].setBackground(new Color(50, 50, 70));

            // LINKED LIST — log miss
            attackHistory.addFirst("T" + (turnCount+1) + " YOU miss (" + r + "," + c + ")");
            setStatus("  💧  Miss at (" + r + "," + c + ").  Shell lost in deep water.");
        }

        turnCount++;
        refreshStats();
        updateHistoryDisplay();

        if (!checkGameOver()) {
            // QUEUE — rotate turns (dequeue human, enqueue back, AI goes next)
            turnQueue.poll();
            turnQueue.add(currentTurn);

            lblTurn.setText("ENEMY TURN  —  AI targeting...");
            lblTurn.setForeground(new Color(255, 80, 80));

            // Delay then AI fires
            javax.swing.Timer t = new javax.swing.Timer(900, e -> aiFires());
            t.setRepeats(false);
            t.start();
        }
    }

    //  BATTLE PHASE — AI fires (uses PriorityQueue for smart targeting)
    private void aiFires() {
        if (gameOver) return;
        int r = -1, c = -1;

        // PRIORITY QUEUE — use smart targets first (neighbors of previous hits)
        while (!aiSmartTargets.isEmpty()) {
            int[] target = aiSmartTargets.poll();
            String k = target[0] + "," + target[1];
            // HASH SET — skip already attacked cells
            if (!computerAttacked.contains(k)) {
                r = target[0]; c = target[1];
                break;
            }
        }

        // If no smart target available, pick random (HASH SET prevents repeats)
        if (r == -1) {
            Random rand = new Random();
            String k;
            do {
                r = rand.nextInt(GRID);
                c = rand.nextInt(GRID);
                k = r + "," + c;
            } while (computerAttacked.contains(k));
        }

        String key = r + "," + c;
        // HASH SET — mark this cell as attacked by AI
        computerAttacked.add(key);

        boolean hit = (humanOcean[r][c] == 'B');
        if (hit) {
            humanOcean[r][c] = 'H';
            humanHP   -= HIT_DAMAGE;
            compHits++;
            humanBtns[r][c].setText("💥");
            humanBtns[r][c].setBackground(new Color(180, 30, 30));

            // PRIORITY QUEUE — add neighbors as high-priority targets after a hit
            int[][] neighbors = {{r-1,c},{r+1,c},{r,c-1},{r,c+1}};
            for (int[] nb : neighbors) {
                if (nb[0] >= 0 && nb[0] < GRID && nb[1] >= 0 && nb[1] < GRID) {
                    // Priority = 10 for direct neighbors (smarter than random pick)
                    aiSmartTargets.offer(new int[]{nb[0], nb[1], 10});
                }
            }

            // LINKED LIST — log AI hit
            attackHistory.addFirst("T" + turnCount + " AI  hit (" + r + "," + c + ")");
            setStatus("  🚨  Enemy hit YOUR ship at (" + r + "," + c + ")!  You take " + HIT_DAMAGE + " damage!");
        } else {
            humanOcean[r][c] = 'X';
            humanBtns[r][c].setText("X");
            humanBtns[r][c].setBackground(new Color(50, 50, 70));

            // LINKED LIST — log AI miss
            attackHistory.addFirst("T" + turnCount + " AI  miss(" + r + "," + c + ")");
            setStatus("  Enemy salvo missed your fleet.");
        }

        turnCount++;
        refreshStats();
        updateHistoryDisplay();

        if (!checkGameOver()) {
            // QUEUE — give turn back to human
            String aiTurn = turnQueue.poll();
            turnQueue.add(aiTurn);
            lblTurn.setText("YOUR TURN  —  Fire at the Enemy grid!");
            lblTurn.setForeground(new Color(80, 220, 120));
        }
    }


    //  WIN / LOSS CHECK
    private boolean checkGameOver() {
        String verdict = null;
        Color  color   = Color.WHITE;

        if (compHP <= 0) {
            verdict = "🏆  YOU WIN!  Enemy fleet destroyed!";
            color   = new Color(80, 220, 80);
        } else if (humanHP <= 0) {
            verdict = "💀  YOU LOSE!  Your fleet was sunk!";
            color   = new Color(255, 80, 80);
        } else if (turnCount >= MAX_TURNS * 2) {
            if (humanHits > compHits) {
                verdict = "🏁  TIME UP!  You win on accuracy!";
                color   = new Color(80, 220, 120);
            } else if (compHits > humanHits) {
                verdict = "🏁  TIME UP!  AI wins on accuracy!";
                color   = new Color(255, 100, 80);
            } else {
                verdict = "🤝  TIME UP!  It's a TIE!";
                color   = new Color(255, 200, 80);
            }
        }

        if (verdict != null) {
            gameOver = true;
            disableAllButtons();
            showEndScreen(verdict, color);
            return true;
        }
        return false;
    }

    //  END SCREEN
    private void showEndScreen(String verdict, Color color) {
        // Short delay then show end screen
        String v = verdict; Color col = color;
        javax.swing.Timer t = new javax.swing.Timer(1500, e -> {
            getContentPane().removeAll();
            getContentPane().setBackground(Color.BLACK);
            setLayout(new GridBagLayout());

            GridBagConstraints g = new GridBagConstraints();
            g.gridx = 0; g.insets = new Insets(12, 30, 12, 30);

            JLabel title = new JLabel(v, SwingConstants.CENTER);
            title.setFont(new Font("Impact", Font.PLAIN, 34));
            title.setForeground(col);
            g.gridy = 0; add(title, g);

            // Stats box
            String statsText =
                    "<html><div style='text-align:center;color:gray;font-size:13px;'>" +
                            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━<br>" +
                            "FINAL BATTLE REPORT<br>" +
                            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━<br><br>" +
                            "Commander : <b style='color:white;'>" + playerName.toUpperCase() + "</b><br>" +
                            "Your Hits : <b style='color:#80ff80;'>" + humanHits + "</b>   |   " +
                            "Your HP Left : <b style='color:#80ff80;'>" + Math.max(0, humanHP) + "</b><br>" +
                            "AI Hits   : <b style='color:#ff8080;'>" + compHits  + "</b>   |   " +
                            "AI HP Left   : <b style='color:#ff8080;'>" + Math.max(0, compHP)  + "</b><br><br>" +
                            "Total Turns Played : <b style='color:white;'>" + (turnCount/2) + " / " + MAX_TURNS + "</b><br><br>" +
                            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━<br>" +
                            "ATTACK LOG (LinkedList — last 6):<br>";

            // Show last 6 from LinkedList
            int shown = 0;
            for (String log : attackHistory) {
                if (shown++ >= 6) break;
                statsText += log + "<br>";
            }
            statsText += "</div></html>";

            JLabel stats = new JLabel(statsText, SwingConstants.CENTER);
            stats.setFont(new Font("Arial", Font.PLAIN, 13));
            g.gridy = 1; add(stats, g);

            // Play again button
            JButton again = new JButton("⚓  PLAY AGAIN");
            again.setFont(new Font("Arial", Font.BOLD, 15));
            again.setBackground(new Color(20, 60, 120));
            again.setForeground(Color.WHITE);
            again.setFocusPainted(false);
            again.setBorder(new EmptyBorder(12, 30, 12, 30));
            again.addActionListener(ev -> initGame());

            JButton quit = new JButton("✕  QUIT");
            quit.setFont(new Font("Arial", Font.BOLD, 15));
            quit.setBackground(new Color(80, 20, 20));
            quit.setForeground(Color.WHITE);
            quit.setFocusPainted(false);
            quit.setBorder(new EmptyBorder(12, 30, 12, 30));
            quit.addActionListener(ev -> System.exit(0));

            JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
            btnRow.setBackground(Color.BLACK);
            btnRow.add(again); btnRow.add(quit);
            g.gridy = 2; add(btnRow, g);

            revalidate(); repaint();
        });
        t.setRepeats(false);
        t.start();
    }

    //  HELPER METHODS
    private void refreshStats() {
        lblStats.setText(getStatsText());
    }

    private String getStatsText() {
        return "Commander " + playerName + "  |  " +
                "Your HP: " + humanHP + "  Hits: " + humanHits +
                "     ┃     " +
                "Enemy HP: " + compHP + "  Hits: " + compHits +
                "     ┃     " +
                "Turns: " + (turnCount/2) + " / " + MAX_TURNS;
    }

    private void setStatus(String msg) {
        if (lblStatus != null) lblStatus.setText(msg);
    }

    // LINKED LIST display — show last 14 entries
    private void updateHistoryDisplay() {
        if (historyArea == null) return;
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (String entry : attackHistory) {
            if (count++ >= 14) break;
            sb.append(entry).append("\n");
        }
        historyArea.setText(sb.toString());
    }

    private void disableAllButtons() {
        for (int i = 0; i < GRID; i++)
            for (int j = 0; j < GRID; j++) {
                humanBtns[i][j].setEnabled(false);
                computerBtns[i][j].setEnabled(false);
            }
        if (btnUndo != null) btnUndo.setEnabled(false);
    }
}
