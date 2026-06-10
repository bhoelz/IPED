# Plano de Implementação: Processamento Distribuído IPED via Kafka

## 1. Princípios Norteadores

| Princípio | Como será respeitado |
|---|---|
| Tasks sem mudança de código | Task Agent usa a mesma abstração `Worker` + `AbstractTask` atual |
| Transparência de modo | `DatasourceRegistry` + `IItemFactory` já existentes resolvem o desacoplamento |
| Um agente por tipo por item | Consumer group do Kafka garante nativamente |
| Múltiplos tipos por item | Pipeline encadeado: item flui por todos os estágios |
| Sem state compartilhado mágico | Item carrega toda a sua state serializada entre estágios |

---

## 2. Visão Geral da Arquitetura

```
┌──────────────────────────────────────────────────────────────────┐
│  Produtores (múltiplas máquinas)                                  │
│  Reader A ──┐                                                     │
│  Reader B ──┼──► Kafka: iped.{caseId}.stage.0 (raw items)        │
│  Reader C ──┘                                                     │
└────────────────────────────────┬─────────────────────────────────┘
                                 │
              ┌──────────────────▼──────────────────┐
              │  Task Agent: TypeDetectionTask        │
              │  Consumer Group: caseId.TypeDetection │
              │  Input : stage.0                      │
              │  Output: stage.1                      │
              └──────────────────┬──────────────────-┘
                                 │
              ┌──────────────────▼──────────────────┐
              │  Task Agent: HashTask                 │
              │  Consumer Group: caseId.HashTask      │
              │  Input : stage.1                      │
              │  Output: stage.2                      │
              └──────────────────┬──────────────────-┘
                                 │
                                ...
                                 │
              ┌──────────────────▼──────────────────┐
              │  Task Agent: IndexTask                │
              │  Input : stage.N-1                    │
              │  Output: stage.N (final)              │
              └──────────────────┬──────────────────-┘
                                 │
                    ┌────────────▼────────────┐
                    │  iped.status (global)    │  ← erros, alertas,
                    └──────────────────────────┘    timeouts, conclusões

                    ┌──────────────────────────┐
                    │  Coordinator Server       │  ← agentes se registram,
                    │  (REST + WebSocket)       │    reportam disponibilidade
                    └──────────────────────────┘
```

**Regra central**: cada item é uma mensagem Kafka que carrega seu estado completo
(`metadata`, `extraAttributes`, flags, referências de stream). Ao transitar de um estágio
para o próximo, o agente serializa o item modificado pela task e publica no tópico seguinte.
**Nenhuma task sabe disso.**

---

## 3. Novo Módulo Maven: `iped-distributed`

```
iped-engine-parent/iped-distributed/
  pom.xml
  src/main/java/iped/distributed/
    kafka/
      KafkaItemMessage.java           ← DTO serializável
      KafkaItemSerializer.java
      KafkaItemDeserializer.java
      KafkaItemProducer.java          ← impl IDatasourceRegistry para produtores
      KafkaItemConsumer.java          ← Kafka consumer loop
    agent/
      TaskAgent.java                  ← wrapper transparente da task
      TaskAgentConfig.java
      TaskAgentLauncher.java          ← entry point CLI do agente
      InputStreamFactoryRegistry.java ← reconstitui factories por nome de classe
    coordinator/
      CoordinatorServer.java
      AgentRegistration.java
      AgentRegistry.java
      CaseLifecycleManager.java
      TopicProvisioner.java           ← cria tópicos Kafka por caso
    status/
      ItemStatusEvent.java
      ItemStatusProducer.java
      ItemStatusConsumer.java
    config/
      DistributedConfig.java
```

**`pom.xml` de `iped-distributed`:**
```xml
<dependencies>
  <dependency>iped:iped-engine-core</dependency>
  <dependency>iped:iped-api</dependency>
  <dependency>org.apache.kafka:kafka-clients:3.7.x</dependency>
  <dependency>com.fasterxml.jackson.core:jackson-databind</dependency>
  <dependency>org.eclipse.jetty:jetty-server</dependency>   <!-- coordinator -->
</dependencies>
```

---

## 4. Serialização de Itens — `KafkaItemMessage`

O desafio central é que `IItem` contém:
- Metadados (`Tika Metadata`) — serializáveis como `Map<String, List<String>>`
- Extra attributes — serializáveis como `Map<String, Object>`
- Flags booleanas — triviais
- **`InputStreamFactory`** — referência ao conteúdo binário, **não serializável diretamente**

