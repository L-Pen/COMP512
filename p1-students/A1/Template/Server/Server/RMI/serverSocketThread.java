package Server.RMI;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class serverSocketThread extends Thread {
    Socket clientSocket;
    RMIMiddleware server;

    serverSocketThread(Socket clientSocket, RMIMiddleware server) {
        this.clientSocket = clientSocket;
        this.server = server;
    }

    public void run() {
        try {
            BufferedReader inFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter outToClient = new PrintWriter(clientSocket.getOutputStream(), true);
            String message = null;
            while ((message = inFromClient.readLine()) != null) {
                System.out.println("message:" + message);
                String result = "Working!";
                String[] params = message.split(",");
                String commandName = params[0];
                if (commandName.inc)
                    // check which function client is trying to call eg AddFlights
                    // forward message to FlightResourceManager thru TCP
                    // wait for res from resource manager...
                    // on res, use outToClient to send res back to client

                    outToClient.println(result);
            }
            clientSocket.close();
        } catch (IOException e) {
        }
    }

    private String sendMessageToSocket(Socket socket, String command) throws IOException {
        // create buffer to send message to RM socket
        PrintWriter outToServer = new PrintWriter(socket.getOutputStream(), true);
        // create buffer to receive messages from RM socket
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        // send to RM socket
        outToServer.println(command);
        // receive from RM socket
        String res = inFromServer.readLine();
        System.out.println("result: " + res);
        return res;
    }

}
