package model;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static model.enums.MessageTypeEnum.GET_USERS;
import static model.enums.MessageTypeEnum.INVITE;
import static model.enums.MessageTypeEnum.LOGIN;
import static model.enums.MessageTypeEnum.MESSAGE;
import static model.enums.MessageTypeEnum.NOT_TYPING;
import static model.enums.MessageTypeEnum.SESSION_EXIT;
import static model.enums.MessageTypeEnum.TYPING;

public class Client {

    private Socket socket;
    private ClientThread thread;
    private String sourceUserName;

    private JFrame loginFrame;
    private JFrame mainFrame;
    private DefaultListModel<String> listModel;
    private Map<String, ChatBox> userNameToChatBox;

    public Client(final Socket socket,
                  final DataInputStream in,
                  final DataOutputStream out) {
        this.socket = socket;
        this.thread = new ClientThread(this, in, out);
        this.userNameToChatBox = new HashMap<>();
        this.mainFrame = null;
        displayLogin();
    }

    public String getSourceUserName() {
        return sourceUserName;
    }
    public JFrame getLoginFrame() {
        return loginFrame;
    }
    public JFrame getMainFrame() {
        return mainFrame;
    }
    public JFrame getChatFrame(final String userName) {
        return userNameToChatBox.get(userName).getChatFrame();
    }

    private void displayLogin() {
        final JTextField loginNameField = new JTextField(15);
        final JButton loginButton = new JButton("Login");
        loginButton.addActionListener(event -> {
            sourceUserName = loginNameField.getText();
            if (sourceUserName.length() > 1) {
                thread.send(new Message().withType(LOGIN).withSourceUser(sourceUserName));
            }
        });

        final JPanel loginPanel = new JPanel(new GridBagLayout());
        loginPanel.add(new JLabel("Pick a username:"));
        loginPanel.add(loginNameField);

        loginFrame = new JFrame();
        loginFrame.add(BorderLayout.CENTER, loginPanel);
        loginFrame.add(BorderLayout.SOUTH, loginButton);
        loginFrame.pack();
        loginFrame.setVisible(true);
    }

    public void alert(final JFrame jFrame, final String subject, final String message) {
        JOptionPane.showMessageDialog(jFrame, message, subject, JOptionPane.PLAIN_MESSAGE);
    }

    public void displayMain(final Set<String> userNames) {
        // If already created, this call is simply to update the list.
        if (null != mainFrame) {
            listModel.clear();
            for (String userName : userNames) {
                if (!sourceUserName.equals(userName)) {
                    listModel.addElement(userName);
                }
            }
            return;
        }

        listModel = new DefaultListModel<>();
        final JList<String> userNameList = new JList<>(listModel);
        final JButton refreshButton = new JButton("Refresh");
        final JButton inviteButton = new JButton("Invite");

        for (String userName : userNames) {
            if (!sourceUserName.equals(userName)) {
                listModel.addElement(userName);
            }
        }

        userNameList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userNameList.setSelectedIndex(0);
        userNameList.setVisibleRowCount(5);

        // Set up refresh button.
        refreshButton.setActionCommand("Refresh");
        refreshButton.addActionListener(event -> thread.send(new Message()
                .withType(GET_USERS)
                .withSourceUser(sourceUserName)));

        // Set up Invite button.
        inviteButton.setActionCommand("Invite");
        inviteButton.addActionListener(event -> thread.send(new Message()
                .withType(INVITE)
                .withSourceUser(sourceUserName)
                .withTargetUser(userNameList.getSelectedValue())));

        // Add everything to the content pane.
        final JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.add(refreshButton);
        buttonPane.add(Box.createHorizontalStrut(5));
        buttonPane.add(new JSeparator(SwingConstants.VERTICAL));
        buttonPane.add(Box.createHorizontalStrut(5));
        buttonPane.add(inviteButton);
        buttonPane.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        final JPanel newContentPane = new JPanel(new BorderLayout());
        newContentPane.add(new JScrollPane(userNameList), BorderLayout.CENTER);
        newContentPane.add(buttonPane, BorderLayout.PAGE_END);
        newContentPane.setOpaque(true);

        mainFrame = new JFrame(sourceUserName + ": Active Users List");
        mainFrame.setContentPane(newContentPane);
        mainFrame.pack();
        mainFrame.setVisible(true);
        loginFrame.setVisible(false);
    }

    public int displayInvite(final String userName) {
        return JOptionPane.showConfirmDialog(mainFrame,
                "Incoming chat request from " +userName+ ".  Accept?",
                "Chat request",
                JOptionPane.YES_NO_OPTION);
    }

