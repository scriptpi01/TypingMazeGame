import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.Random;

public class TypingMazeGame {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainMenuFrame mainMenuFrame = new MainMenuFrame();
            mainMenuFrame.setVisible(true);
        });
    }

    static class MainMenuFrame extends JFrame {
        JTextField playerNameField;
        JButton startButton, scoreBoardButton;

        public MainMenuFrame() {
            setTitle("Typing Maze v1.0.1 - Main Menu");
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            setSize(600, 400);
            setLocationRelativeTo(null);

            JPanel squarePanel = new JPanel(new FlowLayout());
            JPanel square1 = new JPanel();
            square1.setBackground(Color.WHITE);
            square1.setPreferredSize(new Dimension(100, 100));
            squarePanel.add(square1);

            JPanel square2 = new JPanel();
            square2.setBackground(Color.BLACK);
            square2.setPreferredSize(new Dimension(100, 100));
            squarePanel.add(square2);

            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.gridwidth = 2;
            gbc.insets = new Insets(10, 10, 10, 10);
            add(squarePanel, gbc);

            JLabel mainName = new JLabel("Welcome to Typing Maze!");
            mainName.setFont(new Font(mainName.getFont().getName(), Font.BOLD, 20));
            gbc.gridy = 1;
            add(mainName, gbc);

            JLabel nameLabel = new JLabel("Enter your name here");
            nameLabel.setFont(new Font(nameLabel.getFont().getName(), 1 / 2, 13));
            gbc.gridy = 2;
            add(nameLabel, gbc);

            playerNameField = new JTextField(20);
            playerNameField.setFont(new Font(playerNameField.getFont().getName(), Font.BOLD, 20));
            gbc.gridy = 2;
            add(playerNameField, gbc);

            JPanel buttonPanel = new JPanel(new FlowLayout());
            startButton = new JButton("Start Game");
            startButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String playerName = playerNameField.getText();
                    if (!playerName.trim().isEmpty()) {
                        MazeGame game = new MazeGame(40, 21, playerName);
                        game.setVisible(true);
                        MainMenuFrame.this.dispose();
                    } else {
                        JOptionPane.showMessageDialog(MainMenuFrame.this, "Please enter your name.");
                    }
                }
            });
            buttonPanel.add(startButton);

            scoreBoardButton = new JButton("Score Board");
            scoreBoardButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    showScoreBoard();
                }
            });
            buttonPanel.add(scoreBoardButton);

            gbc.gridy = 3;
            add(buttonPanel, gbc);

        }

        static class PlayerScore {
            String name;
            int time;

            public PlayerScore(String name, int time) {
                this.name = name;
                this.time = time;
            }
        }

        private void showScoreBoard() {
            JFrame scoreFrame = new JFrame("Score Board");
            scoreFrame.setLayout(new BorderLayout());
            scoreFrame.setSize(600, 500);
            scoreFrame.setLocationRelativeTo(null);

            String[] columnNames = { "Rank", "Player Name", "Time (seconds)" };
            DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

            JTable table = new JTable(model);
            table.setRowHeight(30);
            table.setFont(new Font("Serif", Font.BOLD, 16));
            table.getTableHeader().setFont(new Font("Serif", Font.BOLD, 18));
            DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
            centerRenderer.setHorizontalAlignment(JLabel.CENTER);

            // Apply center alignment to all columns
            for (int columnIndex = 0; columnIndex < table.getColumnCount(); columnIndex++) {
                table.getColumnModel().getColumn(columnIndex).setCellRenderer(centerRenderer);
            }

            // Adjust width for rank column
            table.getColumnModel().getColumn(0).setPreferredWidth(30);

            JScrollPane scrollPane = new JScrollPane(table);
            scoreFrame.add(scrollPane, BorderLayout.CENTER);

            // Read and sort the data
            List<PlayerScore> scores = new ArrayList<>();
            try (BufferedReader br = new BufferedReader(new FileReader("database.txt"))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] data = line.split(", ");
                    scores.add(new PlayerScore(data[0], Integer.parseInt(data[1])));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Sort the list by time
            scores.sort(Comparator.comparingInt(s -> s.time));

            // Add sorted data to the table model
            int rank = 1;
            for (PlayerScore score : scores) {
                model.addRow(new Object[] { rank++, score.name, score.time });
            }

            scoreFrame.setVisible(true);
        }
    }

    public static class MazeGame extends JFrame implements KeyListener {
        private Maze maze;
        private MazePanel mazePanel;
        private CommandPanel commandPanel;
        private StringBuilder currentTypedCommand = new StringBuilder();
        private JLabel playerNameLabel;
        private JLabel timerLabel;
        private Timer gameTimer;
        private int timeElapsed; // in seconds
        private boolean secretModeActivated = false;
        private Random random = new Random();

        public MazeGame(int width, int height, String playerName) {
            setTitle("Typing Maze v1.0.1 - " + playerName + " (Player)");

            maze = new Maze(width, height);
            maze.generateMaze();
            mazePanel = new MazePanel(maze, this); // Initialize here
            add(mazePanel, BorderLayout.CENTER);

            commandPanel = new CommandPanel();
            add(commandPanel, BorderLayout.SOUTH);

            JPanel northPanel = new JPanel();
            northPanel.setLayout(new FlowLayout(FlowLayout.CENTER));

            playerNameLabel = new JLabel("Player: " + playerName + "   ");
            playerNameLabel.setFont(new Font(playerNameLabel.getFont().getName(), Font.BOLD, 18));

            timerLabel = new JLabel("Time: 0");
            timerLabel.setFont(new Font(timerLabel.getFont().getName(), Font.BOLD, 18));

            northPanel.add(playerNameLabel);
            northPanel.add(timerLabel);

            add(northPanel, BorderLayout.NORTH);

            startTimer();

            addKeyListener(this);
            setResizable(false);
            pack();
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setLocationRelativeTo(null);
            setVisible(true);
        }

        public boolean isSecretModeActivated() {
            return secretModeActivated;
        }

        private void checkForGameCompletion() {
            if (maze.playerHasReachedGoal()) {
                gameTimer.stop();
                saveGameData();
                JOptionPane.showMessageDialog(this, "Congratulations, you've completed the maze!");
        
                // Open the main menu frame
                SwingUtilities.invokeLater(() -> {
                    MainMenuFrame mainMenu = new MainMenuFrame();
                    mainMenu.setVisible(true);
                });
        
                dispose(); // Close the current game window
            }
        }
        

        private void startTimer() {
            timeElapsed = 0;
            gameTimer = new Timer(1000, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    timeElapsed++;
                    timerLabel.setText("Time: " + timeElapsed);
                }
            });
            gameTimer.start();
        }

        private void saveGameData() {
            String playerName = playerNameLabel.getText().replace("Player: ", "").trim(); // Extracting only the
                                                                                          // player's name
            try (FileWriter fw = new FileWriter("database.txt", true);
                    BufferedWriter bw = new BufferedWriter(fw);
                    PrintWriter out = new PrintWriter(bw)) {
                out.println(playerName + ", " + timeElapsed); // Saving in the format "name, time"
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void keyTyped(KeyEvent e) {
            if (e.getKeyChar() != KeyEvent.VK_BACK_SPACE) {
                currentTypedCommand.append(e.getKeyChar());
            }
            commandPanel.updateTypedCommand(currentTypedCommand.toString());
        }

        @Override
        public void keyPressed(KeyEvent e) {
            if (secretModeActivated) {
                // secret mode activated
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_W:
                        maze.movePlayer(-1, 0, true);
                        break; // w
                    case KeyEvent.VK_A:
                        maze.movePlayer(0, -1, true);
                        break; // a
                    case KeyEvent.VK_S:
                        maze.movePlayer(1, 0, true);
                        break; // s
                    case KeyEvent.VK_D:
                        maze.movePlayer(0, 1, true);
                        break; // d
                }
                mazePanel.repaint();
                checkForGameCompletion();
                return;
            }

            if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                if (currentTypedCommand.length() > 0) {
                    currentTypedCommand.deleteCharAt(currentTypedCommand.length() - 1);
                }
                commandPanel.updateTypedCommand(currentTypedCommand.toString());
                return;
            }

            if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                String typedCommand = currentTypedCommand.toString().trim();

                // ตรวจสอบรหัสลับสำหรับโหมดลับ
                if (typedCommand.equals("01100010")) {
                    secretModeActivated = true;
                    JOptionPane.showMessageDialog(this, "Secret mode activated!");
                    currentTypedCommand.setLength(0);
                    commandPanel.updateTypedCommand("");
                    return;
                }

                int direction = commandPanel.getDirectionForCommand(typedCommand);

                if (commandPanel.isValidCommand(typedCommand)) {
                    String directionText = "";
                    switch (direction) {
                        case 0:
                            directionText = "Top";
                            maze.movePlayer(-1, 0, false);
                            break;
                        case 1:
                            directionText = "Left";
                            maze.movePlayer(0, -1, false);
                            break;
                        case 2:
                            directionText = "Bottom";
                            maze.movePlayer(1, 0, false);
                            break;
                        case 3:
                            directionText = "Right";
                            maze.movePlayer(0, 1, false);
                            break;
                    }
                    commandPanel.setNotificationText("Correct! Go " + directionText);
                    checkForGameCompletion();
                } else {
                    commandPanel.setNotificationText("Wrong! Stay put");
                }

                commandPanel.updateCommands();
                currentTypedCommand.setLength(0);
                commandPanel.updateTypedCommand("");

                mazePanel.repaint();
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            // Not used
        }

        class MazePanel extends JPanel {
            private final Maze maze;
            private final int cellSize = 40;
            private MazeGame mazeGame;

            public MazePanel(Maze maze, MazeGame mazeGame) {
                this.maze = maze;
                this.mazeGame = mazeGame;
                setPreferredSize(new Dimension(maze.width * cellSize, maze.height * cellSize));
                setBackground(Color.BLACK);
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // Draw Maze
                g.setColor(Color.WHITE);
                for (int y = 0; y < maze.height; y++) {
                    for (int x = 0; x < maze.width; x++) {
                        Cell cell = maze.cells[y][x];
                        int xCoord = x * cellSize;
                        int yCoord = y * cellSize;
                        if (cell.topWall)
                            g.drawLine(xCoord, yCoord, xCoord + cellSize, yCoord);
                        if (cell.leftWall)
                            g.drawLine(xCoord, yCoord, xCoord, yCoord + cellSize);
                        if (cell.rightWall)
                            g.drawLine(xCoord + cellSize, yCoord, xCoord + cellSize, yCoord + cellSize);
                        if (cell.bottomWall)
                            g.drawLine(xCoord, yCoord + cellSize, xCoord + cellSize, yCoord + cellSize);
                    }
                }
                // Draw Player
                g.setColor(Color.YELLOW);
                g.fillOval(maze.player.x * cellSize, maze.player.y * cellSize, cellSize, cellSize);

                if (mazeGame.isSecretModeActivated()) {
                    g.setColor(new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256)));
                } else {
                    g.setColor(Color.YELLOW);
                }
                g.fillOval(maze.player.x * cellSize, maze.player.y * cellSize, cellSize, cellSize);

                // Draw Goal
                g.setColor(Color.RED);
                g.fillOval((maze.width - 1) * cellSize, (maze.height - 1) * cellSize, cellSize, cellSize);
            }
        }

        class Maze {
            private final int width, height;
            private final Cell[][] cells;
            private final Player player;

            public Maze(int width, int height) {
                this.width = width;
                this.height = height;
                cells = new Cell[height][width];
                player = new Player();
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        cells[y][x] = new Cell(x, y);
                    }
                }
            }

            public boolean playerHasReachedGoal() {
                return player.x == width - 1 && player.y == height - 1; // Assuming the goal is at the bottom-right
                                                                        // corner
            }

            public void generateMaze() {
                Stack<Cell> stack = new Stack<>();
                Cell current, next;

                current = cells[0][0];
                current.visited = true;

                do {
                    List<Cell> unvisitedNeighbors = getUnvisitedNeighbors(current);
                    if (!unvisitedNeighbors.isEmpty()) {
                        next = unvisitedNeighbors.get((int) (Math.random() * unvisitedNeighbors.size()));
                        removeWalls(current, next);
                        stack.push(current);
                        current = next;
                        current.visited = true;
                    } else if (!stack.isEmpty()) {
                        current = stack.pop();
                    }
                } while (!stack.isEmpty());
            }

            private List<Cell> getUnvisitedNeighbors(Cell cell) {
                List<Cell> neighbors = new ArrayList<>();

                int x = cell.x;
                int y = cell.y;

                if (y > 0 && !cells[y - 1][x].visited)
                    neighbors.add(cells[y - 1][x]);
                if (x < width - 1 && !cells[y][x + 1].visited)
                    neighbors.add(cells[y][x + 1]);
                if (y < height - 1 && !cells[y + 1][x].visited)
                    neighbors.add(cells[y + 1][x]);
                if (x > 0 && !cells[y][x - 1].visited)
                    neighbors.add(cells[y][x - 1]);

                return neighbors;
            }

            private void removeWalls(Cell current, Cell next) {
                int dx = next.x - current.x;
                int dy = next.y - current.y;

                if (dx == 1) {
                    current.rightWall = false;
                    next.leftWall = false;
                } else if (dx == -1) {
                    current.leftWall = false;
                    next.rightWall = false;
                } else if (dy == 1) {
                    current.bottomWall = false;
                    next.topWall = false;
                } else if (dy == -1) {
                    current.topWall = false;
                    next.bottomWall = false;
                }
            }

            public void movePlayer(int dy, int dx, boolean throughWalls) {
                int newX = player.x + dx;
                int newY = player.y + dy;

                if (newX >= 0 && newX < width && newY >= 0 && newY < height) {
                    if (throughWalls) {
                        // Move regardless of walls
                        player.x = newX;
                        player.y = newY;
                    } else {
                        // Existing wall checking logic
                        Cell currentCell = cells[player.y][player.x];
                        if (dx == 1 && !currentCell.rightWall) {
                            player.x = newX;
                        } else if (dx == -1 && !currentCell.leftWall) {
                            player.x = newX;
                        } else if (dy == 1 && !currentCell.bottomWall) {
                            player.y = newY;
                        } else if (dy == -1 && !currentCell.topWall) {
                            player.y = newY;
                        }
                    }
                }
            }
        }

        class Cell {
            boolean topWall = true, rightWall = true, bottomWall = true, leftWall = true;
            boolean visited = false;
            int x, y;

            public Cell(int x, int y) {
                this.x = x;
                this.y = y;
            }
        }

        class Player {
            int x = 0, y = 0;
        }

        class CommandPanel extends JPanel {
            private JLabel[] commandLabels = new JLabel[4];
            private JLabel notificationLabel, typedCommandLabel;
            private String[] currentCommands = new String[4];
            private final String[] possibleCommands;

            public CommandPanel() {
                possibleCommands = loadCommandsFromFile("commands.txt");
                setLayout(new BorderLayout());
                setBorder(new EmptyBorder(10, 10, 5, 10));

                // Panel for commands and directions
                JPanel commandsPanel = new JPanel(new GridLayout(1, 4, 10, 0));
                commandsPanel.setBorder(new EmptyBorder(0, 0, 10, 0));

                // Set new bold and larger font for command labels
                Font commandLabelFont = createNewFontSize(getFont(), 16, Font.BOLD);
                for (int i = 0; i < commandLabels.length; i++) {
                    commandLabels[i] = new JLabel("", JLabel.CENTER);
                    commandLabels[i].setFont(commandLabelFont);
                    commandsPanel.add(commandLabels[i]);
                }

                add(commandsPanel, BorderLayout.NORTH);

                // Notification label setup
                notificationLabel = new JLabel("", JLabel.CENTER);
                notificationLabel.setFont(commandLabelFont);
                notificationLabel.setForeground(Color.BLACK);
                notificationLabel.setBackground(Color.YELLOW);
                notificationLabel.setOpaque(true);
                add(notificationLabel, BorderLayout.CENTER);

                // Panel for typed command
                JPanel typedCommandPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
                typedCommandLabel = new JLabel("Typing: ", JLabel.CENTER);
                typedCommandLabel.setFont(commandLabelFont);
                typedCommandPanel.add(typedCommandLabel);
                add(typedCommandPanel, BorderLayout.SOUTH);

                updateCommands();
            }

            private Font createNewFontSize(Font originalFont, int newSize, int newStyle) {
                return new Font(originalFont.getName(), newStyle, newSize);
            }

            public void updateTypedCommand(String typedCommand) {
                typedCommandLabel.setText("Typed: " + typedCommand);
            }

            public void updateCommands() {
                Set<String> usedCommands = new HashSet<>();
                Random random = new Random();

                for (int i = 0; i < currentCommands.length; i++) {
                    String command;
                    do {
                        command = possibleCommands[random.nextInt(possibleCommands.length)];
                    } while (usedCommands.contains(command));

                    usedCommands.add(command);
                    currentCommands[i] = command;
                    commandLabels[i].setText(getDirectionText(i) + ": " + command);
                }
            }

            public void setNotificationText(String text) {
                SwingUtilities.invokeLater(() -> {
                    notificationLabel.setText(text);
                    notificationLabel.setForeground(Color.WHITE);
                    notificationLabel.setBackground(text.startsWith("Correct! Go") ? Color.GREEN : Color.RED);
                    notificationLabel.setOpaque(true);
                    notificationLabel.setVisible(true);

                    int delay = text.startsWith("Correct! Go") ? 2000 : 3000; // 1 second for correct, 5 seconds for
                                                                              // wrong

                    Timer timer = new Timer(delay, e -> {
                        notificationLabel.setText("");
                        notificationLabel.setVisible(false);
                    });
                    timer.setRepeats(false);
                    timer.start();
                });
            }

            private String getDirectionText(int index) {
                switch (index) {
                    case 0:
                        return "Top";
                    case 1:
                        return "Left";
                    case 2:
                        return "Bottom";
                    case 3:
                        return "Right";
                    default:
                        return "";
                }
            }

            public boolean isValidCommand(String command) {
                for (String cmd : currentCommands) {
                    if (cmd.equalsIgnoreCase(command)) {
                        return true;
                    }
                }
                return false;
            }

            public int getDirectionForCommand(String command) {
                for (int i = 0; i < currentCommands.length; i++) {
                    if (currentCommands[i].equalsIgnoreCase(command)) {
                        return i; // 0 for W, 1 for A, 2 for S, 3 for D
                    }
                }
                return -1; // Invalid direction
            }

            private String[] loadCommandsFromFile(String fileName) {
                List<String> commands = new ArrayList<>();
                try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        commands.add(line.trim());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return commands.toArray(new String[0]);
            }

        }
    }
}
