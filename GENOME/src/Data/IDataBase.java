package Data;

import Exception.InvalidStateException;
import RMI.ClientRemote;
import Utils.Logs;
import Utils.Options;

import java.io.*;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;

public class IDataBase implements Serializable {

    private static final long serialVersionUID = 2L;

    /**
     * The name
     */
    private final String m_NAME;
    /**
     * Statistics of this IDataBase
     */
    private final EnumMap<Statistics.Type, Statistics> m_STATISTICS;
    /**
     * Array of values of each Replicon's type
     */
    private final EnumMap<Statistics.Type, Long> m_GENOME_NUMBER;
    /**
     * Last modification's date
     */
    private transient final Date m_MODIFICATIONDATE;
    /**
     * Is the Data loaded or not
     */
    private transient final Boolean m_LOADED;
    /**
     * The number of CDS sequences
     */
    private long m_CDSNumber;
    /**
     * The number of valid CDS sequences
     */
    private long m_validCDSNumber;
    /**
     * The number of underlying organism
     */
    private long m_totalOrganism;
    /**
     * Actual State
     */
    private transient State m_state;
    /**
     * Local index
     */
    private transient int m_index;
    /**
     * Total of finished children
     */
    private transient int m_finished;

    /**
     * Class constructor
     *
     * @param _name the name
     */
    protected IDataBase(String _name) {
        m_NAME = _name;
        m_MODIFICATIONDATE = new Date();
        m_STATISTICS = new EnumMap<>(Statistics.Type.class);
        m_GENOME_NUMBER = new EnumMap<>(Statistics.Type.class);
        m_CDSNumber = 0L;
        m_validCDSNumber = 0L;
        m_totalOrganism = 0L;
        m_state = State.CREATED;
        m_index = -1;
        m_finished = 0;
        m_LOADED = false;
    }

    /**
     * C
     *
     * @param _name the name
     * @param _data previous data
     */
    IDataBase(String _name, IDataBase _data) {
        m_NAME = _name;
        m_MODIFICATIONDATE = new Date();
        m_STATISTICS = _data.m_STATISTICS;
        m_GENOME_NUMBER = _data.m_GENOME_NUMBER;
        m_CDSNumber = _data.m_CDSNumber;
        m_validCDSNumber = _data.m_validCDSNumber;
        m_totalOrganism = _data.m_totalOrganism;
        m_state = State.CREATED;
        m_index = -1;
        m_finished = 0;
        m_LOADED = true;
    }

