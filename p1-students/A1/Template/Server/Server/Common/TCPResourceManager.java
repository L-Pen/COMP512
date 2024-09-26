// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
// -------------------------------

package Server.Common;

import Server.Interface.*;

import java.util.*;
import java.rmi.RemoteException;
import java.io.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Date;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;

public class TCPResourceManager extends ResourceManager {

	public TCPResourceManager(String p_name) {
		super(p_name);
	}

	public static void main(String[] args) throws IOException {
		ServerSocket serverSocket = new ServerSocket(9090); // establish a server socket to receive messages over the
															// network from clients

		System.out.println("Server ready...");

		while (true) // runs forever
		{
			String message = null;
			Socket socket = serverSocket.accept(); // listen for a connection to be made to this socket and accept it
			try {
				BufferedReader inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));

				PrintWriter outToClient = new PrintWriter(socket.getOutputStream(), true);
				while ((message = inFromClient.readLine()) != null) {
					String[] params = message.split(",");

					outToClient.println("hello client from server, your result is: " + res);
				}
			} catch (IOException e) {
			}
		}

	}

}
