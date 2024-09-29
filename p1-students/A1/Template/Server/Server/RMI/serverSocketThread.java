package Server.RMI;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

public class serverSocketThread extends Thread {
    Socket clientSocket;
    RMIMiddleware server;

    int resourceManagerPort = 9030;

    serverSocketThread(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    public void run() {
        while (true) {
            try {
                BufferedReader inFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter outToClient = new PrintWriter(clientSocket.getOutputStream(), true);
                String message = null;
                while ((message = inFromClient.readLine()) != null) {
                    System.out.println("message:" + message);
                    String result = "";
                    String[] params = message.split(",");
                    String commandName = params[0];
                    result = routeMessage(commandName, message);

                    System.out.println("Sending result to client: " + result);

                    outToClient.println(result);
                    clientSocket.close();
                    return;
                }
            } catch (IOException e) {
                System.err.println(e);
                return;
            }
        }
    }

    private String sendTcpRequest(String server, String command) throws IOException {
        Socket socket = new Socket(server, resourceManagerPort);
        PrintWriter outToServer = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        outToServer.println(command);
        String messageInProgress = null;
        String totalMessage = "";
        while ((messageInProgress = inFromServer.readLine()) != null) {
            totalMessage += messageInProgress + "\n";
        }
        socket.close();
        return totalMessage.trim();
    }

    private String routeMessage(String commandName, String message) throws IOException {
        String res = "error";
        switch (commandName) {
            case "AddFlight":
                res = sendTcpRequest(RMIMiddleware.flightsServer, message);
                if (isTrue(res)) {
                    sendTcpRequest(RMIMiddleware.customersServer, message);
                }
                break;
            case "AddCars":
                res = sendTcpRequest(RMIMiddleware.carsServer, message);
                if (isTrue(res)) {
                    sendTcpRequest(RMIMiddleware.customersServer, message);
                }
                break;
            case "AddRooms":
                res = sendTcpRequest(RMIMiddleware.roomsServer, message);
                if (isTrue(res)) {
                    sendTcpRequest(RMIMiddleware.customersServer, message);
                }
                break;
            case "AddCustomer":
                res = sendTcpRequest(RMIMiddleware.customersServer, message);
                String payload = "AddCustomerID," + res;
                sendTcpRequest(RMIMiddleware.flightsServer, payload);
                sendTcpRequest(RMIMiddleware.carsServer, payload);
                sendTcpRequest(RMIMiddleware.roomsServer, payload);
                break;
            case "AddCustomerID":
                res = sendTcpRequest(RMIMiddleware.customersServer, message);
                if (isTrue(res)) {
                    sendTcpRequest(RMIMiddleware.flightsServer, message);
                    sendTcpRequest(RMIMiddleware.carsServer, message);
                    sendTcpRequest(RMIMiddleware.roomsServer, message);
                }
                break;
            case "DeleteFlight":
                res = sendTcpRequest(RMIMiddleware.flightsServer, message);
                if (isTrue(res)) {
                    sendTcpRequest(RMIMiddleware.customersServer, message);
                }
                break;
            case "DeleteCars":
                res = sendTcpRequest(RMIMiddleware.carsServer, message);
                if (isTrue(res)) {
                    sendTcpRequest(RMIMiddleware.customersServer, message);
                }
                break;
            case "DeleteRooms":
                res = sendTcpRequest(RMIMiddleware.roomsServer, message);
                if (isTrue(res)) {
                    sendTcpRequest(RMIMiddleware.customersServer, message);
                }
                break;
            case "DeleteCustomer":
                res = sendTcpRequest(RMIMiddleware.customersServer, message);
                if (isTrue(res)) {
                    sendTcpRequest(RMIMiddleware.flightsServer, message);
                    sendTcpRequest(RMIMiddleware.carsServer, message);
                    sendTcpRequest(RMIMiddleware.roomsServer, message);
                }
                break;
            case "QueryFlight":
                res = sendTcpRequest(RMIMiddleware.flightsServer, message);
                System.out.println("QueryFlight HERE!!!!!!: " + res);
                break;
            case "QueryCars":
                res = sendTcpRequest(RMIMiddleware.carsServer, message);
                break;
            case "QueryRooms":
                res = sendTcpRequest(RMIMiddleware.roomsServer, message);
                break;
            case "QueryCustomer":
                res = sendTcpRequest(RMIMiddleware.customersServer, message);
                break;
            case "QueryFlightPrice":
                res = sendTcpRequest(RMIMiddleware.flightsServer, message);
                break;
            case "QueryCarsPrice":
                res = sendTcpRequest(RMIMiddleware.carsServer, message);
                break;
            case "QueryRoomsPrice":
                res = sendTcpRequest(RMIMiddleware.roomsServer, message);
                break;
            case "ReserveFlight":
                res = sendTcpRequest(RMIMiddleware.flightsServer, message);
                if (isTrue(res)) {
                    sendTcpRequest(RMIMiddleware.customersServer, message);
                }
                break;
            case "ReserveCar":
                res = sendTcpRequest(RMIMiddleware.carsServer, message);
                if (isTrue(res)) {
                    sendTcpRequest(RMIMiddleware.customersServer, message);
                }
                break;
            case "ReserveRoom":
                res = sendTcpRequest(RMIMiddleware.roomsServer, message);
                if (isTrue(res)) {
                    sendTcpRequest(RMIMiddleware.customersServer, message);
                }
                break;
            case "Bundle":
                String[] params = message.split(",");
                boolean preSuccess = true;
                String customerId = params[1];
                Vector<String> flightsToUnreserve = new Vector<String>();
                boolean unreserveCar = false;
                boolean unreserveRoom = false;
                for (int i = 0; i < params.length - 5; ++i) {
                    String flightNumber = params[2 + i];
                    String payloadBF = "ReserveFlight," + customerId + "," + flightNumber;
                    res = sendTcpRequest(RMIMiddleware.flightsServer, payloadBF);
                    boolean success = isTrue(res);
                    if (success) {
                        flightsToUnreserve.add(flightNumber);
                    }
                    preSuccess = success && preSuccess;
                }
                String location = params[params.length - 3];
                boolean wantCar = isTrue(params[params.length - 2]);
                boolean wantRoom = isTrue(params[params.length - 1]);
                if (wantCar) {
                    String payloadBC = "ReserveCar," + customerId + "," + location;
                    res = sendTcpRequest(RMIMiddleware.carsServer, payloadBC);
                    boolean success = isTrue(res);
                    unreserveCar = success;
                    preSuccess = success && preSuccess;
                }
                if (wantRoom) {
                    String payloadBR = "ReserveRoom," + customerId + "," + location;
                    res = sendTcpRequest(RMIMiddleware.roomsServer, payloadBR);
                    boolean success = isTrue(res);
                    unreserveRoom = success;
                    preSuccess = success && preSuccess;
                }

                if (preSuccess) {
                    res = sendTcpRequest(RMIMiddleware.customersServer, message);
                } else {
                    System.out.println("Failed... rolling back");
                    for (String flightNumber : flightsToUnreserve) {
                        sendTcpRequest(RMIMiddleware.flightsServer,
                                "UnreserveFlight," + customerId + "," + flightNumber);
                    }
                    if (unreserveCar)
                        sendTcpRequest(RMIMiddleware.carsServer, "UnreserveCar," + customerId + "," + location);
                    if (unreserveRoom)
                        sendTcpRequest(RMIMiddleware.roomsServer, "UnreserveRoom," + customerId + "," + location);
                    return "false";
                }
                break;
            default:
                System.out.println(commandName);
                return "error";
        }
        return res;
    }

    private static boolean isTrue(String success) {
        return success.equals("true") || success.equals("1");
    }

}
