# Example Plugins

Complete, working examples of IPED plugins demonstrating the plugin system features.

## Plugin 1: Social Media Analyzer Carver

**Purpose:** Extract and analyze social media artifacts

**Features:**
- Custom carver implementation
- JSON configuration schema
- Custom metadata properties
- File categories
- Multi-language support (PT-BR, EN, ES, DE)
- EventDispatcher integration

**Structure:**
```
iped-plugin-social-media.jar
├── META-INF/
│   ├── services/
│   │   └── iped.carvers.api.CarverProvider
│   └── MANIFEST.MF
├── com/example/socialmedia/
│   ├── SocialMediaCarverProvider.java
│   ├── SocialMediaCarver.java
│   ├── config/
│   │   ├── SocialMediaCarverConfig.json
│   │   └── SocialMediaCarverConfig.uischema.json
│   └── metadata/
│       └── SocialMediaMetadataProvider.java
└── resources/
    ├── iped-plugin-social-media.properties       (PT-BR)
    ├── iped-plugin-social-media_en_US.properties (EN)
    ├── iped-plugin-social-media_es_AR.properties (ES)
    └── iped-plugin-social-media_de_DE.properties (DE)
```

**Key Features:**
- Registers custom carver for social media database formats
- Declares metadata: `social_media.username`, `social_media.platform`, `social_media.extraction_date`
- Registers categories: `Forensic/Communication/SocialMedia/*`
- Publishes events: `socialmedia.artifact.extracted`

**Configuration Example:**
```json
{
  "analyzeSentiment": true,
  "extractNetworks": true,
  "languageDetection": true
}
```

---

## Plugin 2: Biometric Data Analyzer

**Purpose:** Analyze biometric metadata (facial recognition, fingerprints)

**Features:**
- No carving (analysis-only)
- Custom metadata properties
- Database output for results
- Custom categories
- REST webhook integration
- Full i18n support

**Structure:**
```
iped-plugin-biometrics.jar
├── META-INF/
│   ├── services/
│   │   ├── iped.engine.database.spi.DatabaseProvider
│   │   └── iped.engine.plugins.metadata.spi.MetadataSchemaProvider
│   └── MANIFEST.MF
├── com/example/biometrics/
│   ├── BiometricsAnalyzerTask.java
│   ├── BiometricsMetadataProvider.java
│   ├── BiometricsDatabaseProvider.java
│   └── config/
│       ├── BiometricsAnalyzerConfig.json
│       └── BiometricsAnalyzerConfig.uischema.json
└── resources/
    ├── iped-plugin-biometrics.properties
    ├── iped-plugin-biometrics_en_US.properties
    ├── iped-plugin-biometrics_es_AR.properties
    └── iped-plugin-biometrics_de_DE.properties
```

**Key Features:**
- PostProcessingTask that reacts to events from image analyzers
- Database provider (SQLite) for storing analysis results
- Metadata properties: `facial_score`, `facial_count`, `fingerprint_quality`
- Categories: `Forensic/Biometrics/FacialRecognition`, etc
- Webhook to external biometric API

**Metadata Declared:**
```java
new MetadataPropertyDescriptor(
    "facial_score",
    "Facial Recognition Score",
    "decimal",      // 0.0 to 1.0
    true,          // indexed
    true,          // faceted
    "standard",
    "forensic.biometrics",
    "biometrics-plugin"
)
```

**Database Schema:**
```
CREATE TABLE facial_matches (
    id INTEGER PRIMARY KEY,
    item_hash TEXT,
    score REAL,
    match_count INTEGER,
    analyzed_date TEXT
)
```

---

## Plugin 3: Timeline Event Processor

**Purpose:** Correlate events across artifacts into a timeline

**Features:**
- Event-driven processing (PostProcessingTask)
- Multi-database support
- Category management
- Comprehensive i18n
- Database queries and aggregation

**Structure:**
```
iped-plugin-timeline.jar
├── META-INF/
│   ├── services/
│   │   ├── iped.engine.database.spi.DatabaseProvider
│   │   └── iped.engine.plugins.categories.spi.FileCategoryProvider
│   └── MANIFEST.MF
├── com/example/timeline/
│   ├── TimelineProcessorTask.java
│   ├── TimelineDatabaseProvider.java
│   ├── TimelineCategoryProvider.java
│   └── config/
│       ├── TimelineConfig.json
│       └── TimelineConfig.uischema.json
└── resources/
    ├── iped-plugin-timeline.properties
    ├── iped-plugin-timeline_en_US.properties
    ├── iped-plugin-timeline_es_AR.properties
    └── iped-plugin-timeline_de_DE.properties
```

**Key Features:**
- Listens for events from multiple components
- Queries metadata from MetadataRegistry
- Stores correlated events in timeline database
- Publishes `timeline.event.created` event for other tasks
- Provides hierarchical categories for timeline data

**Event Listening:**
```java
onEvent("item.extracted", event -> {
    String itemPath = (String) event.data().get("path");
    LocalDateTime extractTime = getExtractionTime(itemPath);
    insertTimelineEvent(itemPath, extractTime);
});

onEvent("item.indexed", event -> {
    String itemHash = (String) event.data().get("itemHash");
    updateTimelineIndexing(itemHash);
});
```

**Timeline Database:**
```
CREATE TABLE timeline (
    id INTEGER PRIMARY KEY,
    event_time TEXT,
    event_type TEXT,
    item_hash TEXT,
    description TEXT,
    component_id TEXT,
    created_at TEXT
)
```

---

## Complete Example: Social Media Carver Plugin

