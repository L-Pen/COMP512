package Client;

import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;
import java.io.*;
import Client.RMIClient;

public class testTCP {

    public static void testAddFlight() throws UnknownHostException, IOException, InterruptedException {
        String commandString = "AddFlight,123,456,789";
        String response = RMIClient.sendTcpRequest(9034, commandString);
        String response2 = RMIClient.sendTcpRequest(9034, commandString);
        System.out.println("Response from server: " + response + "," + response2);
    }

    public static void main(String[] args) throws UnknownHostException, IOException, InterruptedException {
        testAddFlight();
    }

}
