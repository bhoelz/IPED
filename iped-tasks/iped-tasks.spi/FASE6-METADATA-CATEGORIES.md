# FASE 6: Metadata & Categories

## Status: 🚧 IN PROGRESS

Custom metadata properties and file category hierarchies for plugins.

## Architecture Overview

### Metadata Management

```
MetadataSchemaProvider (SPI)
         ↓
   Declares properties: name, type, indexed, analyzer
         ↓
   MetadataRegistry
   ├─ Property registration
   ├─ Type validation
   ├─ Elasticsearch mapping
   └─ Serialization/deserialization
         ↓
   IItem.setMetadata() / getMetadata()
```

### File Category Hierarchy

```
FileCategoryProvider (SPI)
         ↓
   Declares categories: path, displayName, icon, description
         ↓
   FileCategoryRegistry
   ├─ Category tree building
   ├─ Hierarchy traversal
   ├─ Depth-based queries
   └─ Component-based lookup
         ↓
   UI displays hierarchy
   Items are classified into categories
```

## Components Implemented

### 1. MetadataPropertyDescriptor

**File:** `iped-engine/src/main/java/iped/engine/plugins/metadata/MetadataPropertyDescriptor.java`

Descriptor for a custom metadata property.

**Features:**
- Property name, display name, data type
- Indexed flag (manual - plugin decides)
- Faceted flag (for aggregations)
- Elasticsearch analyzer and field mapping
- Category/namespace for organization
- Component ID for audit trail

**Data Types:**
- `date` - LocalDateTime
- `boolean` - Boolean
- `integer` - Integer
- `decimal` - Double/Float
- `string` - Text with optional analyzer

**Usage:**
```java
MetadataPropertyDescriptor prop = new MetadataPropertyDescriptor(
    "facial_score",                    // property name
    "Facial Recognition Score",        // display name
    "decimal",                         // data type
    true,                              // indexed
    true,                              // faceted
    "standard",                        // analyzer
    "forensic.biometrics",             // category
    "face-recognition-plugin"          // component ID
);
```

### 2. MetadataRegistry

**File:** `iped-engine/src/main/java/iped/engine/plugins/metadata/MetadataRegistry.java`

Central registry for all custom metadata properties.

**Features:**
- Thread-safe property registration
- Property validation and lookup
- Elasticsearch mapping generation
- Type serialization/deserialization
- Indexed property tracking
- Component-based property grouping
- Statistics and monitoring

**Methods:**
```java
registry.registerProperty(descriptor);                    // Register new property
registry.getProperty(name);                              // Get property descriptor
registry.getAllProperties();                             // Get all properties
registry.getIndexedProperties();                         // Get indexed only
registry.getPropertiesByComponent(componentId);         // Get by plugin
registry.isIndexed(propertyName);                        // Check if indexed
registry.getElasticsearchMapping();                      // Get ES mapping
```

**Example:**
```java
MetadataRegistry registry = new MetadataRegistry();

// Register metadata properties
MetadataPropertyDescriptor facial = new MetadataPropertyDescriptor(
    "facial_score", "Facial Score", "decimal", true, false, "standard", "biometric"
);
registry.registerProperty(facial);

// Use with items
registry.setProperty(item, "facial_score", 0.95);
Double score = (Double) registry.getProperty(item, "facial_score");

// Get Elasticsearch configuration
Map<String, Object> mapping = registry.getElasticsearchMapping();
```

### 3. FileCategoryProvider (SPI)

**File:** `iped-engine/src/main/java/iped/engine/plugins/categories/spi/FileCategoryProvider.java`

Service Provider Interface for file category definitions.

**Features:**
- Declare category hierarchies
- Nested `CategoryDefinition` record
- Automatic discovery via ServiceLoader
- Support for icons and descriptions

**Usage:**
```java
public class MyCategories implements FileCategoryProvider {
    @Override
    public String componentId() { return "my-categories"; }
    
    @Override
    public List<CategoryDefinition> getCategoryDefinitions() {
        return List.of(
            new CategoryDefinition(
                "Forensic/Custom/Analysis",
                "Custom Analysis Results",
                "analysis",
                "Results from my analysis plugin"
            )
        );
    }
}
```

