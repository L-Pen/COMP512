package Client;

import Server.Interface.*;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.RemoteException;
import java.rmi.NotBoundException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;
import java.io.*;

public class RMIClient extends Client {
	private static String s_serverHost = "tr-open-03";
	// recommended to hange port last digits to your group number
	private static int s_serverPort = 1030;
	private static String s_serverName = "Server";

	private static String s_rmiPrefix = "group_30_";

	public static void main(String args[]) throws UnknownHostException, IOException {
		if (args.length > 0) {
			s_serverHost = args[0];
		}
		if (args.length > 1) {
			s_serverName = args[1];
		}
		if (args.length > 4) {
			System.err.println((char) 27 + "[31;1mClient exception: " + (char) 27
					+ "[0mUsage: java client.RMIClient [server_hostname [server_rmiobject]]");
			System.exit(1);
		}

		// Get a reference to the RMIRegister
		if (args[2].equals("rmi")) {
			try {
				RMIClient client = new RMIClient();
				client.connectServer();
				client.start();
			} catch (Exception e) {
				System.err.println((char) 27 + "[31;1mClient exception: " + (char) 27 + "[0mUncaught exception");
				e.printStackTrace();
				System.exit(1);
			}
		}
		if (args[2].equals("tcp")) {
			int port = 9030;
			if (args.length > 3)
				port = Integer.parseInt(args[3]);
			try {
				while (true) // works forever
				{

					BufferedReader bufferedReader = new java.io.BufferedReader(new InputStreamReader(System.in));
					System.out.print((char) 27 + "[32;1m\n>] " + (char) 27 + "[0m");
					String readerInput = bufferedReader.readLine().trim(); // read user's input
					Vector<String> arguments = parse(readerInput);
					Command cmd = null;
					try {
						cmd = Command.fromString((String) arguments.elementAt(0));
						printInputs(cmd, arguments);
					} catch (IllegalArgumentException e) {
						System.out.println("Invalid command");
						continue;
					}

					if (cmd.equals(Command.Quit))
						break;

					String commandString = cmd.name() + ",";
					for (int i = 1; i < arguments.size(); i++) {
						commandString += (String) arguments.elementAt(i) + ",";
					}

					String res = sendTcpRequest(port, commandString);

					if (cmd.equals(Command.AddCustomer)) {
						System.out.println("Add customer ID: " + res); // what about the bad cases?
					} else if (cmd.equals(Command.QueryFlight)) {
						System.out.println("Number of seats available: " + res);
					} else if (cmd.equals(Command.QueryCars)) {
						System.out.println("Number of cars at this location: " + res);
					} else if (cmd.equals(Command.QueryRooms)) {
						System.out.println("Number of rooms at this location: " + res);
					} else if (cmd.equals(Command.QueryCustomer)) { // check for this
						System.out.println("Customer info: " + res);
					} else if (cmd.equals(Command.QueryFlightPrice)) {
						System.out.println("Price of a seat: " + res);
					} else if (cmd.equals(Command.QueryCarsPrice)) {
						System.out.println("Price of cars at this location: " + res);
					} else if (cmd.equals(Command.QueryRoomsPrice)) {
						System.out.println("Price of rooms at this location: " + res);
					} else {
						System.out.println(res.equals("true") ? "Operation successful" : "Operation failed");
					}

				}
			} catch (Exception e) {
				System.err.println("An error occured... Disconnecting");
			}
		}
	}

	public static String sendTcpRequest(int port, String commandString) throws IOException {
		Socket socket = new Socket(s_serverHost, port);
		PrintWriter outToServer = new PrintWriter(socket.getOutputStream(), true);
		BufferedReader inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		outToServer.println(commandString); // send the user's input via the output stream to the server

		String messageInProgress = null;
		String res = "";
		while ((messageInProgress = inFromServer.readLine()) != null) {
			res += messageInProgress + "\n";
		}
		socket.close();
		return res.trim();
	}

	public RMIClient() {
		super();
	}

	public void connectServer() {
		connectServer(s_serverHost, s_serverPort, s_serverName);
	}

	public void connectServer(String server, int port, String name) {
		try {
			boolean first = true;
			while (true) {
				try {
					Registry registry = LocateRegistry.getRegistry(server, port);
					m_resourceManager = (IResourceManager) registry.lookup(s_rmiPrefix + name);
					System.out.println("Connected to '" + name + "' server [" + server + ":" + port + "/" + s_rmiPrefix
							+ name + "]");
					break;
				} catch (NotBoundException | RemoteException e) {
					if (first) {
						System.out.println("Waiting for '" + name + "' server [" + server + ":" + port + "/"
								+ s_rmiPrefix + name + "]");
						first = false;
					}
				}
				Thread.sleep(500);
			}
		} catch (Exception e) {
			System.err.println((char) 27 + "[31;1mServer exception: " + (char) 27 + "[0mUncaught exception");
			e.printStackTrace();
			System.exit(1);
		}
	}

