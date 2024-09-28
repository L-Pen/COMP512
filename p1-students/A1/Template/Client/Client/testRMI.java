package Client;

import java.util.*;
import java.io.*;
import java.rmi.RemoteException;

public class testRMI {

    public static void main(String[] args) {
        // run tests
    }

    private RMIClient setUpClient(String host) {
        RMIClient client = new RMIClient();
        int port = 1030;
        String name = "Middleware";
        client.connectServer(host, port, name);
        return client;
    }

    private Vector<String> createVector(String... elements) {
        Vector<String> vector = new Vector<>();
        Arrays.stream(elements).forEach(vector::add);
        return vector;
    }

    public void testAddFlight() throws NumberFormatException, RemoteException {
        RMIClient client = setUpClient("");
        client.execute(Command.AddFlight, createVector("1", "1", "1"));
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
