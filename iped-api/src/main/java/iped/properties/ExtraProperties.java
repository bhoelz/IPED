package iped.properties;

import org.apache.tika.metadata.Property;

import java.util.Arrays;
import java.util.List;

/**
 * Extra metadata produced by package parsers.
 *
 * @author Nassif
 */
public class ExtraProperties {

    /** Persistent global id of the item, unique across cases. */
    public static final String GLOBAL_ID = "globalId"; //$NON-NLS-1$

    /** Tika metadata key listing the parsers used to process the item. */
    public static final String TIKA_PARSER_USED_KEY = "X-TIKA:Parsed-By"; //$NON-NLS-1$

    /**
     * @deprecated Prefer {@link #TIKA_PARSER_USED_KEY}.
     */
    @Deprecated
    public static final String TIKA_PARSER_USED = TIKA_PARSER_USED_KEY;

    /** Name of the datasource reader that decoded the item. */
    public static final String DATASOURCE_READER = "X-Reader"; //$NON-NLS-1$

    /** Marks virtual folders created to group embedded items. */
    public static final String EMBEDDED_FOLDER = "IpedEmbeddeFolder"; //$NON-NLS-1$

    /** Last accessed date of embedded items, as a string key. */
    public static final String ACCESSED_KEY = "IpedLastAccessedDate"; //$NON-NLS-1$

    /** Last accessed date of embedded items, as a date property. */
    public static final Property ACCESSED = Property.internalDate(ACCESSED_KEY); //$NON-NLS-1$

    /** Date a browser history URL was visited, as a string key. */
    public static final String VISIT_DATE_KEY = "visitDate"; //$NON-NLS-1$

    /** Date a browser history URL was visited, as a date property. */
    public static final Property VISIT_DATE = Property.internalDate(VISIT_DATE_KEY); //$NON-NLS-1$

    /** Date a file download finished, as a string key. */
    public static final String DOWNLOAD_DATE_KEY = "downloadDate"; //$NON-NLS-1$

    /** Date a file download finished, as a date property. */
    public static final Property DOWNLOAD_DATE = Property.internalDate(DOWNLOAD_DATE_KEY); //$NON-NLS-1$

    /** Total size in bytes of a downloaded file. */
    public static final String DOWNLOAD_TOTAL_BYTES = "totalBytes"; //$NON-NLS-1$

    /** Bytes actually received of a downloaded file. */
    public static final String DOWNLOAD_RECEIVED_BYTES = "receivedBytes"; //$NON-NLS-1$

    /** Marks embedded items recovered from deleted data. */
    public static final String DELETED = "IpedDeletedEmbeddedItem"; //$NON-NLS-1$

    /** Prefix of all e-mail/instant message related metadata keys. */
    public static final String MESSAGE_PREFIX = "Message-"; //$NON-NLS-1$

    /** Subject of an e-mail or message. */
    public static final String MESSAGE_SUBJECT = MESSAGE_PREFIX + "Subject"; //$NON-NLS-1$

    /** Prefix of all chat conversation related metadata keys. */
    public static final String CONVERSATION_PREFIX = "Conversation:";

    /** Application-specific id of the conversation. */
    public static final String CONVERSATION_ID = CONVERSATION_PREFIX + "id";

    /** Account that owns the conversation. */
    public static final String CONVERSATION_ACCOUNT = CONVERSATION_PREFIX + "Account";

    /** Display name of the conversation or group. */
    public static final String CONVERSATION_NAME = CONVERSATION_PREFIX + "Name";

    /** Conversation type, e.g. one-to-one or group. */
    public static final String CONVERSATION_TYPE = CONVERSATION_PREFIX + "Type";

    /** Whether the account owner is a group admin, as a string key. */
    public static final String CONVERSATION_IS_OWNER_ADMIN_KEY = CONVERSATION_PREFIX + "isOwnerAdmin";

    /** Whether the account owner is a group admin, as a boolean property. */
    public static final Property CONVERSATION_IS_OWNER_ADMIN = Property.internalBoolean(CONVERSATION_IS_OWNER_ADMIN_KEY);