	public static void printInputs(Command cmd, Vector<String> arguments) {
		switch (cmd) {
			case Help: {
				if (arguments.size() == 1) {
					System.out.println(Command.description());
				} else if (arguments.size() == 2) {
					Command l_cmd = Command.fromString((String) arguments.elementAt(1));
					System.out.println(l_cmd.toString());
				} else {
					System.err.println((char) 27 + "[31;1mCommand exception: " + (char) 27
							+ "[0mImproper use of help command. Location \"help\" or \"help,<CommandName>\"");
				}
				break;
			}
			case AddFlight: {
				checkArgumentsCount(4, arguments.size());

				System.out.println("Adding a new flight ");
				System.out.println("-Flight Number: " + arguments.elementAt(1));
				System.out.println("-Flight Seats: " + arguments.elementAt(2));
				System.out.println("-Flight Price: " + arguments.elementAt(3));
				break;
			}
			case AddCars: {
				checkArgumentsCount(4, arguments.size());

				System.out.println("Adding new cars");
				System.out.println("-Car Location: " + arguments.elementAt(1));
				System.out.println("-Number of Cars: " + arguments.elementAt(2));
				System.out.println("-Car Price: " + arguments.elementAt(3));
				break;
			}
			case AddRooms: {
				checkArgumentsCount(4, arguments.size());

				System.out.println("Adding new rooms");
				System.out.println("-Room Location: " + arguments.elementAt(1));
				System.out.println("-Number of Rooms: " + arguments.elementAt(2));
				System.out.println("-Room Price: " + arguments.elementAt(3));
				break;
			}
			case AddCustomer: {
				checkArgumentsCount(1, arguments.size());

				System.out.println("Adding a new customer");
				break;
			}
			case AddCustomerID: {
				checkArgumentsCount(2, arguments.size());

				System.out.println("Adding a new customer");
				System.out.println("-Customer ID: " + arguments.elementAt(1));
				break;
			}
			case DeleteFlight: {
				checkArgumentsCount(2, arguments.size());

				System.out.println("Deleting a flight");
				System.out.println("-Flight Number: " + arguments.elementAt(1));
				break;
			}
			case DeleteCars: {
				checkArgumentsCount(2, arguments.size());

				System.out.println("Deleting all cars at a particular location");
				System.out.println("-Car Location: " + arguments.elementAt(1));
				break;
			}
			case DeleteRooms: {
				checkArgumentsCount(2, arguments.size());

				System.out.println("Deleting all rooms at a particular location");
				System.out.println("-Car Location: " + arguments.elementAt(1));
				break;
			}
			case DeleteCustomer: {
				checkArgumentsCount(2, arguments.size());

				System.out.println("Deleting a customer from the database");
				System.out.println("-Customer ID: " + arguments.elementAt(1));
				break;
			}
			case QueryFlight: {
				checkArgumentsCount(2, arguments.size());

				System.out.println("Querying a flight");
				System.out.println("-Flight Number: " + arguments.elementAt(1));
				break;
			}
			case QueryCars: {
				checkArgumentsCount(2, arguments.size());

				System.out.println("Querying cars location");
				System.out.println("-Car Location: " + arguments.elementAt(1));
				break;
			}
			case QueryRooms: {
				checkArgumentsCount(2, arguments.size());

				System.out.println("Querying rooms location");
				System.out.println("-Room Location: " + arguments.elementAt(1));
				break;
			}
			case QueryCustomer: {
				checkArgumentsCount(2, arguments.size());

				System.out.println("Querying customer information");
				System.out.println("-Customer ID: " + arguments.elementAt(1));
				break;
			}
			case QueryFlightPrice: {
				checkArgumentsCount(2, arguments.size());

				System.out.println("Querying a flight price");
				System.out.println("-Flight Number: " + arguments.elementAt(1));
				break;
			}
			case QueryCarsPrice: {
				checkArgumentsCount(2, arguments.size());

				System.out.println("Querying cars price");
				System.out.println("-Car Location: " + arguments.elementAt(1));
				break;
			}
			case QueryRoomsPrice: {
				checkArgumentsCount(2, arguments.size());

				System.out.println("Querying rooms price");
				System.out.println("-Room Location: " + arguments.elementAt(1));
				break;
			}
			case ReserveFlight: {
				checkArgumentsCount(3, arguments.size());

				System.out.println("Reserving seat in a flight");
				System.out.println("-Customer ID: " + arguments.elementAt(1));
				System.out.println("-Flight Number: " + arguments.elementAt(2));
				break;
			}
			case ReserveCar: {
				checkArgumentsCount(3, arguments.size());

				System.out.println("Reserving a car at a location");
				System.out.println("-Customer ID: " + arguments.elementAt(1));
				System.out.println("-Car Location: " + arguments.elementAt(2));
				break;
			}
			case ReserveRoom: {
				checkArgumentsCount(3, arguments.size());

				System.out.println("Reserving a room at a location");
				System.out.println("-Customer ID: " + arguments.elementAt(1));
				System.out.println("-Room Location: " + arguments.elementAt(2));
				break;
			}
			case Bundle: {
				if (arguments.size() < 6) {
					System.err.println((char) 27 + "[31;1mCommand exception: " + (char) 27
							+ "[0mBundle command expects at least 6 arguments. Location \"help\" or \"help,<CommandName>\"");
					break;
				}

				System.out.println("Reserving an bundle");
				System.out.println("-Customer ID: " + arguments.elementAt(1));
				for (int i = 0; i < arguments.size() - 5; ++i) {
					System.out.println("-Flight Number: " + arguments.elementAt(2 + i));
				}
				System.out.println("-Location for Car/Room: " + arguments.elementAt(arguments.size() - 3));
				System.out.println("-Book Car: " + arguments.elementAt(arguments.size() - 2));
				System.out.println("-Book Room: " + arguments.elementAt(arguments.size() - 1));

				break;
			}
			case Quit:
				checkArgumentsCount(1, arguments.size());

				System.out.println("Quitting client");
				System.exit(0);
		}
	}
}
