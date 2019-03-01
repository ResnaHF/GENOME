package Main;

import Data.*;
import Download.CDSParser;
import Download.GenbankCDS;
import Download.GenbankOrganisms;
import Download.OrganismParser;
import Exception.*;
import Manager.ITask;
import Manager.ThreadManager;
import Utils.Logs;
import Utils.Options;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

final class GenomeRMIServerActivity {

    private static final Lock s_WAIT_LOCK = new ReentrantLock();
    private static final Condition s_COND = s_WAIT_LOCK.newCondition();
    private static final Object s_STOP_LOCK = new Object();
    private static final Object s_COMPUTE_LOCK = new Object();
    private static final Object s_RUN_LOCK = new Object();
    private static Boolean s_stop = false;
    private static Boolean s_compute = false;
    private static Boolean s_run = false;
    private static Thread s_activityThread = null;
    private static Thread s_runThread = null;
    private static boolean s_wait = false;

    /**
     * Run main activity
     *
     * @return true if activity is started
     */
    public static boolean genbank() {
        boolean compute = true;
        synchronized (s_COMPUTE_LOCK) {
            if (!s_compute) {
                compute = false;
                s_compute = true;
            }
        }
        if (!compute) {
            Logs.notice("Start", true);
            s_WAIT_LOCK.lock();
            {
                if (s_wait) {
                    s_wait = false;
                }
            }
            s_WAIT_LOCK.unlock();
            synchronized (s_STOP_LOCK) {
                if (s_stop) {
                    s_stop = false;
                }
            }
            s_activityThread = new Thread(() -> {
                Date beg = new Date();
                final int[] fail = {0};
                final int[] index = {0};
                boolean cancel = false;
                ThreadManager threadManager = new ThreadManager(Runtime.getRuntime().availableProcessors() * 4);
                try {
                    final GenbankOrganisms go = new GenbankOrganisms();
                    go.downloadOrganisms();

                    final DataBase currentDataBase = DataBase.load(Options.getGenbankName(), _dataBase -> {
                        _dataBase.save();
                    });
                    currentDataBase.start();

                    Kingdom currentKingdom = Kingdom.load("", currentDataBase, _kingdom -> {
                    });
                    currentKingdom.start();

                    Group currentGroup = Group.load("", currentKingdom, _group -> {
                    });
                    currentGroup.start();

                    SubGroup currentSubGroup = SubGroup.load("", currentGroup, _subGroup -> {
                    });
                    currentSubGroup.start();

                    final Object m_indexLock = new Object();
                    while (go.hasNext()) {
                        wait(GenomeRMIServerActivity.class.toString());
                        synchronized (s_STOP_LOCK) {
                            if (s_stop) {
                                Logs.notice("Stop main loop", true);
                                cancel = true;
                                break;
                            }
                        }
                        final OrganismParser organismParser = go.getNext();
                        final String organismName = organismParser.getName() + "-" + organismParser.getId();

                        final Date dateModif = Organism.loadDate(Options.getGenbankName(), organismParser.getKingdom(), organismParser.getGroup(), organismParser.getSubGroup(), organismName);
                        if (dateModif != null && organismParser.getModificationDate().compareTo(dateModif) <= 0) {
                            Logs.info("Organism " + organismName + " already up to date", false);
                            continue;
                        }

                        if (organismParser.getKingdom().compareTo(currentKingdom.getName()) != 0) {
                            currentKingdom = switchKingdom(currentKingdom, organismParser.getKingdom(), currentDataBase);
                            currentGroup = switchGroup(currentGroup, organismParser.getGroup(), currentKingdom);
                            currentSubGroup = switchSubGroup(currentSubGroup, organismParser.getSubGroup(), currentGroup);
                        } else if (organismParser.getGroup().compareTo(currentGroup.getName()) != 0) {
                            currentGroup = switchGroup(currentGroup, organismParser.getGroup(), currentKingdom);
                            currentSubGroup = switchSubGroup(currentSubGroup, organismParser.getSubGroup(), currentGroup);
                        } else if (organismParser.getSubGroup().compareTo(currentSubGroup.getName()) != 0) {
                            currentSubGroup = switchSubGroup(currentSubGroup, organismParser.getSubGroup(), currentGroup);
                        }

                        Organism organism = Organism.load(organismName, organismParser.getId(), organismParser.getVersion(), currentSubGroup, true, _organism -> {
                            _organism.save();
                        });

                        // Thread
                        threadManager.pushTask(new ITask(organismName) {
                            @Override
                            public void run() {
                                try {
                                    try {
                                        organism.start();
                                    } catch (InvalidStateException e) {
                                        Logs.warning("Unable to start : " + organism.getName());
                                        Logs.exception(e);
                                        return;
                                    }
                                    for (Map.Entry<String, String> ent : organismParser.getReplicons()) {
                                        GenomeRMIServerActivity.wait(getName());
                                        final GenbankCDS cdsDownloader = new GenbankCDS(ent.getKey());
                                        try {
                                            cdsDownloader.download();
                                        } catch (HTTPException | IOException | OutOfMemoryException e) {
                                            Logs.warning("Unable to download : " + ent.getKey());
                                            Logs.exception(e);
                                            throw e;
                                        }
                                        final CDSParser cdsParser = new CDSParser(cdsDownloader.getRefseqData(), ent.getKey());
                                        try {
                                            cdsParser.parse();
                                            if (Options.getSaveGenome()) {
                                                final String path = Options.getGenomeDirectory() + File.separator + organismParser.getKingdom() + File.separator + organismParser.getGroup() + File.separator + organismParser.getSubGroup() + File.separator + organismParser.getName();
                                                cdsParser.saveGenome(path);
                                            }
                                            if (Options.getSaveGene()) {
                                                final String path = Options.getGeneDirectory() + File.separator + organismParser.getKingdom() + File.separator + organismParser.getGroup() + File.separator + organismParser.getSubGroup() + File.separator + organismParser.getName();
                                                cdsParser.saveGene(path);
                                            }
                                        } catch (OperatorException e) {
                                            Logs.warning("Unable to parse : " + ent.getKey());
                                            Logs.exception(e);
                                            throw e;
                                        }

                                        final Replicon replicon = new Replicon(Statistics.Type.isTypeOf(ent.getValue()), ent.getKey(), cdsParser.getTotal(), cdsParser.getValid(), cdsParser.getSequences());
                                        try {
                                            organism.addReplicon(replicon);
                                        } catch (AddException e) {
                                            Logs.warning("Unable to add replicon : " + replicon.getName());
                                            Logs.exception(e);
                                            throw e;
                                        }
                                    }
                                } catch (OutOfMemoryError e) {
                                    Logs.warning("Memory error from organism : " + organism.getName());
                                    Logs.exception(new Exception(e));
                                    try {
                                        organism.cancel();
                                        Logs.info("Cancel organism : " + organism.getName(), true);
                                        synchronized (m_indexLock) {
                                            ++fail[0];
                                        }
                                    } catch (InvalidStateException e1) {
                                        Logs.warning("Unable to cancel : " + organism.getName());
                                        Logs.exception(e);
                                    }
                                } catch (Throwable e) {
                                    Logs.warning("Error from organism : " + organism.getName());
                                    Logs.exception(e);
                                    try {
                                        organism.cancel();
                                        Logs.info("Cancel organism : " + organism.getName(), true);
                                        synchronized (m_indexLock) {
                                            ++fail[0];
                                        }
                                    } catch (InvalidStateException e1) {
                                        Logs.warning("Unable to cancel : " + organism.getName());
                                        Logs.exception(e);
                                    }
                                } finally {
                                    try {
                                        organism.stop();
                                    } catch (InvalidStateException e) {
                                        Logs.warning("Unable to stop : " + organism.getName());
                                        Logs.exception(e);
                                    }
                                    try {
                                        organism.finish();
                                    } catch (InvalidStateException e) {
                                        Logs.warning("Unable to finish : " + organism.getName());
                                        Logs.exception(e);
                                    }
                                }
                            }

                            @Override
                            public void cancel() {
                                try {
                                    organism.start();
                                    organism.stop();
                                    organism.cancel();
                                    organism.finish();
                                } catch (InvalidStateException e) {
                                    Logs.warning("Unable to cancel : " + organism.getName());
                                    Logs.exception(e);
                                }
                            }
                        });
                    }

                    currentDataBase.stop();
                    currentKingdom.stop();
                    currentGroup.stop();
                    currentSubGroup.stop();
                } catch (InvalidStateException | AddException | MissException e) {
                    Logs.warning("Unable to run programme");
                    Logs.exception(e);
                } catch (Throwable e) {
                    Logs.warning("Unable to run programme, unexpected error");
                    Logs.exception(e);
                } finally {
                    Logs.notice("Finished and wait for threads...", true);
                    threadManager.finalizeThreadManager(cancel);
                    synchronized (s_COMPUTE_LOCK) {
                        s_compute = false;
                    }
                    Date end = new Date();
                    Logs.notice("Execution time : " + getDifference(beg, end), true);
                    Logs.notice("Failled organisms : " + fail[0], true);
                }
            });
            s_activityThread.start();
            return true;
        }
        return false;
    }

