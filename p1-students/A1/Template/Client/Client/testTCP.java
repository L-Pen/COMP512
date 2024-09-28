package Client;

import Client.RMIClient;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;
import java.io.*;

public class testTCP {

    public void setUp() throws UnknownHostException, IOException {
        String s_serverHost = "tr-open-03";
        Socket socket = new Socket(s_serverHost, 9030);
        PrintWriter outToServer = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        BufferedReader bufferedReader = new java.io.BufferedReader(new InputStreamReader(System.in));

    }

    public void tearDown() {

    }

    public void testAddFlight() {
    }

    public void testAddCars() {
    }

    public void testAddRooms() {
    }

    public void testAddCustomer() {

    }

    public void testAddCustomerID() {
    }

    public void testDeleteFlight() {
    }

    public void testDeleteCars() {
    }

    public void testDeleteRooms() {
    }

    public void testDeleteCustomer() {
    }

    public void testQueryFlight() {

    }

    public void testQueryCars() {

    }

    public void testQueryRooms() {

    }

    public void testQueryCustomer() {

    }

    public void testQueryFlightPrice() {

    }

    public void testQueryCarsPrice() {

    }

    public void testQueryRoomsPrice() {

    }

    public void testReserveFlight() {
    }

    public void testReserveCar() {
    }

    public void testReserveRoom() {
    }

    public void testBundle() {
    }
}
