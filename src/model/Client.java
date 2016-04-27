package model;

import javax.swing.*;
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
import static model.enums.MessageTypeEnum.SESSION_EXIT;

public class Client {

    private Socket socket;
    private ClientThread thread;
    private String sourceUserName;

    private JFrame loginFrame;
    private JTextField loginNameField;
    private Map<String, ChatBox> userNameToChatBox;

    public Client(final Socket socket,
                  final DataInputStream in,
                  final DataOutputStream out) {
        this.socket = socket;
        this.thread = new ClientThread(this, in, out);
        this.userNameToChatBox = new HashMap<>();

        displayLogin();
    }

    public String getSourceUserName() {
        return sourceUserName;
    }

    private void displayLogin() {
        loginNameField = new JTextField(15);
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
        loginFrame.setSize(300, 300);
        loginFrame.setVisible(true);
    }

    public void setLoginMessage(final String message) {
        loginNameField.setText(message);
    }

    public void displayUsers(final Set<String> userNames) {
        final JFrame userNameFrame = new JFrame();

        for (String userName : userNames) {
            final JButton selectUserButton = new JButton(userName);
            selectUserButton.addActionListener(event ->
                thread.send(new Message()
                    .withType(INVITE)
                    .withSourceUser(sourceUserName)
                    .withTargetUser(userName)));
            userNameFrame.add(selectUserButton);
        }

        final JButton refreshButton = new JButton("Refresh Users");
        refreshButton.addActionListener(event ->
                thread.send(new Message()
                    .withType(GET_USERS)
                    .withSourceUser(sourceUserName)));

        final JPanel userNamePanel = new JPanel(new GridBagLayout());
        userNamePanel.add(new JLabel("Active users, select one to chat."));

        userNameFrame.add(userNamePanel);
        userNameFrame.add(refreshButton);
        userNameFrame.setSize(300, 300);
        userNameFrame.setVisible(true);
        loginFrame.setVisible(false);
    }

    public void addMessage(final String userName, final String text) {
        if (userNameToChatBox.containsKey(userName)) {
            userNameToChatBox.get(userName).getChatBox().append(text + "\n");
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

        public ChatBox(final String targetUserName) {
            this.targetUserName = targetUserName;
            displayChatBox();
        }

        private JTextArea getChatBox() {
            return chatBox;
        }
        private JFrame getChatFrame() {
            return chatFrame;
        }

        public void displayChatBox() {
            chatFrame = new JFrame();
            final JTextField messageBox = new JTextField(30);
            messageBox.requestFocusInWindow();

            final JButton sendButton = new JButton("Send Message");
            sendButton.addActionListener(event -> {
                if (messageBox.getText().length() > 1) {
                    thread.send(new Message()
                            .withType(MESSAGE)
                            .withSourceUser(sourceUserName)
                            .withTargetUser(targetUserName)
                            .withText(messageBox.getText()));
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
            chatFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            chatFrame.setSize(470, 300);
            chatFrame.setVisible(true);
        }
    }
}
