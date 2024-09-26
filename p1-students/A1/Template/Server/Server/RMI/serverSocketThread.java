package Server.RMI;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class serverSocketThread extends Thread {
    Socket socket;
    RMIMiddleware server;

    serverSocketThread(Socket socket, RMIMiddleware server) {
        this.socket = socket;
        this.server = server;
    }

    public void run() {
        try {
            BufferedReader inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter outToClient = new PrintWriter(socket.getOutputStream(), true);
            String message = null;
            while ((message = inFromClient.readLine()) != null) {
                System.out.println("message:" + message);
                String result = "Working!";
                String[] params = message.split(",");

                // check which function client is trying to call eg AddFlights
                // forward message to FlightResourceManager thru TCP
                // wait for res from resource manager...
                // on res, use outToClient to send res back to client

                outToClient.println(result);
            }
            socket.close();
        } catch (IOException e) {
        }
    }

}
