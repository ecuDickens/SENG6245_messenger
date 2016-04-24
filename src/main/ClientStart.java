package main;

import model.Client;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

/**
 * Chat client runner.
 */
public class ClientStart {
    public static void main(String args[]) throws IOException {
        try {
            final Socket socket = new Socket(args[0], Integer.parseInt(args[1]));
            new Client(socket, new DataInputStream(socket.getInputStream()),
                    new DataOutputStream(socket.getOutputStream()),
                    new BufferedReader(new InputStreamReader(System.in)));
        } catch (IOException e) {
            System.out.println("Error in ClientStart.main: " + e.getMessage());
        }
    }
}