# IPED Plugin Development Quick Start

## Overview

The IPED plugin system allows you to extend IPED with custom components:
- **Carvers** - recover files using signatures
- **Parsers** - analyze file formats
- **DataSources** - read custom evidence formats
- **Webhooks** - react to processing events (local or HTTP)
- **Databases** - store custom extracted data
- **Metadata** - add custom item properties
- **Categories** - define file classifications

## Creating Your First Plugin

### Step 1: Create Maven Module

```bash
mkdir -p my-plugin/src/main/{java,resources}
cd my-plugin
cat > pom.xml << 'EOF'
<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.mycompany</groupId>
  <artifactId>iped-plugin-mycarver</artifactId>
  <version>1.0.0</version>
  
  <dependencies>
    <!-- IPED core SPI -->
    <dependency>
      <groupId>br.gov.pf</groupId>
      <artifactId>iped-tasks-spi</artifactId>
      <version>5.2.0</version>
      <scope>provided</scope>
    </dependency>
    
    <!-- For carver plugin -->
    <dependency>
      <groupId>br.gov.pf</groupId>
      <artifactId>iped-carvers-api</artifactId>
      <version>5.2.0</version>
      <scope>provided</scope>
    </dependency>
  </dependencies>
</project>
EOF
```

### Step 2: Implement CarverProvider

**File:** `src/main/java/com/mycompany/MyCarverProvider.java`

```java
package com.mycompany;

import iped.carvers.api.Carver;
import iped.carvers.api.CarverProvider;
import iped.carvers.api.CarverType;
import iped.spi.ComponentDescriptor;

public class MyCarverProvider implements CarverProvider {

    @Override
    public String componentId() {
        return "my-custom-carver";
    }

    @Override
    public ComponentDescriptor descriptor() {
        return ComponentDescriptor.of("my-custom-carver", "carver")
            .withDisplayName("My Custom Carver")
            .withDescription("Recovers custom file format");
    }

    @Override
    public Carver createComponent() throws Exception {
        return new MyCarver();
    }

    @Override
    public CarverType[] getSupportedCarverTypes() {
        return new CarverType[] { CarverType.CUSTOM };
    }
}
```

**File:** `src/main/java/com/mycompany/MyCarver.java`

```java
package com.mycompany;

import iped.carvers.api.Carver;
import iped.carvers.api.CarverType;
import iped.carvers.api.Hit;
import iped.carvers.api.CarvedItemListener;
import iped.data.IItem;
import java.io.IOException;

public class MyCarver implements Carver {

    @Override
    public void notifyHit(IItem parent, Hit hit) throws IOException {
        // Called when a signature is found
    }

    @Override
    public void notifyEnd(IItem parent) throws IOException {
        // Called when carving is complete
    }

    @Override
    public IItem carveFromHeader(IItem parent, Hit header) throws IOException {
        // Extract file from header signature
        return null;
    }

    @Override
    public IItem carveFromFooter(IItem parent, Hit footer) throws IOException {
        // Extract file from footer signature
        return null;
    }

    @Override
    public long getLengthFromHit(IItem parent, Hit hit) throws IOException {
        // Determine file size from signature
        return 0;
    }

    @Override
    public CarverType[] getCarverTypes() {
        return new CarverType[] { CarverType.CUSTOM };
    }

    @Override
    public void registerCarvedItemListener(CarvedItemListener listener) {}

    @Override
    public void removeCarvedItemListener(CarvedItemListener listener) {}

    @Override
    public void setIgnoreCorrupted(boolean ignore) {}
}
```

### Step 3: Register with ServiceLoader

**File:** `src/main/resources/META-INF/services/iped.carvers.api.CarverProvider`

```
com.mycompany.MyCarverProvider
```

### Step 4: Build and Deploy

```bash
mvn clean package

# Copy JAR to IPED plugins directory
cp target/iped-plugin-mycarver-1.0.0.jar ~/.iped/plugins/
```

### Step 5: Configure (Optional)

If your carver needs configuration, create:

**File:** `src/main/resources/conf/MyCarverConfig.txt`

```
# Enable/disable the carver
enabled=true

# Custom properties
threshold=100
```

**File:** `src/main/java/com/mycompany/MyCarverConfig.java`

```java
package com.mycompany;

import iped.configuration.AbstractPropertiesConfigurable;

public class MyCarverConfig extends AbstractPropertiesConfigurable {

    private boolean enabled = true;
    private int threshold = 100;

    @Override
    public String getConfigFileName() {
        return "MyCarverConfig.txt";
    }

    // Getters and setters
}
```

## Other Plugin Types

### ParserProvider

```java
public class MyParserProvider implements ParserProvider {
    @Override
    public String componentId() { return "my-parser"; }
    
    @Override
    public Object createComponent() throws Exception {
        return new MyParser();  // AbstractParser
    }
    
    @Override
    public MediaType[] getSupportedMediaTypes() {
        return new MediaType[] {
            MediaType.parse("application/x-myformat")
        };
    }
    
    @Override
    public ParserDetector[] getDetectors() {
        return new ParserDetector[] {
            (fileName, magicBytes) -> fileName != null && fileName.endsWith(".myformat")
        };
    }
}
```

