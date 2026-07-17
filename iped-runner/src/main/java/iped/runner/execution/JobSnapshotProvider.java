package iped.runner.execution;

import java.util.List;

/**
 * Supplies additional {@link JobSnapshot}s to the Processing Dashboard beyond the locally launched
 * IPED processes — e.g. distributed cases observed on Kafka.
 */
public interface JobSnapshotProvider {

  List<JobSnapshot> snapshots();
}