    /**
     * Load a data from a file
     *
     * @param _name the name of the file to load
     * @return the IDatabase loaded
     */
    public static IDataBase load(String _name) {
        IDataBase result = null;
        if(ClientRemote.getServerRemote() != null){
            try {
                result = ClientRemote.getServerRemote().recup("D_Genbank--K_archaea--G_euryarchaeota--SG_archaeoglobi--O_archaeoglobus_sulfaticallidus_pm70_1-12088");
            } catch (RemoteException e) {
                e.printStackTrace();
                return null;
            }
        }else {

            final File file = new File(Options.getSerializeDirectory() + File.separator + _name + Options.getSerializeExtension());
            ObjectInputStream stream = null;
            if (!file.exists()) {
                return null;
            }
            try {
                stream = new ObjectInputStream((new FileInputStream(file)));
                result = (IDataBase) stream.readObject();
            } catch (IOException | ClassNotFoundException e) {
                Logs.warning("Unable to load : " + _name);
                Logs.exception(e);
            } finally {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e) {
                        Logs.warning("Unable to close : " + _name);
                        Logs.exception(e);
                    }
                }
            }
        }
        return result;
    }

    /**
     * Get actual State
     *
     * @return the State
     */
    public final State getState() {
        return m_state;
    }

    /**
     * Get the last modification's date
     *
     * @return the m_MODIFICATIONDATE
     */
    public final Date getModificationDate() {
        return m_MODIFICATIONDATE;
    }

    /**
     * Get the name
     *
     * @return the m_NAME
     */
    public final String getName() {
        return m_NAME;
    }

    /**
     * Get the statistics
     *
     * @return the statistics
     */
    public final EnumMap<Statistics.Type, Statistics> getStatistics() {
        return m_STATISTICS;
    }

    /**
     * Get number of each Genome's Type
     *
     * @return the number of each Genome's Type
     */
    public final EnumMap<Statistics.Type, Long> getGenomeNumber() {
        return m_GENOME_NUMBER;
    }

    /**
     * Get the number of invalid sequences
     *
     * @return the number of invalid sequences
     */
    public final long getCDSNumber() {
        return m_CDSNumber;
    }

    /**
     * Get the number of valid sequences
     *
     * @return the number of valid sequences
     */
    public final long getValidCDSNumber() {
        return m_validCDSNumber;
    }

    /**
     * Get the number of underlying organism
     *
     * @return the number of underlying organism
     */
    public final long getTotalOrganism() {
        return m_totalOrganism;
    }

    /**
     * Get the number of a genome's specified type
     *
     * @param _type, the Type of the genomes's number to get
     * @return the number of genomes
     */
    final long getTypeNumber(Statistics.Type _type) {
        return m_GENOME_NUMBER.get(_type);
    }

    /**
     * Set the local index
     *
     * @param _id, the index to set
     */
    final void setIndex(int _id) {
        m_index = _id;
    }

    /**
     * Increment by 1 the number of genome to a type
     *
     * @param _type, the Type of the genomes to increment
     */
    final void incrementGenomeNumber(Statistics.Type _type) {
        m_GENOME_NUMBER.merge(_type, 1L, (v1, v2) -> v1 + v2);
    }

    /**
     * Increment the number of genome of a type by the parameter
     *
     * @param _type, the Type of the genomes to increment
     * @param _inc,  the value of the increment
     */
    final void incrementGenomeNumber(Statistics.Type _type, long _inc) {
        m_GENOME_NUMBER.merge(_type, _inc, (v1, v2) -> v1 + v2);
    }

    /**
     * Create statistic if it's not exist and update it
     *
     * @param _statistics, the statistic to used for update
     */
    final void updateStatistics(Statistics _statistics) {
        m_STATISTICS.computeIfAbsent(_statistics.getType(), k -> new Statistics(_statistics.getType()));
        m_STATISTICS.get(_statistics.getType()).update(_statistics);
    }

    /**
     * Compute statistics
     */
    final void computeStatistics() {
        m_STATISTICS.values().parallelStream().forEach(Statistics::compute);
    }

    /**
     * Return true if the array contains the IState
     *
     * @param _arr,  the array to search
     * @param _stat, the IStat =e to find
     * @param <E>,   the class of the array
     * @return the find success
     */
    final <E> boolean contains(ArrayList<E> _arr, IDataBase _stat) {
        try {
            if (_arr.get(_stat.m_index) == null) {
                return false;
            }
        } catch (IndexOutOfBoundsException e) {
            return false;
        }
        return true;
    }

    /**
     * Increment the number of finish children
     */
    final void incrementFinishedChildren() {
        ++m_finished;
    }

    /**
     * Get the number of finish children
     *
     * @return the number of children finished
     */
    final int getFinishedChildren() {
        return m_finished;
    }

    /**
     * Clear data
     */
    final void clear() {
        m_STATISTICS.clear();
        m_GENOME_NUMBER.clear();
    }

    /**
     * Increment the generic totals with those of another
     *
     * @param _data, the data used to increment
     */
    final void incrementGenericTotals(IDataBase _data) {
        m_CDSNumber += _data.m_CDSNumber;
        m_validCDSNumber += _data.m_validCDSNumber;
        m_totalOrganism += _data.m_totalOrganism;
    }

    /**
     * Increment the generic totals with those of a Statistics
     *
     * @param _stat, the data used to increment
     */
    final void incrementGenericTotals(Statistics _stat) {
        m_CDSNumber += _stat.getCDSNumber();
        m_validCDSNumber += _stat.getValidCDSNumber();
    }

    /**
     * Set the total of underlying organism to one
     * used for initialise Organism
     */
    final void setTotalOrganismToOne() {
        m_totalOrganism = 1L;
    }

    /**
     * Start
     *
     * @throws InvalidStateException if it can't be started
     */
    public void start() throws InvalidStateException {
        if (m_state == State.STARTED)
            throw new InvalidStateException("Already started : " + this.getName());
        if (m_state == State.STOPPED || m_state == State.FINISHED)
            throw new InvalidStateException("Can't restart : " + this.getName());
        m_state = State.STARTED;
    }

    /**
     * Stop
     *
     * @throws InvalidStateException if it can't be stopped
     */
    public synchronized void stop() throws InvalidStateException {
        if (m_state == State.CREATED)
            throw new InvalidStateException("Not started : " + this.getName());
        if (m_state == State.STOPPED)
            throw new InvalidStateException("Already stopped : " + this.getName());
        if (m_state == State.FINISHED)
            throw new InvalidStateException("Already finished : " + this.getName());
        m_state = State.STOPPED;
    }

    /**
     * Save this data
     */
    public void save() {
        final File file = new File(Options.getSerializeDirectory() + File.separator + getSavedName() + Options.getSerializeExtension());
        ObjectOutputStream stream = null;
        if (file.exists()) {
            try {
                if (!file.delete()) {
                    Logs.warning("Enable to delete file : " + file.getName());
                }
            } catch (SecurityException e) {
                Logs.warning("Enable to delete file : " + file.getName());
                Logs.exception(e);
            }
        }
        try {
            if (!file.createNewFile()) {
                Logs.warning("Enable to create file : " + file.getName());
            }
            stream = new ObjectOutputStream(new FileOutputStream(file));
            stream.writeObject(this);
            stream.flush();
        } catch (IOException | SecurityException e) {
            Logs.warning("Unable to save : " + getSavedName());
            Logs.exception(e);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    Logs.warning("Unable to close : " + getSavedName());
                    Logs.exception(e);
                }
            }
        }
    }

    /**
     * Finish
     *
     * @throws InvalidStateException if it can't be finished
     */
    protected synchronized void finish() throws InvalidStateException {
        if (m_state == State.CREATED || m_state == State.STARTED)
            throw new InvalidStateException("Not stopped : " + this.getName());
        if (m_state == State.FINISHED)
            throw new InvalidStateException("Already finished : " + this.getName());
        m_state = State.FINISHED;
    }

    /**
     * Get the main part of the save path_name
     *
     * @return the main part of the save path_name
     */
    String getSavedName() {
        return getName();
    }

    /**
     * Unload data
     *
     * @param _data the data to unload
     */
    synchronized void unload(IDataBase _data) throws InvalidStateException {
        if (!m_LOADED)
            throw new InvalidStateException("Not loaded : " + m_NAME + ". Requested by : " + _data.getName());

        m_CDSNumber -= _data.m_CDSNumber;
        m_validCDSNumber -= _data.m_validCDSNumber;
        m_totalOrganism -= _data.m_totalOrganism;

        for (Statistics stat : _data.m_STATISTICS.values()) {
            Statistics.Type type = stat.getType();
            m_STATISTICS.get(type).unload(stat);
            m_GENOME_NUMBER.put(type, m_GENOME_NUMBER.get(type) - _data.m_GENOME_NUMBER.get(type));
        }
    }

    /**
     * Reset all values
     *
     * @throws InvalidStateException if it's finished
     */
    void cancel() throws InvalidStateException {
        if (getState() == State.CREATED || getState() == State.STARTED || getState() == State.STOPPED) {
            m_MODIFICATIONDATE.setTime(0);
            m_STATISTICS.clear();
            m_GENOME_NUMBER.clear();
            m_CDSNumber = 0L;
            m_validCDSNumber = 0L;
            m_totalOrganism = 0L;
            m_finished = 0;
        } else {
            throw new InvalidStateException("Impossible to cancel finished Organism : " + this.getName());
        }
    }

    /**
     * Type of each State
     */
    public enum State {
        CREATED,
        STARTED,
        STOPPED,
        FINISHED
    }

}

