package Data;

import javafx.util.Pair;

import java.util.EnumMap;
import java.util.stream.IntStream;

public class Statistics {

    /**
     * List of the 64 trinucleotide
     */
    public enum Trinucleotide{
        AAA,
        AAC,
        AAG,
        AAT,
        ACA,
        ACC,
        ACG,
        ACT,
        AGA,
        AGC,
        AGG,
        AGT,
        ATA,
        ATC,
        ATG,
        ATT,
        CAA,
        CAC,
        CAG,
        CAT,
        CCA,
        CCC,
        CCG,
        CCT,
        CGA,
        CGC,
        CGG,
        CGT,
        CTA,
        CTC,
        CTG,
        CTT,
        GAA,
        GAC,
        GAG,
        GAT,
        GCA,
        GCC,
        GCG,
        GCT,
        GGA,
        GGC,
        GGG,
        GGT,
        GTA,
        GTC,
        GTG,
        GTT,
        TAA,
        TAC,
        TAG,
        TAT,
        TCA,
        TCC,
        TCG,
        TCT,
        TGA,
        TGC,
        TGG,
        TGT,
        TTA,
        TTC,
        TTG,
        TTT
    }

    /**
     * List of statistics (float)
     */
    public enum StatFloat {
        FREQ0,
        FREQ1,
        FREQ2,
        PREF0,
        PREF1,
        PREF2
    }

    /**
     * List of statistics (long)
     */
    public enum StatLong {
        PHASE0,
        PHASE1,
        PHASE2
    }

    /**
     * This enumeration represent the type of a Statistic
     */
    public enum Type{
        CHROMOSOME,
        MITOCHONDRION,
        PLASMID,
        DNA,
        CHLOROPLAST
    }

    /**
     * Type of this Statistic
     */
    private Type m_type;
    /**
     * Array to store statistics
     */
    private EnumMap<Trinucleotide, Pair<EnumMap<StatFloat, Float>, EnumMap<StatLong, Long>>> m_trinucleotideTable;
    /**
     * Number total of trinucleotide on phase 0
     */
    private long m_totalTrinucleotide;

    /**
     * Class constructor
     */
    protected Statistics(Type _type){
        m_type = _type;
        m_trinucleotideTable = new EnumMap<>(Trinucleotide.class);
        IntStream.range(0,Trinucleotide.values().length).parallel().forEach(i -> {
            EnumMap<StatFloat,Float> arrf = new EnumMap<>(StatFloat.class);
            for(StatFloat stat :  StatFloat.values()) {
                arrf.put(stat, 0F);
            }
            EnumMap<StatLong,Long> arrl = new EnumMap<>(StatLong.class);
            for(StatLong stat :  StatLong.values()) {
                arrl.put(stat, 0L);
            }
            m_trinucleotideTable.put(Trinucleotide.values()[i],new Pair<>(arrf, arrl));
        });
        m_totalTrinucleotide = 0;
    }

    /**
     * Get the type of this Replicon
     * @return the type
     */
    public Type getType() {
        return m_type;
    }

    /**
     * get the total trinucleotide of the phase 0 number
     * @return the m_TotalTriPhase0
     */
    public long getTotalTrinucleotide() {
        return m_totalTrinucleotide;
    }

    /**
     * 
     * @return the m_trinucleotideTable
     */
    public EnumMap<Trinucleotide, Pair<EnumMap<StatFloat, Float>, EnumMap<StatLong, Long>>> getTable() {
        return m_trinucleotideTable;
    }

    // Do not use

    /**
     * Update statistics
     * @param _stats, the stats use to update
     */
    protected void update(Statistics _stats) {
        IntStream.range(0,Trinucleotide.values().length).parallel().forEach(i -> {
            Trinucleotide tri = Trinucleotide.values()[i];
            EnumMap<StatLong, Long> inputRow = _stats.m_trinucleotideTable.get(tri).getValue();
            incrementStat(tri, StatLong.PHASE0, inputRow.get(StatLong.PHASE0));
            incrementStat(tri, StatLong.PHASE1, inputRow.get(StatLong.PHASE1));
            incrementStat(tri, StatLong.PHASE2, inputRow.get(StatLong.PHASE2));
        });
        m_totalTrinucleotide += _stats.m_totalTrinucleotide;
    }

    /**
     * Compute the frequencies and the preferences of each trinucleotide for each phases
     */
    protected void compute(){
        m_trinucleotideTable.values().parallelStream().forEach(row -> {
            row.getKey().put(StatFloat.FREQ0, row.getValue().get(StatLong.PHASE0) / (float) m_totalTrinucleotide);
            row.getKey().put(StatFloat.FREQ1, row.getValue().get(StatLong.PHASE1) / (float) m_totalTrinucleotide);
            row.getKey().put(StatFloat.FREQ2, row.getValue().get(StatLong.PHASE2) / (float) m_totalTrinucleotide);
        });
    }

    /**
     * Increment by 1 the value of a trinucleotide for a stat
     * @param _tri, the Trinucleotide to set
     * @param _stat, the statistic to set
     */
    protected void incrementStat(Trinucleotide _tri, StatLong _stat){
        m_trinucleotideTable.get(_tri).getValue().put(_stat,m_trinucleotideTable.get(_tri).getValue().get(_stat)+1);
    }

    /**
     * Increment by _incr the value of total trinucleotide
     * @param _incr, the value to increment
     */
    protected void incrementTotal(long _incr){
        m_totalTrinucleotide += _incr;
    }

    /**
     * Increment by _incr the value of a trinucleotide for a stat
     * @param _tri, the Trinucleotide to set
     * @param _stat, the statistic to set
     * @param _incr, the value to increment
     */
    private void incrementStat(Trinucleotide _tri, StatLong _stat, long _incr){
        m_trinucleotideTable.get(_tri).getValue().put(_stat,m_trinucleotideTable.get(_tri).getValue().get(_stat)+_incr);
    }
}
