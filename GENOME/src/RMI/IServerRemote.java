package RMI;

import Data.IDataBase;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IServerRemote extends Remote {
    void test(String message) throws RemoteException;
    IDataBase recup(String name) throws RemoteException;
}