**Solução para o conteúdo**: datasources ficam em armazenamento compartilhado
(NFS / MinIO / S3). A mensagem carrega a *referência* para reconstituir a factory,
não o conteúdo em si.

```java
public class KafkaItemMessage {
    // Identidade
    String caseId;
    String itemUuid;          // UUID gerado na entrada no pipeline distribuído
    int    localItemId;       // ID original no caso local
    String dataSourceUuid;

    // Localização no pipeline
    int  pipelineStage;       // estágio atual (0 = raw)

    // Propriedades do item (espelham IItem)
    String path;
    String name;
    String extension;
    Long   length;
    Long   fileOffset;
    boolean isDir, isDeleted, isCarved, isSubItem, isRoot, hasChildren;
    Integer subitemId;
    String  parentItemUuid;
    List<String> parentItemUuids;

    // Datas
    Date accessDate, creationDate, modificationDate, changeDate;

    // Estado de análise
    String hash;
    String mediaType;
    Map<String, List<String>>  metadata;       // Tika Metadata
    Map<String, Object>        extraAttributes;

    // Acesso ao conteúdo (factory reconstituível)
    String              inputStreamFactoryClass;    // FQCN
    Map<String, String> inputStreamFactoryParams;   // ex: {dbPath, objectId}

    // Caminho de saída do caso (para tarefas que escrevem arquivos)
    String caseOutputPath;
    String sharedStorageRoot;
}
```

**Regra de armazenamento compartilhado**: qualquer caminho contido em
`inputStreamFactoryParams` deve ser resolvível a partir de `sharedStorageRoot`
em qualquer nó da rede. O operador é responsável por montar NFS / configurar S3
de forma uniforme em todos os workers.

### `InputStreamFactoryRegistry`

```java
// Cada nó registra as factories que sabe reconstituir
public class InputStreamFactoryRegistry {
    private static final Map<String, Function<Map<String,String>, ISeekableInputStreamFactory>>
        builders = new ConcurrentHashMap<>();

    public static void register(String fqcn,
        Function<Map<String,String>, ISeekableInputStreamFactory> builder) {
        builders.put(fqcn, builder);
    }

    public static ISeekableInputStreamFactory reconstituir(String fqcn,
        Map<String,String> params) {
        return builders.get(fqcn).apply(params);
    }
}
```

No startup de cada agente:
```java
InputStreamFactoryRegistry.register(
    SleuthkitInputStreamFactory.class.getName(),
    params -> new SleuthkitInputStreamFactory(
        Paths.get(params.get("dbPath")),
        Long.parseLong(params.get("objectId"))
    )
);
```

---

## 5. Design dos Tópicos Kafka

### Nomenclatura
```
iped.{caseId}.stage.{N}     N = 0..K  (K = número de tasks no pipeline)
iped.status                  tópico global de status (todos os casos)
iped.agents.heartbeat        tópico de heartbeat dos agentes
```

### Criação dinâmica de tópicos — `TopicProvisioner`

Quando um caso é iniciado via `CaseLifecycleManager.startCase(caseId, taskList)`:

```java
public void provisionTopics(String caseId, List<String> orderedTaskNames,
                             int partitions, short replication) {
    // Cria stage.0 até stage.N
    for (int i = 0; i <= orderedTaskNames.size(); i++) {
        String topic = "iped." + caseId + ".stage." + i;
        adminClient.createTopic(topic, partitions, replication);
    }
    // Armazena mapeamento taskName → stage number no coordinator
    caseRegistry.storeStageMap(caseId, orderedTaskNames);
}
```

### Particionamento
- **Chave de partição**: `itemUuid` (garante que o mesmo item sempre vai para a mesma
  partição, preservando ordering relativo dentro do caso)
- **Partições por tópico**: configurável; padrão = número de workers esperados × 2

---

## 6. Adaptador de Produção: `KafkaItemProducer`

Implementa `IDatasourceRegistry` (já existente em `iped-engine-core`).
**Zero mudança nos readers.**

