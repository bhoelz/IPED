# Plano: Processamento Adicional em Itens Selecionados

## Contexto arquitetural relevante

- O índice Lucene principal é escrito durante o pipeline de tarefas (`IndexTask`) e é somente-leitura após o processamento.
- `IItem` carrega metadados (`Metadata` Tika) e atributos extras (`Map<String,Object> extraAttributeMap`).
- `IPEDSource.getItemByID()` → `IndexItem.getItem()` reconstrói o item a partir do documento Lucene — é o ponto de entrada para toda visualização.
- Tasks operam sobre `IItem` via `AbstractTask.process(IItem)`, modificam o item em memória, e o `IndexTask` (fim do pipeline) persiste as mudanças no Lucene.
- `MultiSearchResult` + `RoaringBitmap` são o modelo de seleção de itens na GUI.

---

## Fase 1 — Interface e modelo de dados do armazenamento adicional

**Objetivo:** Definir os contratos públicos antes de qualquer implementação.

### 1.1 — Adicionar interface `IAdditionalDataSource` em `iped-api`

```java
// iped-api/.../iped/datasource/IAdditionalDataSource.java
public interface IAdditionalDataSource {
    void storeTaskResult(String caseUUID, int itemId, String taskName,
                         Metadata metadata, Map<String, Object> extraAttrs) throws IOException;

    Optional<AdditionalItemData> getTaskResult(String caseUUID, int itemId, String taskName);

    boolean hasTaskResult(String caseUUID, int itemId, String taskName);

    Set<String> getExecutedTasks(String caseUUID, int itemId);

    void commit() throws IOException;
    void close() throws IOException;
}
```

### 1.2 — Adicionar DTO `AdditionalItemData` em `iped-api`

```java
// Dados retornados de uma fonte adicional para um item
public class AdditionalItemData {
    private final String taskName;
    private final Metadata metadata;           // campos Tika adicionais
    private final Map<String, Object> extraAttributes;
    private final Instant processedAt;
}
```

### 1.3 — Adicionar interface `IAdditionalDataSourceManager` em `iped-api`

```java
// Gerencia múltiplas fontes adicionais registradas
public interface IAdditionalDataSourceManager {
    void register(IAdditionalDataSource source);
    List<IAdditionalDataSource> getSources();
    // Retorna dados consolidados de todas as fontes para um item
    List<AdditionalItemData> getAllResults(String caseUUID, int itemId);
}
```

---

## Fase 2 — Implementação do armazenamento em Lucene secundário

**Objetivo:** Persistência concreta dos resultados adicionais em um índice Lucene separado, co-localizado com o caso.

### 2.1 — Criar módulo Maven `iped-additional-index` dentro de `iped-engine-parent`

Dependências: `iped-api`, `lucene-core`, `tika-core`.

### 2.2 — Implementar `LuceneAdditionalDataSource`

- Índice em `<output>/.additional-index/` (criado on-demand).
- Schema do documento Lucene:

| Campo | Tipo Lucene | Observação |
|---|---|---|
| `_itemId` | `IntPoint` + `StoredField` | Chave de busca |
| `_caseUUID` | `StringField` | Suporte multi-caso |
| `_taskName` | `StringField` | Nome da task executada |
| `_processedAt` | `LongPoint` | Timestamp epoch ms |
| `<metadata fields>` | dinâmico, igual ao índice principal | Reutiliza `IndexItem` helpers |

- Usa `IndexWriter` com `StandardAnalyzer` e `OpenMode.CREATE_OR_APPEND`.
- `storeTaskResult()` usa `updateDocument()` com term `_itemId+_caseUUID+_taskName` (upsert — re-executar a task sobrescreve).
- `getTaskResult()` usa `IndexSearcher` com query `+_itemId:<id> +_caseUUID:<uuid> +_taskName:<task>`.

### 2.3 — Implementar `DefaultAdditionalDataSourceManager`

- Singleton por caso, thread-safe.
- Carregado no bootstrap do caso (tanto no modo processamento quanto no modo viewer).
- Registra o `LuceneAdditionalDataSource` automaticamente se o diretório `.additional-index/` existir.

---

## Fase 3 — Integração na leitura de itens

**Objetivo:** Enriquecer transparentemente todo item retornado ao usuário com dados das fontes adicionais.

