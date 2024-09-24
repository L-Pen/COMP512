package Server.RMI;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Vector;

import Server.Interface.IResourceManager;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class RMIMiddleware implements IResourceManager {
    private static String s_serverName = "Middleware";
    private static String s_rmiPrefix = "group_30_";

    private static IResourceManager flightsResourceManager;
    private static IResourceManager carsResourceManager;
    private static IResourceManager roomsResourceManager;
    private static IResourceManager customersResourceManager;

    private static int registryPortNumber = 1030;

    public static void main(String args[]) {

        try {
            // Create a new Server object
            RMIMiddleware server = new RMIMiddleware();

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

            if (args.length != 4)
                throw new Exception(
                        "We need a flights, cars, rooms and customer server names. Whatever u put is not equal to 4");

            RMIMiddleware.flightsResourceManager = connectServer(args[0], registryPortNumber, "Flights");
            RMIMiddleware.carsResourceManager = connectServer(args[1], registryPortNumber, "Cars");
            RMIMiddleware.roomsResourceManager = connectServer(args[2], registryPortNumber, "Rooms");
            RMIMiddleware.customersResourceManager = connectServer(args[3], registryPortNumber, "Customers");

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
        return RMIMiddleware.flightsResourceManager.addFlight(flightNum, flightSeats, flightPrice);
    }

    @Override
    public boolean addCars(String location, int numCars, int price) throws RemoteException {
        return RMIMiddleware.carsResourceManager.addCars(location, numCars, price);
    }

    @Override
    public boolean addRooms(String location, int numRooms, int price) throws RemoteException {
        return RMIMiddleware.roomsResourceManager.addCars(location, numRooms, price);
    }

    @Override
    public int newCustomer() throws RemoteException {
        return RMIMiddleware.customersResourceManager.newCustomer();
    }

    @Override
    public boolean newCustomer(int cid) throws RemoteException {
        return RMIMiddleware.customersResourceManager.newCustomer(cid);
    }

    @Override
    public boolean deleteFlight(int flightNum) throws RemoteException {
        return RMIMiddleware.flightsResourceManager.deleteFlight(flightNum);
    }

    @Override
    public boolean deleteCars(String location) throws RemoteException {
        return RMIMiddleware.carsResourceManager.deleteCars(location);
    }

    @Override
    public boolean deleteRooms(String location) throws RemoteException {
        return RMIMiddleware.roomsResourceManager.deleteRooms(location);
    }

    @Override
    public boolean deleteCustomer(int customerID) throws RemoteException {
        return RMIMiddleware.customersResourceManager.deleteCustomer(customerID);
    }

    @Override
    public int queryFlight(int flightNumber) throws RemoteException {
        return RMIMiddleware.flightsResourceManager.queryFlight(flightNumber);
    }

    @Override
    public int queryCars(String location) throws RemoteException {
        return RMIMiddleware.carsResourceManager.queryCars(location);
    }
    //up to here
    @Override
    public int queryRooms(String location) throws RemoteException {
        // TODO Auto-generated method stub
        return RMIMiddleware.roomsResourceManager.queryRooms(location);
    }

    @Override
    public String queryCustomerInfo(int customerID) throws RemoteException {
        // TODO Auto-generated method stub
        return RMIMiddleware.customersResourceManager.queryCustomerInfo(customerID);
    }

    @Override
    public int queryFlightPrice(int flightNumber) throws RemoteException {
        // TODO Auto-generated method stub
        return RMIMiddleware.flightsResourceManager.queryFlightPrice(flightNumber);
    }

    @Override
    public int queryCarsPrice(String location) throws RemoteException {
        // TODO Auto-generated method stubr
        return RMIMiddleware.carsResourceManager.queryCarsPrice(location);
    }

    @Override
    public int queryRoomsPrice(String location) throws RemoteException {
        // TODO Auto-generated method stub
        return RMIMiddleware.roomsResourceManager.queryRoomsPrice(location);
    }

    @Override
    public boolean reserveFlight(int customerID, int flightNumber) throws RemoteException {
        // TODO Auto-generated method stub
        return RMIMiddleware.flightsResourceManager.reserveFlight(customerID, flightNumber);
    }

    @Override
    public boolean reserveCar(int customerID, String location) throws RemoteException {
        // TODO Auto-generated method stub
        return RMIMiddleware.carsResourceManager.reserveCar(customerID, location)
        throw new UnsupportedOperationException("Unimplemented method 'reserveCar'");
    }

    @Override
    public boolean reserveRoom(int customerID, String location) throws RemoteException {
        // TODO Auto-generated method stub
        return RMIMiddleware.roomsResourceManager.reserveRoom(customerID, location);
    }

    @Override
    public boolean bundle(int customerID, Vector<String> flightNumbers, String location, boolean car, boolean room)
            throws RemoteException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'bundle'");
    }

    @Override
    public String getName() throws RemoteException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getName'");
    }

}