### 4. FileCategoryRegistry

**File:** `iped-engine/src/main/java/iped/engine/plugins/categories/FileCategoryRegistry.java`

Central registry for file category hierarchies.

**Features:**
- Tree-based category hierarchy
- Automatic parent node creation
- Path-based lookup and traversal
- Depth-based queries
- Component-based category grouping
- Category statistics
- Immutable CategoryDescriptor and CategoryHierarchy records

**Methods:**
```java
registry.registerCategory(path, displayName, icon, description, componentId);
registry.getCategory(path);                          // Get by path
registry.getRootCategories();                        // Get top-level
registry.getSubcategories(parentPath);              // Get children
registry.getLeafCategories();                       // Get leaf nodes
registry.getCategoriesByComponent(componentId);     // Get by plugin
registry.getParentCategory(path);                   // Get parent
registry.getHierarchy(path);                        // Get tree
registry.getCategoriesByDepth(depth);               // Get by level
```

**Example:**
```java
FileCategoryRegistry categories = new FileCategoryRegistry();

// Register categories
categories.registerCategory(
    "Forensic/Windows/Registry",
    "Windows Registry",
    "registry",
    "Registry hive files",
    "windows-plugin"
);

// Traverse hierarchy
CategoryHierarchy tree = categories.getHierarchy("Forensic");
List<CategoryDescriptor> leaves = categories.getLeafCategories();
```

### 5. Built-In Category Providers

**File:** `iped-engine/src/main/java/iped/engine/plugins/categories/BuiltInFileCategoryProviders.java`

Pre-built category hierarchies for forensic investigations.

**Providers:**
1. **WindowsForensicsProvider** - Registry, EventLogs, Prefetch, JumpLists, ShadowCopies, RecycleBin
2. **WebArtifactsProvider** - Chrome, Firefox, Safari, Edge/IE
3. **CommunicationArtifactsProvider** - Email, Messaging, SocialMedia, VoIP
4. **MultimediaProvider** - Images, Videos, Audio, Documents
5. **SystemDataProvider** - ApplicationData, Temporary, Configuration, Databases

**Registered via:** `META-INF/services/iped.engine.plugins.categories.spi.FileCategoryProvider`

## Testing Strategy

### Unit Tests

- **MetadataRegistryTest** (10 tests)
  - Property registration
  - Duplicate prevention
  - Collection operations
  - Type validation
  - Elasticsearch mapping

- **FileCategoryRegistryTest** (15 tests)
  - Category registration
  - Hierarchy building
  - Path-based lookup
  - Depth-based queries
  - Statistics

### Integration Tests

```java
@Test
public void testMetadataWithItem() {
    MetadataRegistry registry = new MetadataRegistry();
    MetadataPropertyDescriptor prop = new MetadataPropertyDescriptor(
        "custom_prop", "Custom", "string", true, false, "standard", "test"
    );
    registry.registerProperty(prop);
    
    // Assuming IItem is available (mock)
    IItem item = createTestItem();
    registry.setProperty(item, "custom_prop", "value");
    
    assertEquals("value", registry.getProperty(item, "custom_prop"));
}

@Test
public void testCategoryHierarchy() {
    FileCategoryRegistry categories = new FileCategoryRegistry();
    categories.registerCategory("A/B/C", "C", "icon", "desc", "comp1");
    categories.registerCategory("A/B/D", "D", "icon", "desc", "comp1");
    categories.registerCategory("A/E", "E", "icon", "desc", "comp1");
    
    assertEquals(3, categories.getLeafCategories().size());
    assertEquals(3, categories.getCategoriesByDepth(2).size());
}
```

## File Structure

```
iped-engine/
├── src/main/java/iped/engine/plugins/metadata/
│   ├── MetadataPropertyDescriptor.java         (~180 lines)
│   └── MetadataRegistry.java                   (~250 lines)
├── src/main/java/iped/engine/plugins/categories/
│   ├── FileCategoryRegistry.java               (~350 lines)
│   ├── BuiltInFileCategoryProviders.java       (~200 lines)
│   └── spi/
│       └── FileCategoryProvider.java           (~100 lines)
├── src/test/java/iped/engine/plugins/metadata/
│   └── MetadataRegistryTest.java               (~200 lines)
├── src/test/java/iped/engine/plugins/categories/
│   └── FileCategoryRegistryTest.java           (~250 lines)
└── src/main/resources/META-INF/services/
    └── iped.engine.plugins.categories.spi.FileCategoryProvider (5 providers)
```

