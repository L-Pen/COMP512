package Server.RMI;

import Server.Common.ResourceManager;

import java.util.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.RemoteException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;

public class TCPResourceManager extends ResourceManager {

	public TCPResourceManager(String p_name) {
		super(p_name);
	}

	public static void main(String[] args) throws IOException {

		int port = 9030;
		if (args.length == 2)
			port = Integer.parseInt(args[1]);

		ServerSocket serverSocket = new ServerSocket(port); // establish a server socket to receive messages over the
															// network from clients

		TCPResourceManager tcpResourceManager = new TCPResourceManager(args[0]);

		System.out.println("Server " + tcpResourceManager.m_name + " ready on port " + String.valueOf(port));

		while (true) {
			// waits here
			Socket socket = serverSocket.accept(); // listen for a connection to be made to this socket and accept it
			try {
				BufferedReader inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));

				PrintWriter outToClient = new PrintWriter(socket.getOutputStream(), true);
				String returnMessage = "";
				String message = inFromClient.readLine();

				String[] params = message.split(",");
				System.out.println("Command: " + message);

				returnMessage = tcpResourceManager.runCommand(params);

				System.out.println("return message: " + returnMessage);
				outToClient.println(returnMessage);
				socket.close();
			} catch (IOException e) {
				System.out
						.println("Exception caught when trying to listen on port " + port + " in TCP Resource Manager");
			}
		}

	}

	private String runCommand(String[] params) throws NumberFormatException, RemoteException {
		if (params[0].equals("AddFlight")) {
			return String.valueOf(addFlight(Integer.parseInt(params[1]),
					Integer.parseInt(params[2]), Integer.parseInt(params[3])));
		} else if (params[0].equals("AddCars")) {
			return String.valueOf(addCars(params[1],
					Integer.parseInt(params[2]), Integer.parseInt(params[3])));
		} else if (params[0].equals("AddRooms")) {
			return String.valueOf(addRooms(params[1],
					Integer.parseInt(params[2]), Integer.parseInt(params[3])));
		} else if (params[0].equals("AddCustomer")) {
			return String.valueOf(newCustomer());
		} else if (params[0].equals("AddCustomerID")) {
			return String.valueOf(newCustomer(Integer.parseInt(params[1])));
		} else if (params[0].equals("DeleteFlight")) {
			return String.valueOf(deleteFlight(Integer.parseInt(params[1])));
		} else if (params[0].equals("DeleteCars")) {
			return String.valueOf(deleteCars(params[1]));
		} else if (params[0].equals("DeleteRooms")) {
			return String.valueOf(deleteRooms(params[1]));
		} else if (params[0].equals("DeleteCustomer")) {
			return String.valueOf(deleteCustomer(Integer.parseInt(params[1])));
		} else if (params[0].equals("QueryFlight")) {
			return String.valueOf(queryFlight(Integer.parseInt(params[1])));
		} else if (params[0].equals("QueryCars")) {
			return String.valueOf(queryCars(params[1]));
		} else if (params[0].equals("QueryRooms")) {
			return String.valueOf(queryRooms(params[1]));
		} else if (params[0].equals("QueryCustomer")) {
			return queryCustomerInfo(Integer.parseInt(params[1]));
		} else if (params[0].equals("QueryFlightPrice")) {
			return String.valueOf(queryFlightPrice(Integer.parseInt(params[1])));
		} else if (params[0].equals("QueryCarsPrice")) {
			return String.valueOf(queryCarsPrice(params[1]));
		} else if (params[0].equals("QueryRoomsPrice")) {
			return String.valueOf(queryRoomsPrice(params[1]));
		} else if (params[0].equals("ReserveFlight")) {
			return String.valueOf(reserveFlight(Integer.parseInt(params[1]),
					Integer.parseInt(params[2])));
		} else if (params[0].equals("ReserveCar")) {
			return String.valueOf(reserveCar(Integer.parseInt(params[1]), params[2]));
		} else if (params[0].equals("ReserveRoom")) {
			return String.valueOf(reserveRoom(Integer.parseInt(params[1]), params[2]));
		} else if (params[0].equals("Bundle")) {
			int customerID = Integer.parseInt(params[1]);
			Vector<String> flightNumbers = new Vector<String>();
			String location = params[params.length - 3];
			boolean wantsCar = params[params.length - 2].equals("1");
			boolean wantsRoom = params[params.length - 1].equals("1");

			for (int i = 2; i < params.length - 3; i++) {
				flightNumbers.add(params[i]);
			}

			System.out.println("CustomerId: " + customerID + " Flight numbers: " + flightNumbers.toString()
					+ " location: " + location + " wantsCar: " + wantsCar + " wantsRooms: " + wantsRoom);
			return String.valueOf(bundle(customerID, flightNumbers, location, wantsCar, wantsRoom));
		} else if (params[0].equals("UnreserveFlight")) {
			return String.valueOf(unreserveFlight(Integer.parseInt(params[1]), Integer.parseInt(params[2])));
		} else if (params[0].equals("UnreserveRoom")) {
			return String.valueOf(unreserveRoom(Integer.parseInt(params[1]), params[2]));
		} else if (params[0].equals("UnreserveCar")) {
			return String.valueOf(unreserveCar(Integer.parseInt(params[1]), params[2]));
		}
		return "false";
	}

}
