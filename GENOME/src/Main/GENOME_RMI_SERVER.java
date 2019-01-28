package Main;

import Console.Console;
import Data.IDataBase;
import RMI.IServerRemote;
import Utils.Logs;
import Utils.Options;

import java.io.File;
import java.io.IOException;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

final class GENOME_RMI_SERVER extends UnicastRemoteObject implements IServerRemote {

    public GENOME_RMI_SERVER() throws RemoteException{
        super();
    }

    public void test(String message) throws RemoteException{
        System.out.println("message reçu : " + message);
    }

    public IDataBase recup(String name) throws RemoteException{
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

    /**
     * Main
     *
     * @param args args
     */
    public static void main(String[] args) {
        // if (lock()) {
            try {

                System.out.println("objLocal en préparation") ;
                Registry test = LocateRegistry.createRegistry(20042); //start serveur
                GENOME_RMI_SERVER objLocal = new GENOME_RMI_SERVER();
                Naming.rebind("rmi://localhost:20042/Server",objLocal);  //on publie la ref de l'objet bean
                System.out.println("objLocal pret") ;

                //   System.setProperty("java.rmi.server.hostname","127.0.0.1");
                // Registry registry = LocateRegistry.createRegistry(42012);
                //registry.rebind( "rmi://"+"127.0.0.1"+":"+42012+"/SERVER" ,objLocal) ;

                Logs.setListener((_message, _type) -> {
                    switch (_type) {
                        case EXCEPTION:
                            System.err.println(_message);
                            break;
                        default:
                            System.out.println(_message);
                            break;
                    }
                });
                initializeProgram();
                Runtime.getRuntime().addShutdownHook(new Thread(GENOME_RMI_SERVER::finalizeProgram));
                Console.getSingleton().addStartListener(GenomeRMIServerActivity::genbank);
                Console.getSingleton().addStopListener(GenomeRMIServerActivity::stop);
                Console.getSingleton().addPauseListener(GenomeRMIServerActivity::pause);
                Console.getSingleton().addResumeListener(GenomeRMIServerActivity::resume);
                Console.getSingleton().run();
                Naming.unbind("rmi://localhost:20042/Server");
                System.exit(0);
            } catch (Throwable e) {
                Logs.exception(e);
                e.printStackTrace();
            }
    }

    /**
     * Function call at the begin of the program
     */
    private static void initializeProgram() {
        Logs.initializeLog();
        Logs.info("Log initialized", true);
        Options.initializeOptions();
        Logs.info("Options initialized", true);
        Logs.info("Begin", true);
    }

    /**
     * Function call at the end of the program
     */
    private static void finalizeProgram() {
        GenomeRMIServerActivity.stopAndWait();
        Logs.info("End", true);
        Logs.info("Options finalized", true);
        Options.finalizeOptions();
        Logs.info("Log finalized", true);
        unlock();
        Logs.finalizeLog();
    }

    /**
     * Lock the programme
     *
     * @return true is the programme is lock
     */
    private static boolean lock() {
        File mutex = new File(Options.getMutexFileName());
        try {
            return mutex.createNewFile();
        } catch (IOException | SecurityException e) {
            Logs.warning("Enable to create mutex file : " + Options.getMutexFileName());
        }
        return false;
    }

    /**
     * Unlock the programme
     */
    private static void unlock() {
        File mutex = new File(Options.getMutexFileName());
        if (mutex.exists()) {
            try {
                if (!mutex.delete()) {
                    Logs.warning("Enable to delete mutex file : " + Options.getMutexFileName());
                }
            } catch (SecurityException e) {
                Logs.warning("Enable to delete mutex file : " + Options.getMutexFileName());
                Logs.exception(e);
            }
        }
    }

}