    /**
     * Send run request
     *
     * @return if true if success
     */
    public static boolean run() {
        boolean run = true;
        synchronized (s_RUN_LOCK) {
            if (!s_run) {
                run = false;
                s_run = true;
            }
        }
        if (!run) {
            s_runThread = new Thread(() -> {
                boolean res = genbank();
                if(!res) {
                    Logs.warning("Unable to launch the main program");
                }
                try {
                    Thread.sleep(Options.getServerLoopTime());
                } catch (InterruptedException e) {
                    Logs.warning("Error while sleeping");
                    Logs.exception(e);
                }
            });
            s_runThread.start();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Send stop request
     *
     * @return if true if success
     */
    public static boolean stop() {
        boolean ret = false;
        boolean run = false;
        synchronized (s_COMPUTE_LOCK) {
            if (s_compute) {
                run = true;
            }
        }
        if (run) {
            synchronized (s_STOP_LOCK) {
                if (!s_stop) {
                    Logs.notice("stop requested ...", true);
                    s_stop = true;
                    ret = true;
                    s_WAIT_LOCK.lock();
                    {
                        if (s_wait) {
                            s_wait = false;
                            s_COND.signalAll();
                        }
                    }
                    s_WAIT_LOCK.unlock();
                }
            }
        }
        return ret;
    }

    /**
     * Send stop request and wait all threads
     */
    public static void stopAndWait() {
        stop();
        if (s_activityThread != null) {
            try {
                s_activityThread.join();
            } catch (InterruptedException e) {
                Logs.exception(e);
            }
        }
    }

    /**
     * Send pause request
     *
     * @return if true if success
     */
    public static boolean pause() {
        boolean ret = false;
        boolean run = false;
        synchronized (s_COMPUTE_LOCK) {
            if (s_compute) {
                run = true;
            }
        }
        if (run) {
            s_WAIT_LOCK.lock();
            {
                if (!s_wait) {
                    Logs.notice("pause requested ...", true);
                    s_wait = true;
                    ret = true;
                }
            }
            s_WAIT_LOCK.unlock();
        }
        return ret;
    }

    /**
     * Send resume request
     *
     * @return if true if success
     */
    public static boolean resume() {
        boolean ret = false;
        boolean run = false;
        synchronized (s_COMPUTE_LOCK) {
            if (s_compute) {
                run = true;
            }
        }
        if (run) {
            s_WAIT_LOCK.lock();
            {
                if (s_wait) {
                    Logs.notice("resume requested ...", true);
                    s_wait = false;
                    s_COND.signalAll();
                    ret = true;
                }
            }
            s_WAIT_LOCK.unlock();
        }
        return ret;
    }

    /**
     * Wait if requested
     *
     * @param _name the name of the task
     */
    private static void wait(String _name) {
        s_WAIT_LOCK.lock();
        {
            while (s_wait) {
                Logs.notice(_name + " : wait...", true);
                try {
                    s_COND.await();
                } catch (InterruptedException e) {
                    Logs.exception(e);
                }
            }
        }
        s_WAIT_LOCK.unlock();
    }

    /**
     * Get difference between two date
     *
     * @param _startDate the start date
     * @param _endDate   the and date
     * @return the String displaying the difference
     */
    private static String getDifference(Date _startDate, Date _endDate) {
        long different = _endDate.getTime() - _startDate.getTime();

        long secondsInMilli = 1000;
        long minutesInMilli = secondsInMilli * 60;
        long hoursInMilli = minutesInMilli * 60;
        long daysInMilli = hoursInMilli * 24;

        long elapsedDays = different / daysInMilli;
        different = different % daysInMilli;

        long elapsedHours = different / hoursInMilli;
        different = different % hoursInMilli;

        long elapsedMinutes = different / minutesInMilli;
        different = different % minutesInMilli;

        long elapsedSeconds = different / secondsInMilli;

        return elapsedDays + " day " + elapsedHours + " hours " + elapsedMinutes + " minutes " + elapsedSeconds + " second";
    }

    private static Kingdom switchKingdom(Kingdom _currentKingdom, String _newKingdom, DataBase _parent) throws InvalidStateException, AddException {
        _currentKingdom.stop();
        _currentKingdom = Kingdom.load(_newKingdom, _parent, _kingdom -> {
            _kingdom.save();
        });
        _currentKingdom.start();
        return _currentKingdom;
    }

    private static Group switchGroup(Group _currentGroup, String _newGroup, Kingdom _parent) throws InvalidStateException, AddException {
        _currentGroup.stop();
        _currentGroup = Group.load(_newGroup, _parent, _group -> {
            _group.save();
        });
        _currentGroup.start();
        return _currentGroup;
    }

    private static SubGroup switchSubGroup(SubGroup _currentSubGroup, String _newSubGroup, Group _parent) throws InvalidStateException, AddException {
        _currentSubGroup.stop();
        _currentSubGroup = SubGroup.load(_newSubGroup, _parent, _subGroup -> {
            _subGroup.save();
        });
        _currentSubGroup.start();
        return _currentSubGroup;
    }

}
