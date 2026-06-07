package iped.engine.graph;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GraphConfigurationTest {

    // ---- Static label constants ----

    @Test
    void personLabel_isCorrect() {
        assertEquals("PERSON", GraphConfiguration.PERSON_LABEL);
    }

    @Test
    void organizationLabel_isCorrect() {
        assertEquals("ORGANIZATION", GraphConfiguration.ORGANIZATION_LABEL);
    }

    @Test
    void phoneLabel_isCorrect() {
        assertEquals("PHONE", GraphConfiguration.PHONE_LABEL);
    }

    @Test
    void emailLabel_isCorrect() {
        assertEquals("EMAIL", GraphConfiguration.EMAIL_LABEL);
    }

    @Test
    void carLabel_isCorrect() {
        assertEquals("CAR", GraphConfiguration.CAR_LABEL);
    }

    @Test
    void documentLabel_isCorrect() {
        assertEquals("DOCUMENT", GraphConfiguration.DOCUMENT_LABEL);
    }

    @Test
    void bankAccountLabel_isCorrect() {
        assertEquals("BANK_ACCOUNT", GraphConfiguration.BANK_ACCOUNT_LABEL);
    }

    @Test
    void moneyTransferLabel_isCorrect() {
        assertEquals("MONEY_TRANSFER", GraphConfiguration.MONEY_TRANSFER_LABEL);
    }

    @Test
    void datasourceLabel_isCorrect() {
        assertEquals("DATASOURCE", GraphConfiguration.DATASOURCE_LABEL);
    }

    @Test
    void contactGroupLabel_isCorrect() {
        assertEquals("CONTACT_GROUP", GraphConfiguration.CONTACT_GROUP_LABEL);
    }

    // ---- POJO getter/setter round-trips ----

    @Test
    void getPhoneRegion_defaultIsNull() {
        GraphConfiguration cfg = new GraphConfiguration();
        assertNull(cfg.getPhoneRegion());
    }

    @Test
    void setDefaultEntity_roundTrip() {
        GraphConfiguration cfg = new GraphConfiguration();
        cfg.setDefaultEntity("DOCUMENT");
        assertEquals("DOCUMENT", cfg.getDefaultEntity());
    }

    @Test
    void setDefaultPersonEntity_roundTrip() {
        GraphConfiguration cfg = new GraphConfiguration();
        cfg.setDefaultPersonEntity("PERSON");
        assertEquals("PERSON", cfg.getDefaultPersonEntity());
    }

    @Test
    void setDefaultBusinessEntity_roundTrip() {
        GraphConfiguration cfg = new GraphConfiguration();
        cfg.setDefaultBusinessEntity("ORGANIZATION");
        assertEquals("ORGANIZATION", cfg.getDefaultBusinessEntity());
    }

    @Test
    void setDefaultRelationship_roundTrip() {
        GraphConfiguration cfg = new GraphConfiguration();
        cfg.setDefaultRelationship("PROXIMITY");
        assertEquals("PROXIMITY", cfg.getDefaultRelationship());
    }

    @Test
    void setIncludeCategories_roundTrip() {
        GraphConfiguration cfg = new GraphConfiguration();
        cfg.setIncludeCategories(".*");
        assertEquals(".*", cfg.getIncludeCategories());
    }

    @Test
    void setExcludeCategories_roundTrip() {
        GraphConfiguration cfg = new GraphConfiguration();
        cfg.setExcludeCategories("^$");
        assertEquals("^$", cfg.getExcludeCategories());
    }

    @Test
    void defaultConstructor_mimeLists_areNotNull() {
        GraphConfiguration cfg = new GraphConfiguration();
        assertNotNull(cfg.getMimesToDetectPhones());
        assertNotNull(cfg.getMimesToDontDetectPhones());
    }

    @Test
    void defaultConstructor_detectPhonesOnOtherMimes_isTrue() {
        GraphConfiguration cfg = new GraphConfiguration();
        assertTrue(cfg.getDetectPhonesOnOtherMimes());
    }

    @Test
    void setEntities_roundTrip() {
        GraphConfiguration cfg = new GraphConfiguration();
        cfg.setEntities(new java.util.ArrayList<>());
        assertNotNull(cfg.getEntities());
        assertTrue(cfg.getEntities().isEmpty());
    }

    // ---- Inner class tests ----

    @Test
    void graphEntity_labelGetterSetter() {
        GraphConfiguration.GraphEntity entity = new GraphConfiguration.GraphEntity();
        entity.setLabel("PHONE");
        assertEquals("PHONE", entity.getLabel());
    }

    @Test
    void graphEntity_metadataGetterSetter() {
        GraphConfiguration.GraphEntity entity = new GraphConfiguration.GraphEntity();
        entity.setMetadata(new java.util.ArrayList<>());
        assertNotNull(entity.getMetadata());
    }

    @Test
    void graphEntityMetadata_nameGetterSetter() {
        GraphConfiguration.GraphEntityMetadata meta = new GraphConfiguration.GraphEntityMetadata();
        meta.setName("phone");
        assertEquals("phone", meta.getName());
    }

    @Test
    void graphEntityMetadata_propertyGetterSetter() {
        GraphConfiguration.GraphEntityMetadata meta = new GraphConfiguration.GraphEntityMetadata();
        meta.setProperty("phoneNumber");
        assertEquals("phoneNumber", meta.getProperty());
    }
}