```java
public class KafkaItemProducer implements IDatasourceRegistry {

    private final Producer<String, KafkaItemMessage> kafkaProducer;
    private final String rawTopic;          // iped.{caseId}.stage.0
    private final ItemStatusProducer status;
    private final LocalManager localDelegate; // para createItem, getTrackID, etc.

    @Override
    public IItem createItem() {
        return localDelegate.createItem();   // cria Item local normalmente
    }

    @Override
    public void addItem(IItem item) throws InterruptedException {
        KafkaItemMessage msg = toMessage(item);
        kafkaProducer.send(new ProducerRecord<>(rawTopic, msg.getItemUuid(), msg));
        status.publishDiscovered(msg);
    }

    @Override
    public void addItemFirst(IItem item) throws InterruptedException {
        // Sub-itens urgentes: ainda vão para stage.0 mas com flag de prioridade
        KafkaItemMessage msg = toMessage(item);
        msg.setPriority(true);
        kafkaProducer.send(new ProducerRecord<>(rawTopic, msg.getItemUuid(), msg));
    }

    @Override
    public void prepareForQueue(IItem item, ICaseData caseData) {
        localDelegate.prepareForQueue(item, caseData);
    }

    @Override
    public String getTrackID(IItem item)       { return localDelegate.getTrackID(item); }
    @Override
    public void setEnvVar(String k, String v)  { localDelegate.setEnvVar(k, v); }
    @Override
    public void onProgress(String p, Object o, Object n) {
        status.publishProgress(p, o, n);
    }

    private KafkaItemMessage toMessage(IItem item) { /* ... serialização ... */ }
}
```

**Ativação no `Manager`** (única mudança em código existente):

```java
// Manager.java — no construtor, após inicializar tudo:
DistributedConfig dist = ConfigurationManager.get().findObject(DistributedConfig.class);
if (dist.isEnabled()) {
    KafkaItemProducer kafkaProd = new KafkaItemProducer(dist, this /* localDelegate */);
    DatasourceRegistry.set(kafkaProd);
} else {
    DatasourceRegistry.set(this);   // comportamento atual, sem mudança
}
```

---

## 7. Task Agent — `TaskAgent`

É o componente mais importante. Envolve a task existente **sem alterar uma linha dela**.

### Fluxo de execução do agente

```
Kafka (inputTopic)
    │
    ▼ poll()
KafkaItemMessage
    │
    ▼ deserializar
IItem (objeto local, com InputStreamFactory reconstituída)
    │
    ▼ colocar em ProcessingQueues local (fila interna do agente)
Worker.process(item)          ← task.process(item) chama código INALTERADO
    │
    ▼ task modifica item (setHash, setMediaType, setExtraAttribute, etc.)
    │
    ▼ serializar item modificado
KafkaItemMessage (atualizado)
    │
    ▼ produce()
Kafka (outputTopic)
```

### Classe `TaskAgent`

```java
public class TaskAgent {

    private final AbstractTask         task;
    private final String               inputTopic;
    private final String               outputTopic;
    private final KafkaConsumer<...>   consumer;
    private final KafkaProducer<...>   producer;
    private final ProcessingQueues     localQueue;    // fila interna local
    private final Worker[]             workers;       // Workers locais que executam a task
    private final ItemStatusProducer   status;
    private final CoordinatorClient    coordinator;
    private final int                  parallelism;   // máx. itens simultâneos

    public void start() {
        // Registra no coordinator
        coordinator.register(AgentRegistration.of(
            UUID.randomUUID().toString(),
            task.getClass().getSimpleName(),
            parallelism
        ));

        // Inicia workers locais (cada Worker roda a task em uma thread)
        for (int i = 0; i < parallelism; i++) {
            workers[i] = new Worker(localQueue, List.of(task), statusListener);
            workers[i].start();
        }

        // Loop principal de consumo Kafka
        consumer.subscribe(List.of(inputTopic));
        while (running) {
            var records = consumer.poll(Duration.ofMillis(500));
            for (var record : records) {
                submitForProcessing(record.value());
            }
            coordinator.heartbeat(currentLoad());
        }
    }

    private void submitForProcessing(KafkaItemMessage msg) {
        status.publishStarted(msg, task.getClass().getSimpleName());

        // Reconstitui IItem a partir da mensagem Kafka
        IItem item = fromMessage(msg);

        // Intercepta a finalização do item para publicar no próximo estágio
        item.setTempAttribute("__kafkaOutputTopic", outputTopic);
        item.setTempAttribute("__originalMessage",  msg);

        // Coloca na fila local — Worker executa task.process(item) normalmente
        localQueue.addItem(item);
        // [continua no onItemFinalized()]
    }

    // Chamado pelo Worker ao finalizar o processamento de um item
    // (hook via WorkerListener / statistics já existente)
    private void onItemFinalized(IItem item, boolean success, Exception error) {
        String outTopic = (String) item.getTempAttribute("__kafkaOutputTopic");
        KafkaItemMessage orig = (KafkaItemMessage) item.getTempAttribute("__originalMessage");

        if (success) {
            KafkaItemMessage updated = mergeState(orig, item);
            producer.send(new ProducerRecord<>(outTopic, updated.getItemUuid(), updated));
            status.publishCompleted(orig, task.getClass().getSimpleName());
        } else {
            status.publishError(orig, task.getClass().getSimpleName(), error);
            handleFailure(orig, error);  // DLQ ou retry
        }

        coordinator.releaseSlot();
    }
}
```

