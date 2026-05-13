## 1. Design concreto de tópicos Kafka

Convenção: `iped.<env>.<dominio>.<evento>.v1`

1. `iped.prod.ingest.item-created.v1`
- Entrada bruta do produtor de data source.

2. `iped.prod.classify.quick-analyzed.v1`
- Saída da análise rápida (MIME, tamanho, flags).

3. `iped.prod.route.task-planned.v1`
- Plano de execução por item (lista de tasks elegíveis + dependências).

4. `iped.prod.task.<taskName>.requested.v1`
- Um tópico por task ou por domínio de task.
- Exemplos:
- `iped.prod.task.hash.requested.v1`
- `iped.prod.task.signature.requested.v1`
- `iped.prod.task.parse.requested.v1`
- `iped.prod.task.index.requested.v1`
- `iped.prod.task.thumb-image.requested.v1`
- `iped.prod.task.ocr.requested.v1`
- `iped.prod.task.graph.requested.v1`

5. `iped.prod.task.<taskName>.completed.v1`
- Resultado da task (status + `data_refs`).

6. `iped.prod.task.<taskName>.failed.v1`
- Falha final após retries.

7. `iped.prod.task.<taskName>.dlq.v1`
- Dead letter queue por task.

8. `iped.prod.workflow.item-state.v1`
- Snapshot de progresso do item no DAG.

9. `iped.prod.reprocess.requested.v1`
- Reprocessamento por mudança de versão/config.

Particionamento recomendado:
- Tasks com contexto por caso: key `case_id`.
- Tasks stateless de alta escala: key `item_id`.
- Tópicos de resultado: key `case_id:item_id`.

---

## 2. Contratos de eventos com Protobuf (canônico)

Formato: **Protobuf** (`proto3`) com **Schema Registry** em modo Protobuf.

Regras de compatibilidade:
- Nunca reutilizar `field numbers` removidos (`reserved`).
- Evolução aditiva (novos campos opcionais).
- Versionar por pacote (`iped.events.v1`) e por tópico quando houver breaking change.
- Padronizar geração de clientes para Java, Python, Go e Rust.

### 2.1 Envelope base (`ItemEvent`) — exemplo de estrutura
```proto
syntax = "proto3";
package iped.events.v1;

message ItemEvent {
  string event_id = 1;
  string trace_id = 2;
  int64 timestamp_epoch_ms = 3;
  string env = 4;

  string case_id = 5;
  string item_id = 6;
  string parent_item_id = 7;
  repeated string lineage = 8;

  string producer = 9;
  string event_type = 10;

  TaskRef task = 11;
  Routing routing = 12;
  QuickFeatures quick_features = 13;
  DataRefs data_refs = 14;
  Status status = 15;
}

message TaskRef {
  string name = 1;
  string version = 2;
  int32 attempt = 3;
  int32 max_attempts = 4;
}

message Routing {
  string priority = 1;
  string partition_key = 2;
}

message QuickFeatures {
  string mime = 1;
  int64 size_bytes = 2;
  string ext = 3;
  double entropy = 4;
  repeated string flags = 5;
}

message DataRefs {
  string raw_ref = 1;
  string metadata_ref = 2;
  string text_ref = 3;
  string thumb_ref = 4;
  string vector_ref = 5;
}

message Status {
  string code = 1;
  string message = 2;
  string error_class = 3;
}
```

### 2.2 Evento de plano (`TaskPlanEvent`)
```proto
message TaskPlanEvent {
  string case_id = 1;
  string item_id = 2;
  repeated string eligible_tasks = 3;
  repeated TaskDependency dependencies = 4;
  repeated TaskSkipReason skip_reasons = 5;
}

message TaskDependency {
  string task = 1;
  repeated string depends_on = 2;
}

message TaskSkipReason {
  string task = 1;
  string reason = 2;
}
```

### 2.3 Evento de resultado (`TaskResultEvent`)
```proto
message TaskResultEvent {
  string case_id = 1;
  string item_id = 2;
  TaskRef task = 3;
  map<string, string> outputs = 4;
  DataRefs data_refs = 5;
  TaskMetrics metrics = 6;
  string status = 7;
}

message TaskMetrics {
  int64 duration_ms = 1;
  int64 cpu_ms = 2;
  int64 mem_peak_mb = 3;
}
```

---

## 3. Matriz de tasks mapeando exemplos do IPED

