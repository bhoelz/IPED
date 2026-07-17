package iped.parsers.evtx.model;

import java.nio.ByteBuffer;

public class EvtxNormalSubstitution extends EvtxOptionalSubstitution {

  public EvtxNormalSubstitution(EvtxFile evtxFile, ByteBuffer bb, EvtxElement evtxElement)
      throws EvtxParseException {
    super(evtxFile, bb, evtxElement);
  }
}