### Por que o código das tasks não muda

A task chama `item.setHash("abc")`, `item.setMediaType(...)`, `item.getInputStream()` etc.
O `IItem` é um objeto local normal. Quando a task termina, o agente serializa o estado
atualizado para Kafka. **A task não sabe que existe Kafka.**

### Escala horizontal

Se houver 4 instâncias de `HashTask Agent` na mesma consumer group `{caseId}.HashTask`,
o Kafka distribui as partições entre elas automaticamente. Cada item vai para exatamente
**uma** instância. ✓

---

## 8. Sub-itens Gerados por Tasks

Quando uma task (ex: `ExplodeZipTask`, `EmbeddedDiskProcessTask`) gera um sub-item,
ela chama `DatasourceRegistry.get().addItem(subItem)`. No contexto do agente,
esse registry está configurado para publicar o sub-item **de volta ao `stage.0`**,
para que o sub-item passe por todas as tasks desde o início.

```java
// No TaskAgent, ao configurar o DatasourceRegistry para execução local:
DatasourceRegistry.set(new IDatasourceRegistry() {
    @Override
    public void addItem(IItem subItem) throws InterruptedException {
        KafkaItemMessage subMsg = toMessage(subItem);
        subMsg.setParentItemUuid(currentItemUuid);
        // Sub-item volta ao início do pipeline
        producer.send(new ProducerRecord<>(rawTopic_stage0, subMsg.getItemUuid(), subMsg));
        status.publishSubitemDiscovered(subMsg);
    }
    // ... outros métodos delegam para o agente
});
```

---

## 9. Servidor Coordenador — `CoordinatorServer`

Servidor Jetty/Jersey leve (mesma stack já usada em `iped-engine-core`).

### API REST

```
POST   /api/v1/agents/register           → AgentRegistration    → 200 OK
POST   /api/v1/agents/{id}/heartbeat     → HeartbeatPayload     → 200 OK
DELETE /api/v1/agents/{id}               →                      → 204 No Content

GET    /api/v1/agents/availability       → Map<taskType, AgentAvailability>
GET    /api/v1/agents                    → List<AgentRegistration>

POST   /api/v1/cases/start               → CaseStartRequest     → CaseStartResponse
GET    /api/v1/cases/{caseId}            → CaseStatus
GET    /api/v1/cases/{caseId}/items/{id} → ItemProcessingStatus

GET    /api/v1/pipeline/{caseId}         → Map<taskName, stageNumber>
```

### Estruturas de dados

```java
public class AgentRegistration {
    String  agentId;
    String  hostname;
    int     port;
    String  taskType;          // ex: "HashTask"
    int     stageNumber;       // stage que este agente consome/produz
    int     maxParallelItems;
    int     currentLoad;
    Instant lastHeartbeat;
}

public class AgentAvailability {
    String taskType;
    int    totalAgents;
    int    totalSlots;
    int    freeSlots;
    int    busySlots;
}

public class CaseStartRequest {
    String       caseId;
    String       caseOutputPath;
    List<String> orderedTaskNames;    // ordem das tasks = ordem dos estágios
    int          topicPartitions;
    short        replicationFactor;
}
```

### `AgentRegistry`

- Expira agentes sem heartbeat há > 30 s
- Publica mudanças de disponibilidade via WebSocket (para dashboard)
- Estado mantido em memória (agentes são efêmeros; sem banco necessário)

---

## 10. Rastreamento de Status — Tópico `iped.status`

### Schema `ItemStatusEvent`

```java
public class ItemStatusEvent {
    enum Type { DISCOVERED, STARTED, COMPLETED, SKIPPED, ERROR, TIMEOUT, SUBITEM }

    Type    type;
    String  caseId;
    String  itemUuid;
    String  itemPath;
    String  taskType;      // null para DISCOVERED
    int     pipelineStage;
    Instant timestamp;
    long    durationMs;    // para COMPLETED / ERROR
    String  errorMessage;  // para ERROR
    String  errorClass;
    Map<String, Object> details;
}
```

