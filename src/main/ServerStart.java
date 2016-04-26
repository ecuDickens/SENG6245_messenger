package main;

import model.Server;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * Chat server runner.
 */
public class ServerStart {
    public static void main(String args[]) {
        try {
            new Server(new ServerSocket(1234));
        } catch (IOException e) {
            System.out.println("Error in ServerStart.main: " +e.getMessage());
        }
    }
}