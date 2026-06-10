package iped.parsers.plist;

import com.dd.plist.NSObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.detect.apple.BPListDetector;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.ParseContext;
import org.slf4j.Logger;
import org.xml.sax.SAXException;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Slf4j
public class PListParser extends AbstractPListParser<Void> {

    private static final long serialVersionUID = 3633471688807424828L;


    private static final Set<MediaType> SUPPORTED_TYPES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(BPListDetector.BITUNES,
            BPListDetector.BMEMGRAPH, BPListDetector.BPLIST, BPListDetector.BWEBARCHIVE, BPListDetector.PLIST)));

    @Override
    public Set<MediaType> getSupportedTypes(ParseContext context) {
        return SUPPORTED_TYPES;
    }

    @Override
    protected Logger getLogger() {
        return log;
    }

    @Override
    protected void processAndGenerateHTMLContent(NSObject plistObj, State state) throws SAXException {

        if (plistObj != null) {
            processObject(plistObj, "", state, true); // first elements are open by default
        } else {
            state.xhtml.startElement("p");
            state.xhtml.characters("No PList content found or unexpected structure.");
            state.xhtml.endElement("p");
        }
    }

}
