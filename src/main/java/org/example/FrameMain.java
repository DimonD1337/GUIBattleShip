package org.example;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FrameMain extends JFrame {
    private static final int ROWS = 10;
    private static final int COLS = 10;
    private int index = 0;
    private Game game;
    private JPanel mainPanel;
    private JTextPane gameLog;
    private JButton beginButton;
    private JTable playerOneTable;
    private JTable playerTwoTable;
    private JComboBox<String> orientationBox;
    private JLabel statusLabel;
    private DefaultTableModel playerOneModel;
    private DefaultTableModel playerTwoModel;

    public FrameMain() {
        setTitle("Battleship");
        setContentPane(mainPanel);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        pack();

        gameLog.setEditable(false);
        playerOneModel = new DefaultTableModel(ROWS, COLS);
        playerTwoModel = new DefaultTableModel(ROWS, COLS);
        playerOneTable.setModel(playerOneModel);
        playerTwoTable.setModel(playerTwoModel);
        playerOneTable.setRowHeight(30);
        playerTwoTable.setRowHeight(30);
        playerOneTable.setEnabled(false);
        playerOneTable.setCellSelectionEnabled(false);
        playerTwoTable.setCellSelectionEnabled(false);
        playerTwoTable.setEnabled(false);

        playerOneTable.setDefaultRenderer(Object.class, new CellRenderer());
        playerTwoTable.setDefaultRenderer(Object.class, new CellRenderer());

        Set<String> orientations = new HashSet<>();
        orientations.add("Горизонтально");
        orientations.add("Вертикально");

        DefaultComboBoxModel<String> comboModel = new DefaultComboBoxModel<>();
        comboModel.addAll(orientations);

        orientationBox.setModel(comboModel);
        orientationBox.setSelectedIndex(0);
        orientationBox.setEnabled(false);

        for(int i = 0; i < COLS; i++) {
            playerOneTable.getColumnModel().getColumn(i).setPreferredWidth(30);
            playerTwoTable.getColumnModel().getColumn(i).setPreferredWidth(30);
        }

        beginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                StringBuilder sb = new StringBuilder();
                sb.append("Пришло время расставить корабли!\n");
                statusLabel.setText("Расстановка кораблей");
                game = new Game();
                game.initiatePlacingPhase();
                initialiseTable(playerOneTable);
                initialiseTable(playerTwoTable);
                beginButton.setEnabled(false);
                playerOneTable.setEnabled(true);
                gameLog.setText(sb.toString());
                orientationBox.setEnabled(true);
                game.automaticShipPlacement();
                game.takenPositions.clear();
            }
        });

        playerOneTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent event) {
                gameLog.setText(" ");
                StringBuilder sb = new StringBuilder();
                int row = playerOneTable.rowAtPoint(event.getPoint());
                int col = playerOneTable.columnAtPoint(event.getPoint());
                if(game.getStatus() == GameStatus.SHIP_PLACEMENT && index < game.shipSizes.length) {
                    if(game.placeShip(row,col,game.shipSizes[index], game.getPlayer1(),orientationBox.getSelectedIndex())) {
                        insertDataIntoGameField(playerOneTable, game.getPlayer1().getShips().get(game.getPlayer1().getShips().size() - 1));
                        index++;
                    }
                }
                if(index >= game.shipSizes.length) {
                    sb.append("Корабли размещены!\n Пришло время начать бой. Нажмите клетку на вражеском поле, чтобы совершить по ней выстрел.");
                    gameLog.setText(sb.toString());
                    statusLabel.setText("Бой");
                    game.initiateFightingPhase();
                    orientationBox.setEnabled(false);
                    playerOneTable.setEnabled(false);
                    playerTwoTable.setEnabled(true);
                }
            }
        });

        playerTwoTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent event) {
                StringBuilder sb = new StringBuilder();
                int row = playerOneTable.rowAtPoint(event.getPoint());
                int col = playerTwoTable.columnAtPoint(event.getPoint());
                if(game.getStatus() == GameStatus.BATTLE_PHASE) {
                    if(game.makeShotAt(row,col,game.getPlayer2())) {
                        insertFightDataIntoGameField(playerTwoTable, row,col, true , game.getNeighbourPositions());
                        gameLog.setText("Вы совершили выстрел в точку y = " + row + ", x = " + col + " и попали! У вас есть право совершить второй выстрел!");
                        game.neighbourPositions.clear();
                        if(game.getPlayer2().isOutOfShips()) {
                            game.endGame();
                            statusLabel.setText("Закончена");
                            gameLog.setText("Все корабли противника уничтожены! Игрок 1 побеждает!");
                            beginButton.setEnabled(true);
                            playerOneTable.setEnabled(false);
                            playerTwoTable.setEnabled(false);
                        }
                    } else {
                        insertFightDataIntoGameField(playerTwoTable, row, col, false,game.getNeighbourPositions());
                        sb.append("Вы совершили выстрел в точку y = ").append(row).append(", x = ").append(col).append(" и промахнулись! \n");
                        while (game.automaticMakeShotAt()) {
                            insertFightDataIntoGameField(playerOneTable, game.AIrow, game.AIcol, true,game.getNeighbourPositions());
                            sb.append("Противник совершил выстрел в точку y = ").append(game.AIrow).append(", x = ").append(game.AIcol).append(" и попал! \n");
                            game.neighbourPositions.clear();
                            if(game.getPlayer1().isOutOfShips()) {
                                game.endGame();
                                statusLabel.setText("Закончена");
                                gameLog.setText("Все союзные корабли уничтожены! Игрок 2 побеждает!");
                                beginButton.setEnabled(true);
                                playerOneTable.setEnabled(false);
                                playerTwoTable.setEnabled(false);
                            }
                        }
                        insertFightDataIntoGameField(playerOneTable, game.AIrow, game.AIcol, false,game.getNeighbourPositions());
                        sb.append("Противник совершил выстрел в точку y = ").append(game.AIrow).append(", x = ").append(game.AIcol).append(" и промахнулся!\nТеперь ваша очередь!");
                        gameLog.setText(sb.toString());
                    }
                }
            }
        });
    }

    private class CellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            Component cell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);

            if(value == null || value.equals(' ')) {
                cell.setBackground(Color.BLUE);
            } else if (value.equals('X')){
                cell.setBackground(Color.RED);
            } else if (value.equals('O')) {
                cell.setBackground(Color.CYAN);
            } else if (value.equals('.')) {
                cell.setBackground(Color.DARK_GRAY);
            } else {
                cell.setBackground(Color.BLACK);
            }

            return cell;
        }
    }
    public static void insertDataIntoGameField(JTable table, Ship ship) {
        TableModel model = table.getModel();
        List<Integer> positions = ship.getPositions();
        for(Integer position : positions) {
            model.setValueAt('.', (position-1) / 10, (position-1) % 10);
        }
    }

    public static void insertFightDataIntoGameField(JTable table, int row, int col, boolean isHit , Set<Integer> neighbourPositions ) {
        TableModel model = table.getModel();
        if(isHit) {
            model.setValueAt('X', row, col);
        } else {
            model.setValueAt('O', row, col);
        }
        for(int position : neighbourPositions) {
            model.setValueAt('O', position / 10, (position - 1) % 10);
        }
    }

    public static void initialiseTable(JTable table) {
        TableModel model = table.getModel();
        for(int i = 0; i < 10; i++) {
            for(int j = 0; j < 10; j++) {
                model.setValueAt(null, i, j);
            }
        }
    }
    public static void main(String[] args) {
        FrameMain win = new FrameMain();
        win.setVisible(true);
    }
}