### Detecção de timeout

Consumer separado lê `iped.status` e rastreia itens com `STARTED` sem `COMPLETED`
há mais de N minutos. Publica evento `TIMEOUT` e pode acionar reprocessamento via
reenvio para o tópico de entrada do agente responsável.

### Conclusão do caso

O monitor rastreia:
1. Total de itens descobertos (eventos `DISCOVERED`)
2. Total de itens que chegaram ao `stage.K` final

Quando `completados == descobertos − sub-itens-em-progresso`, o caso está concluído.

---

## 11. `DistributedConfig` e Toggle de Modo

```java
// DistributedConfig.java — em iped-distributed/config/
// Lida pelo ConfigurationManager existente via arquivo de propriedades

public class DistributedConfig extends AbstractPropertiesConfigurable {

    boolean enabled                = false;
    String  kafkaBootstrapServers  = "localhost:9092";
    String  coordinatorServerUrl   = "http://coordinator:8484";
    String  sharedStorageRoot      = "/mnt/iped-shared";
    int     topicPartitions        = 8;
    short   topicReplication       = 1;
    int     agentParallelism       = 4;       // itens simultâneos por agente
    long    itemTimeoutSeconds     = 3600;    // timeout por item/task
    String  deadLetterTopicSuffix  = ".dlq";
    boolean exactlyOnce            = false;   // Kafka transactions (mais lento)
}
```

**`DistributedConfig.toml`** (arquivo de configuração do usuário):
```properties
# Habilitar modo distribuído
enableDistributed       = true
kafkaBootstrapServers   = broker1:9092,broker2:9092
coordinatorServerUrl    = http://coordinator-host:8484
sharedStorageRoot       = /mnt/evidence-nas
topicPartitions         = 16
agentParallelism        = 8
```

---

## 12. Entry Point do Agente — `TaskAgentLauncher`

Processo independente iniciado em cada worker, **sem `Manager` completo**:

```
java -cp iped-distributed.jar TaskAgentLauncher \
     --taskType   HashTask              \
     --caseId     abc123                \
     --kafka      broker1:9092          \
     --coordinator http://coord:8484    \
     --parallelism 8                    \
     --config     /etc/iped/iped.config
```

```java
public class TaskAgentLauncher {
    public static void main(String[] args) {
        var cfg = parseArgs(args);

        // Carrega config do IPED (para tasks que precisam de FileSystemConfig, etc.)
        ConfigurationManager.loadFrom(cfg.configPath);

        // Obtém stage number a partir do coordinator
        int stage = CoordinatorClient.getStageForTask(
            cfg.coordinatorUrl, cfg.caseId, cfg.taskType);

        // Instancia a task via reflection — zero mudança na task
        AbstractTask task = (AbstractTask) Class.forName(cfg.taskClass).newInstance();
        task.init(ConfigurationManager.get());

        // Registra InputStreamFactories conhecidas neste nó
        InputStreamFactoryRegistry.registerDefaults(cfg.sharedStorageRoot);

        // Inicia o agente
        new TaskAgent(task, cfg, stage).start();
    }
}
```

---

## 13. Mudanças no Código Existente (mínimas)

| Arquivo | Mudança | Linhas aprox. |
|---|---|---|
| `Manager.java` | Toggle `DatasourceRegistry` conforme `DistributedConfig` | ~15 |
| `Worker.java` | Adicionar hook `onItemFinalized(IItem, boolean, Exception)` via listener | ~10 |
| `iped-engine-parent/pom.xml` | Adicionar `iped-distributed` como módulo | 1 |
| `DistributedConfig.toml` | Novo arquivo de configuração (não é código) | — |

**Zero mudanças em tasks, readers, ou qualquer outro arquivo existente.**

---

## 14. Topologia de Implantação (exemplo)

