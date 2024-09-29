package Client;

import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;
import java.io.*;
import Client.RMIClient;

public class testTCP {

    public static void testAddFlight() throws UnknownHostException, IOException, InterruptedException {
        String commandString1 = "AddFlight,123,100,789";
        String commandString2 = "AddFlight,123,200,789";
        String commandString3 = "queryFlight,123";
        String response = RMIClient.sendTcpRequest(9034, commandString1);
        String response2 = RMIClient.sendTcpRequest(9034, commandString2);
        String response3 = RMIClient.sendTcpRequest(9034, commandString3);
        if (response3.equals("300")) {
            System.out.println("Test Passed");
        } else {
            System.out.println("Test Failed");
        }
    }

    public static void main(String[] args) throws UnknownHostException, IOException, InterruptedException {
        testAddFlight();
    }

}
