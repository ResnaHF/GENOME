package Data;

import java.util.ArrayList;

public final class Replicon extends Statistics {

    /**
     * The name
     */
    private final String m_NAME;
    /**
     * Array of all the sequences of this Replicon
     */
    private transient final ArrayList<StringBuilder> m_SEQUENCES;
    /**
     * Local index
     */
    private transient int m_index;

    /**
     * Class constructor
     *
     * @param _type, the type of this Replicon
     * @param _name, the name of the organism
     */
    public Replicon(Type _type, String _name, long _total, long _valid, ArrayList<StringBuilder> _sequences) {
        super(_type);
        m_NAME = _name;
        m_SEQUENCES = _sequences;
        m_index = -1;
        super.incrementCDS(_total);
        super.incrementValidCDS(_valid);
    }

    /**
     * Get the name
     *
     * @return the m_NAME
     */
    public String getName() {
        return m_NAME;
    }

    /**
     * Compute statistics of this Replicon
     */
    void computeStatistic() {
        int idx, length;
        for (StringBuilder sequence : m_SEQUENCES) {
            final Statistics temp = new Statistics(getType());
            length = sequence.length();
            idx = 0;
            while (idx + 6 < length) {
                temp.incrementStat(Trinucleotide.valueOf(sequence.substring(idx, idx + 3)), StatLong.PHASE0);
                temp.incrementStat(Trinucleotide.valueOf(sequence.substring(idx + 1, idx + 4)), StatLong.PHASE1);
                temp.incrementStat(Trinucleotide.valueOf(sequence.substring(idx + 2, idx + 5)), StatLong.PHASE2);
                temp.incrementStat(Trinucleotide.valueOf(sequence.substring(idx + 3, idx + 6)), StatLong.PHASE0);
                temp.incrementStat(Trinucleotide.valueOf(sequence.substring(idx + 4, idx + 7)), StatLong.PHASE1);
                temp.incrementStat(Trinucleotide.valueOf(sequence.substring(idx + 5, idx + 8)), StatLong.PHASE2);

                temp.incrementStat(Dinucleotide.valueOf(sequence.substring(idx, idx + 2)), StatLong.PHASE0);
                temp.incrementStat(Dinucleotide.valueOf(sequence.substring(idx + 1, idx + 3)), StatLong.PHASE1);
                temp.incrementStat(Dinucleotide.valueOf(sequence.substring(idx + 2, idx + 4)), StatLong.PHASE0);
                temp.incrementStat(Dinucleotide.valueOf(sequence.substring(idx + 3, idx + 5)), StatLong.PHASE1);
                temp.incrementStat(Dinucleotide.valueOf(sequence.substring(idx + 4, idx + 6)), StatLong.PHASE0);
                temp.incrementStat(Dinucleotide.valueOf(sequence.substring(idx + 5, idx + 7)), StatLong.PHASE1);
                idx += 6;
            }
            super.incrementTriTotal(idx / 3);
            super.incrementDiTotal(idx / 2);
            if (idx + 3 < length) {
                temp.incrementStat(Trinucleotide.valueOf(sequence.substring(idx, idx + 3)), StatLong.PHASE0);
                temp.incrementStat(Trinucleotide.valueOf(sequence.substring(idx + 1, idx + 4)), StatLong.PHASE1);
                temp.incrementStat(Trinucleotide.valueOf(sequence.substring(idx + 2, idx + 5)), StatLong.PHASE2);

                temp.incrementStat(Dinucleotide.valueOf(sequence.substring(idx, idx + 2)), StatLong.PHASE0);
                temp.incrementStat(Dinucleotide.valueOf(sequence.substring(idx + 1, idx + 3)), StatLong.PHASE1);
                super.incrementTriTotal(1);
                super.incrementDiTotal(1);
            }

            long val0, val1, val2;
            for (Tuple tuple : temp.getTriTable()) {
                val0 = tuple.get(Statistics.StatLong.PHASE0);
                val1 = tuple.get(Statistics.StatLong.PHASE1);
                val2 = tuple.get(Statistics.StatLong.PHASE2);
                if (val1 <= val0 && val2 <= val0)
                    tuple.incr(Statistics.StatLong.PREF0, 1);
                if (val0 <= val1 && val2 <= val1)
                    tuple.incr(Statistics.StatLong.PREF1, 1);
                if (val0 <= val2 && val1 <= val2)
                    tuple.incr(Statistics.StatLong.PREF2, 1);
            }
            for (Tuple tuple : temp.getDiTable()) {
                val0 = tuple.get(Statistics.StatLong.PHASE0);
                val1 = tuple.get(Statistics.StatLong.PHASE1);
                if (val1 <= val0)
                    tuple.incr(Statistics.StatLong.PREF0, 1);
                if (val0 <= val1)
                    tuple.incr(Statistics.StatLong.PREF1, 1);
            }
            update(temp);
            super.compute();
        }
    }

    /**
     * Get the local index
     *
     * @return the local index
     */
    int getIndex() {
        return m_index;
    }

    /**
     * Set the local index
     *
     * @param _id, the index to set
     */
    void setIndex(int _id) {
        m_index = _id;
    }

}
