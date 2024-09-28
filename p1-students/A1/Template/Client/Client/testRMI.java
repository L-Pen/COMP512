package Client;

public class testRMI {

    static RMIClient client;

    public static void main(String[] args) {
        client = new RMIClient();
        String host = "";
        int port = 1030;
        String name = "Middleware";
        client.connectServer(host, port, name);
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
