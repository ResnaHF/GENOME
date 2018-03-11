package Download;

import Exception.OperatorException;
import Utils.Logs;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

public class CDSParser {

    /**
     * Key used to get CDS
     */
    private static final String s_CDS_KEY = "CDS             ";
    /**
     * Key used to get full sequence
     */
    private static final String s_ORIGIN_KEY = "ORIGIN ";
    /**
     * Key used to get the end of the file
     */
    private static final String s_ORIGIN_END_KEY = "//";
    /**
     * Key used to get join
     */
    private static final String s_JOIN_KEY = "JOIN";
    /**
     * Key used to get complement
     */
    private static final String s_COMPLEMENT_KEY = "COMPLEMENT";
    /**
     * Regex used to get all sequences
     */
    private static final String s_ADN_REGEX = "[A-Z]";
    /**
     * Regex used to get interval descriptor
     */
    private static final String s_INTERVAL_REGEX = " *<?[0-9]+\\.\\.>?[0-9]+[ )]*";
    /**
     * Regex used to get unique descriptor
     */
    private static final String s_DESCRIPTOR_REGEX = " *[<>]?[0-9]+[, )]*";
    /**
     * Regex used to get number
     */
    private static final String s_NUMBER_REGEX = "[0-9]+";
    /**
     * Name
     */
    private String m_name;
    /**
     * Initial buffer
     */
    private StringBuilder m_buffer;
    /**
     * Total of CDS
     */
    private long m_total;
    /**
     * Total of valid CDS
     */
    private long m_valid;
    /**
     * List of CDS
     */
    private ArrayList<StringBuilder> m_cdsList;
    /**
     * Full sequences
     */
    private StringBuilder m_origin;
    /**
     * Sequences
     */
    private final ArrayList<StringBuilder> m_sequences;

    /**
     * Constructor
     *
     * @param _buffer the CDS
     */
    public CDSParser(StringBuilder _buffer, String _name) {
        m_buffer = _buffer;
        m_name = _name;
        m_total = 0;
        m_valid = 0;
        m_cdsList = new ArrayList<>();
        m_origin = new StringBuilder();
        m_sequences = new ArrayList<>();
    }

    /**
     * Parse the CDS file
     */
    public void parse() throws OperatorException {
        try {

            int originIndex = -s_CDS_KEY.length();
            int begIndex = -s_ORIGIN_KEY.length();
            int endIndex;
            while ((originIndex = m_buffer.indexOf(s_CDS_KEY, originIndex + s_CDS_KEY.length())) != -1) {

                begIndex = originIndex;
                endIndex = m_buffer.indexOf("\n", begIndex);
                StringBuilder cds = new StringBuilder(m_buffer.substring(begIndex + s_CDS_KEY.length(), endIndex).toUpperCase());

                long open = cds.codePoints().filter(ch -> ch == '(').count();
                long close = cds.codePoints().filter(ch -> ch == ')').count();
                while (open != close) {
                    begIndex = endIndex + 1;
                    endIndex = m_buffer.indexOf("\n", begIndex);
                    String temp = m_buffer.substring(begIndex, endIndex).toUpperCase();
                    cds.append(temp);
                    open += temp.codePoints().filter(ch -> ch == '(').count();
                    close += temp.codePoints().filter(ch -> ch == ')').count();
                }
                m_cdsList.add(cds);

            }

            begIndex = m_buffer.indexOf(s_ORIGIN_KEY, begIndex);
            endIndex = m_buffer.indexOf(s_ORIGIN_END_KEY, begIndex);
            Pattern pattern = Pattern.compile(s_ADN_REGEX, Pattern.CASE_INSENSITIVE);
            Matcher m = pattern.matcher(m_buffer.substring(begIndex + s_ORIGIN_KEY.length(), endIndex));
            while (m.find()) {
                m_origin.append(m.group(0).toUpperCase());
            }

            m_total = m_cdsList.size();
            parseCDS();

        } catch (StringIndexOutOfBoundsException e) {
            String message = "Unable to parse data " + m_name + " : " + m_buffer;
            Logs.warning(message);
            Logs.exception(e);
            throw new OperatorException(message);
        }
    }

    /**
     * Get the total of CDS
     *
     * @return the total of CDS
     */
    public long getTotal() {
        return m_total;
    }

