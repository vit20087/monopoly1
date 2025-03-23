import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class MonopolyGame extends JFrame {
    private static final int BOARD_SIZE = 40;
    private static final int DICE_SIDES = 6;
    private static final int STARTING_MONEY = 1500;

    private ArrayList<Player> players;
    private ArrayList<Property> properties;
    private int currentPlayerIndex;
    private JPanel boardPanel;
    private JTextArea gameLog;
    private JButton rollDiceButton;
    private JButton buyPropertyButton;
    private JButton endTurnButton;
    private Random random;
    private Color[] playerColors = {Color.RED, Color.BLUE, Color.GREEN, Color.ORANGE};
    private int previousPosition = 0; // To track player movement

    public MonopolyGame() {
        super("Monopoly Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 800);
        setLayout(new BorderLayout());

        random = new Random();
        initializeGame();
        initializeGUI();

        setVisible(true);
    }

    private void initializeGame() {
        // Initialize players
        players = new ArrayList<>();
        String[] playerNames = {"Player 1", "Player 2", "Player 3", "Player 4"};
        for (int i = 0; i < playerNames.length; i++) {
            players.add(new Player(playerNames[i], playerColors[i]));
        }
        currentPlayerIndex = 0;

        // Initialize properties
        properties = new ArrayList<>();
        initializeProperties();
    }

    private void initializeProperties() {
        // Create properties (simplified version with just a few properties)
        String[] propertyNames = {
                "Go", "Mediterranean Avenue", "Community Chest", "Baltic Avenue",
                "Income Tax", "Reading Railroad", "Oriental Avenue", "Chance",
                "Vermont Avenue", "Connecticut Avenue", "Jail", "St. Charles Place",
                "Electric Company", "States Avenue", "Virginia Avenue",
                "Pennsylvania Railroad", "St. James Place", "Community Chest",
                "Tennessee Avenue", "New York Avenue", "Free Parking", "Kentucky Avenue",
                "Chance", "Indiana Avenue", "Illinois Avenue", "B&O Railroad",
                "Atlantic Avenue", "Ventnor Avenue", "Water Works", "Marvin Gardens",
                "Go To Jail", "Pacific Avenue", "North Carolina Avenue", "Community Chest",
                "Pennsylvania Avenue", "Short Line Railroad", "Chance", "Park Place",
                "Luxury Tax", "Boardwalk"
        };

        int[] propertyCosts = {
                0, 60, 0, 60, 0, 200, 100, 0, 100, 120,
                0, 140, 150, 140, 160, 200, 180, 0, 180, 200,
                0, 220, 0, 220, 240, 200, 260, 260, 150, 280,
                0, 300, 300, 0, 320, 200, 0, 350, 0, 400
        };

        String[] colorGroups = {
                "NONE", "BROWN", "NONE", "BROWN", "NONE", "RAILROAD", "LIGHT_BLUE", "NONE",
                "LIGHT_BLUE", "LIGHT_BLUE", "NONE", "PURPLE", "UTILITY", "PURPLE", "PURPLE",
                "RAILROAD", "ORANGE", "NONE", "ORANGE", "ORANGE", "NONE", "RED", "NONE",
                "RED", "RED", "RAILROAD", "YELLOW", "YELLOW", "UTILITY", "YELLOW",
                "NONE", "GREEN", "GREEN", "NONE", "GREEN", "RAILROAD", "NONE", "BLUE", "NONE", "BLUE"
        };

        for (int i = 0; i < BOARD_SIZE; i++) {
            PropertyType type;
            if (i == 0) type = PropertyType.GO;
            else if (i == 10) type = PropertyType.JAIL;
            else if (i == 20) type = PropertyType.FREE_PARKING;
            else if (i == 30) type = PropertyType.GO_TO_JAIL;
            else if (propertyNames[i].contains("Chance")) type = PropertyType.CHANCE;
            else if (propertyNames[i].contains("Community Chest")) type = PropertyType.COMMUNITY_CHEST;
            else if (propertyNames[i].contains("Tax")) type = PropertyType.TAX;
            else if (colorGroups[i].equals("RAILROAD")) type = PropertyType.RAILROAD;
            else if (colorGroups[i].equals("UTILITY")) type = PropertyType.UTILITY;
            else type = PropertyType.PROPERTY;

            properties.add(new Property(propertyNames[i], propertyCosts[i], type, colorGroups[i]));
        }
    }

    private void initializeGUI() {
        // Create the board panel
        boardPanel = new JPanel();
        boardPanel.setLayout(new GridLayout(11, 11));
        boardPanel.setPreferredSize(new Dimension(600, 600));

        // Create the game log
        gameLog = new JTextArea();
        gameLog.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(gameLog);
        scrollPane.setPreferredSize(new Dimension(300, 600));

        // Create the control panel
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new GridLayout(2, 1));

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout());
        rollDiceButton = new JButton("Roll Dice");
        buyPropertyButton = new JButton("Buy Property");
        endTurnButton = new JButton("End Turn");

        buyPropertyButton.setEnabled(false);

        buttonPanel.add(rollDiceButton);
        buttonPanel.add(buyPropertyButton);
        buttonPanel.add(endTurnButton);

        // Player info panel
        JPanel playerInfoPanel = new JPanel(new GridLayout(1, players.size()));
        for (Player player : players) {
            JPanel panel = new JPanel();
            panel.setBackground(player.getColor());
            panel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            JLabel label = new JLabel(player.getName() + ": $" + player.getMoney());
            label.setForeground(getContrastColor(player.getColor()));
            panel.add(label);
            playerInfoPanel.add(panel);
        }

        controlPanel.add(playerInfoPanel);
        controlPanel.add(buttonPanel);

        // Add action listeners
        rollDiceButton.addActionListener(e -> rollDice());
        buyPropertyButton.addActionListener(e -> buyProperty());
        endTurnButton.addActionListener(e -> endTurn());

        // Add components to the frame
        add(boardPanel, BorderLayout.CENTER);
        add(scrollPane, BorderLayout.EAST);
        add(controlPanel, BorderLayout.SOUTH);

        // Initialize the board
        initializeBoard();
        updateGameState();
    }

    private Color getContrastColor(Color backgroundColor) {
        // Calculate the perceptive luminance (for text readability)
        double luminance = 0.299 * backgroundColor.getRed() +
                0.587 * backgroundColor.getGreen() +
                0.114 * backgroundColor.getBlue();

        return luminance > 128 ? Color.BLACK : Color.WHITE;
    }

    private void initializeBoard() {
        boardPanel.removeAll();

        // Create cells for the board
        for (int row = 0; row < 11; row++) {
            for (int col = 0; col < 11; col++) {
                JPanel cellPanel = new JPanel();
                cellPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
                cellPanel.setLayout(new BorderLayout());

                // Calculate property index based on position
                int propertyIndex = -1;

                if (row == 0) {
                    // Top row (20-30)
                    propertyIndex = 20 + (10 - col);
                } else if (row == 10) {
                    // Bottom row (0-10)
                    propertyIndex = col;
                } else if (col == 0) {
                    // Left column (19-11)
                    propertyIndex = 20 - row;
                } else if (col == 10) {
                    // Right column (31-39)
                    propertyIndex = 30 + row;
                } else {
                    // Center of the board
                    cellPanel.setBackground(Color.LIGHT_GRAY);
                }

                // Fill property cells
                if (propertyIndex >= 0 && propertyIndex < BOARD_SIZE) {
                    Property property = properties.get(propertyIndex);
                    cellPanel.setBackground(getColorForProperty(property));

                    JLabel nameLabel = new JLabel("<html><div style='text-align: center;'>" +
                            property.getName() + "</div></html>");
                    nameLabel.setHorizontalAlignment(JLabel.CENTER);
                    nameLabel.setFont(new Font("Arial", Font.PLAIN, 8));

                    cellPanel.add(nameLabel, BorderLayout.CENTER);

                    // Add price if it's a purchasable property
                    if (property.getCost() > 0) {
                        JLabel priceLabel = new JLabel("$" + property.getCost());
                        priceLabel.setHorizontalAlignment(JLabel.CENTER);
                        priceLabel.setFont(new Font("Arial", Font.PLAIN, 8));
                        cellPanel.add(priceLabel, BorderLayout.SOUTH);
                    }

                    // Add player tokens
                    JPanel playerPanel = new JPanel();
                    playerPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 2, 0));
                    playerPanel.setOpaque(false);

                    // Check if this is the previous position
                    boolean isAnimatedCell = false;
                    Player currentPlayer = players.get(currentPlayerIndex);
                    if (currentPlayer.getPosition() != previousPosition &&
                            propertyIndex == previousPosition &&
                            previousPosition != 0) {
                        // Add trail indicator
                        JLabel trailLabel = new JLabel("↑");
                        trailLabel.setForeground(currentPlayer.getColor());
                        trailLabel.setFont(new Font("Arial", Font.BOLD, 12));
                        playerPanel.add(trailLabel);
                        isAnimatedCell = true;
                    }

                    // Add player tokens
                    for (int i = 0; i < players.size(); i++) {
                        Player player = players.get(i);
                        if (player.getPosition() == propertyIndex) {
                            // Create custom token for player
                            JLabel playerToken = new JLabel();
                            if (i == currentPlayerIndex) {
                                playerToken.setText("★"); // Current player gets a star
                            } else {
                                playerToken.setText("●"); // Others get a circle
                            }
                            playerToken.setForeground(player.getColor());
                            playerToken.setFont(new Font("Arial", Font.BOLD, 14));
                            playerToken.setToolTipText(player.getName() + " ($" + player.getMoney() + ")");
                            playerPanel.add(playerToken);

                            // If this is current player's new position, add animation indicator
                            if (i == currentPlayerIndex &&
                                    currentPlayer.getPosition() != previousPosition &&
                                    !isAnimatedCell) {
                                JLabel moveLabel = new JLabel("⬇");
                                moveLabel.setForeground(player.getColor());
                                moveLabel.setFont(new Font("Arial", Font.BOLD, 12));
                                playerPanel.add(moveLabel);
                            }
                        }
                    }

                    cellPanel.add(playerPanel, BorderLayout.NORTH);

                    // Show owner indicator if property is owned
                    if (property.getOwner() != null) {
                        JPanel ownerPanel = new JPanel();
                        ownerPanel.setBackground(property.getOwner().getColor());
                        ownerPanel.setPreferredSize(new Dimension(10, 10));
                        cellPanel.add(ownerPanel, BorderLayout.EAST);
                    }
                }

                boardPanel.add(cellPanel);
            }
        }

        boardPanel.revalidate();
        boardPanel.repaint();
    }

    private Color getColorForProperty(Property property) {
        switch (property.getColorGroup()) {
            case "BROWN": return new Color(150, 75, 0);
            case "LIGHT_BLUE": return new Color(173, 216, 230);
            case "PURPLE": return new Color(218, 112, 214);
            case "ORANGE": return new Color(255, 165, 0);
            case "RED": return new Color(255, 0, 0);
            case "YELLOW": return new Color(255, 255, 0);
            case "GREEN": return new Color(0, 128, 0);
            case "BLUE": return new Color(0, 0, 255);
            case "RAILROAD": return new Color(128, 128, 128);
            case "UTILITY": return new Color(192, 192, 192);
            default: return new Color(240, 240, 240);
        }
    }

    private void rollDice() {
        Player currentPlayer = players.get(currentPlayerIndex);
        previousPosition = currentPlayer.getPosition(); // Store current position before moving

        int dice1 = random.nextInt(DICE_SIDES) + 1;
        int dice2 = random.nextInt(DICE_SIDES) + 1;
        int totalRoll = dice1 + dice2;

        logGameEvent(currentPlayer.getName() + " rolled " + dice1 + " and " + dice2 + " (total: " + totalRoll + ")");

        // Move the player
        int newPosition = (currentPlayer.getPosition() + totalRoll) % BOARD_SIZE;

        // Animate the movement (we'll show starting and ending positions with visual cues)
        logGameEvent(currentPlayer.getName() + " moves from " +
                properties.get(previousPosition).getName() + " to " +
                properties.get(newPosition).getName());

        // Check if player passes GO
        if (newPosition < currentPlayer.getPosition()) {
            currentPlayer.addMoney(200);
            logGameEvent(currentPlayer.getName() + " passed GO and collected $200");
        }

        currentPlayer.setPosition(newPosition);

        // Handle landing on property
        Property landedProperty = properties.get(newPosition);
        logGameEvent(currentPlayer.getName() + " landed on " + landedProperty.getName());

        // Handle special properties
        handleSpecialProperty(landedProperty);

        // Update UI
        rollDiceButton.setEnabled(false);
        if (landedProperty.getType() == PropertyType.PROPERTY &&
                landedProperty.getOwner() == null &&
                currentPlayer.getMoney() >= landedProperty.getCost()) {
            buyPropertyButton.setEnabled(true);
        } else {
            buyPropertyButton.setEnabled(false);
        }
        endTurnButton.setEnabled(true);

        updateGameState();
    }

    private void handleSpecialProperty(Property property) {
        Player currentPlayer = players.get(currentPlayerIndex);

        switch (property.getType()) {
            case GO_TO_JAIL:
                // Store current position for animation
                previousPosition = currentPlayer.getPosition();

                // Move to jail (position 10)
                currentPlayer.setPosition(10);
                logGameEvent(currentPlayer.getName() + " was sent to Jail");
                break;

            case TAX:
                // Pay tax
                int taxAmount = property.getName().contains("Income") ? 200 : 100;
                currentPlayer.subtractMoney(taxAmount);
                logGameEvent(currentPlayer.getName() + " paid $" + taxAmount + " in taxes");
                break;

            case PROPERTY:
            case RAILROAD:
            case UTILITY:
                // Check if the property is owned by someone else
                if (property.getOwner() != null && property.getOwner() != currentPlayer) {
                    int rent = calculateRent(property);
                    currentPlayer.subtractMoney(rent);
                    property.getOwner().addMoney(rent);
                    logGameEvent(currentPlayer.getName() + " paid $" + rent + " rent to " +
                            property.getOwner().getName());
                }
                break;

            case CHANCE:
                handleChanceCard();
                break;

            case COMMUNITY_CHEST:
                handleCommunityChest();
                break;

            default:
                // No special action for other properties
                break;
        }
    }

    private int calculateRent(Property property) {
        // A simple rent calculation
        if (property.getType() == PropertyType.RAILROAD) {
            int railroadCount = 0;
            for (Property p : properties) {
                if (p.getType() == PropertyType.RAILROAD && p.getOwner() == property.getOwner()) {
                    railroadCount++;
                }
            }
            return 25 * (int)Math.pow(2, railroadCount - 1);
        } else if (property.getType() == PropertyType.UTILITY) {
            int utilityCount = 0;
            for (Property p : properties) {
                if (p.getType() == PropertyType.UTILITY && p.getOwner() == property.getOwner()) {
                    utilityCount++;
                }
            }
            return utilityCount == 1 ? 4 * (random.nextInt(6) + 1 + random.nextInt(6) + 1) :
                    10 * (random.nextInt(6) + 1 + random.nextInt(6) + 1);
        } else {
            // Basic rent is 10% of the property cost
            return property.getCost() / 10;
        }
    }

    private void handleChanceCard() {
        Player currentPlayer = players.get(currentPlayerIndex);
        int card = random.nextInt(10);

        // Store current position for animation
        previousPosition = currentPlayer.getPosition();

        switch (card) {
            case 0:
                // Advance to GO
                currentPlayer.setPosition(0);
                currentPlayer.addMoney(200);
                logGameEvent("Chance: Advance to GO. Collect $200");
                break;
            case 1:
                // Advance to Boardwalk
                currentPlayer.setPosition(39);
                logGameEvent("Chance: Advance to Boardwalk");
                break;
            case 2:
                // Pay $50 fine
                currentPlayer.subtractMoney(50);
                logGameEvent("Chance: Pay $50 fine");
                break;
            case 3:
                // Get $150
                currentPlayer.addMoney(150);
                logGameEvent("Chance: Bank pays you $150");
                break;
            default:
                // No effect for simplicity
                logGameEvent("Chance: This card has no effect");
                break;
        }
    }

    private void handleCommunityChest() {
        Player currentPlayer = players.get(currentPlayerIndex);
        int card = random.nextInt(10);

        // Store current position for animation
        previousPosition = currentPlayer.getPosition();

        switch (card) {
            case 0:
                // Get $200
                currentPlayer.addMoney(200);
                logGameEvent("Community Chest: Bank error in your favor. Collect $200");
                break;
            case 1:
                // Pay $50 to hospital
                currentPlayer.subtractMoney(50);
                logGameEvent("Community Chest: Doctor's fee. Pay $50");
                break;
            case 2:
                // Get $50 from every player
                for (Player player : players) {
                    if (player != currentPlayer) {
                        player.subtractMoney(50);
                        currentPlayer.addMoney(50);
                    }
                }
                logGameEvent("Community Chest: It's your birthday. Collect $50 from every player");
                break;
            default:
                // No effect for simplicity
                logGameEvent("Community Chest: This card has no effect");
                break;
        }
    }

    private void buyProperty() {
        Player currentPlayer = players.get(currentPlayerIndex);
        Property currentProperty = properties.get(currentPlayer.getPosition());

        if (currentProperty.getOwner() == null && currentPlayer.getMoney() >= currentProperty.getCost()) {
            currentPlayer.subtractMoney(currentProperty.getCost());
            currentProperty.setOwner(currentPlayer);
            currentPlayer.addProperty(currentProperty);

            logGameEvent(currentPlayer.getName() + " bought " + currentProperty.getName() +
                    " for $" + currentProperty.getCost());

            buyPropertyButton.setEnabled(false);
            updateGameState();
        }
    }

    private void endTurn() {
        // Reset previous position tracker
        previousPosition = 0;

        // Move to the next player
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();

        // Reset buttons
        rollDiceButton.setEnabled(true);
        buyPropertyButton.setEnabled(false);
        endTurnButton.setEnabled(false);

        updateGameState();
    }

    private void updateGameState() {
        // Update the board
        initializeBoard();

        // Update player information in the bottom panel
        try {
            Container contentPane = getContentPane();
            if (contentPane.getComponentCount() > 2) {
                Component comp = contentPane.getComponent(2);
                if (comp instanceof JPanel) {
                    JPanel controlPanel = (JPanel) comp;
                    if (controlPanel.getComponentCount() > 0) {
                        Component playerInfoComp = controlPanel.getComponent(0);
                        if (playerInfoComp instanceof JPanel) {
                            JPanel playerInfoPanel = (JPanel) playerInfoComp;
                            Component[] components = playerInfoPanel.getComponents();

                            for (int i = 0; i < Math.min(players.size(), components.length); i++) {
                                Player player = players.get(i);
                                if (components[i] instanceof JPanel) {
                                    JPanel playerPanel = (JPanel) components[i];
                                    if (playerPanel.getComponentCount() > 0 && playerPanel.getComponent(0) instanceof JLabel) {
                                        JLabel label = (JLabel) playerPanel.getComponent(0);
                                        label.setText(player.getName() + ": $" + player.getMoney());

                                        // Highlight current player
                                        if (i == currentPlayerIndex) {
                                            playerPanel.setBorder(BorderFactory.createLineBorder(Color.WHITE, 3));
                                        } else {
                                            playerPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error updating player panels: " + e.getMessage());
            // Continue with the rest of the method
        }

        // Update player information
        Player currentPlayer = players.get(currentPlayerIndex);
        logGameEvent("\nCurrent Turn: " + currentPlayer.getName() +
                " (Money: $" + currentPlayer.getMoney() + ")");

        // List owned properties
        if (!currentPlayer.getOwnedProperties().isEmpty()) {
            StringBuilder sb = new StringBuilder("Properties owned: ");
            for (Property p : currentPlayer.getOwnedProperties()) {
                sb.append(p.getName()).append(", ");
            }
            logGameEvent(sb.substring(0, sb.length() - 2));
        }
    }
    private void logGameEvent(String event) {
        gameLog.append(event + "\n");
        // Scroll to the bottom
        gameLog.setCaretPosition(gameLog.getDocument().getLength());
    }

    // Main method
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MonopolyGame());
    }

    // Inner classes

    class Player {
        private String name;
        private int money;
        private int position;
        private ArrayList<Property> ownedProperties;
        private Color color;

        public Player(String name, Color color) {
            this.name = name;
            this.money = STARTING_MONEY;
            this.position = 0;
            this.ownedProperties = new ArrayList<>();
            this.color = color;
        }

        public String getName() {
            return name;
        }

        public int getMoney() {
            return money;
        }

        public Color getColor() {
            return color;
        }

        public void addMoney(int amount) {
            money += amount;
        }

        public void subtractMoney(int amount) {
            money -= amount;
            if (money < 0) {
                // For simplicity, we don't handle bankruptcy fully
                logGameEvent(name + " is bankrupt!");
                money = 0;
            }
        }

        public int getPosition() {
            return position;
        }

        public void setPosition(int position) {
            this.position = position;
        }

        public void addProperty(Property property) {
            ownedProperties.add(property);
        }

        public List<Property> getOwnedProperties() {
            return ownedProperties;
        }
    }

    enum PropertyType {
        PROPERTY, RAILROAD, UTILITY, GO, JAIL, FREE_PARKING, GO_TO_JAIL, CHANCE, COMMUNITY_CHEST, TAX
    }

    class Property {
        private String name;
        private int cost;
        private PropertyType type;
        private String colorGroup;
        private Player owner;

        public Property(String name, int cost, PropertyType type, String colorGroup) {
            this.name = name;
            this.cost = cost;
            this.type = type;
            this.colorGroup = colorGroup;
            this.owner = null;
        }

        public String getName() {
            return name;
        }

        public int getCost() {
            return cost;
        }

        public PropertyType getType() {
            return type;
        }

        public String getColorGroup() {
            return colorGroup;
        }

        public Player getOwner() {
            return owner;
        }

        public void setOwner(Player owner) {
            this.owner = owner;
        }
    }
}