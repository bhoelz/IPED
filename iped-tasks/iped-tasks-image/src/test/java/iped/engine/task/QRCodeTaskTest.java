package iped.engine.task;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.EncodeHintType;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.google.zxing.qrcode.QRCodeWriter;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class QRCodeTaskTest {

    // ---- getHex() pure-logic tests ----

    @Test
    void getHex_emptyArray_returnsEmptyString() {
        assertEquals("", QRCodeTask.getHex(new byte[0]));
    }

    @Test
    void getHex_singleZeroByte_returns00() {
        assertEquals("00", QRCodeTask.getHex(new byte[]{0}));
    }

    @Test
    void getHex_singleFFByte_returnsFF() {
        assertEquals("FF", QRCodeTask.getHex(new byte[]{(byte) 0xFF}));
    }

    @Test
    void getHex_multipleBytes_returnsUpperCase() {
        byte[] bytes = {(byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF};
        assertEquals("DEADBEEF", QRCodeTask.getHex(bytes));
    }

    @Test
    void getHex_allZeroBytes_returnsAllZeros() {
        byte[] bytes = new byte[4];
        assertEquals("00000000", QRCodeTask.getHex(bytes));
    }

    @Test
    void getHex_asciiBytes_correctHex() {
        // 'A'=0x41, 'B'=0x42, 'C'=0x43
        byte[] bytes = {'A', 'B', 'C'};
        assertEquals("414243", QRCodeTask.getHex(bytes));
    }

    @Test
    void getHex_md5LikeLength_returns32Chars() {
        byte[] bytes = new byte[16]; // 16 bytes = 32 hex chars
        String hex = QRCodeTask.getHex(bytes);
        assertEquals(32, hex.length());
    }

    // ---- isEnabled / setEnabled ----

    @Test
    void isEnabled_defaultFalse() {
        QRCodeTask.setEnabled(false); // reset to known state
        QRCodeTask task = new QRCodeTask();
        assertFalse(task.isEnabled());
    }

    @Test
    void setEnabled_true_isEnabledReturnsTrue() {
        try {
            QRCodeTask.setEnabled(true);
            QRCodeTask task = new QRCodeTask();
            assertTrue(task.isEnabled());
        } finally {
            QRCodeTask.setEnabled(false); // cleanup static state
        }
    }

    // ---- getConfigurables ----

    @Test
    void getConfigurables_returnsNonNullList() {
        QRCodeTask task = new QRCodeTask();
        assertNotNull(task.getConfigurables());
    }

    @Test
    void getConfigurables_returnsOneElement() {
        QRCodeTask task = new QRCodeTask();
        assertEquals(1, task.getConfigurables().size());
    }

    // ---- ZXing QR decode pipeline (the core logic used inside process()) ----

    @Test
    void qrDecode_pipeline_decodesGeneratedQRCode() throws Exception {
        String text = "Hello IPED QRCode Test";

        // Generate a QR code image using ZXing writer
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(text, BarcodeFormat.QR_CODE, 200, 200,
                Map.of(EncodeHintType.MARGIN, 2));
        BufferedImage img = MatrixToImageWriter.toBufferedImage(matrix);

        // Decode using the same pipeline as QRCodeTask.process()
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(img)));
        QRCodeReader reader = new QRCodeReader();
        Result result = reader.decode(bitmap);

        assertNotNull(result);
        assertEquals(text, result.getText());
    }

    @Test
    void qrDecode_pipeline_rawBytesConvertedToHex() throws Exception {
        String text = "IPED";

        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(text, BarcodeFormat.QR_CODE, 200, 200,
                Map.of(EncodeHintType.MARGIN, 2));
        BufferedImage img = MatrixToImageWriter.toBufferedImage(matrix);

        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(img)));
        QRCodeReader reader = new QRCodeReader();
        Result result = reader.decode(bitmap);

        assertNotNull(result.getRawBytes());
        String hex = QRCodeTask.getHex(result.getRawBytes());
        assertNotNull(hex);
        assertFalse(hex.isEmpty());
        // Hex string should only contain uppercase hex digits
        assertTrue(hex.matches("[0-9A-F]+"), "Hex should only contain [0-9A-F], got: " + hex);
    }

    @Test
    void qrDecode_pipeline_urlContent_decodesCorrectly() throws Exception {
        String url = "https://www.example.com/iped/case/12345";

        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(url, BarcodeFormat.QR_CODE, 300, 300,
                Map.of(EncodeHintType.MARGIN, 2));
        BufferedImage img = MatrixToImageWriter.toBufferedImage(matrix);

        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(img)));
        QRCodeReader reader = new QRCodeReader();
        Result result = reader.decode(bitmap);

        assertEquals(url, result.getText());
        assertEquals(BarcodeFormat.QR_CODE, result.getBarcodeFormat());
    }

    @Test
    void qrDecode_resultPoints_arePresentAndNonNull() throws Exception {
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode("test", BarcodeFormat.QR_CODE, 200, 200,
                Map.of(EncodeHintType.MARGIN, 2));
        BufferedImage img = MatrixToImageWriter.toBufferedImage(matrix);

        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(img)));
        QRCodeReader reader = new QRCodeReader();
        Result result = reader.decode(bitmap);

        assertNotNull(result.getResultPoints());
        assertTrue(result.getResultPoints().length > 0);
    }
}