    /**
     * GEt to total of valid CDS
     *
     * @return the total of valid CDS
     */
    public long getValid() {
        return m_valid;
    }

    /**
     * Get all sequences computed
     * @return the sequences
     */
    public ArrayList<StringBuilder> getSequences() {
        return m_sequences;
    }

    /**
     * Parse the buffer
     */
    private void parseCDS() {
        Pattern interval = Pattern.compile(s_INTERVAL_REGEX, Pattern.CASE_INSENSITIVE);
        Pattern descriptor = Pattern.compile(s_DESCRIPTOR_REGEX, Pattern.CASE_INSENSITIVE);
        Pattern number = Pattern.compile(s_NUMBER_REGEX, Pattern.CASE_INSENSITIVE);

        for (StringBuilder sb : m_cdsList) {

            Operator operator = new Container();
            try {

                for (String op : sb.toString().split("[(,]")) {

                    Matcher mDescriptor = interval.matcher(op);
                    if (mDescriptor.matches()) {

                        // TODO '<' and '>'
                        try {
                            Matcher mNumber = number.matcher(op);
                            mNumber.find();
                            int beg = Integer.valueOf(mNumber.group(0));
                            mNumber.find();
                            int end = Integer.valueOf(mNumber.group(0));
                            IntervalOperator d = new IntervalOperator(operator, beg - 1, end);
                            operator.addOperator(d);
                            long occurrence = mDescriptor.group(0).codePoints().filter(ch -> ch == ')').count();
                            for (long i = 0; i < occurrence; ++i) {
                                operator = operator.getParent();
                            }
                        } catch (NumberFormatException | IndexOutOfBoundsException e) {
                            Logs.warning("Unable to create IntervalOperator");
                            Logs.exception(e);
                            throw new OperatorException("Unable to parse " + m_name + " : " + sb + " at : " + op);
                        }

                    } else if (op.contains(s_COMPLEMENT_KEY)) {

                        ComplementOperator c = new ComplementOperator(operator);
                        operator.addOperator(c);
                        operator = c;

                    } else if (op.contains(s_JOIN_KEY)) {

                        JoinOperator j = new JoinOperator(operator);
                        operator.addOperator(j);
                        operator = j;

                    } else {

                        Matcher mDescriptorSimple = descriptor.matcher(op);
                        if (mDescriptorSimple.matches()) {

                            // TODO '<' and '>'
                            try {
                                Matcher mNumber = number.matcher(op);
                                mNumber.find();
                                int index = Integer.valueOf(mNumber.group(0));
                                DescriptorOperator d = new DescriptorOperator(operator, index - 1);
                                operator.addOperator(d);
                                long occurrence = mDescriptorSimple.group(0).codePoints().filter(ch -> ch == ')').count();
                                for (long i = 0; i < occurrence; ++i) {
                                    operator = operator.getParent();
                                }
                            } catch (NumberFormatException | IndexOutOfBoundsException e) {
                                Logs.warning("Unable to create DescriptorOperator");
                                Logs.exception(e);
                                throw new OperatorException("Unable to parse " + m_name + " : " + sb + " at : " + op);
                            }

                        } else {
                            throw new OperatorException("Unable to parse " + m_name + " : " + sb + " at : " + op);
                        }
                    }

                }
            } catch (OperatorException e) {
                Logs.warning("Unable to parse " + m_name + " : " + sb);
                Logs.exception(e);
                continue;
            }

            try {

                StringBuilder s = operator.compute();
                if (s.length() % 3 == 0) {

                    try {
                        StartTrinucleotide.valueOf(s.substring(0, 3));
                        StopTrinucleotide.valueOf(s.substring(s.length() - 3, s.length()));
                        if (s.codePoints().parallel().filter(c -> c != 'A' && c != 'T' && c != 'C' && c != 'G').count() == 0) {
                            ++m_valid;
                            m_sequences.add(s);
                        }
                    } catch (IllegalArgumentException e) {
                    }

                }

            } catch (OperatorException e) {
                Logs.warning("Unable to compute " + m_name + ": " + sb);
                Logs.exception(e);
            }
        }
    }

    /**
     * Enum used to check sequence
     */
    private enum StartTrinucleotide {
        ATG,
        CTG,
        TTG,
        GTG,
        ATA,
        ATC,
        ATT,
        TTA
    }

