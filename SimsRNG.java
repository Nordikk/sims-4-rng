import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.datatransfer.StringSelection;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import javax.swing.Timer;

public class SimsRNG extends JFrame {
    private JTable table;
    private DefaultTableModel tableModel;
    private JButton[] btnZimmer;
    private JButton btnSave;
    private JButton btnReload;
    private JButton btnStartTimer;
    private JButton btnStopTimer;
    private JTextField timerDelayField;
    private JPanel zimmerPanel;
    private Timer timer;

    private String filePath;
    private int timeRemaining;

    public SimsRNG() {
        setTitle("Sims-RNG");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

        filePath = "attributes.csv";

        // Create the table with the initial data
        tableModel = new DefaultTableModel(new Object[][] {}, new Object[] {});
        table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane);

        // Create the buttons with the names from the "Zimmer" column
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(2, 2)); // Added grid layout for better button positioning
        add(buttonPanel);

        // Create a new JPanel for Save and Reload buttons
        JPanel saveReloadPanel = new JPanel();
        btnSave = new JButton("Save CSV");
        btnSave.addActionListener(new SaveButtonActionListener());
        saveReloadPanel.add(btnSave);

        btnReload = new JButton("Reload CSV");
        btnReload.addActionListener(new ReloadButtonActionListener());
        saveReloadPanel.add(btnReload);

        // Add saveReloadPanel to the North of buttonPanel
        buttonPanel.add(saveReloadPanel, BorderLayout.NORTH);

        zimmerPanel = new JPanel(); // Initialize zimmerPanel
        buttonPanel.add(zimmerPanel, BorderLayout.CENTER); // Add it to the buttonPanel

        // Timer panel
        JPanel timerPanel = new JPanel();
        add(timerPanel);

        JLabel lblTimer = new JLabel("Timer (minutes):");
        timerPanel.add(lblTimer);

        JTextField timerDelayField = new JTextField("0", 5);
        timerPanel.add(timerDelayField);

        JButton btnStartTimer = new JButton("Start Timer");
        timerPanel.add(btnStartTimer);

        JButton btnStopTimer = new JButton("Stop Timer");
        timerPanel.add(btnStopTimer);

        JLabel lblRemainingTime = new JLabel("00:00");
        timerPanel.add(lblRemainingTime);

        Timer timer = new Timer(0, null);
        timer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                timeRemaining--;
                int minutes = timeRemaining / 60;
                int seconds = timeRemaining % 60;
                lblRemainingTime.setText(String.format("%02d:%02d", minutes, seconds));
                if (timeRemaining <= 0) {
                    ((Timer) e.getSource()).stop();
                    JOptionPane.showMessageDialog(SimsRNG.this, "Timer abgelaufen.", "Timer", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });

        btnStartTimer.addActionListener(e -> {
            int minutes = Integer.parseInt(timerDelayField.getText());
            timeRemaining = minutes * 60;
            timer.setDelay(1000); // Count down every second
            timer.start();
        });

        btnStopTimer.addActionListener(e -> timer.stop());

        setLocationRelativeTo(null);

        // Load the attributes from the CSV file
        loadAttributes();

        pack();
        setVisible(true);
    }

    private void loadAttributes() {
        List<String> attributes = loadAttributesFromFile();
        if (attributes != null) {
            String[] columnNames = attributes.get(0).split(",");
            List<String[]> data = new ArrayList<>();
            for (int i = 1; i < attributes.size(); i++) {
                String[] values = attributes.get(i).split(",");
                data.add(values);
            }
            tableModel.setDataVector(data.toArray(new Object[0][]), columnNames);
            updateButtons();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to load attributes file.", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private List<String> loadAttributesFromFile() {
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8));
            List<String> attributes = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                attributes.add(line);
            }
            reader.close();
            return attributes;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String[] getColumnData(String columnName) {
        int columnIndex = tableModel.findColumn(columnName);
        if (columnIndex != -1) {
            List<String> columnData = new ArrayList<>();
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                Object value = tableModel.getValueAt(i, columnIndex);
                columnData.add(value != null ? value.toString() : "");
            }
            return columnData.toArray(new String[0]);
        }
        return new String[0];
    }

    private void updateButtons() {
        zimmerPanel.removeAll();

        // Create the buttons with the names from the "Zimmer" column
        String[] zimmerValues = getColumnData("Zimmer");
        btnZimmer = new JButton[zimmerValues.length];
        for (int i = 0; i < zimmerValues.length; i++) {
            btnZimmer[i] = new JButton(zimmerValues[i]);
            btnZimmer[i].addActionListener(new ZimmerButtonActionListener(zimmerValues[i]));
            zimmerPanel.add(btnZimmer[i]);
        }

        zimmerPanel.revalidate();
        zimmerPanel.repaint();
    }

    private class ZimmerButtonActionListener implements ActionListener {
        private String zimmerValue;
        private List<String> unusedStilValues; // Used to keep track of which Stil values have been used
        private List<String> unusedFarbeValues; // Used to keep track of which Farbe values have been used

        public ZimmerButtonActionListener(String zimmerValue) {
            this.zimmerValue = zimmerValue;
            this.unusedStilValues = new ArrayList<>(Arrays.asList(getColumnData("Stil")));
            this.unusedStilValues.removeIf(String::isEmpty);
            this.unusedFarbeValues = new ArrayList<>(Arrays.asList(getColumnData("Farbe")));
            this.unusedFarbeValues.removeIf(String::isEmpty);

        }

        @Override
        public void actionPerformed(ActionEvent e) {
            List<Integer> matchingRows = new ArrayList<>();
            int zimmerColumnIndex = tableModel.findColumn("Zimmer");
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                String value = tableModel.getValueAt(i, zimmerColumnIndex).toString();
                if (value.equals(zimmerValue)) {
                    matchingRows.add(i);
                }
            }
            Random random = new Random();
            int randomRow = matchingRows.get(random.nextInt(matchingRows.size()));
            StringBuilder attributeValues = new StringBuilder();
            attributeValues.append("Zimmer: ").append(zimmerValue).append("\n");
            for (int i = 1; i < tableModel.getColumnCount(); i++) {
                String columnName = tableModel.getColumnName(i);
                String value = getRandomAttributeValue(randomRow, i);
                attributeValues.append(columnName).append(": ").append(value).append("\n");
            }
            JOptionPane pane = new JOptionPane(attributeValues.toString(), JOptionPane.INFORMATION_MESSAGE);
            JButton copy = new JButton("Copy");
            copy.addActionListener(ae -> {
                StringSelection stringSelection = new StringSelection(attributeValues.toString());
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(stringSelection, null);
            });
            pane.setOptions(new Object[] { copy });
            JDialog dialog = pane.createDialog(SimsRNG.this, "Random Attributes");
            dialog.setVisible(true);
        }

        private String getRandomAttributeValue(int row, int column) {
            String columnName = tableModel.getColumnName(column);
            Random random = new Random();
            if (columnName.equals("Stil")) {
                if (unusedStilValues.isEmpty()) {
                    return "";
                } else {
                    return unusedStilValues.remove(random.nextInt(unusedStilValues.size()));
                }
            } else if (columnName.equals("Farbe")) {
                if (unusedFarbeValues.isEmpty()) {
                    return "";
                } else {
                    return unusedFarbeValues.remove(random.nextInt(unusedFarbeValues.size()));
                }
            } else {
                List<String> attributeValues = new ArrayList<>();
                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    String value = tableModel.getValueAt(i, column).toString();
                    if (!value.isEmpty()) {
                        attributeValues.add(value);
                    }
                }
                if (!attributeValues.isEmpty()) {
                    return attributeValues.get(random.nextInt(attributeValues.size()));
                }
                return "";
            }
        }
    }

    private class SaveButtonActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            saveChanges();
            loadAttributes();
        }
    }

    private void saveChanges() {
        try {
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8));
            StringBuilder headerLine = new StringBuilder();
            for (int i = 0; i < tableModel.getColumnCount(); i++) {
                if (i > 0) {
                    headerLine.append(",");
                }
                headerLine.append(tableModel.getColumnName(i));
            }
            writer.write(headerLine.toString());
            writer.newLine();
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                StringBuilder dataLine = new StringBuilder();
                for (int j = 0; j < tableModel.getColumnCount(); j++) {
                    if (j > 0) {
                        dataLine.append(",");
                    }
                    dataLine.append(tableModel.getValueAt(i, j));
                }
                writer.write(dataLine.toString());
                writer.newLine();
            }
            writer.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private class ReloadButtonActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            loadAttributes();
        }
    }

    public static void main(String[] args) {
        new SimsRNG();
    }
}
