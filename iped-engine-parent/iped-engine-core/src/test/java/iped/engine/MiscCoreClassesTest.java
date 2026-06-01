package iped.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import iped.exception.ZipBombException;

class MiscCoreClassesTest {

    @Test
    void cmdLineArgsInterfaceShouldExposeExpectedMethods() throws Exception {
        Method[] methods = CmdLineArgs.class.getDeclaredMethods();
        assertTrue(methods.length >= 20);
        boolean found = false;
        for (Method m : methods) {
            if ("getDatasources".equals(m.getName())) {
                found = true;
                break;
            }
        }
        assertTrue(found);
    }

    @Test
    void versionConstantsShouldBeConsistent() {
        assertTrue(Version.APP_NAME.contains(Version.APP_VERSION));
        assertTrue(Version.APP_NAME.startsWith(Version.APP_NAME_PREFIX));
        assertEquals("IPED", Version.APP_EXT);
    }

    @Test
    void zipBombHelperShouldRespectThresholds() throws Exception {
        assertTrue(ZipBombException.isZipBomb(1L, (long) ZipBombException.ZIPBOMB_MIN_SIZE * 101));
        assertEquals(false, ZipBombException.isZipBomb(null, Long.MAX_VALUE));
        assertEquals(false, ZipBombException.isZipBomb(1000L, 1000L));
    }
}