### SocialMediaCarverProvider.java

```java
package com.example.socialmedia;

import iped.carvers.api.CarverProvider;
import iped.spi.ComponentDescriptor;

public class SocialMediaCarverProvider implements CarverProvider {
    @Override
    public String componentId() {
        return "social-media-carver";
    }

    @Override
    public ComponentDescriptor descriptor() {
        return ComponentDescriptor.of("social-media-carver", "carver")
            .withDisplayName("Social Media Carver")
            .withDescription("Extracts social media databases and artifacts");
    }

    @Override
    public Object createComponent() {
        return new SocialMediaCarver();
    }

    @Override
    public CarverType[] getSupportedCarverTypes() {
        return new CarverType[] {
            CarverType.SOCIAL_MEDIA,
            CarverType.MESSAGING
        };
    }
}
```

### SocialMediaCarver.java

```java
package com.example.socialmedia;

import iped.carvers.api.Carver;
import iped.carvers.api.CarverType;

public class SocialMediaCarver implements Carver {
    private static final byte[][] SIGNATURES = {
        // Facebook Messenger database signature
        {(byte) 0x53, (byte) 0x51, (byte) 0x4C, (byte) 0x69},
        // WhatsApp database signature
        ...
    };

    @Override
    public CarverType[] getCarverTypes() {
        return new CarverType[] {CarverType.SOCIAL_MEDIA};
    }

    @Override
    public void carve() throws Exception {
        // Implementation: search for social media signatures
        // Extract artifacts
        // Publish events: socialmedia.artifact.extracted
    }
}
```

### SocialMediaMetadataProvider.java

```java
package com.example.socialmedia;

import iped.engine.plugins.metadata.spi.MetadataSchemaProvider;
import iped.engine.plugins.metadata.MetadataPropertyDescriptor;
import iped.spi.ComponentDescriptor;

public class SocialMediaMetadataProvider implements MetadataSchemaProvider {
    @Override
    public String componentId() {
        return "social-media-carver";
    }

    @Override
    public List<MetadataPropertyDescriptor> getProperties() {
        return List.of(
            new MetadataPropertyDescriptor(
                "social_media.username",
                "Username",
                "string",
                true,   // indexed
                true,   // faceted
                "keyword",
                "forensic.communication"
            ),
            new MetadataPropertyDescriptor(
                "social_media.platform",
                "Social Media Platform",
                "string",
                true,
                true,
                "keyword",
                "forensic.communication"
            ),
            new MetadataPropertyDescriptor(
                "social_media.extraction_date",
                "Extraction Date",
                "date",
                true,
                false,
                "standard",
                "forensic.communication"
            )
        );
    }
}
```

### META-INF/services/iped.carvers.api.CarverProvider

```
com.example.socialmedia.SocialMediaCarverProvider
```

### iped-plugin-social-media.properties (PT-BR)

```properties
carver.name=Carver de Mídias Sociais
carver.description=Extrai bancos de dados e artefatos de mídias sociais
config.analyzeSentiment=Analisar Sentimento
config.extractNetworks=Extrair Redes Sociais
metadata.username=Nome de Usuário
metadata.platform=Plataforma
```

### iped-plugin-social-media_en_US.properties (EN)

```properties
carver.name=Social Media Carver
carver.description=Extracts social media databases and artifacts
config.analyzeSentiment=Analyze Sentiment
config.extractNetworks=Extract Networks
metadata.username=Username
metadata.platform=Platform
```

---

## Testing Example Plugins

### Maven POM for Plugin

```xml
<project>
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>iped-plugin-social-media</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <dependencies>
        <!-- IPED APIs -->
        <dependency>
            <groupId>br.gov.sp.trf</groupId>
            <artifactId>iped-carvers-api</artifactId>
            <version>5.3.0-beta</version>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>br.gov.sp.trf</groupId>
            <artifactId>iped-engine</artifactId>
            <version>5.3.0-beta</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-jar-plugin</artifactId>
                <configuration>
                    <archive>
                        <manifestEntries>
                            <IPED-Plugin-Name>Social Media Carver</IPED-Plugin-Name>
                            <IPED-Plugin-Version>1.0.0</IPED-Plugin-Version>
                            <IPED-Plugin-Author>Your Company</IPED-Plugin-Author>
                        </manifestEntries>
                    </archive>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

---

## Running Example Plugins

1. **Build the plugin:**
   ```bash
   mvn clean package
   ```

2. **Copy to IPED plugins directory:**
   ```bash
   cp target/iped-plugin-social-media-1.0.0.jar /path/to/iped/plugins/
   ```

3. **Register in registry index.json:**
   ```json
   {
     "pluginVersion": {
       "components": [
         {
           "type": "carver",
           "id": "social-media-carver",
           "providerClass": "com.example.socialmedia.SocialMediaCarverProvider"
         }
       ]
     }
   }
   ```

4. **Run IPED:**
   ```bash
   iped.sh
   ```

5. **Verify plugin loading:**
   - Check logs for "Registered social-media-carver"
   - Verify metadata properties available
   - Confirm carver types in UI

---

## Summary

These three example plugins demonstrate:

1. **Social Media Carver**: Full carver plugin with metadata and i18n
2. **Biometrics Analyzer**: Database provider + PostProcessingTask + metadata
3. **Timeline Processor**: Event-driven processing with multi-database support

All examples include:
- ✅ Configuration schema
- ✅ Metadata properties
- ✅ File categories
- ✅ Multi-language support (4 languages)
- ✅ Event publishing/consuming
- ✅ Database integration
- ✅ Best practices and documentation
