package iped.parsers.evtx.model;

import java.nio.ByteBuffer;

public class BinXmlToken {
  public static final byte LIBFWEVT_XML_TOKEN_END_OF_FILE = 0x00;
  public static final byte LIBFWEVT_XML_TOKEN_OPEN_START_ELEMENT_TAG = 0x01;
  public static final byte LIBFWEVT_XML_TOKEN_CLOSE_START_ELEMENT_TAG = 0x02;
  public static final byte LIBFWEVT_XML_TOKEN_CLOSE_EMPTY_ELEMENT_TAG = 0x03;
  public static final byte LIBFWEVT_XML_TOKEN_END_ELEMENT_TAG = 0x04;
  public static final byte LIBFWEVT_XML_TOKEN_VALUE = 0x05;
  public static final byte LIBFWEVT_XML_TOKEN_ATTRIBUTE = 0x06;
  public static final byte LIBFWEVT_XML_TOKEN_CDATA_SECTION = 0x07;
  public static final byte LIBFWEVT_XML_TOKEN_CHARACTER_REFERENCE = 0x08;
  public static final byte LIBFWEVT_XML_TOKEN_ENTITY_REFERENCE = 0x09;
  public static final byte LIBFWEVT_XML_TOKEN_PI_TARGET = 0x0a;
  public static final byte LIBFWEVT_XML_TOKEN_PI_DATA = 0x0b;
  public static final byte LIBFWEVT_XML_TOKEN_TEMPLATE_INSTANCE = 0x0c;
  public static final byte LIBFWEVT_XML_TOKEN_NORMAL_SUBSTITUTION = 0x0d;
  public static final byte LIBFWEVT_XML_TOKEN_OPTIONAL_SUBSTITUTION = 0x0e;
  public static final byte LIBFWEVT_XML_TOKEN_FRAGMENT_HEADER = 0x0f;

  public byte type;

  public BinXmlToken(EvtxFile evtxFile, ByteBuffer bb) {
    type = bb.get();
  }

  public byte getType() {
    return type;
  }

  public void setType(byte type) {
    this.type = type;
  }

  @Override
  public String toString() {
    return Byte.toString(type) + "(" + Integer.toHexString(type) + ")";
  }
}
