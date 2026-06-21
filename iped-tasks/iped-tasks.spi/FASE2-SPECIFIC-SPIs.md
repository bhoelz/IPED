# FASE 2: Specific Component SPIs

## Status: ✅ COMPLETO

FASE 2 implementa as **7 SPIs específicas** para cada tipo de componente plugável em IPED.

## Arquivos Criados

### 1. CarverProvider (iped-carvers-api)
**File:** `iped-carvers/iped-carvers-api/src/main/java/iped/carvers/api/CarverProvider.java`

- Estende `ComponentProvider<Carver>`
- Método: `getSupportedCarverTypes()` - retorna tipos suportados (DER, EML, PDF, ZIP, etc)
- Descoberta: ServiceLoader via `META-INF/services/iped.carvers.api.CarverProvider`

**Uso:**
```java
public class PDFCarverProvider implements CarverProvider {
    @Override
    public String componentId() { return "pdf-carver"; }
    
    @Override
    public Carver createComponent() throws Exception {
        return new PDFCarver();
    }
    
    @Override
    public CarverType[] getSupportedCarverTypes() {
        return new CarverType[] { CarverType.PDF };
    }
}
```

### 2. ParserProvider (iped-parsers-api)
**File:** `iped-parsers/iped-parsers-api/src/main/java/iped/parsers/spi/ParserProvider.java`

- Estende `ComponentProvider<Object>` (retorna AbstractParser ou similar)
- Métodos:
  - `getSupportedMediaTypes()` - MIME types (application/pdf, etc)
  - `getDetectors()` - custom detection via filename/magic bytes
- Integração: Apache Tika framework
- Descoberta: ServiceLoader

**Uso:**
```java
public class APKParserProvider implements ParserProvider {
    @Override
    public String componentId() { return "apk-parser"; }
    
    @Override
    public Object createComponent() throws Exception {
        return new APKParser();
    }
    
    @Override
    public MediaType[] getSupportedMediaTypes() {
        return new MediaType[] {
            MediaType.parse("application/vnd.android.package-archive")
        };
    }
}
```

### 3. DataSourceProvider (iped-engine)
**File:** `iped-engine/src/main/java/iped/engine/datasource/spi/DataSourceProvider.java`

- Estende `ComponentProvider<DataSourceReader>`
- Interface aninhada: `DataSourceReader` com método `read(File)`
- Métodos:
  - `canRead(File)` - detecta se pode ler o arquivo
  - `getSupportedExtensions()` - extensões suportadas (.ad1, .raw, etc)
- Caso de uso: lê AD1, raw images, pastas, UFED XML, etc

**Uso:**
```java
public class AD1DataSourceProvider implements DataSourceProvider {
    @Override
    public DataSourceReader createComponent() throws Exception {
        return new AD1Reader();
    }
    
    @Override
    public boolean canRead(File file) {
        return file.getName().endsWith(".ad1");
    }
    
    @Override
    public String[] getSupportedExtensions() {
        return new String[] { ".ad1", ".001" };
    }
}
```

### 4. WebhookProvider (iped-engine) ⭐
**File:** `iped-engine/src/main/java/iped/engine/plugins/webhook/spi/WebhookProvider.java`

- Estende `ComponentProvider<WebhookHandler>`
- Record aninhado: `WebhookConfig` (local vs remoto HTTP)
- Interface aninhada: `WebhookHandler` com método `handle(eventType, source, data)`
- **Suporte a HTTP remoto com retry exponencial:**
  - Timeout configurável
  - Max retries com exponential backoff
  - 2xx = sucesso, 4xx = sem retry, 5xx = retry automático

**Uso - Local handler:**
```java
public class LoggingWebhookProvider implements WebhookProvider {
    @Override
    public WebhookHandler createComponent() throws Exception {
        return new LoggingHandler();
    }
    
    @Override
    public String[] getEventTypes() {
        return new String[] { "task.completed", "task.failed" };
    }
    
    @Override
    public WebhookConfig getWebhookConfig() {
        return WebhookConfig.local();  // In-process
    }
}
```

**Uso - Remote HTTP webhook:**
```java
public class SlackWebhookProvider implements WebhookProvider {
    @Override
    public WebhookHandler createComponent() throws Exception {
        return new SlackHandler();
    }
    
    @Override
    public String[] getEventTypes() {
        return new String[] { "task.completed", "item.processed" };
    }
    
    @Override
    public WebhookConfig getWebhookConfig() {
        return WebhookConfig.remote("https://hooks.slack.com/...");
    }
}
```

### 5. DatabaseProvider (iped-engine)
**File:** `iped-engine/src/main/java/iped/engine/database/spi/DatabaseProvider.java`

- Estende `ComponentProvider<Database>`
- Interfaces aninhadas:
  - `Database` - inicializa/abre conexões/fecha
  - `DatabaseConnection` - executa queries, inserts, updates, creates tables
- Método: `getDatabaseType()` - retorna tipo (sqlite, postgresql, mysql)
- Caso de uso: plugins criam databases de output para armazenar dados extraídos

**Uso:**
```java
public class SQLiteDatabaseProvider implements DatabaseProvider {
    @Override
    public String componentId() { return "sqlite-db"; }
    
    @Override
    public Database createComponent() throws Exception {
        return new SQLiteDatabase();
    }
    
    @Override
    public String getDatabaseType() { return "sqlite"; }
}
```

