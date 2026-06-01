package iped.engine.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import iped.data.ICaseData;

class CaseDataTest {

    @Test
    void shouldTrackCountersAndFlagsAndObjects() {
        CaseData data = new CaseData();
        data.incDiscoveredEvidences(2);
        data.incDiscoveredEvidences(3);
        data.incDiscoveredVolume(10L);
        data.incDiscoveredVolume(null);
        data.setContainsReport(true);
        data.setIpedReport(true);
        data.putCaseObject("k", "v");

        assertEquals(5, data.getDiscoveredEvidences());
        assertEquals(10L, data.getDiscoveredVolume());
        assertTrue(data.containsReport());
        assertTrue(data.isIpedReport());
        assertEquals("v", data.getCaseObject("k"));
        assertEquals("v", data.addCaseObject("k", "v2"));
        assertEquals("v2", data.getCaseObject("k"));
    }

    @Test
    void shouldSaveAndLoad() throws Exception {
        CaseData data = new CaseData();
        data.incDiscoveredEvidences(7);
        data.incDiscoveredVolume(99L);
        data.setContainsReport(true);
        data.putCaseObject("name", "case");

        Path dir = Files.createTempDirectory("case-data-test");
        File file = dir.resolve("nested").resolve("case.gz").toFile();
        data.save(file);

        ICaseData loaded = CaseData.load(file);
        assertTrue(file.exists());
        assertTrue(loaded instanceof CaseData);
        CaseData loadedCaseData = (CaseData) loaded;
        assertEquals(7, loadedCaseData.getDiscoveredEvidences());
        assertEquals(99L, loadedCaseData.getDiscoveredVolume());
        assertTrue(loadedCaseData.containsReport());
        assertEquals("case", loadedCaseData.getCaseObject("name"));
    }

    @Test
    void containsReportDefaultsToFalse() {
        assertFalse(new CaseData().containsReport());
    }
}

