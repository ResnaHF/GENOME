package Utils;

import java.io.*;
import java.lang.reflect.Field;
import java.util.Properties;
import java.util.Set;

public final class Options {

    /**
     * File's name
     */
    private static String s_OPTIONS_FILE_NAME = "options.ini";
    /**
     * Mutex file name
     */
    private static String s_MUTEX_FILE_NAME = "locker.mutex";
    /**
     * Maximum number of thread
     */
    private static String s_MAX_THREAD = "32";
    /**
     * Connection timeout in ms
     */
    private static String s_DOWNLOAD_CONNECTION_TIMEOUT = "30000";
    /**
     * Number of organisms to download by request
     */
    private static String s_DOWNLOAD_STEP_ORGANISM = "100000";
    /**
     * Base URL of genbank REST API for organism
     */
    private static String s_ORGANISM_BASE_URL = "https://www.ncbi.nlm.nih.gov/Structure/ngram";
    /**
     * Base URL of genbank REST API for CDS
     */
    private static String s_CDS_BASE_URL = "https://www.ncbi.nlm.nih.gov/sviewer/viewer.fcgi";
    /**
     * Set to true to save full sequences
     */
    private static String s_SAVE_GENOME = "false";
    /**
     * Extension used for save genome
     */
    private static String s_GENOME_EXTENSION = ".txt";
    /**
     * Prefix used for save full sequences
     */
    private static String s_GENOME_DIRECTORY = "Genomes";
    /**
     * Set to true to save gene
     */
    private static String s_SAVE_GENE = "false";
    /**
     * Extension used for save gene
     */
    private static String s_GENE_EXTENSION = ".txt";
    /**
     * Prefix used for save gene
     */
    private static String s_GENE_DIRECTORY = "Genes";
    /**
     * Output directory for serialized data
     */
    private static String s_SERIALIZE_DIRECTORY = "Save";
    /**
     * Prefix used for serialization
     */
    private static String s_SERIALIZATION_SPLITER = "--";
    /**
     * Prefix used for Database serialization
     */
    private static String s_DATABASE_SERIALIZATION_PREFIX = "D_";
    /**
     * Prefix used for Kingdom serialization
     */
    private static String s_KINGDOM_SERIALIZATION_PREFIX = "K_";
    /**
     * Prefix used for Group serialization
     */
    private static String s_GROUP_SERIALIZATION_PREFIX = "G_";
    /**
     * Prefix used for SubGroup serialization
     */
    private static String s_SUBGROUP_SERIALIZATION_PREFIX = "SG_";
    /**
     * Prefix used for Organism serialization
     */
    private static String s_ORGANISM_SERIALIZATION_PREFIX = "O_";
    /**
     * Extension used for serialization
     */
    private static String s_SERIALIZE_EXTENSION = s_SERIALIZATION_SPLITER + ".ser";
    /**
     * Extension used for serialization
     */
    private static String s_DATEMODIF_SERIALIZE_EXTENSION = s_SERIALIZATION_SPLITER + "DATEMODIF" + s_SERIALIZE_EXTENSION;
    /**
     * Directory where store excel files
     */
    private static String s_RESULT_DIRECTORY = "Results";
    /**
     * Total file prefix
     */
    private static String s_TOTAL_PREFIX = "Total_";
    /**
     * Sum file prefix
     */
    private static String s_SUM_PREFIX = "Sum_";
    /**
     * Excel file extension
     */
    private static String s_EXCEL_EXTENSION = ".xlsx";
    /**
     * The name of the genbank
     */
    private static String s_GENBANK_NAME = "Genbank";
    /**
     * The time of each execution of the RMI server (in second)
     */
    private static String s_SERVER_LOOP_TIME = "86400";

    /**
     * Open the options's file in order to read options and fill the static fields with it
     */
    public static void initializeOptions() {
        File file = new File(s_OPTIONS_FILE_NAME);
        if (!file.exists()) {
            return;
        }

        // Load properties
        Properties properties = new Properties();
        FileReader fr = null;
        try {
            fr = new FileReader(file);
        } catch (FileNotFoundException e) {
            Logs.exception(e);
        }

        try {
            if (fr != null) {
                properties.load(fr);
                fr.close();
            }
        } catch (IOException e) {
            Logs.exception(e);
            return;
        }

        // Set properties to static fields
        Set<String> keys = properties.stringPropertyNames();
        for (String key : keys) {
            if (properties.getProperty(key) != null) {
                try {
                    Options.class.getDeclaredField(key).set(Options.class, properties.getProperty(key));
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    Logs.exception(e);
                }
            }
        }
    }