### 3.1 — Criar `EnrichedItemReader` (decorator de `IItemReader`)

```java
public class EnrichedItemReader implements IItemReader {
    private final IItemReader delegate;          // item do índice principal
    private final List<AdditionalItemData> additionalData;

    @Override
    public Metadata getMetadata() {
        Metadata merged = new Metadata();
        // copia metadados do delegate
        // depois sobrescreve/adiciona campos de cada AdditionalItemData
        return merged;
    }

    @Override
    public Map<String, Object> getExtraAttributeMap() {
        Map<String, Object> merged = new LinkedHashMap<>(delegate.getExtraAttributeMap());
        additionalData.forEach(d -> merged.putAll(d.getExtraAttributes()));
        return merged;
    }
    // demais métodos delegam para `delegate`
}
```

### 3.2 — Modificar `IPEDSource.getItemByLuceneID()`

Após reconstruir o item via `IndexItem.getItem()`, verificar o manager:

```java
IItemReader item = IndexItem.getItem(doc, this, false);
List<AdditionalItemData> extra = additionalDataSourceManager.getAllResults(caseUUID, item.getId());
if (!extra.isEmpty()) {
    item = new EnrichedItemReader(item, extra);
}
return item;
```

Ponto único de mudança — toda a GUI e todos os viewers se beneficiam sem alterações adicionais.

---

## Fase 4 — Runner de tarefas adicionais

**Objetivo:** Executar tasks existentes em itens já indexados e persistir os resultados na fonte adicional.

### 4.1 — Criar `AdditionalTaskWorker`

Replica apenas o mínimo do `Worker` necessário para executar uma task:
- Recebe `IPEDSource` (para ler streams dos itens via `ISeekableInputStreamFactory`).
- Recebe `IAdditionalDataSource` como destino de escrita.
- **Não** possui `IndexWriter` — evita escrita acidental no índice principal.
- Implementa um `IndexWriter` stub que lança exceção se acionado (fail-fast para tasks que tentem criar novos itens filhos — fora do escopo desta fase).

### 4.2 — Criar `AdditionalIndexTask` (task terminal do pipeline adicional)

Substitui o `IndexTask` no pipeline adicional:

```java
@Override
public void process(IItem item) throws Exception {
    additionalDataSource.storeTaskResult(
        caseUUID, item.getId(), currentTaskName,
        item.getMetadata(), item.getExtraAttributeMap()
    );
}
```

### 4.3 — Criar `AdditionalTaskRunner`

```java
public class AdditionalTaskRunner {
    public CompletableFuture<AdditionalTaskProgress> run(
            Collection<IItemId> selectedItems,
            Class<? extends AbstractTask> taskClass,
            IPEDSource source,
            IAdditionalDataSource destination,
            Consumer<AdditionalTaskProgress> progressCallback)
```

- Cria thread pool configurável (padrão: `Runtime.availableProcessors() / 2`).
- Para cada item:
  1. Reconstrói `IItem` via `source.getItemByID()`.
  2. Instancia a task via reflexão e chama `init()`.
  3. Constrói pipeline mínimo: `[taskClass] → AdditionalIndexTask`.
  4. Chama `task.processAndSendToNextTask(item)`.
  5. Reporta progresso via callback.
- Commit do índice adicional ao fim (ou a cada N itens para durabilidade).
- Suporta cancelamento via `Future.cancel()`.

### 4.4 — Metadados de tarefa executável