### 6. MetadataSchemaProvider (iped-engine)
**File:** `iped-engine/src/main/java/iped/engine/plugins/metadata/spi/MetadataSchemaProvider.java`

- Estende `ComponentProvider<MetadataSchema>`
- Interface aninhada: `MetadataSchema` com métodos de validação
- Método: `getProperties()` - retorna lista de `MetadataPropertyDescriptor`
- **Manual indexing:** Plugin declara quais propriedades indexar (não automático)
- Tipos suportados: date, boolean, integer, string, decimal

**Uso:**
```java
public class SocialMediaMetadataProvider implements MetadataSchemaProvider {
    @Override
    public String componentId() { return "social-media-metadata"; }
    
    @Override
    public List<MetadataPropertyDescriptor> getProperties() {
        return List.of(
            MetadataPropertyDescriptor.indexed("facebook_account", 
                "Facebook Account", "string"),
            MetadataPropertyDescriptor.faceted("instagram_followers", 
                "Instagram Followers", "integer", "standard"),
            MetadataPropertyDescriptor.notIndexed("twitter_raw_json", 
                "Twitter JSON", "string")
        );
    }
}
```

### 7. FileCategoryProvider (iped-engine)
**File:** `iped-engine/src/main/java/iped/engine/plugins/categories/spi/FileCategoryProvider.java`

- Estende `ComponentProvider<FileCategorySchema>`
- Record aninhado: `CategoryDefinition` (path, displayName, icon, description)
- Interface aninhada: `FileCategorySchema` com métodos de query de categorias
- Método: `getCategoryDefinitions()` - retorna lista de categorias
- Caso de uso: plugins definem categorias customizadas (Forensic/Windows/Registry, etc)

**Uso:**
```java
public class ForensicArtifactsProvider implements FileCategoryProvider {
    @Override
    public String componentId() { return "forensic-artifacts"; }
    
    @Override
    public List<CategoryDefinition> getCategoryDefinitions() {
        return List.of(
            new CategoryDefinition("Forensic/Windows/Registry", 
                "Windows Registry", "⚙️", "Registry hive files"),
            new CategoryDefinition("Forensic/Linux/Logs", 
                "Linux Logs", "📋", "Linux log files")
        );
    }
}
```

### 8. EventDispatcher (iped-engine)
**File:** `iped-engine/src/main/java/iped/engine/plugins/webhook/EventDispatcher.java`

- Gerencia webhooks locais (sync) e remotos (async HTTP POST)
- Métodos:
  - `registerLocalHandler()` - registra handler em-processo
  - `registerRemoteWebhook()` - registra webhook HTTP remoto
  - `dispatch()` - dispara evento para todos os handlers
- **HTTP Retry Logic:**
  - Exponential backoff: inicial backoff * 2 a cada retry
  - Max retry cap: 30 segundos
  - Status codes:
    - 2xx = sucesso (fim)
    - 4xx = falha permanente (sem retry)
    - 5xx ou 429 = retry automático
- Records aninhados:
  - `PluginEvent` - tipo, source, dados
  - `RemoteWebhookConfig` - endpoint, timeout, maxRetries, backoff
- Thread-safe: ExecutorService (4 threads) para webhooks async

## Eventos Padrão

Tipos de eventos que tarefas e componentes podem publicar:

```
task.started        - Tarefa começou
task.completed      - Tarefa completou com sucesso
task.failed         - Tarefa falhou
item.processed      - Item foi processado
database.created    - Database de output foi criado
```

## Integração com ComponentRegistry

Cada SPI é descoberta automaticamente via `ServiceLoader.load(ParserProvider.class, classLoader)` etc.

ComponentRegistry centralizado gerencia todos:
```
registry.getComponentsByType("carver")       // Lista todos carvers
registry.getComponentsByType("webhook")      // Lista todos webhooks
registry.getComponent("parser", "apk-parser") // Busca específico
```

## Padrão de Implementação

Todos os SPIs seguem padrão similar:

1. **Interface extends ComponentProvider<T>**
   - `componentType()` - retorna tipo como string
   - `createComponent()` - factory method
   - Métodos específicos do tipo (getSupportedTypes(), getProperties(), etc)

2. **ServiceLoader Registration**
   - `META-INF/services/iped.xxx.spi.XxxProvider`
   - Lista FQN de implementações

3. **Documentação com @Override defaults**
   - Exemplos de uso em JavaDoc
   - Métodos opcionais com default implementations

## Testing

Cada SPI deveria ter testes:
- Mock implementations
- ServiceLoader discovery
- Component creation
- Type-specific method validation

## Success Criteria (Fase 2)

✅ 7 SPIs criadas e documentadas
✅ EventDispatcher com HTTP retry exponencial
✅ Manual indexing de metadados
✅ ServiceLoader autodiscovery para cada tipo
✅ Zero breaking changes

## Próximas Fases

### FASE 3: Migração Carvers
- Envolver DERCarver, EMLCarver, etc como providers
- Registrar via META-INF/services/CarverProvider

### FASE 4: Migração Parsers
- Envolver 20+ parsers como providers
- Integração com Tika detectors

### FASE 5: Databases & Webhooks
- Implementar SQLite database provider
- Integrar EventDispatcher com pipeline de tasks

### FASE 6: Metadados & Categorias
- Implementar MetadataRegistry
- Elasticsearch indexing de metadados indexed=true

### FASE 7: i18n & Polish
- PluginResourceBundleLoader
- Traduções PT-BR, EN, ES, DE
