package Download.Tests;

import Download.GenbankOrganisms;
import Download.OrganismParser;
import Exception.MissException;
import Utils.Logs;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenbankOrganismsTest {
    @Test
    void runTest() throws MissException {

        GenbankOrganisms go = GenbankOrganisms.getInstance();
        go.downloadOrganisms();

        ArrayList<String> kingdom = new ArrayList<>();
        ArrayList<ArrayList<String>> group = new ArrayList<>();
        ArrayList<ArrayList<String>> subgroup = new ArrayList<>();

        String lastKingdom = "";
        String lastGroup = "";
        String lastSubGroup = "";

        int count = 0;
        Logs.initializeLog();
        while (go.hasNext()) {
            OrganismParser ro = go.getNext();
            ro.parse();

            assertTrue(ro.getId() != -1);
            assertTrue(ro.getName() != null);
            assertTrue(ro.getKingdom() != null);
            assertTrue(ro.getGroup() != null);
            assertTrue(ro.getSubGroup() != null);
            assertTrue(ro.getVersion() != -1);
            assertTrue(ro.getModificationDate() != null);

            for (String CDS : ro.getReplicons()) {
                assertTrue(CDS.indexOf("NC_") == 0);
            }

            count++;

            String kin = ro.getKingdom().toUpperCase();
            String gro = ro.getGroup().toUpperCase();
            String sub = ro.getSubGroup().toUpperCase();
            Logs.info(kin + " : " + gro + " : " + sub);

            if (kin.compareTo(lastKingdom) != 0) {
                kingdom.add(kin);
                group.add(new ArrayList<>());
                group.get(group.size() - 1).add(gro);
                subgroup.add(new ArrayList<>());
                subgroup.get(subgroup.size() - 1).add(sub);
            } else {
                if (gro.compareTo(lastGroup) != 0) {
                    group.get(group.size() - 1).add(gro);
                    subgroup.add(new ArrayList<>());
                    subgroup.get(subgroup.size() - 1).add(sub);
                } else {
                    if (sub.compareTo(lastSubGroup) != 0) {
                        subgroup.get(subgroup.size() - 1).add(sub);
                    }
                }
            }

            lastKingdom = kin;
            lastGroup = gro;
            lastSubGroup = sub;
        }
        Logs.finalizeLog();

        boolean kingdomSorted = true;
        for (int i = 0; i < kingdom.size(); ++i) {
            for (int j = i + 1; j < kingdom.size(); ++j) {
                if (kingdom.get(i).compareTo(kingdom.get(j)) == 0) {
                    kingdomSorted = false;
                }
            }
        }
        assertTrue(kingdomSorted);

        for (ArrayList<String> arr : group) {
            boolean groupSorted = true;
            for (int i = 0; i < arr.size(); ++i) {
                for (int j = i + 1; j < arr.size(); ++j) {
                    if (arr.get(i).compareTo(arr.get(j)) == 0) {
                        groupSorted = false;
                    }
                }
            }
            assertTrue(groupSorted);
        }

        for (ArrayList<String> arr : subgroup) {
            boolean subSorted = true;
            for (int i = 0; i < arr.size(); ++i) {
                for (int j = i + 1; j < arr.size(); ++j) {
                    if (arr.get(i).compareTo(arr.get(j)) == 0) {
                        subSorted = false;
                    }
                }
            }
            assertTrue(subSorted);
        }

        count += go.getFailedOrganism();

        assertEquals(count, go.getTotalCount());
    }

}