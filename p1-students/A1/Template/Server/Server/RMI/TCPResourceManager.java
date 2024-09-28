// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
// -------------------------------

package Server.RMI;

import Server.Common.ResourceManager;

import java.util.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;

public class TCPResourceManager extends ResourceManager {

	public TCPResourceManager(String p_name) {
		super(p_name);
	}

	public static void main(String[] args) throws IOException {
		ServerSocket serverSocket = new ServerSocket(9030); // establish a server socket to receive messages over the
															// network from clients

		TCPResourceManager tcpResourceManager = new TCPResourceManager(args[0]);

		System.out.println("Server ready..." + tcpResourceManager.m_name);

		while (true) // runs forever
		{
			String message = null;
			Socket socket = serverSocket.accept(); // listen for a connection to be made to this socket and accept it
			try {
				BufferedReader inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));

				PrintWriter outToClient = new PrintWriter(socket.getOutputStream(), true);
				String returnMessage = "";

				while ((message = inFromClient.readLine()) != null) {

					System.out.println("In TCP Resource Manager while loop");

					String[] params = message.split(",");
					System.out.println(message);

					if (params[0].equals("quit")) {
						break;
					} else if (params[0].equals("AddFlight")) {
						returnMessage = String.valueOf(tcpResourceManager.addFlight(Integer.parseInt(params[1]),
								Integer.parseInt(params[2]), Integer.parseInt(params[3])));
						break;
					} else if (params[0].equals("AddCars")) {
						returnMessage = String.valueOf(tcpResourceManager.addCars(params[1],
								Integer.parseInt(params[2]), Integer.parseInt(params[3])));
						break;
					} else if (params[0].equals("AddRooms")) {
						returnMessage = String.valueOf(tcpResourceManager.addRooms(params[1],
								Integer.parseInt(params[2]), Integer.parseInt(params[3])));
						break;
					} else if (params[0].equals("AddCustomer")) {
						returnMessage = String.valueOf(tcpResourceManager.newCustomer());
						break;
					} else if (params[0].equals("AddCustomerID")) {
						returnMessage = String.valueOf(tcpResourceManager.newCustomer(Integer.parseInt(params[1])));
						break;
					} else if (params[0].equals("DeleteFlight")) {
						returnMessage = String.valueOf(tcpResourceManager.deleteFlight(Integer.parseInt(params[1])));
						break;
					} else if (params[0].equals("DeleteCars")) {
						returnMessage = String.valueOf(tcpResourceManager.deleteCars(params[1]));
						break;
					} else if (params[0].equals("DeleteRooms")) {
						returnMessage = String.valueOf(tcpResourceManager.deleteRooms(params[1]));
						break;
					} else if (params[0].equals("DeleteCustomer")) {
						returnMessage = String.valueOf(tcpResourceManager.deleteCustomer(Integer.parseInt(params[1])));
						break;
					} else if (params[0].equals("QueryFlight")) {
						returnMessage = String.valueOf(tcpResourceManager.queryFlight(Integer.parseInt(params[1])));
						break;
					} else if (params[0].equals("QueryCars")) {
						returnMessage = String.valueOf(tcpResourceManager.queryCars(params[1]));
						break;
					} else if (params[0].equals("QueryRooms")) {
						returnMessage = String.valueOf(tcpResourceManager.queryRooms(params[1]));
						break;
					} else if (params[0].equals("QueryCustomer")) {
						returnMessage = tcpResourceManager.queryCustomerInfoTCP(Integer.parseInt(params[1]));
						System.out.println("HERE ! QueryCustomer: " + returnMessage);
						break;
					} else if (params[0].equals("QueryFlightPrice")) {
						returnMessage = String
								.valueOf(tcpResourceManager.queryFlightPrice(Integer.parseInt(params[1])));
						break;
					} else if (params[0].equals("QueryCarsPrice")) {
						returnMessage = String.valueOf(tcpResourceManager.queryCarsPrice(params[1]));
						break;
					} else if (params[0].equals("QueryRoomsPrice")) {
						returnMessage = String.valueOf(tcpResourceManager.queryRoomsPrice(params[1]));
						break;
					} else if (params[0].equals("ReserveFlight")) {
						returnMessage = String.valueOf(tcpResourceManager.reserveFlight(Integer.parseInt(params[1]),
								Integer.parseInt(params[2])));
						break;
					} else if (params[0].equals("ReserveCar")) {
						returnMessage = String
								.valueOf(tcpResourceManager.reserveCar(Integer.parseInt(params[1]), params[2]));
						break;
					} else if (params[0].equals("ReserveRoom")) {
						returnMessage = String
								.valueOf(tcpResourceManager.reserveRoom(Integer.parseInt(params[1]), params[2]));
						break;
					} else if (params[0].equals("Bundle")) {

						int customerID = Integer.parseInt(params[1]);
						Vector<String> flightNumbers = new Vector<String>();
						String location = params[params.length - 3];
						boolean wantsCar = params[params.length - 2].equals("1") ? true : false;
						boolean wantsRoom = params[params.length - 1].equals("1") ? true : false;

						// find alpha numberic element
						for (int i = 2; i < params.length - 5; i++) {
							flightNumbers.add(params[i]);
						}

						returnMessage = String.valueOf(
								tcpResourceManager.bundle(customerID, flightNumbers, location, wantsCar, wantsRoom));
						break;
					}
				}

				outToClient.println(returnMessage);
			} catch (IOException e) {
				System.out
						.println("Exception caught when trying to listen on port " + 9030 + " in TCP Resource Manager");
			}
		}

	}

}
