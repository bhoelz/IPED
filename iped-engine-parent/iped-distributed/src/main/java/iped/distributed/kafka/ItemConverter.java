package iped.distributed.kafka;

import iped.data.IItem;
import iped.distributed.agent.InputStreamFactoryRegistry;
import iped.engine.datasource.DatasourceRegistry;
import iped.io.ISeekableInputStreamFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.metadata.Metadata;

import java.util.*;

/**
 * Converts between {@link IItem} (engine in-memory representation) and
 * {@link KafkaItemMessage} (serializable Kafka payload).
 *
 * <p><b>Content streams</b>: The actual binary content of an item is NOT embedded
 * in the message. Instead {@link KafkaItemMessage#getInputStreamFactoryClass()} and
 * {@link KafkaItemMessage#getInputStreamFactoryParams()} are populated so that any
 * worker node can call {@link InputStreamFactoryRegistry#reconstruct} to obtain a
 * live factory — provided the datasource files are reachable via shared storage.
 */
@Slf4j
public final class ItemConverter {


    /** Temp-attribute key used to store the originating message inside the item. */
    public static final String ATTR_KAFKA_MSG     = "__distributed.kafkaMessage";
    /** Temp-attribute key used to store the Kafka output topic inside the item. */
    public static final String ATTR_OUTPUT_TOPIC  = "__distributed.outputTopic";
    /** Temp-attribute key used to store the raw-topic (stage 0) reference. */
    public static final String ATTR_RAW_TOPIC     = "__distributed.rawTopic";
    /** Temp-attribute key storing the item UUID assigned at pipeline entry. */
    public static final String ATTR_ITEM_UUID     = "__distributed.itemUuid";

    private ItemConverter() {}

    // -----------------------------------------------------------------------
    // IItem → KafkaItemMessage
    // -----------------------------------------------------------------------

    /**
     * Creates a new {@link KafkaItemMessage} from an {@link IItem}.
     * The message stage is set to 0 (raw — just produced by a reader).
     *
     * @param item   the source item
     * @param caseId the case identifier
     * @return a fully populated message ready to publish to the stage-0 topic
     */
    public static KafkaItemMessage toMessage(IItem item, String caseId) {
        KafkaItemMessage msg = new KafkaItemMessage();

        // Identity
        String uuid = (String) item.getTempAttribute(ATTR_ITEM_UUID);
        if (uuid == null) {
            uuid = UUID.randomUUID().toString();
            item.setTempAttribute(ATTR_ITEM_UUID, uuid);
        }
        msg.setItemUuid(uuid);
        msg.setCaseId(caseId);
        msg.setLocalItemId(item.getId());

        if (item.getDataSource() != null) {
            msg.setDataSourceUuid(item.getDataSource().getUUID());
            msg.setDataSourceName(item.getDataSource().getName());
        }

        // Pipeline
        msg.setPipelineStage(0);

        // Item properties
        msg.setPath(item.getPath());
        msg.setName(item.getName());
        msg.setExtension(item.getExt());
        msg.setLength(item.getLength());
        msg.setFileOffset(item.getFileOffset());
        msg.setDir(item.isDir());
        msg.setDeleted(item.isDeleted());
        msg.setCarved(item.isCarved());
        msg.setSubItem(item.isSubItem());
        msg.setRoot(item.isRoot());
        msg.setHasChildren(item.hasChildren());
        msg.setSumVolume(item.isToSumVolume());
        msg.setSubitemId(item.getSubitemId());
        msg.setIdInDataSource(item.getIdInDataSource());

        // Dates
        msg.setAccessDate(item.getAccessDate());
        msg.setCreationDate(item.getCreationDate());
        msg.setModificationDate(item.getModDate());
        msg.setChangeDate(item.getChangeDate());

        // Analysis state
        msg.setHash(item.getHash());
        msg.setMediaType(item.getMediaTypeString());

        // Tika metadata
        msg.setMetadata(metadataToMap(item.getMetadata()));

        // Extra attributes
        msg.setExtraAttributes(extraAttrsToMap(item));

        // InputStreamFactory reference (content pointer, not content itself)
        ISeekableInputStreamFactory factory = item.getInputStreamFactory();
        if (factory != null) {
            msg.setInputStreamFactoryClass(factory.getClass().getName());
            msg.setInputStreamFactoryParams(factory.getSerializableParams());
        }

        return msg;
    }

    /**
     * Merges updated item state back into an existing message (used by Task Agents
     * after running a task: the original message provides identity/stage, the item
     * provides the updated analysis state).
     */
    public static KafkaItemMessage mergeState(KafkaItemMessage original, IItem item) {
        KafkaItemMessage updated = copyIdentity(original);

        updated.setPipelineStage(original.getPipelineStage() + 1);

        // Updated analysis state
        updated.setHash(item.getHash());
        if (item.getMediaTypeString() != null) {
            updated.setMediaType(item.getMediaTypeString());
        }
        updated.setMetadata(metadataToMap(item.getMetadata()));
        updated.setExtraAttributes(extraAttrsToMap(item));

        // Propagate any property updates from the task
        updated.setDeleted(item.isDeleted());
        updated.setHasChildren(item.hasChildren());
        updated.setLength(item.getLength());

        // InputStreamFactory may be updated by a task (e.g. ExportFileTask)
        ISeekableInputStreamFactory factory = item.getInputStreamFactory();
        if (factory != null) {
            updated.setInputStreamFactoryClass(factory.getClass().getName());
            updated.setInputStreamFactoryParams(factory.getSerializableParams());
        }

        return updated;
    }

    // -----------------------------------------------------------------------
    // KafkaItemMessage → IItem
    // -----------------------------------------------------------------------