    /** Administrators of the conversation group. */
    public static final String CONVERSATION_ADMINS = CONVERSATION_PREFIX + "Admins";

    /** Participants of the conversation. */
    public static final String CONVERSATION_PARTICIPANTS = CONVERSATION_PREFIX + "Participants";

    /** Number of messages in the conversation, as a string key. */
    public static final String CONVERSATION_MESSAGES_COUNT_KEY = CONVERSATION_PREFIX + "messagesCount";

    /** Number of messages in the conversation, as an integer property. */
    public static final Property CONVERSATION_MESSAGES_COUNT = Property.internalInteger(CONVERSATION_MESSAGES_COUNT_KEY);

    /** Suffix of participant/admin id sub-keys. */
    public static final String CONVERSATION_SUFFIX_ID = ":id";
    /** Suffix of participant/admin name sub-keys. */
    public static final String CONVERSATION_SUFFIX_NAME = ":name";
    /** Suffix of participant/admin phone number sub-keys. */
    public static final String CONVERSATION_SUFFIX_PHONE = ":phoneNumber";
    /** Suffix of participant/admin username sub-keys. */
    public static final String CONVERSATION_SUFFIX_USERNAME = ":username";

    /** Prefix of all communication (call/message) related metadata keys. */
    public static final String COMMUNICATION_PREFIX = "Communication:";

    /** Direction of the communication, e.g. incoming or outgoing. */
    public static final String COMMUNICATION_DIRECTION = COMMUNICATION_PREFIX + "Direction";

    /** Sender of the communication. */
    public static final String COMMUNICATION_FROM = COMMUNICATION_PREFIX + "From";

    /** Recipients of the communication. */
    public static final String COMMUNICATION_TO = COMMUNICATION_PREFIX + "To";

    /** Date of the communication, as a string key. */
    public static final String COMMUNICATION_DATE_KEY = COMMUNICATION_PREFIX + "Date"; //$NON-NLS-1$

    /** Date of the communication, as a date property. */
    public static final Property COMMUNICATION_DATE = Property.internalDate(COMMUNICATION_DATE_KEY); //$NON-NLS-1$

    /** Alias of {@link #COMMUNICATION_DATE} for messages. */
    public static final Property MESSAGE_DATE = COMMUNICATION_DATE;

    /** Participants of the communication. */
    public static final String PARTICIPANTS = COMMUNICATION_PREFIX + "Participants";

    /** Id of the group a message belongs to. */
    public static final String GROUP_ID = "GroupID";

    /** Whether the message was sent to a group. */
    public static final String IS_GROUP_MESSAGE = "isGroupMessage";

    /** Body text of an e-mail or message. */
    public static final String MESSAGE_BODY = MESSAGE_PREFIX + "Body"; //$NON-NLS-1$

    /** Whether the item is an e-mail attachment. */
    public static final String MESSAGE_IS_ATTACHMENT = MESSAGE_PREFIX + "IsEmailAttachment"; //$NON-NLS-1$

    /** Number of attachments of a message, as a string key. */
    public static final String MESSAGE_ATTACHMENT_COUNT_KEY = MESSAGE_PREFIX + "AttachmentCount"; //$NON-NLS-1$

    /** Number of attachments of a message, as an integer property. */
    public static final Property MESSAGE_ATTACHMENT_COUNT = Property.internalInteger(MESSAGE_ATTACHMENT_COUNT_KEY); //$NON-NLS-1$

    /** Number of CSAM hash database hits inside a container or disk. */
    public static final String CSAM_HASH_HITS = "childPornHashHits"; //$NON-NLS-1$

    /** Number of P2P application registry/history entries found. */
    public static final String P2P_REGISTRY_COUNT = "p2pHistoryEntries"; //$NON-NLS-1$

    /** Hashes of files shared through P2P applications. */
    public static final String SHARED_HASHES = "sharedHashes"; //$NON-NLS-1$

    /** Items shared through P2P applications. */
    public static final String SHARED_ITEMS = "sharedItems"; //$NON-NLS-1$

    /** Query that links other items to this one, e.g. P2P downloads. */
    public static final String LINKED_ITEMS = "linkedItems"; //$NON-NLS-1$

