package Main;

import Console.Console;
import RMI.ServerRemote;
import Utils.Logs;
import Utils.Options;

import java.io.File;
import java.io.IOException;

final class GENOME_RMI_SERVER {

    /**
     * Main
     *
     * @param args args
     */
    public static void main(String[] args) {
        // if (lock()) {
            try {
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
                ServerRemote.startServerRemote();
                Console.getSingleton().run();
                ServerRemote.stopServerRemote();
            } catch (Throwable e) {
                Logs.exception(e);
                e.printStackTrace();
            }
        //}
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