    /**
     * Reconstructs an {@link IItem} from a {@link KafkaItemMessage}.
     * The returned item is a live, locally addressable object that tasks can use
     * without knowing they are in a distributed pipeline.
     */
    public static IItem toItem(KafkaItemMessage msg) {
        IItem item = DatasourceRegistry.get().createItem();

        // Identity
        item.setTempAttribute(ATTR_ITEM_UUID, msg.getItemUuid());
        if (msg.getLocalItemId() >= 0) {
            item.setId(msg.getLocalItemId());
        }
        item.setIdInDataSource(msg.getIdInDataSource());

        // Properties
        item.setPath(msg.getPath());
        item.setName(msg.getName());
        item.setExtension(msg.getExtension());
        item.setLength(msg.getLength());
        if (msg.getFileOffset() != null) item.setFileOffset(msg.getFileOffset());
        item.setIsDir(msg.isDir());
        item.setDeleted(msg.isDeleted());
        item.setCarved(msg.isCarved());
        item.setSubItem(msg.isSubItem());
        item.setRoot(msg.isRoot());
        item.setHasChildren(msg.isHasChildren());
        item.setSumVolume(msg.isSumVolume());
        if (msg.getSubitemId() != null) item.setSubitemId(msg.getSubitemId());

        // Dates
        item.setAccessDate(msg.getAccessDate());
        item.setCreationDate(msg.getCreationDate());
        item.setModificationDate(msg.getModificationDate());
        item.setChangeDate(msg.getChangeDate());

        // Analysis state
        item.setHash(msg.getHash());
        if (msg.getMediaType() != null) item.setMediaType(msg.getMediaType());

        // Tika metadata
        if (msg.getMetadata() != null) {
            Metadata metadata = new Metadata();
            for (Map.Entry<String, List<String>> e : msg.getMetadata().entrySet()) {
                for (String v : e.getValue()) {
                    metadata.add(e.getKey(), v);
                }
            }
            item.setMetadata(metadata);
        }

        // Extra attributes
        if (msg.getExtraAttributes() != null) {
            for (Map.Entry<String, Object> e : msg.getExtraAttributes().entrySet()) {
                item.setExtraAttribute(e.getKey(), e.getValue());
            }
        }

        // Reconstruct InputStreamFactory if available
        if (msg.getInputStreamFactoryClass() != null
                && InputStreamFactoryRegistry.isRegistered(msg.getInputStreamFactoryClass())) {
            try {
                ISeekableInputStreamFactory factory = InputStreamFactoryRegistry.reconstruct(
                        msg.getInputStreamFactoryClass(),
                        msg.getInputStreamFactoryParams());
                item.setInputStreamFactory(factory);
            } catch (Exception e) {
                log.warn("Could not reconstruct InputStreamFactory '{}' for item '{}': {}",
                        msg.getInputStreamFactoryClass(), msg.getItemUuid(), e.getMessage());
            }
        }

        return item;
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static Map<String, List<String>> metadataToMap(Object metaObj) {
        if (!(metaObj instanceof Metadata)) return Collections.emptyMap();
        Metadata meta = (Metadata) metaObj;
        Map<String, List<String>> map = new LinkedHashMap<>();
        for (String name : meta.names()) {
            map.put(name, Arrays.asList(meta.getValues(name)));
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> extraAttrsToMap(IItem item) {
        try {
            Map<String, Object> map = new LinkedHashMap<>();
            // IItem.getExtraAttributeMap() is not in the public API; use reflection
            // as a best-effort approach. If unavailable, attributes set before this
            // call will be lost across stages, but attributes set by subsequent tasks
            // will still be propagated.
            java.lang.reflect.Method m = item.getClass().getMethod("getExtraAttributeMap");
            Map<String, Object> src = (Map<String, Object>) m.invoke(item);
            if (src != null) {
                src.forEach((k, v) -> {
                    // Skip non-serializable temp attributes (start with "__")
                    if (!k.startsWith("__")) {
                        map.put(k, v);
                    }
                });
            }
            return map;
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private static KafkaItemMessage copyIdentity(KafkaItemMessage src) {
        KafkaItemMessage copy = new KafkaItemMessage();
        copy.setCaseId(src.getCaseId());
        copy.setItemUuid(src.getItemUuid());
        copy.setLocalItemId(src.getLocalItemId());
        copy.setDataSourceUuid(src.getDataSourceUuid());
        copy.setDataSourceName(src.getDataSourceName());
        copy.setPath(src.getPath());
        copy.setName(src.getName());
        copy.setExtension(src.getExtension());
        copy.setLength(src.getLength());
        copy.setFileOffset(src.getFileOffset());
        copy.setDir(src.isDir());
        copy.setCarved(src.isCarved());
        copy.setSubItem(src.isSubItem());
        copy.setRoot(src.isRoot());
        copy.setSumVolume(src.isSumVolume());
        copy.setSubitemId(src.getSubitemId());
        copy.setParentItemUuid(src.getParentItemUuid());
        copy.setParentItemUuids(src.getParentItemUuids());
        copy.setIdInDataSource(src.getIdInDataSource());
        copy.setAccessDate(src.getAccessDate());
        copy.setCreationDate(src.getCreationDate());
        copy.setModificationDate(src.getModificationDate());
        copy.setChangeDate(src.getChangeDate());
        copy.setCaseOutputPath(src.getCaseOutputPath());
        copy.setSharedStorageRoot(src.getSharedStorageRoot());
        copy.setPriority(src.isPriority());
        // Keep original factory reference as fallback
        copy.setInputStreamFactoryClass(src.getInputStreamFactoryClass());
        copy.setInputStreamFactoryParams(src.getInputStreamFactoryParams());
        return copy;
    }
}
