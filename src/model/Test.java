package model;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

public class Test {
    public Test() {
        final DefaultListModel<String> listModel = new DefaultListModel<>();
        final JList<String> list = new JList<>(listModel);
        final JButton fireButton = new JButton("Fire");
        final JButton hireButton = new JButton("Hire");
        final JTextField employeeName = new JTextField(10);

        // Add elements.
        listModel.addElement("Jane Doe");
        listModel.addElement("John Smith");
        listModel.addElement("Kathy Green");

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setSelectedIndex(0);
        list.setVisibleRowCount(5);
        list.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                if (list.getSelectedIndex() == -1) {
                    fireButton.setEnabled(false);
                } else {
                    fireButton.setEnabled(true);
                }
            }
        });

        // Set up fire button.
        fireButton.setActionCommand("Fire");
        fireButton.addActionListener(event -> {
            int index = list.getSelectedIndex();
            listModel.remove(index);
            int size = listModel.getSize();
            if (size == 0) {
                fireButton.setEnabled(false);
            } else {
                if (index == listModel.getSize()) {
                    index--;
                }
                list.setSelectedIndex(index);
                list.ensureIndexIsVisible(index);
            }
        });

        // Set up hire button.
        hireButton.setActionCommand("Hire");
        hireButton.setEnabled(false);
        hireButton.addActionListener(event -> {
            String name = employeeName.getText();
            if (name.equals("") || listModel.contains(name)) {
                Toolkit.getDefaultToolkit().beep();
                employeeName.requestFocusInWindow();
                employeeName.selectAll();
                return;
            }

            int index = list.getSelectedIndex();
            if (index == -1) {
                index = 0;
            } else {
                index++;
            }

            listModel.insertElementAt(employeeName.getText(), index);
            employeeName.requestFocusInWindow();
            employeeName.setText("");
            list.setSelectedIndex(index);
            list.ensureIndexIsVisible(index);
        });

        // Set up employee name.
        employeeName.addActionListener(event -> {
            final String name = employeeName.getText();
            if (name.equals("") || listModel.contains(name)) {
                Toolkit.getDefaultToolkit().beep();
                employeeName.requestFocusInWindow();
                employeeName.selectAll();
                return;
            }
            int index = list.getSelectedIndex();
            if (index == -1) {
                index = 0;
            } else {
                index++;
            }

            listModel.insertElementAt(employeeName.getText(), index);
            employeeName.requestFocusInWindow();
            employeeName.setText("");
            list.setSelectedIndex(index);
            list.ensureIndexIsVisible(index);
        });
        employeeName.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                if (!hireButton.isEnabled()) {
                    hireButton.setEnabled(true);
                }
            }
            @Override
            public void removeUpdate(DocumentEvent e) {
                if (e.getDocument().getLength() <= 0) {
                    hireButton.setEnabled(false);
                }
            }
            @Override
            public void changedUpdate(DocumentEvent e) {
                if (e.getDocument().getLength() > 0 && !hireButton.isEnabled()) {
                    hireButton.setEnabled(true);
                }
            }
        });

        // Add everything to the content pane.
        final JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.add(fireButton);
        buttonPane.add(Box.createHorizontalStrut(5));
        buttonPane.add(new JSeparator(SwingConstants.VERTICAL));
        buttonPane.add(Box.createHorizontalStrut(5));
        buttonPane.add(employeeName);
        buttonPane.add(hireButton);
        buttonPane.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        final JPanel newContentPane = new JPanel(new BorderLayout());
        newContentPane.add(new JScrollPane(list), BorderLayout.CENTER);
        newContentPane.add(buttonPane, BorderLayout.PAGE_END);
        newContentPane.setOpaque(true);

        final JFrame frame = new JFrame("ListDemo");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setContentPane(newContentPane);
        frame.pack();
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(Test::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        new Test();
    }
}