    /** Prefix of generic document metadata keys. */
    public static final String GENERIC_META_PREFIX = "meta:"; //$NON-NLS-1$

    /** Prefix of metadata keys common to several file formats. */
    public static final String COMMON_META_PREFIX = "common:"; //$NON-NLS-1$

    /** Prefix of audio file metadata keys. */
    public static final String AUDIO_META_PREFIX = "audio:"; //$NON-NLS-1$

    /** Prefix of image file metadata keys. */
    public static final String IMAGE_META_PREFIX = "image:"; //$NON-NLS-1$

    /** Prefix of video file metadata keys. */
    public static final String VIDEO_META_PREFIX = "video:"; //$NON-NLS-1$

    /** Prefix of PDF file metadata keys. */
    public static final String PDF_META_PREFIX = "pdf:"; //$NON-NLS-1$

    /** Prefix of HTML file metadata keys. */
    public static final String HTML_META_PREFIX = "html:"; //$NON-NLS-1$

    /** Prefix of office document metadata keys. */
    public static final String OFFICE_META_PREFIX = "office:"; //$NON-NLS-1$

    /** Prefix of metadata keys decoded from UFED extractions. */
    public static final String UFED_META_PREFIX = "ufed:"; //$NON-NLS-1$

    /** UFED id of the decoded entity. */
    public static final String UFED_ID = UFED_META_PREFIX + "id";

    /** UFED id of the file the decoded entity refers to. */
    public static final String UFED_FILE_ID = UFED_META_PREFIX + "file_id";

    /** UFED id of the coordinate linked to the decoded entity. */
    public static final String UFED_COORDINATE_ID = UFED_META_PREFIX + "coordinate_id";

    /** UFED ids of entities this one jumps (links) to. */
    public static final String UFED_JUMP_TARGETS = UFED_META_PREFIX + "jumpTargets";

    /** UFED source models the decoded entity was built from. */
    public static final String UFED_SOURCE_MODELS = UFED_META_PREFIX + "sourceModels";

    /** Prefix of metadata keys decoded from P2P applications. */
    public static final String P2P_META_PREFIX = "p2p:"; //$NON-NLS-1$

    /** Temporary virtual id of an item during processing. */
    public static final String ITEM_VIRTUAL_ID = "itemVirtualIdentifier"; //$NON-NLS-1$

    /** Virtual id ({@link #ITEM_VIRTUAL_ID}) of the item's parent. */
    public static final String PARENT_VIRTUAL_ID = "parentVirtualIdentifier"; //$NON-NLS-1$

    /** Geographic locations (lat;long) associated with the item. */
    public static final String LOCATIONS = COMMON_META_PREFIX + "geo:locations"; //$NON-NLS-1$

    /** URL related to the item, e.g. of a browser history entry. */
    public static final String URL = "url"; //$NON-NLS-1$

    /** Local file system path related to the item, e.g. of a download. */
    public static final String LOCAL_PATH = "localPath"; //$NON-NLS-1$

    /** Phone number of a user account or contact. */
    public static final String USER_PHONE = "phoneNumber"; //$NON-NLS-1$

    /** Display name of a user account or contact. */
    public static final String USER_NAME = "userName"; //$NON-NLS-1$

    /** Account identifier of a user account or contact. */
    public static final String USER_ACCOUNT = "userAccount"; //$NON-NLS-1$

    /** Account this contact belongs to. */
    public static final String CONTACT_OF_ACCOUNT = "contactOfAccount"; //$NON-NLS-1$

    /** Type (application/service) of a user account. */
    public static final String USER_ACCOUNT_TYPE = "accountType"; //$NON-NLS-1$

    /** E-mail address of a user account or contact. */
    public static final String USER_EMAIL = "emailAddress"; //$NON-NLS-1$

    /** Postal address of a user account or contact. */
    public static final String USER_ADDRESS = "userAddress"; //$NON-NLS-1$

    /** Organization of a user account or contact. */
    public static final String USER_ORGANIZATION = "userOrganization"; //$NON-NLS-1$

