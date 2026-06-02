package iped.engine.graph;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to verify GraphServiceFactoryImpl supports per-database isolation.
 *
 * These tests verify that multiple GraphService instances can coexist
 * independently for different database folders.
 */
public class GraphServiceFactoryImplTest {

    @Test
    void testFactoryInstanceExists() {
        GraphServiceFactory factory = GraphServiceFactoryImpl.getInstance();
        assertNotNull(factory, "Factory singleton should exist");
    }

    @Test
    void testGetGraphServiceWithoutFolder() {
        GraphServiceFactory factory = GraphServiceFactoryImpl.getInstance();
        GraphService service = factory.getGraphService();
        assertNotNull(service, "GraphService should be created");
    }

    @Test
    void testGetGraphServiceWithSpecificFolder() {
        GraphServiceFactory factory = GraphServiceFactoryImpl.getInstance();
        File folder1 = new File("./test-graph-db-1");

        GraphService service1 = factory.getGraphService(folder1);
        assertNotNull(service1, "GraphService should be created for folder 1");
    }

    @Test
    void testMultipleGraphServicesByFolder() {
        GraphServiceFactory factory = GraphServiceFactoryImpl.getInstance();
        File folder1 = new File("./test-graph-db-1");
        File folder2 = new File("./test-graph-db-2");

        GraphService service1 = factory.getGraphService(folder1);
        GraphService service2 = factory.getGraphService(folder2);

        assertNotNull(service1, "Service for folder 1 should exist");
        assertNotNull(service2, "Service for folder 2 should exist");
        assertNotSame(service1, service2, "Services for different folders should be different instances");
    }

    @Test
    void testSameGraphServiceForSameFolder() {
        GraphServiceFactory factory = GraphServiceFactoryImpl.getInstance();
        File folder = new File("./test-graph-db-same");

        GraphService service1 = factory.getGraphService(folder);
        GraphService service2 = factory.getGraphService(folder);

        assertSame(service1, service2, "Services for same folder should be the same instance");
    }

    @Test
    void testCaseIsolationByFolder() {
        GraphServiceFactory factory = GraphServiceFactoryImpl.getInstance();

        File case1Folder = new File("./test-case-1/neo4j");
        File case2Folder = new File("./test-case-2/neo4j");

        GraphService case1Service = factory.getGraphService(case1Folder);
        GraphService case2Service = factory.getGraphService(case2Folder);

        assertNotNull(case1Service, "Case 1 should have its own GraphService");
        assertNotNull(case2Service, "Case 2 should have its own GraphService");
        assertNotSame(case1Service, case2Service, "Cases should have separate services");
    }

    @Test
    void testBackwardsCompatibilityNoFolder() {
        GraphServiceFactory factory = GraphServiceFactoryImpl.getInstance();

        GraphService service1 = factory.getGraphService();
        GraphService service2 = factory.getGraphService();

        assertNotNull(service1, "Service without folder should be created");
        assertNotNull(service2, "Another service without folder should be created");
        assertNotSame(service1, service2, "Services without folder should be new instances each time");
    }
}