## Success Criteria

✅ MetadataPropertyDescriptor describes metadata properties
✅ MetadataRegistry manages property registration and lookup
✅ FileCategoryProvider SPI for category definitions
✅ FileCategoryRegistry builds and traverses category hierarchies
✅ 5 built-in category providers for common artifacts
✅ Thread-safe concurrent access
✅ Elasticsearch mapping generation
✅ Type validation and serialization
✅ Comprehensive unit tests (25+ tests)
✅ No breaking changes

## What's Next

**Remaining Phase 6 Tasks:**
- [ ] Elasticsearch integration
- [ ] Integration with IItem
- [ ] ConfigurationManager initialization
- [ ] Example plugins using metadata/categories
- [ ] Performance benchmarks

**Phase 7 (Final):**
- i18n support (PT-BR, EN, ES, DE)
- PluginResourceBundleLoader
- Example plugins
- Release as v5.3.0 beta

## Integration Points

### 1. ConfigurationManager

Must initialize MetadataRegistry and FileCategoryRegistry:

```java
MetadataRegistry metaRegistry = new MetadataRegistry();
FileCategoryRegistry catRegistry = new FileCategoryRegistry();

// Register from plugins
for (ComponentRegistration<MetadataSchemaProvider> reg : 
        componentRegistry.getComponentsByType("metadata")) {
    for (MetadataPropertyDescriptor prop : reg.provider().getProperties()) {
        metaRegistry.registerProperty(prop);
    }
}

for (ComponentRegistration<FileCategoryProvider> reg : 
        componentRegistry.getComponentsByType("category")) {
    for (FileCategoryProvider.CategoryDefinition cat : reg.provider().getCategoryDefinitions()) {
        catRegistry.registerCategory(cat.path(), cat.displayName(), 
            cat.icon(), cat.description(), reg.provider().componentId());
    }
}

// Store in CaseData
caseData.putCaseObject("__iped_metadata_registry__", metaRegistry);
caseData.putCaseObject("__iped_category_registry__", catRegistry);
```

### 2. AbstractTask Integration

Access registries from tasks:

```java
public abstract class AbstractTask {
    protected MetadataRegistry getMetadataRegistry() {
        return (MetadataRegistry) caseData.getCaseObject("__iped_metadata_registry__");
    }
    
    protected FileCategoryRegistry getCategoryRegistry() {
        return (FileCategoryRegistry) caseData.getCaseObject("__iped_category_registry__");
    }
}
```

### 3. IItem Integration

Extend item metadata handling:

```java
MetadataRegistry registry = getMetadataRegistry();
registry.setProperty(item, "facial_score", 0.95);
registry.setProperty(item, "extracted_from", "carver_xyz");

// Get all custom metadata
Map<String, Object> metadata = registry.getMetadata(item);
```

## Known Limitations

1. **Elasticsearch Integration Not Yet Implemented**
   - Mapping generation is ready
   - Actual ES client integration pending

2. **IItem Integration Not Yet Implemented**
   - Registries created but not yet integrated with IItem

3. **No Automatic Indexing**
   - Plugins must explicitly declare indexed properties
   - Prevents uncontrolled index bloat

4. **Category Display**
   - Hierarchy computed on-demand
   - Could be cached for performance

---

**FASE 6 STATUS**: 🚧 In Progress (60%)

Completed:
- ✅ MetadataPropertyDescriptor
- ✅ MetadataRegistry (thread-safe)
- ✅ FileCategoryProvider SPI
- ✅ FileCategoryRegistry (with tree hierarchy)
- ✅ 5 built-in category providers
- ✅ 25+ unit tests

Remaining:
- ⏳ Elasticsearch integration
- ⏳ IItem integration
- ⏳ ConfigurationManager initialization
- ⏳ Example plugins
- ⏳ Performance benchmarks

**Estimated Remaining Work:** 2-3 hours
