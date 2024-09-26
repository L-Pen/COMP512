package Server.RMI;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class serverSocketThread extends Thread {
    Socket clientSocket;
    RMIMiddleware server;

    serverSocketThread(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    public void run() {
        try {
            BufferedReader inFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter outToClient = new PrintWriter(clientSocket.getOutputStream(), true);
            String message = null;
            while ((message = inFromClient.readLine()) != null) {
                System.out.println("message:" + message);
                String result = "";
                String[] params = message.split(",");
                String commandName = params[0];
                // check which function client is trying to call eg AddFlights
                // forward message to FlightResourceManager thru TCP
                // wait for res from resource manager...
                // on res, use outToClient to send res back to client
                result = routeMessage(commandName, message);

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

    private String routeMessage(String commandName, String message) throws IOException {
        String res = "error";
        switch (commandName) {
            case "AddFlight":
                res = sendMessageToSocket(RMIMiddleware.flightsSocket, message);
                if (checkSuccess(res)) {
                    sendMessageToSocket(RMIMiddleware.customersSocket, message);
                }
                break;
            case "AddCars":
                res = sendMessageToSocket(RMIMiddleware.carsSocket, message);
                if (checkSuccess(res)) {
                    sendMessageToSocket(RMIMiddleware.customersSocket, message);
                }
                break;
            case "AddRooms":
                res = sendMessageToSocket(RMIMiddleware.roomsSocket, message);
                if (checkSuccess(res)) {
                    sendMessageToSocket(RMIMiddleware.customersSocket, message);
                }
                break;
            case "AddCustomer":
                res = sendMessageToSocket(RMIMiddleware.customersSocket, message);
                String payload = "AddCustomer," + res;
                sendMessageToSocket(RMIMiddleware.flightsSocket, payload);
                sendMessageToSocket(RMIMiddleware.carsSocket, payload);
                sendMessageToSocket(RMIMiddleware.roomsSocket, payload);
                break;
            case "AddCustomerID":
                res = sendMessageToSocket(RMIMiddleware.customersSocket, message);
                if (checkSuccess(res)) {
                    sendMessageToSocket(RMIMiddleware.flightsSocket, message);
                    sendMessageToSocket(RMIMiddleware.carsSocket, message);
                    sendMessageToSocket(RMIMiddleware.roomsSocket, message);
                }
                break;
            case "DeleteFlight":
                res = sendMessageToSocket(RMIMiddleware.flightsSocket, message);
                if (checkSuccess(res)) {
                    sendMessageToSocket(RMIMiddleware.customersSocket, message);
                }
                break;
            case "DeleteCars":
                res = sendMessageToSocket(RMIMiddleware.carsSocket, message);
                if (checkSuccess(res)) {
                    sendMessageToSocket(RMIMiddleware.customersSocket, message);
                }
                break;
            case "DeleteRooms":
                res = sendMessageToSocket(RMIMiddleware.roomsSocket, message);
                if (checkSuccess(res)) {
                    sendMessageToSocket(RMIMiddleware.customersSocket, message);
                }
                break;
            case "DeleteCustomer":
                res = sendMessageToSocket(RMIMiddleware.customersSocket, message);
                if (checkSuccess(res)) {
                    sendMessageToSocket(RMIMiddleware.flightsSocket, message);
                    sendMessageToSocket(RMIMiddleware.carsSocket, message);
                    sendMessageToSocket(RMIMiddleware.roomsSocket, message);
                }
                break;
            case "QueryFlight":
                res = sendMessageToSocket(RMIMiddleware.flightsSocket, message);
                break;
            case "QueryCars":
                res = sendMessageToSocket(RMIMiddleware.carsSocket, message);
                break;
            case "QueryRooms":
                res = sendMessageToSocket(RMIMiddleware.roomsSocket, message);
                break;
            case "QueryCustomer":
                res = sendMessageToSocket(RMIMiddleware.customersSocket, message);
                break;
            case "QueryFlightPrice":
                res = sendMessageToSocket(RMIMiddleware.flightsSocket, message);
                break;
            case "QueryCarsPrice":
                res = sendMessageToSocket(RMIMiddleware.carsSocket, message);
                break;
            case "QueryRoomsPrice":
                res = sendMessageToSocket(RMIMiddleware.roomsSocket, message);
                break;
            case "ReserveFlight":
                res = sendMessageToSocket(RMIMiddleware.flightsSocket, message);
                if (checkSuccess(res)) {
                    sendMessageToSocket(RMIMiddleware.customersSocket, message);
                }
                break;
            case "ReserveCar":
                res = sendMessageToSocket(RMIMiddleware.carsSocket, message);
                if (checkSuccess(res)) {
                    sendMessageToSocket(RMIMiddleware.customersSocket, message);
                }
                break;
            case "ReserveRoom":
                res = sendMessageToSocket(RMIMiddleware.roomsSocket, message);
                if (checkSuccess(res)) {
                    sendMessageToSocket(RMIMiddleware.customersSocket, message);
                }
                break;
            case "Bundle":
                break;
            default:
                return "error";
        }
        return res;
    }

    private static boolean checkSuccess(String success) {
        return success.equals("true");
    }

}
