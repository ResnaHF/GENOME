package RMI;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public class ClientRemote {
    private static IServerRemote s_serverRemote = null;

    public static void connectServer() throws RemoteException, NotBoundException, MalformedURLException {
        s_serverRemote = (IServerRemote) Naming.lookup("rmi://localhost:20042/Server");
    }

    public static void disconnectServer(){
        s_serverRemote = null;
    }

    public static IServerRemote getServerRemote(){
        return s_serverRemote;
    }
}