```
┌─────────────┐   ┌─────────────┐
│ Reader #1    │   │ Reader #2    │   ← múltiplas máquinas lendo datasources
│ SleuthkitRdr │   │ UfedXmlRdr  │
│ + KafkaProd  │   │ + KafkaProd  │
└──────┬──────┘   └──────┬──────┘
       └────────┬─────────┘
                ▼
     ┌─────────────────────┐
     │     Kafka Cluster    │
     │  iped.case1.stage.0  │  16 partições
     │  iped.case1.stage.1  │
     │        ...           │
     │  iped.case1.stage.N  │
     │  iped.status         │
     └──────────┬──────────┘
                │
   ┌────────────┼────────────┐
   ▼            ▼            ▼
┌──────┐   ┌──────┐    ┌──────┐
│TypeD.│   │TypeD.│    │TypeD.│   ← 3 instâncias, consumer group comum
│ #1   │   │ #2   │    │ #3   │     stage 0 → 1
└──────┘   └──────┘    └──────┘
   ▼
┌──────┐   ┌──────┐
│Hash  │   │Hash  │               ← 2 instâncias, stage 1 → 2
│ #1   │   │ #2   │
└──────┘   └──────┘
   ▼
┌──────┐
│OCR   │                          ← 1 instância com GPU, stage 2 → 3
│ #1   │
└──────┘
   ▼
┌──────┐
│Index │                          ← 1 instância (Lucene não é thread-safe
│ #1   │                            para escritas concorrentes)
└──────┘

                    ┌──────────────────────────┐
                    │   Coordinator Server      │
                    │   :8484                   │
                    │   - Registry de agentes   │
                    │   - Status do caso        │
                    │   - Criação de tópicos    │
                    └──────────────────────────┘
```

---

## 15. Fases de Implementação

### Fase 1 — Serialização e Infraestrutura Kafka (3 semanas)
- `KafkaItemMessage` + serialização Jackson completa
- `KafkaItemProducer` implementando `IDatasourceRegistry`
- `TopicProvisioner` para criar tópicos por caso
- `DistributedConfig` + leitura via `ConfigurationManager`
- Toggle em `Manager` (modo local vs distribuído)
- **Entrega**: reader local → Kafka → consumidor de validação (dump de mensagens)

### Fase 2 — Task Agent (4 semanas)
- `TaskAgent` completo com loop Kafka + `ProcessingQueues` local
- `InputStreamFactoryRegistry` + registro dos tipos existentes (`Sleuthkit`, `AD1`, `UFDR`)
- `TaskAgentLauncher` (CLI)
- Hook `onItemFinalized` em `Worker`
- Roteamento de sub-itens de volta ao `stage.0`
- **Entrega**: pipeline de 3 tasks end-to-end em modo distribuído com case real

### Fase 3 — Coordenador e Status (2 semanas)
- `CoordinatorServer` com API REST completa
- `AgentRegistry` com expiração por heartbeat
- `CaseLifecycleManager` (start / stop / status de caso)
- `ItemStatusProducer` + `ItemStatusConsumer`
- Detecção de timeout e dead letter queue (DLQ)
- **Entrega**: dashboard CLI de progresso lendo `iped.status`

### Fase 4 — Hardening (2 semanas)
- Retry com exponential backoff para itens em DLQ
- Exactly-once opcional via Kafka transactions
- Consumer de status para dashboard web simples
- Detecção automática de conclusão de caso
- **Entrega**: testes de resiliência (agente caindo durante processamento)

### Fase 5 — Integração e Deploy (1 semana)
- Docker Compose de referência (Kafka + Coordinator + N workers)
- Helm chart para Kubernetes (opcional)
- Documentação de operação e troubleshooting
- Testes de integração com datasources reais (E01, UFDR, AD1)

---

## 16. Pontos de Atenção Críticos

**Armazenamento compartilhado obrigatório**: Todos os datasources (E01, AD1, UFDR)
devem estar montados via NFS / S3 com o **mesmo caminho absoluto** em todos os nós.
O operador é responsável por isso. O IPED não gerencia replicação de evidências.

**Consistência do índice Lucene**: No modo distribuído, o `IndexTask` de múltiplos
agentes não pode escrever no mesmo índice Lucene simultaneamente. Solução: um único
agente de indexação por caso (paralelismo = 1 para `IndexTask`), ou uso de índice
distribuído (Elasticsearch) como evolução futura.

**TrackID determinístico**: O `trackID` calculado via `Util.calctrackIDAndUpdateID`
usa `idInDataSource` + `parentTrackID`, ambos preservados na serialização. O resultado
será idêntico em modo local e distribuído, garantindo reconhecimento de itens em
reprocessamentos (`--continue`).

**Ordering de sub-itens**: Sub-itens gerados em `stage.N` voltam ao `stage.0`.
Isso é correto — o sub-item passa pelo pipeline completo, exatamente como no modo local.

**Rollback / Reprocessamento**: Se um agente falha permanentemente (DLQ esgotado),
o Coordinator expõe:
```
POST /api/v1/cases/{caseId}/items/{itemId}/retry?fromStage=N
```
para reinjetar o item em qualquer estágio do pipeline.
