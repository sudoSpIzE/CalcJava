import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

public class SimpleCalculator extends JFrame {

    private JTextField display;
    private JTextArea history;
    private JTextField varA, varB, varC;
    private double memory = 0;
    private double lastResult = 0;

    private static final String HISTORY_FILE = "calc_history.txt";
    private static final String VARS_FILE = "calc_vars.txt";

    public SimpleCalculator() {
        setTitle("🧮 Умный калькулятор");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);

        createUI();
        loadData();

        setVisible(true);
    }

    private void createUI() {
        // Основной layout
        setLayout(new BorderLayout(5, 5));

        // ========== ВЕРХ: ПОЛЕ ВВОДА ==========
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        display = new JTextField();
        display.setFont(new Font("Arial", Font.BOLD, 24));
        display.setHorizontalAlignment(JTextField.RIGHT);
        display.addActionListener(e -> calculate());
        topPanel.add(display, BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);

        // ========== ЦЕНТР: КНОПКИ И ИСТОРИЯ ==========
        JSplitPane centerPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        centerPane.setDividerLocation(400);

        // Левая панель - кнопки
        centerPane.setLeftComponent(createButtonPanel());

        // Правая панель - история
        centerPane.setRightComponent(createHistoryPanel());

        add(centerPane, BorderLayout.CENTER);

        // ========== НИЗ: ПЕРЕМЕННЫЕ ==========
        add(createVariablePanel(), BorderLayout.SOUTH);
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Калькулятор"));

        // Сетка кнопок
        JPanel grid = new JPanel(new GridLayout(5, 4, 5, 5));
        grid.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Массив кнопок
        String[] buttons = {
                "7", "8", "9", "/",
                "4", "5", "6", "*",
                "1", "2", "3", "-",
                "0", ".", "=", "+",
                "C", "CE", "(", ")"
        };

        for (String text : buttons) {
            JButton btn = new JButton(text);
            btn.setFont(new Font("Arial", Font.BOLD, 16));
            btn.addActionListener(new ButtonListener());
            grid.add(btn);
        }

        panel.add(grid, BorderLayout.CENTER);


        JPanel memoryPanel = new JPanel(new GridLayout(1, 5, 5, 5));
        String[] memoryButtons = {"MC", "MR", "M+", "M-", "MS"};
        for (String text : memoryButtons) {
            JButton btn = new JButton(text);
            btn.addActionListener(new MemoryButtonListener());
            memoryPanel.add(btn);
        }

        panel.add(memoryPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("История операций"));

        history = new JTextArea(15, 25);
        history.setFont(new Font("Monospaced", Font.PLAIN, 12));
        history.setEditable(false);

        JScrollPane scroll = new JScrollPane(history);
        panel.add(scroll, BorderLayout.CENTER);

        // Кнопки управления историей
        JPanel controlPanel = new JPanel();

        JButton clearBtn = new JButton("Очистить");
        clearBtn.addActionListener(e -> history.setText(""));

        JButton explainBtn = new JButton("Объяснить");
        explainBtn.addActionListener(e -> showExplanation());

        controlPanel.add(clearBtn);
        controlPanel.add(explainBtn);

        panel.add(controlPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createVariablePanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Переменные"));

        // Переменная A
        panel.add(new JLabel("A:"));
        varA = new JTextField("0", 8);
        panel.add(varA);

        JButton saveA = new JButton("Сохранить");
        saveA.addActionListener(e -> saveVariable("A", varA.getText()));
        panel.add(saveA);

        JButton useA = new JButton("Вставить A");
        useA.addActionListener(e -> display.setText(display.getText() + varA.getText()));
        panel.add(useA);

        // Переменная B
        panel.add(new JLabel("B:"));
        varB = new JTextField("0", 8);
        panel.add(varB);

        JButton saveB = new JButton("Сохранить");
        saveB.addActionListener(e -> saveVariable("B", varB.getText()));
        panel.add(saveB);

        JButton useB = new JButton("Вставить B");
        useB.addActionListener(e -> display.setText(display.getText() + varB.getText()));
        panel.add(useB);

        // Переменная C
        panel.add(new JLabel("C:"));
        varC = new JTextField("0", 8);
        panel.add(varC);

        JButton saveC = new JButton("Сохранить");
        saveC.addActionListener(e -> saveVariable("C", varC.getText()));
        panel.add(saveC);

        JButton useC = new JButton("Вставить C");
        useC.addActionListener(e -> display.setText(display.getText() + varC.getText()));
        panel.add(useC);

        return panel;
    }

    // ========== ОБРАБОТЧИКИ СОБЫТИЙ ==========

    private class ButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            String cmd = ((JButton) e.getSource()).getText();

            switch (cmd) {
                case "=" -> calculate();
                case "C" -> display.setText("");
                case "CE" -> {
                    display.setText("");
                    history.setText("");
                    saveHistory();
                }
                case "(" -> display.setText(display.getText() + "(");
                case ")" -> display.setText(display.getText() + ")");
                default -> display.setText(display.getText() + cmd);
            }
        }
    }

    private class MemoryButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            String cmd = ((JButton) e.getSource()).getText();

            try {
                switch (cmd) {
                    case "MC" -> {
                        memory = 0;
                        addToHistory("Память очищена");
                    }
                    case "MR" -> display.setText(display.getText() + memory);
                    case "M+" -> {
                        double current = getCurrentValue();
                        memory += current;
                        addToHistory("M+ : " + current + " (память = " + memory + ")");
                    }
                    case "M-" -> {
                        double current = getCurrentValue();
                        memory -= current;
                        addToHistory("M- : " + current + " (память = " + memory + ")");
                    }
                    case "MS" -> {
                        double current = getCurrentValue();
                        memory = current;
                        addToHistory("MS : сохранено " + current);
                    }
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(SimpleCalculator.this,
                        "Введите число перед операцией с памятью");
            }
        }
    }

    private void calculate() {
        try {
            String expr = display.getText().trim();

            if (expr.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Введите выражение");
                return;
            }

            // Замена переменных
            expr = expr.replace("A", varA.getText())
                    .replace("B", varB.getText())
                    .replace("C", varC.getText());

            // Простой парсер для базовых операций
            double result = simpleEval(expr);
            lastResult = result;

            // Форматирование
            String resultStr;
            if (result == (long) result) {
                resultStr = String.format("%d", (long) result);
            } else {
                resultStr = String.format("%.6f", result).replaceAll("0*$", "").replaceAll("\\.$", "");
            }

            // Добавление в историю
            String time = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
            history.append(time + " | " + display.getText() + " = " + resultStr + "\n");

            // Прокрутка
            history.setCaretPosition(history.getDocument().getLength());

            // Показ результата
            display.setText(resultStr);

            // Сохранение
            saveHistory();
            saveVariables();

        } catch (ArithmeticException ex) {
            display.setText("Ошибка: " + ex.getMessage());
        } catch (Exception ex) {
            display.setText("Ошибка вычисления");
            history.append("ОШИБКА: " + display.getText() + "\n");
        }
    }


    private double simpleEval(String expr) throws ArithmeticException {
        expr = expr.replaceAll("\\s+", "");

        

        int opIndex = -1;
        char operator = ' ';

        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);
            if (c == '+' || c == '-' || c == '*' || c == '/') {
               
                if (i > 0 && Character.isDigit(expr.charAt(i-1))) {
                    opIndex = i;
                    operator = c;
                    break;
                }
            }
        }

        if (opIndex == -1) {
            
            try {
                return Double.parseDouble(expr);
            } catch (NumberFormatException e) {
                throw new ArithmeticException("Неверное выражение");
            }
        }

      
        String leftStr = expr.substring(0, opIndex);
        String rightStr = expr.substring(opIndex + 1);

        double left, right;
        try {
            left = Double.parseDouble(leftStr);
            right = Double.parseDouble(rightStr);
        } catch (NumberFormatException e) {
            throw new ArithmeticException("Неверные числа в выражении");
        }

        // Выполняем операцию
        return switch (operator) {
            case '+' -> left + right;
            case '-' -> left - right;
            case '*' -> left * right;
            case '/' -> {
                if (right == 0) throw new ArithmeticException("Деление на ноль");
                yield left / right;
            }
            default -> throw new ArithmeticException("Неизвестный оператор");
        };
    }

    private void showExplanation() {
        String expr = display.getText();
        String explanation = """
            📝 Объяснение вычисления
            
            Текущее выражение: %s
            
            Как работает калькулятор:
            1. Заменяет переменные A, B, C их значениями
            2. Вычисляет выражение слева направо
            3. Поддерживает операции: +, -, *, /
            4. Результат сохраняется в истории
            
            Примеры:
            2+2 = 4
            10-3 = 7
            5*4 = 20
            20/4 = 5
            
            Переменные:
            A = %s
            B = %s
            C = %s
            
            Память: %s
            """.formatted(expr, varA.getText(), varB.getText(), varC.getText(), memory);

        JOptionPane.showMessageDialog(this, explanation, "Объяснение",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void saveVariable(String name, String value) {
        try {
            double val = Double.parseDouble(value);
            JOptionPane.showMessageDialog(this,
                    "Переменная " + name + " = " + val + " сохранена");
            saveVariables();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                    "Введите число для переменной " + name);
        }
    }

    private double getCurrentValue() throws NumberFormatException {
        String text = display.getText().trim();
        if (text.isEmpty()) {
            throw new NumberFormatException("Пустое поле");
        }
        return Double.parseDouble(text);
    }

    private void addToHistory(String message) {
        String time = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
        history.append(time + " | " + message + "\n");
        history.setCaretPosition(history.getDocument().getLength());
    }

    // ========== СОХРАНЕНИЕ И ЗАГРУЗКА ==========

    private void saveHistory() {
        try (PrintWriter writer = new PrintWriter(HISTORY_FILE)) {
            writer.print(history.getText());
        } catch (IOException e) {
            // Игнорируем
        }
    }

    private void loadHistory() {
        try (BufferedReader reader = new BufferedReader(new FileReader(HISTORY_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                history.append(line + "\n");
            }
        } catch (IOException e) {
            // Файла нет - это нормально
        }
    }

    private void saveVariables() {
        try (PrintWriter writer = new PrintWriter(VARS_FILE)) {
            writer.println("A=" + varA.getText());
            writer.println("B=" + varB.getText());
            writer.println("C=" + varC.getText());
        } catch (IOException e) {
            
        }
    }

    private void loadVariables() {
        try (BufferedReader reader = new BufferedReader(new FileReader(VARS_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("=");
                if (parts.length == 2) {
                    switch (parts[0]) {
                        case "A" -> varA.setText(parts[1]);
                        case "B" -> varB.setText(parts[1]);
                        case "C" -> varC.setText(parts[1]);
                    }
                }
            }
        } catch (IOException e) {
           
        }
    }

    private void loadData() {
        loadHistory();
        loadVariables();
    }

  

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new SimpleCalculator();
        });
    }
}
