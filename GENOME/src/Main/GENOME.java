package Main;

import Data.IDataBase;
import GUI.MainFrame;
import GUI.WarningFrame;
import RMI.ClientRemote;
import Utils.Logs;
import Utils.Options;

import javax.swing.*;
import java.io.File;
import java.io.IOException;

final class GENOME {

    /**
     * Main
     *
     * @param args args
     */
    public static void main(String[] args) {


        System.exit(0);
        if (lock()) {
            try {
                Logs.setListener((_message, _type) -> {
                    MainFrame.getSingleton().updateLog(_message, _type);
                    switch (_type) {
                        case EXCEPTION:
                            System.err.println(_message);
                            break;
                        default:
                            System.out.println(_message);
                            break;
                    }
                });
                ClientRemote.connectServer();
                MainFrame.getSingleton().addStartListener(GenomeActivity::genbank);
                MainFrame.getSingleton().addStopListener(GenomeActivity::stop);
                MainFrame.getSingleton().addPauseListener(GenomeActivity::pause);
                MainFrame.getSingleton().addResumeListener(GenomeActivity::resume);
                MainFrame.getSingleton().addTreeListener(_info -> {
                    IDataBase organism = IDataBase.load(_info);
                    if (organism != null) {
                        MainFrame.getSingleton().updateInformation(JDataBase.createComponent(organism));
                    } else {
                        MainFrame.getSingleton().updateInformation(new JTextArea());
                    }
                });
                initializeProgram();
                Runtime.getRuntime().addShutdownHook(new Thread(GENOME::finalizeProgram));
            } catch (Throwable e) {
                Logs.exception(e);
            }
        } else {
            new WarningFrame();
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
        GenomeActivity.stopAndWait();
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