Criar anotação `@AdditionalProcessingCapable` para marcar tasks que suportam re-execução pós-indexação:

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AdditionalProcessingCapable {
    String displayName();
    String description();
    boolean requiresTextContent() default false; // OCR requer stream do arquivo
    boolean requiresParsedContent() default false; // transcrição requer áudio parseado
}
```

Anotar tasks elegíveis existentes: `OCRTask`, `NamedEntityTask`, `FaceRecognitionTask`, etc.

---

## Fase 5 — Interface gráfica

**Objetivo:** Expor o recurso ao usuário de forma coerente com o fluxo de trabalho atual.

### 5.1 — Botão/menu contextual "Processar selecionados"

- Aparece no menu de contexto da tabela de resultados e na barra de ferramentas.
- Habilitado apenas quando há itens selecionados (checkbox) e ao menos uma task `@AdditionalProcessingCapable` disponível.

### 5.2 — Diálogo `AdditionalProcessingDialog`

- Lista as tasks elegíveis com nome, descrição e estimativa de tempo.
- Checkbox para pular itens que já tiveram aquela task executada.
- Campo de configuração de threads.
- Botão "Iniciar".

### 5.3 — Painel de progresso `AdditionalTaskProgressPanel`

- Barra de progresso por task + contador `N/Total`.
- Log de erros por item (não interrompe o processamento dos demais).
- Botão "Cancelar" que aciona `Future.cancel(true)`.
- Ao concluir: botão "Recarregar resultados" que re-executa a query atual para atualizar metadados na tabela.

### 5.4 — Indicador visual na tabela de resultados

- Coluna opcional (toggleável) "Processamento adicional" com ícone de check quando o item possui dados adicionais.
- Tooltip mostra quais tasks foram executadas e quando.

### 5.5 — Filtro por task executada

- Novo `IResultSetFilterer` que filtra itens com/sem determinada task adicional executada.
- Integrado ao `FilterManager` existente.

---

## Fase 6 — Bootstrap e persistência entre sessões

### 6.1 — Abertura do viewer

No bootstrap do `IPEDSource` (ou `IPEDMultiSource` para multi-caso):

```java
Path additionalIndexPath = output.toPath().resolve(".additional-index");
if (Files.exists(additionalIndexPath)) {
    IAdditionalDataSource ads = new LuceneAdditionalDataSource(additionalIndexPath);
    additionalDataSourceManager.register(ads);
}
```

### 6.2 — Múltiplos casos (multi-source)

`IPEDMultiSource` já mantém um `IPEDSource` por caso. Cada `IPEDSource` terá seu próprio `additionalDataSourceManager` apontando para o `.additional-index/` local de cada caso. O `EnrichedItemReader` usa o `caseUUID` correto para não misturar resultados.

### 6.3 — Migração / backward compatibility

- A ausência do diretório `.additional-index/` é silenciosa — o comportamento é idêntico ao atual.
- Nenhuma mudança no formato do índice Lucene principal.

---

## Ordem de implementação

| # | Entregável | Módulo | Depende de |
|---|---|---|---|
| 1 | Interfaces + DTOs (`IAdditionalDataSource`, `AdditionalItemData`) | `iped-api` | — |
| 2 | `LuceneAdditionalDataSource` + testes unitários | `iped-additional-index` (novo) | 1 |
| 3 | `EnrichedItemReader` | `iped-additional-index` | 1 |
| 4 | Integração em `IPEDSource.getItemByLuceneID()` | `iped-engine` | 2, 3 |
| 5 | `AdditionalTaskWorker` + `AdditionalIndexTask` | `iped-engine` | 2 |
| 6 | `AdditionalTaskRunner` | `iped-engine` | 5 |
| 7 | Anotação `@AdditionalProcessingCapable` + anotar tasks existentes | `iped-tasks-*` | — |
| 8 | `AdditionalProcessingDialog` + `AdditionalTaskProgressPanel` | `iped-app` | 6, 7 |
| 9 | Filtro + indicador visual na tabela | `iped-app` | 4, 8 |

---

## Pontos de atenção e decisões de design

1. **Re-execução e idempotência:** O upsert por `(itemId, caseUUID, taskName)` no Lucene adicional garante que re-executar a task sobrescreve o resultado anterior — desejável para correções de configuração.

2. **Tasks que criam itens filhos** (ex: `ParsingTask`): Fora do escopo desta fase. O `AdditionalTaskWorker` deve detectar e rejeitar tasks que tentem inserir itens novos na fila.

3. **Streams de arquivo:** Tasks como OCR precisam do stream binário do arquivo original. O `IItem` reconstruído via `IPEDSource` já contém o `ISeekableInputStreamFactory` apontando para a fonte original (imagem forense, ZIP, etc.) — funciona sem mudanças.

4. **Consistência do índice adicional durante leitura:** Usar `DirectoryReader.openIfChanged()` no `LuceneAdditionalDataSource` para reabrir o reader após cada commit do runner, mantendo visibilidade dos novos resultados sem reiniciar o viewer.

5. **Segurança do índice principal:** O `AdditionalTaskWorker` nunca recebe referência ao `IndexWriter` do caso — impossível corromper o índice principal por acidente.
