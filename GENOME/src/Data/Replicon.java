package Data;

import java.util.Date;
import java.util.ArrayList;

public final class Replicon extends Statistics {

	/**
	 * The name
	 */
	private String m_name;
	/**
	 * Local index
	 */
	private int m_index;
	/**
	 * Last modification's date
	 */
	private Date m_modificationDate;
	/**
	 * Array of all the sequences of this Replicon
	 */
	private ArrayList<StringBuffer> m_sequences;

	/**
	 * Class constructor
	 * @param _type, the type of this Replicon
     * @param _name, the name of the organism
     */
	public Replicon(Type _type, String _name) {
		super(_type);
		m_name = _name;
		m_modificationDate = new Date();
		m_sequences = new ArrayList<>();
		m_index = -1;
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
	 * Add a sequence 
	 * @param _sequence, the sequence to add
	 * @return the insertion success
	 * @throws Exception if the _sequence are already added
	 */
	public boolean addSequence(StringBuffer _sequence) throws Exception {
		if(m_sequences.contains(_sequence))
			throw new Exception("Sequence already added");
		return m_sequences.add(_sequence);
	}

	// Do not use

	/**
	 * Compute statistics of this Replicon
	 */
	protected void computeStatistic() {
		int idx,length;
		for( StringBuffer sequence : m_sequences) {
			idx = 0;
			length = sequence.length();
			while(length-idx > 5){
				incrementStat(Trinucleotide.valueOf(sequence.substring(idx,idx+3)), StatLong.PHASE0);
				incrementStat(Trinucleotide.valueOf(sequence.substring(idx+1,idx+4)), StatLong.PHASE1);
				incrementStat(Trinucleotide.valueOf(sequence.substring(idx+2,idx+5)), StatLong.PHASE2);
                   idx+=3;
			}
			incrementTotal(idx/3);
		}
		super.compute();
	}

	/**
	 * Set the local index
	 * @param _id, the index to set
	 */
	protected void setIndex(int _id){
		m_index = _id;
	}

	/**
	 * Get the local index
	 * @return the local index
	 */
	protected int getIndex(){
		return m_index;
	}

}
