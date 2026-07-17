package iped.distributed.kafka;

import java.util.*;
import java.util.concurrent.ExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.errors.TopicExistsException;

/**
 * Creates and deletes Kafka topics for a distributed IPED processing case.
 *
 * <p>Topics follow the naming convention:
 *
 * <pre>
 *   iped.{caseId}.stage.{N}   — pipeline stages, N = 0 (raw) .. K (post-last-task)
 *   iped.status                — global status topic (not created here; must exist)
 * </pre>
 */
@Slf4j
public class TopicProvisioner implements AutoCloseable {

  /** Prefix used for all per-case pipeline topics. */
  public static final String TOPIC_PREFIX = "iped.";

  public static final String STAGE_INFIX = ".stage.";

  private final AdminClient admin;

  public TopicProvisioner(String bootstrapServers) {
    Properties props = new Properties();
    props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    this.admin = AdminClient.create(props);
  }

  /**
   * Computes the topic name for a given case and pipeline stage.
   *
   * @param caseId case identifier (alphanumeric + dashes/underscores)
   * @param stageNumber 0 = raw items from readers; N = after task at position N-1
   */
  public static String stageTopic(String caseId, int stageNumber) {
    return TOPIC_PREFIX + caseId + STAGE_INFIX + stageNumber;
  }

  /**
   * Creates all pipeline topics for a case.
   *
   * @param caseId case identifier
   * @param taskCount number of tasks in the pipeline (topics 0..taskCount are created)
   * @param partitions Kafka partition count per topic
   * @param replication replication factor
   */
  public void provisionCase(String caseId, int taskCount, int partitions, short replication) {
    List<NewTopic> topics = new ArrayList<>();
    for (int i = 0; i <= taskCount; i++) {
      String name = stageTopic(caseId, i);
      topics.add(new NewTopic(name, partitions, replication));
      log.info(
          "Provisioning topic '{}' (partitions={}, replication={})", name, partitions, replication);
    }
    // Also ensure the status topic exists
    topics.add(new NewTopic("iped.status", partitions, replication));

    CreateTopicsResult result = admin.createTopics(topics);
    result
        .values()
        .forEach(
            (name, future) -> {
              try {
                future.get();
                log.info("Topic '{}' created", name);
              } catch (ExecutionException e) {
                if (e.getCause() instanceof TopicExistsException) {
                  log.debug("Topic '{}' already exists — reusing", name);
                } else {
                  log.error("Failed to create topic '{}'", name, e.getCause());
                  throw new RuntimeException("Topic creation failed: " + name, e.getCause());
                }
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while creating topic " + name, e);
              }
            });
  }

  /** Deletes all pipeline topics for a case (called after case completion or abort). */
  public void deprovisionCase(String caseId, int taskCount) {
    List<String> names = new ArrayList<>();
    for (int i = 0; i <= taskCount; i++) {
      names.add(stageTopic(caseId, i));
    }
    try {
      admin.deleteTopics(names).all().get();
      log.info("Deleted {} pipeline topics for case '{}'", names.size(), caseId);
    } catch (Exception e) {
      log.warn("Could not fully delete topics for case '{}': {}", caseId, e.getMessage());
    }
  }

  /** Returns the set of existing stage topics for a case. */
  public Set<String> listCaseTopics(String caseId) throws Exception {
    Set<String> result = new HashSet<>();
    String prefix = TOPIC_PREFIX + caseId + STAGE_INFIX;
    admin.listTopics().names().get().stream()
        .filter(n -> n.startsWith(prefix))
        .forEach(result::add);
    return result;
  }

  @Override
  public void close() {
    admin.close();
  }
}
