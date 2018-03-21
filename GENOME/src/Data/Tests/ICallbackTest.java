package Data.Tests;

import Data.*;
import Exception.AddException;
import Exception.InvalidStateException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ICallbackTest {

    @Test
    public void stopFinish() throws InvalidStateException, AddException {

        final ArrayList<Boolean> check = new ArrayList<>();
        check.add(false);
        check.add(false);
        check.add(false);
        check.add(false);
        check.add(false);

        DataBase dataBase = new DataBase("DataBase", _dataBase -> {
            check.set(0, true);
            assertEquals(1, _dataBase.getStatistics().size());
        });
        Kingdom kingdom = new Kingdom("Kingdom", _kingdom -> {
            check.set(1, true);
            assertEquals(1, _kingdom.getStatistics().size());
        });
        Group group = new Group("Group", _group -> {
            check.set(2, true);
            assertEquals(1, _group.getStatistics().size());
        });
        SubGroup subGroup = new SubGroup("SubGroup", _subGroup -> {
            check.set(3, true);
            assertEquals(1, _subGroup.getStatistics().size());
        });
        Organism organism = new Organism("Organism", 1234L, 4321L, _organism -> {
            check.set(4, true);
            assertEquals(1, _organism.getStatistics().size());
            assertEquals(1, _organism.getReplicons().size());
        });
        ArrayList<StringBuilder> sequences = new ArrayList<>();
        sequences.add(new StringBuilder("ATGAAATAA"));
        sequences.add(new StringBuilder("ATGATAA"));
        Replicon replicon = new Replicon(Statistics.Type.CHLOROPLAST, "Replicon", 2, 1, sequences);

        dataBase.start();
        kingdom.start();
        group.start();
        subGroup.start();
        organism.start();

        dataBase.addKingdom(kingdom);
        kingdom.addGroup(group);
        group.addSubGroup(subGroup);
        subGroup.addOrganism(organism);
        organism.addReplicon(replicon);

        organism.stop();
        group.stop();
        subGroup.stop();
        kingdom.stop();
        dataBase.stop();

        organism.finish();

        assertEquals(true, check.get(0));
        assertEquals(true, check.get(1));
        assertEquals(true, check.get(2));
        assertEquals(true, check.get(3));
        assertEquals(true, check.get(4));

        assertEquals(IDataBase.State.FINISHED, organism.getState());
        assertEquals(IDataBase.State.FINISHED, group.getState());
        assertEquals(IDataBase.State.FINISHED, subGroup.getState());
        assertEquals(IDataBase.State.FINISHED, kingdom.getState());
        assertEquals(IDataBase.State.FINISHED, dataBase.getState());
    }

    @Test
    public void finishStop() throws InvalidStateException, AddException {

        final ArrayList<Boolean> check = new ArrayList<>();
        check.add(false);
        check.add(false);
        check.add(false);
        check.add(false);
        check.add(false);

        DataBase dataBase = new DataBase("DataBase", _dataBase -> {
            check.set(0, true);
            assertEquals(1, _dataBase.getStatistics().size());
        });
        Kingdom kingdom = new Kingdom("Kingdom", _kingdom -> {
            check.set(1, true);
            assertEquals(1, _kingdom.getStatistics().size());
        });
        Group group = new Group("Group", _group -> {
            check.set(2, true);
            assertEquals(1, _group.getStatistics().size());
        });
        SubGroup subGroup = new SubGroup("SubGroup", _subGroup -> {
            check.set(3, true);
            assertEquals(1, _subGroup.getStatistics().size());
        });
        Organism organism = new Organism("Organism", 1234L, 4321L, _organism -> {
            check.set(4, true);
            assertEquals(1, _organism.getStatistics().size());
            assertEquals(1, _organism.getReplicons().size());
        });
        ArrayList<StringBuilder> sequences = new ArrayList<>();
        sequences.add(new StringBuilder("ATGAAATAA"));
        sequences.add(new StringBuilder("ATGATAA"));
        Replicon replicon = new Replicon(Statistics.Type.CHLOROPLAST, "Replicon", 2, 1, sequences);

        dataBase.start();
        kingdom.start();
        group.start();
        subGroup.start();
        organism.start();

        dataBase.addKingdom(kingdom);
        kingdom.addGroup(group);
        group.addSubGroup(subGroup);
        subGroup.addOrganism(organism);
        organism.addReplicon(replicon);

        organism.stop();
        organism.finish();

        group.stop();
        subGroup.stop();
        kingdom.stop();
        dataBase.stop();

        assertEquals(true, check.get(0));
        assertEquals(true, check.get(1));
        assertEquals(true, check.get(2));
        assertEquals(true, check.get(3));
        assertEquals(true, check.get(4));

        assertEquals(IDataBase.State.FINISHED, organism.getState());
        assertEquals(IDataBase.State.FINISHED, group.getState());
        assertEquals(IDataBase.State.FINISHED, subGroup.getState());
        assertEquals(IDataBase.State.FINISHED, kingdom.getState());
        assertEquals(IDataBase.State.FINISHED, dataBase.getState());
    }
}