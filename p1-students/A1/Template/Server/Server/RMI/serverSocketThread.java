package Server.RMI;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.UnknownHostException;

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
            System.err.println(e);
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
        String res = null;
        while (true) {
            res = inFromServer.readLine();
            if (res != null) {
                System.out.println("result: " + res);
                return res;
            }
            System.out.println("waiting for answer...");
        }
    }

    private String routeMessage(String commandName, String message) throws IOException {
        Socket flightsSocket = connectTcp(RMIMiddleware.flightsServer);
        Socket carsSocket = connectTcp(RMIMiddleware.carsServer);
        Socket roomsSocket = connectTcp(RMIMiddleware.roomsServer);
        Socket customersSocket = connectTcp(RMIMiddleware.customersServer);
        String res = "error";
        switch (commandName) {
            case "AddFlight":
                res = sendMessageToSocket(flightsSocket, message);
                if (isTrue(res)) {
                    sendMessageToSocket(customersSocket, message);
                }
                break;
            case "AddCars":
                res = sendMessageToSocket(carsSocket, message);
                if (isTrue(res)) {
                    sendMessageToSocket(customersSocket, message);
                }
                break;
            case "AddRooms":
                res = sendMessageToSocket(roomsSocket, message);
                if (isTrue(res)) {
                    sendMessageToSocket(customersSocket, message);
                }
                break;
            case "AddCustomer":
                res = sendMessageToSocket(customersSocket, message);
                String payload = "AddCustomerID," + res;
                sendMessageToSocket(flightsSocket, payload);
                sendMessageToSocket(carsSocket, payload);
                sendMessageToSocket(roomsSocket, payload);
                break;
            case "AddCustomerID":
                res = sendMessageToSocket(customersSocket, message);
                if (isTrue(res)) {
                    sendMessageToSocket(flightsSocket, message);
                    sendMessageToSocket(carsSocket, message);
                    sendMessageToSocket(roomsSocket, message);
                }
                break;
            case "DeleteFlight":
                res = sendMessageToSocket(flightsSocket, message);
                if (isTrue(res)) {
                    sendMessageToSocket(customersSocket, message);
                }
                break;
            case "DeleteCars":
                res = sendMessageToSocket(carsSocket, message);
                if (isTrue(res)) {
                    sendMessageToSocket(customersSocket, message);
                }
                break;
            case "DeleteRooms":
                res = sendMessageToSocket(roomsSocket, message);
                if (isTrue(res)) {
                    sendMessageToSocket(customersSocket, message);
                }
                break;
            case "DeleteCustomer":
                res = sendMessageToSocket(customersSocket, message);
                if (isTrue(res)) {
                    sendMessageToSocket(flightsSocket, message);
                    sendMessageToSocket(carsSocket, message);
                    sendMessageToSocket(roomsSocket, message);
                }
                break;
            case "QueryFlight":
                res = sendMessageToSocket(flightsSocket, message);
                break;
            case "QueryCars":
                res = sendMessageToSocket(carsSocket, message);
                break;
            case "QueryRooms":
                res = sendMessageToSocket(roomsSocket, message);
                break;
            case "QueryCustomer":
                res = sendMessageToSocket(customersSocket, message);
                break;
            case "QueryFlightPrice":
                res = sendMessageToSocket(flightsSocket, message);
                break;
            case "QueryCarsPrice":
                res = sendMessageToSocket(carsSocket, message);
                break;
            case "QueryRoomsPrice":
                res = sendMessageToSocket(roomsSocket, message);
                break;
            case "ReserveFlight":
                res = sendMessageToSocket(flightsSocket, message);
                if (isTrue(res)) {
                    sendMessageToSocket(customersSocket, message);
                }
                break;
            case "ReserveCar":
                res = sendMessageToSocket(carsSocket, message);
                if (isTrue(res)) {
                    sendMessageToSocket(customersSocket, message);
                }
                break;
            case "ReserveRoom":
                res = sendMessageToSocket(roomsSocket, message);
                if (isTrue(res)) {
                    sendMessageToSocket(customersSocket, message);
                }
                break;
            case "Bundle":
                flightsSocket.close();
                String[] params = message.split(",");
                boolean preSuccess = true;
                String customerId = params[1];
                for (int i = 0; i < params.length - 5; ++i) {
                    flightsSocket = connectTcp(RMIMiddleware.flightsServer);
                    String flightNumber = params[2 + i];
                    String payloadBF = "ReserveFlight," + customerId + "," + flightNumber;
                    res = sendMessageToSocket(flightsSocket, payloadBF);
                    preSuccess = isTrue(res) && preSuccess;
                    flightsSocket.close();
                }
                String location = params[params.length - 3];
                boolean wantCar = isTrue(params[params.length - 2]);
                boolean wantRoom = isTrue(params[params.length - 1]);
                if (wantCar && preSuccess) {
                    String payloadBC = "ReserveCar," + customerId + "," + location;
                    res = sendMessageToSocket(carsSocket, payloadBC);
                    preSuccess = isTrue(res) && preSuccess;
                }
                if (wantRoom && preSuccess) {
                    String payloadBR = "ReserveRoom," + customerId + "," + location;
                    res = sendMessageToSocket(roomsSocket, payloadBR);
                    preSuccess = isTrue(res) && preSuccess;
                }

                if (preSuccess) {
                    res = sendMessageToSocket(customersSocket, message);
                } else
                    res = "false";
                break;
            default:
                System.out.println(commandName);
                return "error";
        }
        flightsSocket.close();
        carsSocket.close();
        roomsSocket.close();
        customersSocket.close();
        return res;
    }

    private static boolean isTrue(String success) {
        return success.equals("true") || success.equals("1");
    }

    private static Socket connectTcp(String hostname) throws UnknownHostException, IOException {
        return new Socket(hostname, 9030); // establish a socket with a server using the given port#
    }

}
