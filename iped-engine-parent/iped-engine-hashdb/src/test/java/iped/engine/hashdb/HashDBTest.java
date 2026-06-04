package iped.engine.hashdb;

import org.junit.jupiter.api.Test;

import java.util.*;

import static iped.engine.hashdb.HashDB.*;
import static org.junit.jupiter.api.Assertions.*;

class HashDBTest {

    // --- hashStrToBytes / hashBytesToStr round-trip ---

    @Test
    void hashStrToBytes_andHashBytesToStr_roundTripMD5() {
        String md5 = "D41D8CD98F00B204E9800998ECF8427E";
        byte[] bytes = hashStrToBytes(md5, 16);
        assertEquals(md5, hashBytesToStr(bytes));
    }

    @Test
    void hashStrToBytes_andHashBytesToStr_roundTripSHA1() {
        String sha1 = "DA39A3EE5E6B4B0D3255BFEF95601890AFD80709";
        byte[] bytes = hashStrToBytes(sha1, 20);
        assertEquals(sha1, hashBytesToStr(bytes));
    }

    @Test
    void hashStrToBytes_andHashBytesToStr_roundTripSHA256() {
        String sha256 = "E3B0C44298FC1C149AFBF4C8996FB92427AE41E4649B934CA495991B7852B855";
        byte[] bytes = hashStrToBytes(sha256, 32);
        assertEquals(sha256, hashBytesToStr(bytes));
    }

    @Test
    void hashStrToBytes_whenEmptyString_thenNull() {
        assertNull(hashStrToBytes("", 16));
    }

    @Test
    void hashStrToBytes_whenWrongLength_thenEmptyByteArray() {
        byte[] result = hashStrToBytes("ABCD", 16); // too short for MD5
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    void hashBytesToStr_producesUppercase() {
        byte[] bytes = {0x0A, (byte) 0xBF};
        assertEquals("0ABF", hashBytesToStr(bytes));
    }

    // --- hashType ---

    @Test
    void hashType_MD5_thenZero() {
        assertEquals(0, hashType("MD5"));
    }

    @Test
    void hashType_MD5CaseInsensitive_thenZero() {
        assertEquals(0, hashType("md5"));
    }

    @Test
    void hashType_SHA1_thenOne() {
        assertEquals(1, hashType("SHA1"));
    }

    @Test
    void hashType_SHA256_thenTwo() {
        assertEquals(2, hashType("SHA256"));
    }

    @Test
    void hashType_SHA512_thenThree() {
        assertEquals(3, hashType("SHA512"));
    }

    @Test
    void hashType_EDONKEY_thenFour() {
        assertEquals(4, hashType("EDONKEY"));
    }

    @Test
    void hashType_withDash_thenStrippedAndMatched() {
        // SHA-256 → SHA256 → index 2
        assertEquals(2, hashType("SHA-256"));
    }

    @Test
    void hashType_unknown_thenMinusOne() {
        assertEquals(-1, hashType("UNKNOWN"));
    }

    // --- toStr / toSet round-trip ---

    @Test
    void toStr_andToSet_roundTrip() {
        Set<String> original = new HashSet<>(Arrays.asList("alpha", "beta", "gamma"));
        String str = toStr(original);
        Set<String> restored = toSet(str);
        assertEquals(original, restored);
    }

    @Test
    void toStr_sortsAlphabetically() {
        Set<String> set = new LinkedHashSet<>(Arrays.asList("z", "a", "m"));
        String result = toStr(set);
        assertEquals("a|m|z", result);
    }

    @Test
    void toList_sortsAlphabetically() {
        List<String> result = toList("c|a|b");
        assertEquals(List.of("a", "b", "c"), result);
    }

    // --- containsIgnoreCase ---

    @Test
    void containsIgnoreCase_whenExactMatch_thenTrue() {
        assertTrue(containsIgnoreCase("NSRL", "nsrl"));
    }

    @Test
    void containsIgnoreCase_whenPipeSeparated_thenFindsItem() {
        assertTrue(containsIgnoreCase("NSRL|CAID|VIC", "caid"));
    }

    @Test
    void containsIgnoreCase_whenNotPresent_thenFalse() {
        assertFalse(containsIgnoreCase("NSRL|CAID", "unknown"));
    }

    @Test
    void containsIgnoreCase_withSetParam_thenFindsItem() {
        Set<String> find = new HashSet<>(Arrays.asList("caid", "vic"));
        assertTrue(containsIgnoreCase("NSRL|CAID|VIC", find));
    }

    @Test
    void containsIgnoreCase_withSetParam_whenNotPresent_thenFalse() {
        Set<String> find = new HashSet<>(Collections.singletonList("unknown"));
        assertFalse(containsIgnoreCase("NSRL|CAID", find));
    }

    // --- mergeProperties ---

    @Test
    void mergeProperties_whenSame_thenReturnsSame() {
        assertEquals("NSRL", mergeProperties("NSRL", "NSRL"));
    }

    @Test
    void mergeProperties_whenDifferent_thenMergedAndSorted() {
        String result = mergeProperties("CAID", "NSRL");
        assertTrue(result.contains("CAID"));
        assertTrue(result.contains("NSRL"));
        assertTrue(result.contains("|"));
    }

    @Test
    void mergeProperties_whenOverlapping_thenNoDuplicates() {
        String result = mergeProperties("NSRL|CAID", "CAID|VIC");
        Set<String> items = new HashSet<>(Arrays.asList(result.split("\\|")));
        assertEquals(3, items.size()); // NSRL, CAID, VIC
    }

    // --- convertHexToBase64 ---

    @Test
    void convertHexToBase64_whenValidHex_thenBase64String() {
        // "hello" in hex is 68656c6c6f, base64 is "aGVsbG8="
        String result = convertHexToBase64("68656c6c6f");
        assertEquals("aGVsbG8=", result);
    }

    @Test
    void convertHexToBase64_whenInvalidHex_thenThrows() {
        assertThrows(IllegalArgumentException.class, () -> convertHexToBase64("ZZZZ"));
    }

    // --- zeroLengthHash constants ---

    @Test
    void zeroLengthHash_md5_hasCorrectLength() {
        assertEquals(16, zeroLengthHash[0].length);
    }

    @Test
    void zeroLengthHash_sha1_hasCorrectLength() {
        assertEquals(20, zeroLengthHash[1].length);
    }
}
