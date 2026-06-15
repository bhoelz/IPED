package iped.engine.task.carver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import iped.carvers.api.Carver;
import iped.carvers.api.CarverType;
import iped.carvers.api.Signature.SignatureType;
import iped.carvers.standard.DefaultCarver;
import org.apache.tika.mime.MediaType;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;

/**
 * Loads carver definitions from {@code CarverConfig.toml}, the TOML successor
 * to the legacy {@code CarverConfig.xml}. Extends {@link XMLCarverConfiguration}
 * so XML user-override files can still be applied on top after TOML is loaded.
 *
 * <p>When a {@code [[carverType]]} entry carries no inline {@code headers} or
 * {@code footers}, the named {@code carverClass} is instantiated and its own
 * {@link Carver#getCarverTypes()} result is used, exactly as the XML reader does.
 */
public class TomlCarverConfiguration extends XMLCarverConfiguration {

    private static final long serialVersionUID = 1L;

    private static final TomlMapper MAPPER = new TomlMapper();

    public void loadTomlConfigFile(File confFile) throws IOException {
        JsonNode root;
        try {
            root = MAPPER.readTree(confFile);
        } catch (Exception e) {
            throw new IOException("Failed to parse TOML carver config: " + confFile, e);
        }

        JsonNode ignoreCorruptedNode = root.get("ignoreCorrupted");
        if (ignoreCorruptedNode != null) {
            ignoreCorrupted = ignoreCorruptedNode.asBoolean(true);
        }

        JsonNode typesToNotProcess = root.get("typesToNotProcess");
        if (typesToNotProcess != null && typesToNotProcess.isArray()) {
            for (JsonNode typeNode : typesToNotProcess) {
                TYPES_TO_NOT_PROCESS.add(typeNode.asText().trim());
            }
        }

        JsonNode typesToProcess = root.get("typesToProcess");
        if (typesToProcess != null && typesToProcess.isArray()) {
            if (TYPES_TO_PROCESS == null) {
                TYPES_TO_PROCESS = new HashSet<>();
            }
            for (JsonNode typeNode : typesToProcess) {
                TYPES_TO_PROCESS.add(MediaType.parse(typeNode.asText().trim()));
            }
        }

        JsonNode carverTypesNode = root.get("carverType");
        if (carverTypesNode != null && carverTypesNode.isArray()) {
            for (JsonNode ctNode : carverTypesNode) {
                try {
                    processCarverTypeNode(ctNode);
                } catch (IOException e) {
                    throw e;
                } catch (Exception e) {
                    JsonNode nameNode = ctNode.get("name");
                    throw new IOException("Error processing carverType '"
                            + (nameNode != null ? nameNode.asText() : "<unnamed>") + "'", e);
                }
            }
        }
    }

    private void processCarverTypeNode(JsonNode ctNode) throws Exception {
        JsonNode headersNode      = ctNode.get("headers");
        JsonNode footersNode      = ctNode.get("footers");
        JsonNode escapeFooters    = ctNode.get("escapeFooters");
        JsonNode lengthRefs       = ctNode.get("lengthRefs");
        JsonNode controls         = ctNode.get("controls");
        JsonNode carverClassNode  = ctNode.get("carverClass");

        boolean hasSigDefs = hasItems(headersNode) || hasItems(footersNode)
                || hasItems(escapeFooters) || hasItems(lengthRefs) || hasItems(controls);

        if (hasSigDefs) {
            CarverType ct = new CarverType();
            applyMetadata(ct, ctNode);
            if (headersNode != null) {
                for (JsonNode sig : headersNode) ct.addHeader(sig.asText());
            }
            if (footersNode != null) {
                for (JsonNode sig : footersNode) ct.addFooter(sig.asText());
            }
            if (escapeFooters != null) {
                for (JsonNode sig : escapeFooters) ct.addSignature(sig.asText(), SignatureType.ESCAPEFOOTER);
            }
            if (lengthRefs != null) {
                for (JsonNode sig : lengthRefs) ct.addSignature(sig.asText(), SignatureType.LENGTHREF);
            }
            if (controls != null) {
                for (JsonNode sig : controls) ct.addSignature(sig.asText(), SignatureType.CONTROL);
            }
            TYPES_TO_CARVE.add(ct.getMimeType());
            carverTypesArray.add(ct);
        } else if (carverClassNode != null) {
            Class<?> clazz = Class.forName(carverClassNode.asText().trim());
            Carver cv = (Carver) clazz.getDeclaredConstructor().newInstance();
            for (CarverType ct : cv.getCarverTypes()) {
                applyMetadata(ct, ctNode);
                TYPES_TO_CARVE.add(ct.getMimeType());
                carverTypesArray.add(ct);
            }
        }
    }

    private static boolean hasItems(JsonNode node) {
        return node != null && node.isArray() && node.size() > 0;
    }

    private static void applyMetadata(CarverType ct, JsonNode node) {
        JsonNode name            = node.get("name");
        JsonNode mediaType       = node.get("mediaType");
        JsonNode carverClass     = node.get("carverClass");
        JsonNode minLength       = node.get("minLength");
        JsonNode maxLength       = node.get("maxLength");
        JsonNode lengthOffset    = node.get("lengthOffset");
        JsonNode lengthSizeBytes = node.get("lengthSizeBytes");
        JsonNode lengthBigEndian = node.get("lengthBigEndian");
        JsonNode stopOnNext      = node.get("stopOnNextHeader");

        if (name != null)            ct.setName(name.asText());
        if (mediaType != null)       ct.setMimeType(MediaType.parse(mediaType.asText().trim()));
        if (carverClass != null)     ct.setCarverClass(carverClass.asText().trim());
        else                         ct.setCarverClass(DefaultCarver.class.getName());
        if (minLength != null)       ct.setMinLength(minLength.asInt());
        if (maxLength != null)       ct.setMaxLength(maxLength.asLong());
        if (lengthOffset != null)    ct.setSizePos(lengthOffset.asInt());
        if (lengthSizeBytes != null) ct.setSizeBytes(lengthSizeBytes.asInt());
        if (lengthBigEndian != null) ct.setBigendian(lengthBigEndian.asBoolean(false));
        else                         ct.setBigendian(false);
        if (stopOnNext != null)      ct.setStopOnNextHeader(stopOnNext.asBoolean(false));
    }
}
