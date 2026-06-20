package iped.carvers.api;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;

public class Signature implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    CarverType carverType = null;
    public byte[][] seqs = null;
    public int[] seqEndPos;
    SignatureType signatureType;
    String sigString;
    int length;

    public Signature(CarverType carverType, String sigString, SignatureType sigType) {
        this.carverType = carverType;
        this.sigString = sigString;
        this.signatureType = sigType;
    }

    public SignatureType getSignatureType() {
        return signatureType;
    }

    // Back-reference to the owning CarverType, which in turn holds this Signature in
    // its own signatures list — a genuine bidirectional cycle, not just a deep object
    // graph. Excluded from JSON serialization (schema validation, etc.); the JSON view
    // of a carver type only needs to walk forward (CarverType -> signatures), never back.
    @JsonIgnore
    public CarverType getCarverType() {
        return carverType;
    }

    public boolean isFooter() {
        return signatureType == SignatureType.FOOTER;
    }

    public boolean isHeader() {
        return signatureType == SignatureType.HEADER;
    }

    public int getLength() {
        return length;
    }

    public enum SignatureType implements Serializable {
        HEADER, FOOTER, ESCAPEFOOTER, LENGTHREF, CONTROL;
    }

    public String getSigString() {
        return sigString;
    }

    public void setSigString(String sigString) {
        this.sigString = sigString;
    }
}
