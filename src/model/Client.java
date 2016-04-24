package model;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import static model.Message.END_CHAT_MESSAGE;

public class Client {

    private Socket socket;
    private ClientReceiveThread receive;
    private DataOutputStream streamOut;

    private JFrame newFrame;
    private JButton sendMessage;
    private JTextField messageBox;
    private JTextArea chatBox;
    private JTextField usernameChooser;
    private JFrame preFrame;

    private String username;

    public Client(final Socket socket,
                  final DataInputStream in,
                  final DataOutputStream out) {
        this.socket = socket;
        this.receive = new ClientReceiveThread(this, in);
        this.streamOut = out;
        this.newFrame = new JFrame();
        this.preFrame = new JFrame();
        this.usernameChooser = new JTextField(15);

        newFrame.setVisible(false);
        final JLabel chooseUsernameLabel = new JLabel("Pick a username:");

        final JButton enterServer = new JButton("Enter Chat Server");
        enterServer.addActionListener(new enterServerButtonListener());

        final JPanel prePanel = new JPanel(new GridBagLayout());

        prePanel.add(chooseUsernameLabel);
        prePanel.add(usernameChooser);
        preFrame.add(BorderLayout.CENTER, prePanel);
        preFrame.add(BorderLayout.SOUTH, enterServer);
        preFrame.setSize(300, 300);
        preFrame.setVisible(true);
    }

    public void handle(final String msg) {
        if (msg.equals(END_CHAT_MESSAGE)) {
            System.out.println("Good bye. Press RETURN to exit ...");
            stop();
        } else {
            chatBox.append(msg + "\n");
        }
    }

    public void stop() {
        receive.close();
        try {
            if (null != streamOut) {
                streamOut.close();
            }
            if (null != socket) {
                socket.close();
            }
        } catch (IOException e) {
            System.out.println("Error in Client.stop: " + e.getMessage());
        }
    }

    public void display() {
        final JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());

        final JPanel southPanel = new JPanel();
        southPanel.setBackground(Color.BLUE);
        southPanel.setLayout(new GridBagLayout());

        messageBox = new JTextField(30);
        messageBox.requestFocusInWindow();

        sendMessage = new JButton("Send Message");
        sendMessage.addActionListener(new sendMessageButtonListener());

        chatBox = new JTextArea();
        chatBox.setEditable(false);
        chatBox.setFont(new Font("Serif", Font.PLAIN, 15));
        chatBox.setLineWrap(true);

        mainPanel.add(new JScrollPane(chatBox), BorderLayout.CENTER);

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

        southPanel.add(messageBox, left);
        southPanel.add(sendMessage, right);

        mainPanel.add(BorderLayout.SOUTH, southPanel);

        newFrame.add(mainPanel);
        newFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        newFrame.setSize(470, 300);
        newFrame.setVisible(true);
    }

    class sendMessageButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            if (messageBox.getText().length() < 1) {
                // do nothing
            } else if (messageBox.getText().equals(".clear")) {
                chatBox.setText("Cleared all messages\n");
                messageBox.setText("");
            } else {
                try {
                    final String text = "<" + username + ">:  " + messageBox.getText();
                    streamOut.writeUTF(text);
                    streamOut.flush();
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    chatBox.append("Error: " + e.getMessage() + "\n");
                }
                messageBox.setText("");
            }
            messageBox.requestFocusInWindow();
        }
    }

    class enterServerButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            username = usernameChooser.getText();
            if (username.length() < 1) {
                System.out.println("No!");
            } else {
                preFrame.setVisible(false);
                display();
            }
        }
    }
}
