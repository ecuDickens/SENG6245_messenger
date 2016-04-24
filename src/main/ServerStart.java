package main;

import model.ServerThreadPool;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * Chat server runner.
 */
public class ServerStart {
    public static void main(String args[]) {
        try {
            new ServerThreadPool(new ServerSocket(Integer.parseInt(args[0])));
        } catch (IOException e) {
            System.out.println("Error in ServerStart.main: " +e.getMessage());
        }
    }
}