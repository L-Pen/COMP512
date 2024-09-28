package Client;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeEach;
import org.junit.Test;
import Client.RMIClient;


public class testRMI {

    private testRMI client;

    @BeforeEach
    public void setUp() {
        RMIClient client = new RMIClient();
    }

    // @AfterEach
    // public void tearDown() {
    //     // cleanup logic after each test
    // }

    @Test
    public void testAddFlight() {
    }

    @Test
    public void testAddCars() {
    }

    @Test
    public void testAddRooms() {
    }

    @Test
    public void testAddCustomer() {

    }

    @Test
    public void testAddCustomerID() {
    }

    @Test
    public void testDeleteFlight() {
    }

    @Test
    public void testDeleteCars() {
        assertTrue(client.DeleteCars("NYC"));
    }

    @Test
    public void testDeleteRooms() {
        assertTrue(client.DeleteRooms("NYC"));
    }

    @Test
    public void testDeleteCustomer() {
        assertTrue(client.DeleteCustomer(12345));
    }

    @Test
    public void testQueryFlight() {
        int seats = client.QueryFlight(100);
        assertTrue(seats >= 0);
    }

    @Test
    public void testQueryCars() {
        int cars = client.QueryCars("NYC");
        assertTrue(cars >= 0);
    }

    @Test
    public void testQueryRooms() {
        int rooms = client.QueryRooms("NYC");
        assertTrue(rooms >= 0);
    }

    @Test
    public void testQueryCustomer() {
        double bill = client.QueryCustomer(12345);
        assertTrue(bill >= 0);
    }

    @Test
    public void testQueryFlightPrice() {
        int price = client.QueryFlightPrice(100);
        assertTrue(price >= 0);
    }

    @Test
    public void testQueryCarsPrice() {
        int price = client.QueryCarsPrice("NYC");
        assertTrue(price >= 0);
    }

    @Test
    public void testQueryRoomsPrice() {
        int price = client.QueryRoomsPrice("NYC");
        assertTrue(price >= 0);
    }

    @Test
    public void testReserveFlight() {
        assertTrue(client.ReserveFlight(12345, 100));
    }

    @Test
    public void testReserveCar() {
        assertTrue(client.ReserveCar(12345, "NYC"));
    }

    @Test
    public void testReserveRoom() {
        assertTrue(client.ReserveRoom(12345, "NYC"));
    }

    @Test
    public void testBundle() {
        assertTrue(client.Bundle("12345,100,101,NYC,Y,Y"));
    }
}