    /**
     * Write the option's file
     */
    public static void finalizeOptions() {
        // Set all unmodified properties
        Properties properties = new Properties();
        for (Field field : Options.class.getDeclaredFields()) {
            if (field.getName().compareTo("s_OPTIONS_FILE_NAME") != 0) {
                try {
                    properties.put(field.getName(), field.get(""));
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    Logs.exception(e);
                }
            }
        }

        // Write file
        File file = new File(s_OPTIONS_FILE_NAME);
        FileOutputStream out;
        try {
            out = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            Logs.exception(e);
            return;
        }

        try {
            properties.store(out, "");
        } catch (IOException e) {
            Logs.exception(e);
            return;
        }

        // Close stream
        try {
            out.close();
        } catch (IOException e) {
            Logs.exception(e);
        }
    }

    public static String getMutexFileName() {
        return s_MUTEX_FILE_NAME;
    }

    public static int getMaxThread() {
        return Integer.parseInt(s_MAX_THREAD);
    }

    public static int getDownloadStep() {
        return Integer.parseInt(s_DOWNLOAD_STEP_ORGANISM);
    }

    public static int getConnectionTimeout() {
        return Integer.parseInt(s_DOWNLOAD_CONNECTION_TIMEOUT);
    }

    public static String getOrganismBaseUrl() {
        return s_ORGANISM_BASE_URL;
    }

    public static String getCDSBaseUrl() {
        return s_CDS_BASE_URL;
    }

    public static boolean getSaveGenome() {
        return Boolean.valueOf(s_SAVE_GENOME);
    }

    public static String getGeneDirectory() {
        return System.getProperty("user.dir") + File.separator + s_GENE_DIRECTORY;
    }

    public static String getGeneExtension() {
        return s_GENE_EXTENSION;
    }

    public static boolean getSaveGene() {
        return Boolean.valueOf(s_SAVE_GENE);
    }

    public static String getGenomeDirectory() {
        return System.getProperty("user.dir") + File.separator + s_GENOME_DIRECTORY;
    }

    public static String getGenomeExtension() {
        return s_GENOME_EXTENSION;
    }

    public static String getSerializeDirectory() {
        return System.getProperty("user.dir") + File.separator + s_SERIALIZE_DIRECTORY;
    }

    public static String getSerializeExtension() {
        return s_SERIALIZE_EXTENSION;
    }

    public static String getDateModifSerializeExtension() {
        return s_DATEMODIF_SERIALIZE_EXTENSION;
    }

    public static String getResultDirectory() {
        return System.getProperty("user.dir") + File.separator + s_RESULT_DIRECTORY;
    }

    public static String getTotalPrefix() {
        return s_TOTAL_PREFIX;
    }

    public static String getSumPrefix() {
        return s_SUM_PREFIX;
    }

    public static String getExcelExtension() {
        return s_EXCEL_EXTENSION;
    }

    public static String getGenbankName() {
        return s_GENBANK_NAME;
    }

    public static String getSerializationSpliter() {
        return s_SERIALIZATION_SPLITER;
    }

    public static String getDatabaseSerializationPrefix() {
        return s_DATABASE_SERIALIZATION_PREFIX;
    }

    public static String getKingdomSerializationPrefix() {
        return s_KINGDOM_SERIALIZATION_PREFIX;
    }

    public static String getGroupSerializationPrefix() {
        return s_GROUP_SERIALIZATION_PREFIX;
    }

    public static String getSubGroupSerializationPrefix() {
        return s_SUBGROUP_SERIALIZATION_PREFIX;
    }

    public static String getOrganismSerializationPrefix() {
        return s_ORGANISM_SERIALIZATION_PREFIX;
    }

    public static int getServerLoopTime() {
        return Integer.parseInt(s_SERVER_LOOP_TIME ) * 1000;
    }

}