| Task IPED | Tipo | Exige contexto de caso? | Dependências principais | Entrada | Saída |
|---|---|---|---|---|---|
| `SignatureTask` / `SetTypeTask` | Stateless | Não | Nenhuma | `raw_ref` | MIME/assinatura em metadados |
| `HashTask` | Stateless | Não | Nenhuma | `raw_ref` | hashes em metadados |
| `HashDBLookupTask` | Stateless com cache externo | Não | `HashTask` | hashes | classificação known/unknown |
| `ParsingTask` | Stateful leve | Parcial | assinatura/tipo | `raw_ref` | texto, metadados, subitens |
| `BaseCarveTask` / `CarverTask` | Stateful | Sim | tipo/parse | `raw_ref` | novos subitens + artefatos |
| `IndexTask` | Stateful | Sim | parse, enriquecimentos | metadados + texto | documento indexado |
| `ImageThumbTask` / `DocThumbTask` / `VideoThumbTask` | Stateless | Não | assinatura/tipo | `raw_ref` | `thumb_ref` |
| `OCR` (via parse) | Stateless pesado | Não | imagem/pdf render | imagem/page refs | texto OCR |
| `LanguageDetectTask` | Stateless | Não | texto | texto | idioma/probabilidades |
| `EntropyTask` | Stateless | Não | nenhuma | `raw_ref` | taxa de compressão |
| `QRCodeTask` | Stateless | Não | assinatura imagem | `raw_ref` | conteúdo QR |
| `PhotoDNATask` / similaridade imagem | Stateless pesado | Não | imagem decodificada | `raw_ref` | `vector_ref`/features |
| `GraphTask` | Stateful | Sim | parse + entidades | metadados estruturados | nós/arestas no grafo |
| `ExportFileTask` | Stateful | Sim | parse/carve | artefatos | FS/SQLite/object storage |
| `ExportCSVTask` | Stateful | Sim | index/metadados | metadados | CSV consolidado |
| `SkipCommitedTask` | Stateful de workflow | Sim | estado anterior | estado/item | decisão de skip |

Regra prática:
- Stateless: escalar horizontalmente entre múltiplos casos.
- Stateful/contextual: particionar por `case_id` para preservar consistência local.

---

## 4. Estratégia polyglot (Java, Python, Go, Rust)

### 4.1 Contratos e SDKs
1. Criar repositório `iped-event-contracts` com `.proto` versionados.
2. Pipeline CI para gerar artefatos:
- Java (`.jar` com classes protobuf).
- Python (`wheel`).
- Go (módulo Go).
- Rust (`prost`/`tonic` crate).
3. Publicar artefatos em registry interno e travar versão por consumidor.

### 4.2 Bibliotecas Kafka por linguagem
- Java: `spring-kafka` ou cliente nativo.
- Python: `confluent-kafka`.
- Go: `franz-go` ou `confluent-kafka-go`.
- Rust: `rdkafka`.

### 4.3 Padrão de implementação
1. `Deserializer Protobuf` + validação de contrato.
2. Executor da task.
3. Publicador de `completed`/`failed`.
4. Idempotência por `(case_id, item_id, task_name, task_version)`.

---

## 5. Estratégia de deploy dos consumidores com contêineres

### 5.1 Modelo de runtime
1. Um deployment por task ou família de task.
2. Imagem de contêiner específica por dependência técnica e linguagem.
3. Configuração por `ConfigMap/Secret` e variáveis de ambiente.
4. Autoscaling por lag Kafka e uso de CPU/memória.

### 5.2 Perfis de imagem
1. `iped-consumer-jvm-base`
- Tasks Java puras (`HashTask`, `SignatureTask`, `LanguageDetectTask`).

2. `iped-consumer-python-ocr`
- `tesseract`, pacotes de idioma, render PDF.

3. `iped-consumer-go-media`
- `ffmpeg`, `ImageMagick`, libs nativas para thumbnail/vídeo.

4. `iped-consumer-rust-ml`
- Runtime para inferência/vetores (CPU ou GPU), foco em baixa latência.

5. `iped-consumer-jvm-graph`
- Cliente/driver de grafo e tuning de escrita em lote.

### 5.3 Deploy no Kubernetes
1. `Deployment` por consumidor com `consumerGroupId` próprio.
2. `HorizontalPodAutoscaler` baseado em:
- Kafka lag (`keda` recomendado).
- CPU/memória.
3. `Node pools` especializados:
- CPU geral para stateless leves.
- Memória alta para parse/index.
- GPU opcional para ML.
4. `Affinity`:
- Consumers stateful por `case_id` (mesma partição).
5. `Rolling update` com versionamento de schema e task (`task.version`).

### 5.4 Configuração e isolamento
1. Secrets por task para acesso a storage/index/vector DB.
2. Feature flags para habilitar/desabilitar tasks por caso.
3. Limites de recursos por contêiner para evitar noisy neighbor.
4. Sidecar opcional para observabilidade (OTel collector).

---

## 6. Fluxo operacional resumido

1. Ingestor publica `item-created`.
2. Quick classifier gera `quick-analyzed`.
3. Router publica `task-planned` e `task-requested`.
4. Consumers polyglot executam e publicam `task-completed`/`failed`.
5. Orchestrator observa dependências e dispara próximas tasks.
6. Index/export/graph finalizam e `item-state` marca concluído.

---

Próxima iteração recomendada:
1. Criar `iped-event-contracts` com `proto3` e CI de geração Java/Python/Go/Rust.
2. Definir 4 consumidores piloto (`hash`, `parse`, `thumb-image`, `index`) com uma linguagem alvo por serviço.
3. Subir ambiente de referência com Kafka + Schema Registry + KEDA + dashboards de lag/erros.
