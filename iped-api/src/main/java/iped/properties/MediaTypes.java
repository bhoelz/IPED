package iped.properties;

import iped.data.IItemReader;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.mime.MediaType;
import org.apache.tika.mime.MediaTypeRegistry;

/**
 * IPED-specific media types and helpers to query the Tika media type
 * hierarchy.
 */
public class MediaTypes {

    private static final MediaTypeRegistry MEDIA_TYPE_REGISTRY = TikaConfig.getDefaultConfig().getMediaTypeRegistry();

    /** Parent type of items that are metadata entries, not real files. */
    public static final MediaType METADATA_ENTRY = MediaType.application("x-metadata-entry"); //$NON-NLS-1$
    /** Chat message decoded from an application database. */
    public static final MediaType CHAT_MESSAGE_MIME = MediaType.parse("message/x-chat-message"); //$NON-NLS-1$
    /** E-mail decoded from a UFED extraction. */
    public static final MediaType UFED_EMAIL_MIME = MediaType.application("x-ufed-email");
    /** Instant message decoded from a UFED extraction. */
    public static final MediaType UFED_MESSAGE_MIME = MediaType.application("x-ufed-instantmessage"); //$NON-NLS-1$
    /** Attachment decoded from a UFED extraction. */
    public static final MediaType UFED_ATTACHMENT_MIME = MediaType.application("x-ufed-attachment");
    /** Phone call record decoded from a UFED extraction. */
    public static final MediaType UFED_CALL_MIME = MediaType.application("x-ufed-call"); //$NON-NLS-1$
    /** SMS message decoded from a UFED extraction. */
    public static final MediaType UFED_SMS_MIME = MediaType.application("x-ufed-sms"); //$NON-NLS-1$
    /** MMS message decoded from a UFED extraction. */
    public static final MediaType UFED_MMS_MIME = MediaType.application("x-ufed-mms"); //$NON-NLS-1$
    /** Contact decoded from a UFED extraction. */
    public static final MediaType UFED_CONTACT_MIME = MediaType.application("x-ufed-contact"); //$NON-NLS-1$
    /** User account decoded from a UFED extraction. */
    public static final MediaType UFED_USER_ACCOUNT_MIME = MediaType.application("x-ufed-useraccount");
    /** Device information entry decoded from a UFED extraction. */
    public static final MediaType UFED_DEVICE_INFO = MediaType.application("x-ufed-deviceinfo"); //$NON-NLS-1$
    /** Unallocated disk space. */
    public static final MediaType UNALLOCATED = MediaType.application("x-unallocated"); //$NON-NLS-1$
    /** Microsoft Outlook MSG file. */
    public static final MediaType OUTLOOK_MSG = MediaType.application("vnd.ms-outlook");
    /** Generic disk image. */
    public static final MediaType DISK_IMAGE = MediaType.application("x-disk-image"); //$NON-NLS-1$
    /** Raw (dd) disk image. */
    public static final MediaType RAW_IMAGE = MediaType.application("x-raw-image"); //$NON-NLS-1$
    /** Expert Witness Format (EWF) disk image. */
    public static final MediaType EWF_IMAGE = MediaType.application("x-ewf-image"); //$NON-NLS-1$
    /** Expert Witness Format version 2 disk image. */
    public static final MediaType EWF2_IMAGE = MediaType.application("x-ewf2-image"); //$NON-NLS-1$
    /** EnCase E01 disk image. */
    public static final MediaType E01_IMAGE = MediaType.application("x-e01-image"); //$NON-NLS-1$
    /** EnCase Ex01 disk image. */
    public static final MediaType EX01_IMAGE = MediaType.application("x-ex01-image"); //$NON-NLS-1$
    /** ISO 9660 optical disc image. */
    public static final MediaType ISO_IMAGE = MediaType.application("x-iso9660-image"); //$NON-NLS-1$
    /** VMware VMDK virtual disk. */
    public static final MediaType VMDK = MediaType.application("x-vmdk"); //$NON-NLS-1$
    /** Data extent of a VMware VMDK virtual disk. */
    public static final MediaType VMDK_DATA = MediaType.application("x-vmdk-data"); //$NON-NLS-1$
    /** Descriptor file of a VMware VMDK virtual disk. */
    public static final MediaType VMDK_DESCRIPTOR = MediaType.application("x-vmdk-descriptor"); //$NON-NLS-1$
    /** Microsoft VHD virtual disk. */
    public static final MediaType VHD = MediaType.application("x-vhd"); //$NON-NLS-1$
    /** Microsoft VHDX virtual disk. */
    public static final MediaType VHDX = MediaType.application("x-vhdx"); //$NON-NLS-1$
    /** VirtualBox VDI virtual disk. */
    public static final MediaType VDI = MediaType.application("x-vdi"); //$NON-NLS-1$
    /** Microsoft Publisher document. */
    public static final MediaType MS_PUBLISHER = MediaType.application("x-mspublisher"); //$NON-NLS-1$

