package Server.RMI;

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

    public static void main(String args[]) {

        try {
            // Create a new Server object
            RMIMiddleware server = new RMIMiddleware();

            // Dynamically generate the stub (client proxy)
            IResourceManager middleware = (IResourceManager) UnicastRemoteObject.exportObject(server, 0);

            // Bind the remote object's stub in the registry; adjust port if appropriate
            Registry l_registry;
            try {
                l_registry = LocateRegistry.createRegistry(1030);
            } catch (RemoteException e) {
                l_registry = LocateRegistry.getRegistry(1030);
            }
            final Registry registry = l_registry;

            // group_30_Middleware in registry
            registry.rebind(s_rmiPrefix + s_serverName, middleware);

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

    @Override
    public boolean addFlight(int flightNum, int flightSeats, int flightPrice) throws RemoteException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'addFlight'");
    }

    @Override
    public boolean addCars(String location, int numCars, int price) throws RemoteException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'addCars'");
    }

    @Override
    public boolean addRooms(String location, int numRooms, int price) throws RemoteException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'addRooms'");
    }

    @Override
    public int newCustomer() throws RemoteException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'newCustomer'");
    }

    @Override
    public boolean newCustomer(int cid) throws RemoteException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'newCustomer'");
    }

    @Override
    public boolean deleteFlight(int flightNum) throws RemoteException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deleteFlight'");
    }

    @Override
    public boolean deleteCars(String location) throws RemoteException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deleteCars'");
    }

    @Override
    public boolean deleteRooms(String location) throws RemoteException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deleteRooms'");
    }

    @Override
    public boolean deleteCustomer(int customerID) throws RemoteException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deleteCustomer'");
    }

    @Override
    public int queryFlight(int flightNumber) throws RemoteException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'queryFlight'");
    }

    @Override
    public int queryCars(String location) throws RemoteException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'queryCars'");
    }

    @Override
    public int queryRooms(String location) throws RemoteException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'queryRooms'");
    }

    @Override
    public String queryCustomerInfo(int customerID) throws RemoteException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'queryCustomerInfo'");
    }

    @Override
    public int queryFlightPrice(int flightNumber) throws RemoteException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'queryFlightPrice'");
    }

    @Override
    public int queryCarsPrice(String location) throws RemoteException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'queryCarsPrice'");
    }

    @Override
    public int queryRoomsPrice(String location) throws RemoteException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'queryRoomsPrice'");
    }

    @Override
    public boolean reserveFlight(int customerID, int flightNumber) throws RemoteException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'reserveFlight'");
    }

    @Override
    public boolean reserveCar(int customerID, String location) throws RemoteException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'reserveCar'");
    }

    @Override
    public boolean reserveRoom(int customerID, String location) throws RemoteException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'reserveRoom'");
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
