package Client;

import Client.RMIClient;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;
import java.io.*;

public class testTCP {

    public static void testAddFlight() throws UnknownHostException, IOException {

        Socket socket = new Socket("localhost", 9030);
        PrintWriter outToServer = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        outToServer.println("AddFlight,1,1,1");
        String res = inFromServer.readLine();
        System.out.println("RESULT OF TEST: " + res);
        socket.close();

    }

    public static void main(String[] args) throws UnknownHostException, IOException {
        testAddFlight();
    }

}
