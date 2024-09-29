package Server.RMI;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Vector;

import Server.Interface.IResourceManager;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class RMIMiddleware implements IResourceManager {
    private static String s_serverName = "Middleware";
    private static String s_rmiPrefix = "group_30_";

    private static IResourceManager flightsResourceManager;
    private static IResourceManager carsResourceManager;
    private static IResourceManager roomsResourceManager;
    private static IResourceManager customersResourceManager;

    public static String flightsServer;
    public static String carsServer;
    public static String roomsServer;
    public static String customersServer;

    private static int registryPortNumber = 1030;

    private static String runType;

    public static void main(String args[]) throws Exception {

        if (args.length < 5)
            throw new Exception(
                    "We need a flights, cars, rooms and customer server names. Whatever u put is not equal to 4");

        // Create a new Server object
        RMIMiddleware server = new RMIMiddleware();

        flightsServer = args[0];
        carsServer = args[1];
        roomsServer = args[2];
        customersServer = args[3];
        runType = args[4];

        int port = 9034;

        if (runType.equals("rmi")) {
            runRMI(flightsServer, carsServer, roomsServer, customersServer, server);
        } else if (runType.equals("tcp")) {
            runTCP(port);
        } else {
            System.err.println("Unknown run type");
        }

    }

    private static void runTCP(int port)
            throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Middleware Server ready...");
        // connect to resource managers...
        while (true) {
            Socket socket = serverSocket.accept();
            new serverSocketThread(socket).start();
        }
    }

    private static void runRMI(String flightsServer, String carsServer, String roomsServer, String customersServer,
            RMIMiddleware server) {
        try {

            // Dynamically generate the stub (client proxy)
            IResourceManager middleware = (IResourceManager) UnicastRemoteObject.exportObject(server, 0);

            // Bind the remote object's stub in the registry; adjust port if appropriate
            Registry l_registry;
            try {
                l_registry = LocateRegistry.createRegistry(registryPortNumber);
            } catch (RemoteException e) {
                l_registry = LocateRegistry.getRegistry(registryPortNumber);
            }
            final Registry registry = l_registry;

            // group_30_Middleware in registry
            registry.rebind(s_rmiPrefix + s_serverName, middleware);

            RMIMiddleware.flightsResourceManager = connectServer(flightsServer, registryPortNumber, "Flights");
            RMIMiddleware.carsResourceManager = connectServer(carsServer, registryPortNumber, "Cars");
            RMIMiddleware.roomsResourceManager = connectServer(roomsServer, registryPortNumber, "Rooms");
            RMIMiddleware.customersResourceManager = connectServer(customersServer, registryPortNumber, "Customers");

            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    try {
                        registry.unbind(s_rmiPrefix + s_serverName);
                        System.out.println("'" + s_serverName + "' resource manager unbound");
                    } catch (Exception e) {
                        System.err
                                .println((char) 27 + "[31;1mServer exception: " + (char) 27 + "[0mUncaught exception");
                        e.printStackTrace();
                    }
                }
            });
            System.out.println("'" + s_serverName + "' resource manager server ready and bound to '" + s_rmiPrefix
                    + s_serverName + "'");
        } catch (Exception e) {
            System.err.println((char) 27 + "[31;1mServer exception: " + (char) 27 + "[0mUncaught exception");
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static IResourceManager connectServer(String server, int port, String name)
            throws RemoteException, NotBoundException {
        Registry registry = LocateRegistry.getRegistry(server, port);
        IResourceManager m_resourceManager = (IResourceManager) registry.lookup(s_rmiPrefix + name);
        System.out.println("Connected to '" + name + "' server [" + server + ":" + port + "/" + s_rmiPrefix
                + name + "]");
        return m_resourceManager;
    }

    @Override
    public boolean addFlight(int flightNum, int flightSeats, int flightPrice) throws RemoteException {
        boolean success = RMIMiddleware.flightsResourceManager.addFlight(flightNum, flightSeats, flightPrice);
        if (success) {
            RMIMiddleware.customersResourceManager.addFlight(flightNum, flightSeats, flightPrice);
        }
        return success;
    }

    @Override
    public boolean addCars(String location, int numCars, int price) throws RemoteException {
        boolean success = RMIMiddleware.carsResourceManager.addCars(location, numCars, price);
        if (success) {
            RMIMiddleware.customersResourceManager.addCars(location, numCars, price);
        }
        return success;
    }

    @Override
    public boolean addRooms(String location, int numRooms, int price) throws RemoteException {
        boolean success = RMIMiddleware.roomsResourceManager.addRooms(location, numRooms, price);
        if (success) {
            RMIMiddleware.customersResourceManager.addRooms(location, numRooms, price);
        }
        return success;
    }

    @Override
    public int newCustomer() throws RemoteException {
        int customerId = RMIMiddleware.customersResourceManager.newCustomer();
        RMIMiddleware.flightsResourceManager.newCustomer(customerId);
        RMIMiddleware.roomsResourceManager.newCustomer(customerId);
        RMIMiddleware.carsResourceManager.newCustomer(customerId);
        return customerId;
    }

    @Override
    public boolean newCustomer(int cid) throws RemoteException {
        boolean success = RMIMiddleware.customersResourceManager.newCustomer(cid);
        if (success) {
            RMIMiddleware.flightsResourceManager.newCustomer(cid);
            RMIMiddleware.roomsResourceManager.newCustomer(cid);
            RMIMiddleware.carsResourceManager.newCustomer(cid);
        }
        return success;
    }

    @Override
    public boolean deleteFlight(int flightNum) throws RemoteException {
        boolean success = RMIMiddleware.flightsResourceManager.deleteFlight(flightNum);
        if (success) {
            RMIMiddleware.customersResourceManager.deleteFlight(flightNum);
        }
        return success;
    }

    @Override
    public boolean deleteCars(String location) throws RemoteException {
        boolean success = RMIMiddleware.carsResourceManager.deleteCars(location);
        if (success) {
            RMIMiddleware.customersResourceManager.deleteCars(location);
        }
        return success;
    }

    @Override
    public boolean deleteRooms(String location) throws RemoteException {
        boolean success = RMIMiddleware.roomsResourceManager.deleteRooms(location);
        if (success) {
            RMIMiddleware.customersResourceManager.deleteRooms(location);
        }
        return success;
    }

    @Override
    public boolean deleteCustomer(int customerID) throws RemoteException {
        boolean success = RMIMiddleware.customersResourceManager.deleteCustomer(customerID);
        if (success) {
            RMIMiddleware.flightsResourceManager.deleteCustomer(customerID);
            RMIMiddleware.roomsResourceManager.deleteCustomer(customerID);
            RMIMiddleware.carsResourceManager.deleteCustomer(customerID);
        }
        return success;
    }

    @Override
    public int queryFlight(int flightNumber) throws RemoteException {
        return RMIMiddleware.flightsResourceManager.queryFlight(flightNumber);
    }

    @Override
    public int queryCars(String location) throws RemoteException {
        return RMIMiddleware.carsResourceManager.queryCars(location);
    }

    @Override
    public int queryRooms(String location) throws RemoteException {
        return RMIMiddleware.roomsResourceManager.queryRooms(location);
    }

    @Override
    public String queryCustomerInfo(int customerID) throws RemoteException {
        return RMIMiddleware.customersResourceManager.queryCustomerInfo(customerID);
    }

    @Override
    public int queryFlightPrice(int flightNumber) throws RemoteException {
        return RMIMiddleware.flightsResourceManager.queryFlightPrice(flightNumber);
    }

    @Override
    public int queryCarsPrice(String location) throws RemoteException {
        return RMIMiddleware.carsResourceManager.queryCarsPrice(location);
    }

    @Override
    public int queryRoomsPrice(String location) throws RemoteException {
        return RMIMiddleware.roomsResourceManager.queryRoomsPrice(location);
    }

    @Override
    public boolean reserveFlight(int customerID, int flightNumber) throws RemoteException {
        boolean success = RMIMiddleware.flightsResourceManager.reserveFlight(customerID, flightNumber);
        if (success) {
            RMIMiddleware.customersResourceManager.reserveFlight(customerID, flightNumber);
        }
        return success;
    }

    @Override
    public boolean reserveCar(int customerID, String location) throws RemoteException {
        boolean success = RMIMiddleware.carsResourceManager.reserveCar(customerID, location);
        if (success) {
            RMIMiddleware.customersResourceManager.reserveCar(customerID, location);
        }
        return success;
    }

    @Override
    public boolean reserveRoom(int customerID, String location) throws RemoteException {
        boolean success = RMIMiddleware.roomsResourceManager.reserveRoom(customerID, location);
        if (success) {
            RMIMiddleware.customersResourceManager.reserveRoom(customerID, location);
        }
        return success;
    }

    @Override
    public boolean bundle(int customerID, Vector<String> flightNumbers, String location, boolean car, boolean room)
            throws RemoteException {

        boolean preSuccess = true;

        for (String flightNumber : flightNumbers) {
            preSuccess = preSuccess
                    && RMIMiddleware.flightsResourceManager.reserveFlight(customerID, Integer.parseInt(flightNumber));
        }
        System.out.println("PreSuccess: " + preSuccess);
        System.out.println("Reserving car: " + car);
        if (car && preSuccess) {
            System.out.println("Reserving car");
            preSuccess = preSuccess && RMIMiddleware.carsResourceManager.reserveCar(customerID, location);
        }
        System.out.println("Reserving room: " + room);
        System.out.println("PreSuccess: " + preSuccess);
        if (room && preSuccess) {
            System.out.println("Reserving room");
            preSuccess = preSuccess && RMIMiddleware.roomsResourceManager.reserveRoom(customerID, location);
        }

        if (preSuccess) {
            return RMIMiddleware.customersResourceManager.bundle(customerID, flightNumbers, location, car, room);
        } else {
            System.out.println("Failed... rolling back");
            for (String flightNumber : flightNumbers) {
                RMIMiddleware.flightsResourceManager.unreserveFlight(customerID, Integer.parseInt(flightNumber));
            }
            RMIMiddleware.carsResourceManager.unreserveCar(customerID, location);
            RMIMiddleware.roomsResourceManager.unreserveRoom(customerID, location);
        }

        return false;
    }

    @Override
    public String getName() throws RemoteException {
        throw new UnsupportedOperationException("Unimplemented method 'getName'");
    }

    @Override
    public boolean unreserveFlight(int customerID, int flightNumber) throws RemoteException {
        throw new UnsupportedOperationException("Unimplemented method 'unreserveFlight'");
    }

    @Override
    public boolean unreserveCar(int customerID, String location) throws RemoteException {
        throw new UnsupportedOperationException("Unimplemented method 'unreserveCar'");
    }

    @Override
    public boolean unreserveRoom(int customerID, String location) throws RemoteException {
        throw new UnsupportedOperationException("Unimplemented method 'unreserveRoom'");
    }

}
