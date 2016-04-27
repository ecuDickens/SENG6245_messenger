package main;

import model.Client;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import static javax.swing.UIManager.getSystemLookAndFeelClassName;
import static javax.swing.UIManager.setLookAndFeel;

/**
 * Chat client runner.
 */
public class ClientStart {
    public static void main(final String args[]) throws IOException {
        SwingUtilities.invokeLater(() -> {
            try {
                setLookAndFeel(getSystemLookAndFeelClassName());
                final Socket socket = new Socket("localhost", 1234);
                new Client(socket, new DataInputStream(socket.getInputStream()), new DataOutputStream(socket.getOutputStream()));
            } catch (Exception e) {
                System.out.println("Error in ClientStart.main: " + e.getMessage());
            }
        });
    }
}