### WebhookProvider (Local Handler)

```java
public class MyWebhookProvider implements WebhookProvider {
    @Override
    public String componentId() { return "my-logger"; }
    
    @Override
    public WebhookHandler createComponent() throws Exception {
        return (eventType, source, data) -> {
            System.out.println("Event: " + eventType + " from " + source);
        };
    }
    
    @Override
    public String[] getEventTypes() {
        return new String[] { "task.completed", "item.processed" };
    }
    
    @Override
    public WebhookConfig getWebhookConfig() {
        return WebhookConfig.local();
    }
}
```

### WebhookProvider (Remote HTTP)

```java
public class SlackWebhookProvider implements WebhookProvider {
    @Override
    public String componentId() { return "slack-webhook"; }
    
    @Override
    public WebhookHandler createComponent() throws Exception {
        return (eventType, source, data) -> {
            // Handler for processing before HTTP send
        };
    }
    
    @Override
    public String[] getEventTypes() {
        return new String[] { "task.completed" };
    }
    
    @Override
    public WebhookConfig getWebhookConfig() {
        return WebhookConfig.remote("https://hooks.slack.com/services/YOUR/WEBHOOK/URL");
    }
}
```

### MetadataSchemaProvider

```java
public class SocialMediaMetadataProvider implements MetadataSchemaProvider {
    @Override
    public String componentId() { return "social-media"; }
    
    @Override
    public List<MetadataPropertyDescriptor> getProperties() {
        return List.of(
            MetadataPropertyDescriptor.indexed("facebook_account", 
                "Facebook Account", "string"),
            MetadataPropertyDescriptor.faceted("instagram_followers", 
                "Instagram Followers", "integer", "standard")
        );
    }
    
    @Override
    public MetadataSchema createComponent() throws Exception {
        return new SocialMediaSchema();
    }
}
```

### FileCategoryProvider

```java
public class ForensicCategoryProvider implements FileCategoryProvider {
    @Override
    public String componentId() { return "forensic"; }
    
    @Override
    public List<CategoryDefinition> getCategoryDefinitions() {
        return List.of(
            new CategoryDefinition("Forensic/Windows/Registry",
                "Windows Registry", "⚙️", "Registry files"),
            new CategoryDefinition("Forensic/Linux/Logs",
                "Linux Logs", "📋", "Log files")
        );
    }
    
    @Override
    public FileCategorySchema createComponent() throws Exception {
        return new ForensicSchema();
    }
}
```

## File Structure

```
my-plugin/
├── pom.xml
├── src/main/
│   ├── java/com/mycompany/
│   │   ├── MyCarverProvider.java
│   │   ├── MyCarver.java
│   │   └── MyCarverConfig.java (optional)
│   └── resources/
│       ├── META-INF/services/
│       │   └── iped.carvers.api.CarverProvider
│       └── conf/
│           ├── MyCarverConfig.txt (optional)
│           ├── MyCarver.schema.json (optional)
│           └── MyCarver.uischema.json (optional)
└── target/
    └── iped-plugin-mycarver-1.0.0.jar
```

## Key Points

1. **Service Provider Discovery**
   - Plugin MUST have entry in `META-INF/services/`
   - ServiceLoader discovers automatically at startup

2. **Unique IDs**
   - `componentId()` must be unique globally
   - Format: kebab-case (my-custom-carver)

3. **No Configuration Required**
   - Basic plugin works with just provider + implementation
   - Configuration (Config class, JSON schema) is optional

4. **Logging**
   - Use SLF4J: `import org.slf4j.LoggerFactory;`
   - IPED configures logging automatically

5. **Dependencies**
   - Add `provided` scope for IPED dependencies
   - Your plugin JAR must be self-contained
   - Don't include IPED JARs in plugin

6. **Error Handling**
   - Exceptions in `initialize()` prevent plugin load
   - Exceptions in `createComponent()` are logged but continue
   - Exceptions in handlers are logged (don't stop others)

## Testing Your Plugin

1. **Build:** `mvn clean package`
2. **Deploy:** Copy JAR to `~/.iped/plugins/`
3. **Run:** Start IPED (ComponentRegistry will discover your provider)
4. **Verify:** Check logs for "Registered component: type=carver, id=my-custom-carver"

## Resources

- **SPI Documentation:** See `/docs/FASE1-COMPONENT-ARCHITECTURE.md`
- **Provider Details:** See `/docs/FASE2-SPECIFIC-SPIs.md`
- **Architecture:** See `/docs/PLUGIN-SYSTEM-IMPLEMENTATION-SUMMARY.md`
- **Full Plan:** See `/plans/quero-generalizar-a-abordagem-jolly-pearl.md`

## Examples

See each SPI interface JavaDoc for complete examples:
- `CarverProvider` - DER, EML, PDF, ZIP examples
- `ParserProvider` - APK, BitTorrent examples
- `WebhookProvider` - Slack, logging examples
- `MetadataSchemaProvider` - Social media, artifacts examples
- `FileCategoryProvider` - Forensic artifacts examples

---

**Happy plugin development!** 🔌