    /** Subtype prefix common to all media types decoded from UFED extractions. */
    public static final String UFED_MIME_PREFIX = "x-ufed-"; //$NON-NLS-1$

    /**
     * @return the Tika media type registry with the type hierarchy
     */
    public static MediaTypeRegistry getMediaTypeRegistry() {
        return MEDIA_TYPE_REGISTRY;
    }

    /**
     * Normalizes a media type to its canonical form, resolving aliases.
     *
     * @param type the type to normalize
     * @return the canonical media type
     */
    public static MediaType normalize(MediaType type) {
        return MEDIA_TYPE_REGISTRY.normalize(type);
    }

    /**
     * Gets the parent of a media type in the type hierarchy, skipping the
     * generic {@code application/octet-stream} ancestor. UFED types always
     * resolve to {@link #METADATA_ENTRY}.
     *
     * @param type the type whose parent is wanted
     * @return the parent media type
     */
    public static MediaType getParentType(MediaType type) {
        MediaType parent = MEDIA_TYPE_REGISTRY.getSupertype(type);
        while (MediaType.OCTET_STREAM.equals(parent)) {
            MediaType oldParent = parent;
            parent = MEDIA_TYPE_REGISTRY.getSupertype(parent);
            if (oldParent.equals(parent)) {
                break;
            }
        }
        if (type != null && type.toString().contains(UFED_MIME_PREFIX)) {
            parent = METADATA_ENTRY;
        }
        return parent;
    }

    /**
     * Checks whether a media type equals or specializes another.
     *
     * @param instance the candidate type
     * @param parent   the supertype to test against
     * @return {@code true} if {@code instance} is {@code parent} or one of its
     *         specializations
     */
    public static boolean isInstanceOf(MediaType instance, MediaType parent) {
        return MEDIA_TYPE_REGISTRY.isSpecializationOf(instance, parent);
    }

    /**
     * Checks whether a media type equals or specializes another, accepting the
     * candidate as any object whose string form is a media type.
     *
     * @param instance the candidate type, as a {@link MediaType} or any object
     *                 whose {@code toString()} is a media type string
     * @param parent   the supertype to test against
     * @return {@code true} if {@code instance} is {@code parent} or one of its
     *         specializations, {@code false} if {@code instance} is null
     */
    public static boolean isInstanceOf(Object instance, MediaType parent) {
        if (instance == null) {
            return false;
        }
        if (instance instanceof MediaType mediaType) {
            return isInstanceOf(mediaType, parent);
        }
        return isInstanceOf(MediaType.parse(instance.toString()), parent);
    }

    /**
     * @param type the type to test
     * @return {@code true} if the type is a metadata entry, not a real file
     */
    public static boolean isMetadataEntryType(MediaType type) {
        return isInstanceOf(type, METADATA_ENTRY);
    }

    /**
     * @param type the type to test, as any object whose string form is a media
     *             type
     * @return {@code true} if the type is a metadata entry, not a real file
     */
    public static boolean isMetadataEntryType(Object type) {
        return isInstanceOf(type, METADATA_ENTRY);
    }

    /**
     * Gets an item's media type as a string.
     *
     * @param item the item to read
     * @return the media type string, or {@code null} if not detected yet
     */
    public static String getMimeTypeString(IItemReader item) {
        Object mediaType = item.getMediaType();
        if (mediaType == null) {
            return null;
        }
        return mediaType.toString();
    }
}
