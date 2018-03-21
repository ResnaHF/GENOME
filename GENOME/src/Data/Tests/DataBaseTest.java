package Data.Tests;

import Data.*;
import Exception.AddException;
import Exception.InvalidStateException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DataBaseTest {

    static private DataBase db;

    @BeforeAll
    static void setUp() throws InvalidStateException {
        db = new DataBase("", _dataBase -> {
        });
        db.start();
    }

    @AfterAll
    static void tear() throws InvalidStateException {
        db.stop();
    }

    @Test
    void addKingdom() throws AddException {
        db.addKingdom(new Kingdom("", _kingdom -> {
        }));
        assertEquals(1, db.getKingdoms().size());
        db.addKingdom(new Kingdom("", _kingdom -> {
        }));
        assertEquals(2, db.getKingdoms().size());
    }

    @Test
    void getKingdoms() throws AddException {
        Kingdom k1 = new Kingdom("one", _kingdom -> {
        });
        Kingdom k2 = new Kingdom("two", _kingdom -> {
        });
        db.addKingdom(k1);
        db.addKingdom(k2);
        assertEquals(true, db.getKingdoms().contains(k1));
        assertEquals(true, db.getKingdoms().contains(k2));

        Kingdom k3 = new Kingdom("three", _kingdom -> {
        });
        assertEquals(false, db.getKingdoms().contains(k3));
    }

    @Test
    void getModificationDate() {
        Calendar caldb = Calendar.getInstance();
        caldb.setTime(db.getModificationDate());
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        assertEquals(caldb.get(Calendar.YEAR), cal.get(Calendar.YEAR));
        assertEquals(caldb.get(Calendar.MONTH), cal.get(Calendar.MONTH));
        assertEquals(caldb.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    void dataBase() throws AddException, InvalidStateException {
        DataBase dataBase = new DataBase("d1", _dataBase -> {
        });
        assertEquals(IDataBase.State.CREATED, dataBase.getState());

        // Database start and stop
        Kingdom k1 = new Kingdom("k1", _kingdom -> {
        });
        assertEquals(false, dataBase.addKingdom(k1));
        dataBase.start();
        assertEquals(true, dataBase.addKingdom(k1));

        Kingdom k2 = new Kingdom("k2", _kingdom -> {
        });
        assertEquals(true, dataBase.addKingdom(k2));

        assertThrows(Exception.class, () -> dataBase.addKingdom(k2));
        dataBase.stop();

        Kingdom k3 = new Kingdom("k3", _kingdom -> {
        });
        assertEquals(false, dataBase.addKingdom(k3));
        assertEquals(IDataBase.State.STOPPED, dataBase.getState());

        // Group start and Kingdom start and stop
        for (Kingdom k : dataBase.getKingdoms()) {
            Group g1 = new Group("g1_" + k.getName(), _group -> {
            });
            g1.start();
            assertEquals(false, k.addGroup(g1));
            k.start();
            assertEquals(true, k.addGroup(g1));

            Group g2 = new Group("g2_" + k.getName(), _group -> {
            });
            g2.start();
            assertEquals(true, k.addGroup(g2));
            assertThrows(Exception.class, () -> k.addGroup(g2));
            k.stop();

            Group g3 = new Group("g3_" + k.getName(), _group -> {
            });
            g3.start();
            assertEquals(false, k.addGroup(g3));
            assertEquals(IDataBase.State.STOPPED, k.getState());
        }

        // Group stop
        for (Kingdom k : dataBase.getKingdoms()) {
            for (Group g : k.getGroups()) {
                SubGroup s1 = new SubGroup("s1_" + g.getName(), _subGroup -> {
                });
                assertEquals(true, g.addSubGroup(s1));

                SubGroup s2 = new SubGroup("s2_" + g.getName(), _subGroup -> {
                });
                assertEquals(true, g.addSubGroup(s2));
                assertThrows(Exception.class, () -> g.addSubGroup(s2));

                g.stop();
                SubGroup s3 = new SubGroup("s3_" + g.getName(), _subGroup -> {
                });
                assertEquals(false, g.addSubGroup(s3));
                assertEquals(IDataBase.State.STOPPED, g.getState());
            }
        }

        ArrayList<Organism> list = new ArrayList<>();

        // Subgroup start and stop
        for (Kingdom k : dataBase.getKingdoms()) {
            for (Group g : k.getGroups()) {
                for (SubGroup s : g.getSubGroups()) {
                    Organism o1 = new Organism("'Brassica napus' phytoplasma", 152753L, 1592820474201505800L, _organism -> {
                    });
                    assertEquals(false, s.addOrganism(o1));
                    s.start();
                    assertEquals(true, s.addOrganism(o1));
                    list.add(o1);

                    Organism o2 = new Organism("'Brassica napus' phytoplasma", 152753L, 1592820474201505800L, _organism -> {
                    });
                    assertEquals(true, s.addOrganism(o2));
                    assertThrows(Exception.class, () -> s.addOrganism(o2));
                    s.stop();
                    list.add(o2);

                    Organism o3 = new Organism("'Brassica napus' phytoplasma", 152753L, 1592820474201505800L, _organism -> {
                    });
                    assertEquals(false, s.addOrganism(o3));
                    assertEquals(IDataBase.State.STOPPED, s.getState());
                }
            }
        }

        for (Kingdom k : dataBase.getKingdoms()) {
            for (Group g : k.getGroups()) {
                for (SubGroup s : g.getSubGroups()) {
                    for (Organism o : s.getOrganisms()) {
                        o.start();
                        ArrayList<StringBuilder> sequences = new ArrayList<>();
                        sequences.add(new StringBuilder("ATGAAATAA"));
                        sequences.add(new StringBuilder("ATGATAA"));

                        Replicon r1 = new Replicon(Replicon.Type.CHLOROPLAST, "r1_" + o.getName(), 2, 1, sequences);
                        assertEquals(true, o.addReplicon(r1));
                        assertEquals(Replicon.Type.CHLOROPLAST, r1.getType());

                        Replicon r2 = new Replicon(Replicon.Type.CHLOROPLAST, "r2_" + o.getName(), 2, 1, sequences);
                        assertEquals(true, o.addReplicon(r2));
                        assertEquals(Replicon.Type.CHLOROPLAST, r2.getType());

                        Replicon r3 = new Replicon(Replicon.Type.MITOCHONDRION, "r3_" + o.getName(), 2, 1, sequences);
                        assertEquals(true, o.addReplicon(r3));
                        assertEquals(Replicon.Type.MITOCHONDRION, r3.getType());

                        assertThrows(Exception.class, () -> o.addReplicon(r3));
                        o.stop();
                    }
                }
            }
        }

        for (Organism o : list) {
            o.finish();
        }
    }

    @Test
    void nameTest() throws AddException, InvalidStateException {

        Kingdom k = new Kingdom("KINGDOM", _kingdom -> {
        });
        k.start();

        Group g = new Group("GROUP", _group -> {
        });
        g.start();
        k.addGroup(g);

        SubGroup s = new SubGroup("SUBGROUP", _subGroup -> {
        });
        s.start();
        g.addSubGroup(s);

        Organism o = new Organism("'Brassica napus' phytoplasma", 152753L, 1592820474201505800L, _organism -> {
        });
        s.addOrganism(o);

        assertEquals("KINGDOM", o.getKingdomName());
        assertEquals("GROUP", o.getGroupName());
        assertEquals("SUBGROUP", o.getSubGroupName());

        assertEquals("KINGDOM", s.getKingdomName());
        assertEquals("GROUP", s.getGroupName());

        assertEquals("KINGDOM", g.getKingdomName());

        s.stop();
        g.stop();
        k.stop();
    }


}