    /**
     * Enum used to check sequence
     */
    private enum StopTrinucleotide {
        TAA,
        TAG,
        TGA,
        TTA
    }

    /**
     *
     */
    private abstract class Operator {
        private Operator m_parent;

        protected Operator(Operator _parent) {
            m_parent = _parent;
        }

        protected Operator getParent() {
            return m_parent;
        }

        protected abstract void addOperator(Operator _op) throws OperatorException;

        protected abstract StringBuilder compute() throws OperatorException;
    }

    /**
     *
     */
    private final class Container extends Operator {

        private Operator m_operator;

        private Container() {
            super(null);
            m_operator = null;
        }

        @Override
        protected Operator getParent() {
            return this;
        }

        @Override
        protected void addOperator(Operator _op) throws OperatorException {
            if (m_operator == null) {
                m_operator = _op;
            } else {
                throw new OperatorException("Can't add multiple operator");
            }
        }

        @Override
        protected StringBuilder compute() throws OperatorException {
            if (m_operator == null) {
                throw new OperatorException("Not enough data");
            }
            return m_operator.compute();
        }
    }

    /**
     *
     */
    private final class IntervalOperator extends Operator {

        private final int m_begin;
        private final int m_end;

        private IntervalOperator(Operator _parent, int _begin, int _end) {
            super(_parent);
            m_begin = _begin;
            m_end = _end;
        }

        @Override
        protected void addOperator(Operator _op) throws OperatorException {
            throw new OperatorException("Can't add operator");
        }

        @Override
        protected StringBuilder compute() throws OperatorException {
            // TODO
            try {
                return new StringBuilder(m_origin.substring(m_begin, m_end));
            } catch (StringIndexOutOfBoundsException e) {
                final String message = "Bad index : " + m_origin + " to " + m_end;
                Logs.warning(message);
                Logs.exception(e);
                throw new OperatorException(message);
            }
        }
    }

    /**
     *
     */
    private final class DescriptorOperator extends Operator {

        private final int m_index;

        private DescriptorOperator(Operator _parent, int _index) {
            super(_parent);
            m_index = _index;
        }

        @Override
        protected void addOperator(Operator _op) throws OperatorException {
            throw new OperatorException("Can't add operator");
        }

        @Override
        protected StringBuilder compute() throws OperatorException {
            // TODO
            try {
                return new StringBuilder(""+m_origin.charAt(m_index));
            } catch (StringIndexOutOfBoundsException e) {
                final String message = "Bad index : " + m_index;
                Logs.warning(message);
                Logs.exception(e);
                throw new OperatorException(message);
            }
        }
    }

    /**
     *
     */
    private final class ComplementOperator extends Operator {

        private Operator m_operator;

        private ComplementOperator(Operator _parent) {
            super(_parent);
            m_operator = null;
        }

        @Override
        protected void addOperator(Operator _op) throws OperatorException {
            if (m_operator == null) {
                m_operator = _op;
            } else {
                throw new OperatorException("Can't add multiple operator");
            }
        }

        @Override
        protected StringBuilder compute() throws OperatorException {
            // TODO
            if (m_operator == null) {
                throw new OperatorException("Not enough data");
            }

            final StringBuilder res = m_operator.compute();
            final StringBuilder comp = new StringBuilder(res);
            int size = res.length() - 1;

            IntStream.range(0, size + 1).parallel().forEach(i -> {
                char c = res.charAt(i);
                switch (c) {
                    case 'A':
                        c = 'T';
                        break;
                    case 'C':
                        c = 'G';
                        break;
                    case 'G':
                        c = 'C';
                        break;
                    case 'T':
                        c = 'A';
                        break;
                }
                comp.setCharAt(size - i, c);
            });
            return comp;
        }
    }

    /**
     *
     */
    private final class JoinOperator extends Operator {

        private final ArrayList<Operator> m_operators;

        private JoinOperator(Operator _parent) {
            super(_parent);
            m_operators = new ArrayList<>();
        }

        @Override
        protected void addOperator(Operator _op) {
            m_operators.add(_op);
        }

        @Override
        protected StringBuilder compute() throws OperatorException {
            if (m_operators.size() == 0) {
                throw new OperatorException("Not enough data");
            }
            final StringBuilder res = new StringBuilder();
            for (Operator op : m_operators) {
                res.append(op.compute());
            }
            return res;
        }
    }

}
