package RMI;

import Data.IDataBase;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;

public class ServerRemote extends UnicastRemoteObject implements IServerRemote {

    private ServerRemote() throws RemoteException{
        super();
    }

    public static void startServerRemote() throws RemoteException, MalformedURLException {
        LocateRegistry.createRegistry(20042);
        ServerRemote objLocal = new ServerRemote();
        Naming.rebind("rmi://localhost:20042/Server",objLocal);
        System.out.println("ServerRemote ready") ;
    }

    public static void stopServerRemote() throws RemoteException, NotBoundException, MalformedURLException {
        Naming.unbind("rmi://localhost:20042/Server");
        System.exit(0);
    }

    @Override
    public void test(String message){
        System.out.println("message reçu : " + message);
    }

    @Override
    public IDataBase recup(String name){
        IDataBase data = IDataBase.load(name);
        if (data == null){
            System.out.println("t'es null !");
        }
        else
        {
            System.out.println(data.getName());
            System.out.println(data.getCDSNumber());
            System.out.println(data.getValidCDSNumber());
            System.out.println(data.getState());
            System.out.println(data.getTotalOrganism());
            System.out.println(data.getGenomeNumber());
        }
        return data;
    }
}
