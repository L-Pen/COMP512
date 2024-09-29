package Client;

import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;
import java.io.*;

public class testTCP {

    public static void testAddFlight() throws UnknownHostException, IOException {

        Socket client1 = new Socket("tr-open-03", 9030);
        PrintWriter outToServer = new PrintWriter(client1.getOutputStream(), true);
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(client1.getInputStream()));

        Socket client2 = new Socket("tr-open-03", 9030);
        PrintWriter outToServer2 = new PrintWriter(client2.getOutputStream(), true);
        BufferedReader inFromServer2 = new BufferedReader(new InputStreamReader(client2.getInputStream()));

        outToServer.println("AddFlight,101,200,150");
        outToServer2.println("queryFlight,101");

        boolean test1 = inFromServer2.readLine().equals("150");
        System.out.println("test1: " + test1);
        if(test1) {
            System.out.println("Test 1 passed");
        } else {
            System.out.println("Test 1 failed");
        }

        client1.close();
        client2.close();

    }

    public static void main(String[] args) throws UnknownHostException, IOException {
        testAddFlight();
    }

}
