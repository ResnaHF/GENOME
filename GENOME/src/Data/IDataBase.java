package Data;

import java.util.Date;
import java.util.EnumMap;

public class IDataBase {

    /**
     * The name
     */
    private String m_name;
    /**
     * Last modification's date
     */
    private Date m_modificationDate;
    /**
     * Statistics of this IDataBase
     */
    private EnumMap<Statistics.Type,Statistics> m_statistics;
    /**
     * Array of values of each Replicon's type
     */
    private EnumMap<Statistics.Type,Long> m_genomeNumber;

    /**
     * Class constructor
     */
    protected IDataBase(String _name){
        m_name = _name;
        m_modificationDate = new Date();
        m_statistics = new EnumMap<>(Statistics.Type.class);
        m_genomeNumber = new EnumMap<>(Statistics.Type.class);
    }

    /**
     * Get the last modification's date
     * @return the m_modificationDate
     */
    public Date getModificationDate() {
        return m_modificationDate;
    }

    /**
     * Get the name
     * @return the m_name
     */
    public String getName(){
        return m_name;
    }

    /**
     * Get the statistics
     * @return the statistics
     */
    public EnumMap<Statistics.Type,Statistics> getStatistics() {
         return m_statistics;
    }

    /**
     * Get number of each Genome's Type
     * @return the number of each Genome's Type
     */
    public EnumMap<Statistics.Type, Long> getGenomeNumber() {
        return m_genomeNumber;
    }

    // Do not used

    /**
     * Get the number of a genome's specified type
     * @param _type, the Type of the genomes's number to get
     * @return the number of genomes
     */
    protected Long getTypeNumber(Statistics.Type _type) {
        return m_genomeNumber.get(_type);
    }

    /**
     * Increment by 1 the number of genome to a type
     * @param _type, the Type of the genomes to increment
     */
    protected void incrementGenomeNumber(Statistics.Type _type) {
        m_genomeNumber.merge(_type, 1L, (v1,v2) -> v1 + v2);
    }

    /**
     * Increment by _incr the number of genome of a type
     * @param _type, the Type of the genomes to increment
     * @param _incr, the value of the increment
     */
    protected void incrementGenomeNumber(Statistics.Type _type,long _incr){
        m_genomeNumber.merge(_type, _incr, (v1,v2) -> v1 + v2);
    }

    /**
     * Create statistic if it's not exist and update it
     * @param _statistics, the statistic to used for update
     */
    protected void updateStatistics(Statistics _statistics){
        if(m_statistics.get(_statistics.getType()) == null){
            m_statistics.put(_statistics.getType(), new Statistics(_statistics.getType()));
        }
        m_statistics.get(_statistics.getType()).update(_statistics);
    }

    /**
     * Compute statistics
     */
    protected void computeStatistics(){
        m_statistics.values().parallelStream().forEach(Statistics::compute);
    }

}

