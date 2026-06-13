package iped.distributed.dualrun;

/**
 * A single attribute discrepancy for an item that exists in both paths but whose
 * metadata does not agree.
 *
 * @param path               canonical item path (the matching key between both paths)
 * @param field              name of the mismatching attribute ({@code "mediaType"}, {@code "lengthBytes"})
 * @param distributedValue   value observed on the distributed side
 * @param referenceValue     value observed on the reference (monolithic) side
 */
public record AttributeMismatch(String path, String field, String distributedValue, String referenceValue) {}
