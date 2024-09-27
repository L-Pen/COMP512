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
	private static String s_serverHost = "localhost";
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
		if (args.length > 3) {
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
			Socket socket = new Socket(s_serverHost, 9030); // establish a socket with a server using the given port#

			PrintWriter outToServer = new PrintWriter(socket.getOutputStream(), true); // open an output stream to the
																						// server...
			BufferedReader inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream())); // open an
																												// input
																												// stream
																												// from
																												// the
																												// server...

			BufferedReader bufferedReader = new java.io.BufferedReader(new InputStreamReader(System.in)); // to read
																											// user's
																											// input
			while (true) // works forever
			{
				System.out.print((char) 27 + "[32;1m\n>] " + (char) 27 + "[0m");
				String readerInput = bufferedReader.readLine().trim(); // read user's input
				Vector<String> arguments = parse(readerInput);
				Command cmd = null;
				try{
					cmd = Command.fromString((String) arguments.elementAt(0));
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
				outToServer.println(commandString); // send the user's input via the output stream to the server
				String res = inFromServer.readLine(); // receive the server's result via the input stream from the
														// server
				System.out.println("result: " + res); // print the server result to the user
			}

			socket.close();
		}

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
}