    /** Notes about a user account or contact. */
    public static final String USER_NOTES = "userNotes"; //$NON-NLS-1$

    /** URLs of a user account or contact. */
    public static final String USER_URLS = "userUrls"; //$NON-NLS-1$

    /** Birth date of a user account or contact, as a string key. */
    public static final String USER_BIRTH_KEY = "userBirthday"; //$NON-NLS-1$

    /** Birth date of a user account or contact, as a date property. */
    public static final Property USER_BIRTH = Property.internalDate(USER_BIRTH_KEY); //$NON-NLS-1$

    /** Item thumbnail encoded as Base64. */
    public static final String THUMBNAIL_BASE64 = "thumbnailBase64"; //$NON-NLS-1$

    /** Position of the item within its parent's view, e.g. a chat. */
    public static final String PARENT_VIEW_POSITION = "parentViewPosition"; //$NON-NLS-1$

    /** Name of the carver that recovered the item. */
    public static final String CARVEDBY_METADATA_NAME = "CarvedBy"; //$NON-NLS-1$

    /** Offset of the carved item within its parent, as a string key. */
    public static final String CARVEDOFFSET_METADATA_NAME_KEY = "CarvedOffset"; //$NON-NLS-1$

    /** Offset of the carved item within its parent, as an integer property. */
    public static final Property CARVEDOFFSET_METADATA_NAME = Property.internalInteger(CARVEDOFFSET_METADATA_NAME_KEY); //$NON-NLS-1$

    /** Transcription of an audio item. */
    public static final String TRANSCRIPT_ATTR = AUDIO_META_PREFIX + "transcription";

    /** Confidence score of the audio transcription. */
    public static final String CONFIDENCE_ATTR = AUDIO_META_PREFIX + "transcriptConfidence";

    /** Internal ordinals of the item's timeline events. */
    public static final String TIME_EVENT_ORDS = "timeEventOrds";

    /** Groups of timeline event types sharing the same timestamp. */
    public static final String TIME_EVENT_GROUPS = "timeEventGroups";

    /** Marks items decoded from application data, not present as files. */
    public static final String DECODED_DATA = "isDecodedData";

    /** Marks items whose content was downloaded from an external source. */
    public static final String DOWNLOADED_DATA = "downloadedData";

    /** Marks items extracted from containers to the file system. */
    public static final String EXTRACTED_FILE = "extractedFile";

    /** Number of faces detected in an image. */
    public static final String FACE_COUNT = "face_count";

    /** Bounding boxes of faces detected in an image. */
    public static final String FACE_LOCATIONS = "face_locations";

    /** Feature vectors of faces detected in an image. */
    public static final String FACE_ENCODINGS = "face_encodings";

    /** Age range labels of faces detected in an image. */
    public static final String FACE_AGE_LABELS = "faceAge:labels";

    /** Prefix of hash database lookup metadata keys. */
    public static final String HASHDB_PREFIX = "hashDb:";
    /** Hash database property holding the item status, e.g. known-good. */
    public static final String STATUS_PROPERTY = "status";
    /** Hash database property holding the hash set names. */
    public static final String SET_PROPERTY = "set";
    /** Item status returned by hash database lookups. */
    public static final String HASHDB_STATUS = HASHDB_PREFIX + STATUS_PROPERTY;
    /** Hash sets the item's hash was found in. */
    public static final String HASHDB_SET = HASHDB_PREFIX + SET_PROPERTY;

    /**
     * MetadataProperty to be set if the evidence is an animated image (i.e., contain multiple
     * frames). Only set if the number of frames is greater than one.
     */
    public static final String ANIMATION_FRAMES_PROP = IMAGE_META_PREFIX + "AnimationFrames";

    /** Message metadata keys treated as basic properties when indexing. */
    public static final List<String> COMMUNICATION_BASIC_PROPS = Arrays.asList(MESSAGE_SUBJECT, MESSAGE_BODY,
        MESSAGE_PREFIX + "CC", MESSAGE_PREFIX + "BCC", MESSAGE_PREFIX + "Recipient-Address", MESSAGE_IS_ATTACHMENT,
        MESSAGE_ATTACHMENT_COUNT.getName());
}
