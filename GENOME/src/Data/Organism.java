package Data;

import Exception.AddException;
import Exception.InvalidStateException;
import Utils.Logs;
import Utils.Options;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;

public final class Organism extends IDataBase {

    /**
     * Prefix used for serialization
     */
    private static final String s_SERIALIZATION_PREFIX = Options.getSerializationSpliter() + Options.getOrganismSerializationPrefix();
    /**
     * Array of this organism's Replicon
     */
    private final ArrayList<Replicon> m_REPLICONS;
    /**
     * The id of this organism
     */
    private transient final long m_ID;
    /**
     * The version of the organism
     */
    private transient final long m_VERSION;
    /**
     * Event to call when compute are finished
     */
    private transient IOrganismCallback m_event;
    /**
     * Reference to the parent
     */
    private transient SubGroup m_parent;
    /**
     * True if it's canceled
     */
    private boolean m_cancel;

    /**
     * Class constructor
     *
     * @param _name    the name of the organism
     * @param _id      the id of the organism
     * @param _version the version of the organism
     * @param _event   the event call when compute is finished
     */
    private Organism(String _name, long _id, long _version, IOrganismCallback _event) {
        super(_name);
        m_ID = _id;
        m_VERSION = _version;
        m_REPLICONS = new ArrayList<>();
        m_parent = null;
        m_event = _event;
        m_cancel = false;
        super.setTotalOrganismToOne();
    }

    /**
     * Load a Organism with his name, his id and his version and affect the event
     * You can choose to create a newOne with unloadTheLas if it exist
     *
     * @param _name                the name of the organism
     * @param _id                  the id of the organism
     * @param _version             the version of the organism
     * @param _parent              the parent SubGroup (used to know the path_name and to unload it)
     * @param _unloadLastCreateNew true for create a new one and unfold the last, false to get the last
     * @param _event               the Callback you want to apply
     * @return the Organism loaded or created
     */
    public static Organism load(String _name, long _id, long _version, SubGroup _parent, Boolean _unloadLastCreateNew, IOrganismCallback _event) throws AddException, InvalidStateException {
        Organism lastOne = (Organism) IDataBase.load(_parent.getSavedName() + s_SERIALIZATION_PREFIX + _name);
        if (_unloadLastCreateNew) {
            if (lastOne != null)
                _parent.unload(lastOne);
            lastOne = new Organism(_name, _id, _version, _event);
            _parent.addOrganism(lastOne);
        }
        return lastOne;
    }

    /**
     * Load date
     *
     * @param _db   the name of the database
     * @param _ki   the name of the kingdom
     * @param _gp   the name of the group
     * @param _sg   the name of the subgroup
     * @param _name the name of the organism
     * @return the loaded date
     */
    public static Date loadDate(String _db, String _ki, String _gp, String _sg, String _name) {
        String fileName = DataBase.s_SERIALIZATION_PREFIX + _db + Kingdom.s_SERIALIZATION_PREFIX + _ki + Group.s_SERIALIZATION_PREFIX + _gp + SubGroup.s_SERIALIZATION_PREFIX + _sg + s_SERIALIZATION_PREFIX + _name;
        final File file = new File(Options.getSerializeDirectory() + File.separator + fileName + Options.getDateModifSerializeExtension());
        ObjectInputStream stream = null;
        if (!file.exists()) {
            return null;
        }
        try {
            stream = new ObjectInputStream((new FileInputStream(file)));
            return (Date) stream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            Logs.warning("Unable to load : " + fileName);
            Logs.exception(e);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    Logs.warning("Unable to close : " + fileName);
                    Logs.exception(e);
                }
            }
        }
        return null;
    }

    /**
     * Start
     *
     * @throws InvalidStateException if it can't be started
     */
    @Override
    public synchronized void start() throws InvalidStateException {
        if (m_parent == null)
            throw new InvalidStateException("Unable to start without been add in a SubGroup : " + getName());
        super.start();
    }

    /**
     * Add a Replicon to this Organism
     *
     * @param _replicon, the Replicon to insert
     * @return the insertion success
     * @throws AddException if it _replicon are already added
     */
    public boolean addReplicon(Replicon _replicon) throws AddException {
        if (m_cancel) {
            throw new AddException("Impossible to add replicon to a canceled Organism");
        }
        if (super.getState() == State.STARTED) {
            try {
                if (m_REPLICONS.get(_replicon.getIndex()) != null)
                    throw new AddException("Replicon already added : " + _replicon.getName());
            } catch (IndexOutOfBoundsException ignored) {
            }
            _replicon.setIndex(m_REPLICONS.size());
            return m_REPLICONS.add(_replicon);
        } else return false;
    }

    /**
     * Update the statistics
     *
     * @throws InvalidStateException if it can't be finished
     */
    @Override
    public synchronized void finish() throws InvalidStateException {
        m_REPLICONS.parallelStream().forEach(Replicon::computeStatistic);
        for (Replicon rep : m_REPLICONS) {
            super.updateStatistics(rep);
            super.incrementGenomeNumber(rep.getType());
            super.incrementGenericTotals(rep);
        }
        super.computeStatistics();
        m_event.finish(this);
        m_parent.finish(this);
        super.finish();
        m_REPLICONS.clear();
        super.clear();
    }

    /**
     * Get the replicons Organism
     *
     * @return the m_REPLICONS
     */
    public ArrayList<Replicon> getReplicons() {
        return m_REPLICONS;
    }

    /**
     * Get the SubGroup's name
     *
     * @return the SubGroup's name
     */
    public String getSubGroupName() {
        return m_parent.getName();
    }

    /**
     * Get the Group's name
     *
     * @return the Group's name
     */
    public String getGroupName() {
        return m_parent.getGroupName();
    }

    /**
     * Get the Kingdom's name
     *
     * @return the Kingdom's name
     */
    public String getKingdomName() {
        return m_parent.getKingdomName();
    }

    /**
     * Get the version of the organism
     *
     * @return the version's number
     */
    public long getVersion() {
        return m_VERSION;
    }

    /**
     * Get the id of the organism
     *
     * @return the id's number
     */
    public long getId() {
        return m_ID;
    }

    /**
     * Save this organism
     */
    @Override
    public void save() {
        super.save();

        //Saving the Date
        final File file = new File(Options.getSerializeDirectory() + File.separator + getSavedName() + Options.getDateModifSerializeExtension());
        final ObjectOutputStream stream;
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
            stream.writeObject(getModificationDate());
            stream.flush();
            stream.close();
        } catch (IOException | SecurityException e) {
            Logs.warning("Unable to save : " + getSavedName());
            Logs.exception(e);
        }
    }

    /**
     * Get the main part of the save path_name
     *
     * @return the main part of the save path_name
     */
    @Override
    public String getSavedName() {
        return m_parent.getSavedName() + s_SERIALIZATION_PREFIX + getName();
    }

    /**
     * Cancel
     *
     * @throws InvalidStateException if it's finished
     */
    public void cancel() throws InvalidStateException {
        super.cancel();
        m_event = _organism -> {
        };
        m_REPLICONS.clear();
        m_cancel = true;
    }

    /**
     * Set the parent
     *
     * @param _subGroup, the parent to set
     */
    void setParent(SubGroup _subGroup) {
        m_parent = _subGroup;
    }

}