    public void addMessage(final String userName, final String text) {
        if (userNameToChatBox.containsKey(userName)) {
            userNameToChatBox.get(userName).getChatBox().append(String.format("<%s>: %s\n", userName, text));
        }
    }

    public void openChatBox(final String userName) {
        if (userNameToChatBox.containsKey(userName)) {
            userNameToChatBox.get(userName).getChatFrame().setVisible(true);
        } else {
            userNameToChatBox.put(userName, new ChatBox(userName));
        }
    }

    public void stop() {
        thread.close();
        try {
            if (null != socket) {
                socket.close();
            }
        } catch (IOException e) {
            System.out.println("Error in Client.stop: " + e.getMessage());
        }
    }

    private class ChatBox {
        private String targetUserName;
        private JFrame chatFrame;
        private JTextArea chatBox;
        private boolean isTyping;

        public ChatBox(final String targetUserName) {
            this.targetUserName = targetUserName;
            this.isTyping = false;
            displayChatBox();
        }

        private JTextArea getChatBox() {
            return chatBox;
        }
        private JFrame getChatFrame() {
            return chatFrame;
        }

        public void displayChatBox() {
            chatFrame = new JFrame(sourceUserName + " -> " + targetUserName);
            final JTextField messageBox = new JTextField(30);
            messageBox.requestFocusInWindow();
            messageBox.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    if (!isTyping) {
                        thread.send(new Message()
                                .withType(TYPING)
                                .withSourceUser(sourceUserName)
                                .withTargetUser(targetUserName));
                        isTyping = true;
                    }
                }
                @Override
                public void removeUpdate(DocumentEvent e) {
                    if (e.getDocument().getLength() <= 0 && isTyping) {
                        thread.send(new Message()
                                .withType(NOT_TYPING)
                                .withSourceUser(sourceUserName)
                                .withTargetUser(targetUserName));
                        isTyping = false;
                    }
                }
                @Override
                public void changedUpdate(DocumentEvent e) {
                    if (e.getDocument().getLength() > 0 && !isTyping) {
                        thread.send(new Message()
                                .withType(TYPING)
                                .withSourceUser(sourceUserName)
                                .withTargetUser(targetUserName));
                        isTyping = true;
                    }
                }
            });

            final JButton sendButton = new JButton("Send Message");
            sendButton.addActionListener(event -> {
                if (messageBox.getText().length() > 1) {
                    thread.send(new Message()
                            .withType(MESSAGE)
                            .withSourceUser(sourceUserName)
                            .withTargetUser(targetUserName)
                            .withText(messageBox.getText()));
                    chatBox.append(String.format("<%s>: %s\n", sourceUserName, messageBox.getText()));
                    isTyping = false;
                    messageBox.setText("");
                }
                messageBox.requestFocusInWindow();
            });

            final JButton exitButton = new JButton("Close Chat");
            exitButton.addActionListener(event -> {
                thread.send(new Message()
                        .withType(SESSION_EXIT)
                        .withSourceUser(sourceUserName)
                        .withTargetUser(targetUserName));
                chatFrame.setVisible(false);
            });

            chatBox = new JTextArea();
            chatBox.setEditable(false);
            chatBox.setFont(new Font("Serif", Font.PLAIN, 15));
            chatBox.setLineWrap(true);

            final GridBagConstraints left = new GridBagConstraints();
            left.anchor = GridBagConstraints.LINE_START;
            left.fill = GridBagConstraints.HORIZONTAL;
            left.weightx = 512.0D;
            left.weighty = 1.0D;

            final GridBagConstraints right = new GridBagConstraints();
            right.insets = new Insets(0, 10, 0, 0);
            right.anchor = GridBagConstraints.LINE_END;
            right.fill = GridBagConstraints.NONE;
            right.weightx = 1.0D;
            right.weighty = 1.0D;

            final JPanel southPanel = new JPanel();
            southPanel.setBackground(Color.GRAY);
            southPanel.setLayout(new GridBagLayout());
            southPanel.add(messageBox, left);
            southPanel.add(sendButton, right);

            final JPanel mainPanel = new JPanel();
            mainPanel.setLayout(new BorderLayout());
            mainPanel.add(new JScrollPane(chatBox), BorderLayout.CENTER);
            mainPanel.add(BorderLayout.SOUTH, southPanel);

            chatFrame.add(mainPanel);
            chatFrame.setSize(470, 300);
            chatFrame.setVisible(true);
        }
    }
}
