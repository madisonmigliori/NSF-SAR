package com.nsf.langchain;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.nsf.langchain.service.RagService;
import com.nsf.langchain.utils.ServiceBoundaryUtils;
import com.nsf.langchain.utils.ServiceBoundaryUtils.ArchitectureMap;
import com.nsf.langchain.utils.ServiceBoundaryUtils.ServiceBoundary;

@SpringBootTest
public class PromptTest {

    @Autowired
    RagService ragService;

    ServiceBoundaryUtils utils = new ServiceBoundaryUtils();


    @Test
    public void shouldGenerateAnalysisPrompt() {
        String result = ragService.testPromptOnly(
            "analysis",
            "repo-123",
            "Spring Boot, Kafka",
            "└── order-service",
            "└── ecommerce-platform",
            "Analysis pending..."
        );
        System.out.println(result);
        assert(result).contains("Dependencies:");
    }

    @Test
public void testBoundaryAndContextExtraction() throws IOException {
    Map<String, String> files = Map.of(
        "service/user/UserController.java", "@RestController public class UserController {}",
        "service/user/UserService.java", "@Service public class UserService {}",
        "common/Logger.java", "public class Logger {}"
    );

    List<ServiceBoundary> boundaries = utils.extractFiles(files);
    ArchitectureMap map = utils.fallback(boundaries);

    String asciiDiagram = utils.generateBoundaryContextDiagram(map);
    String asciiDiagram2 = utils.generateLayeredDiagramWithMultiArrowsCompact(map);

    System.out.println("Map: " + map.services.entrySet());
    System.out.println("Service keys: " + map.services.keySet());
    System.out.println("User API: " + map.services.get("user"));
    System.out.println("Ascii (boundary context): \n" + asciiDiagram);
    System.out.println("Ascii (layered compact): \n" + asciiDiagram2);

    assertTrue(map.services.containsKey("user"));
    assertFalse(map.services.get("user").api.isEmpty());
    assertTrue(map.services.get("user").api.stream()
        .anyMatch(snippet -> snippet.contains("UserController")));
    assertTrue(map.services.get("user").business.stream()
        .anyMatch(snippet -> snippet.contains("UserService")));


    assertNotNull(asciiDiagram);
    assertTrue(asciiDiagram.contains("UserController"));
    assertTrue(asciiDiagram.contains("UserService"));
    assertTrue(asciiDiagram.contains("user"));
    assertTrue(asciiDiagram.contains("api") || asciiDiagram.contains("business"));


    assertNotNull(asciiDiagram2);
    assertFalse(asciiDiagram2.isBlank());
    assertTrue(asciiDiagram2.contains("+---")); 
    assertTrue(asciiDiagram2.contains("| user")); 
    assertTrue(map.services.get("user").api.stream()
    .anyMatch(snippet -> snippet.contains("UserController")));
    assertTrue(map.services.get("user").business.stream()
    .anyMatch(snippet -> snippet.contains("UserService")));

}

    
    @Test
public void testRelationsInDiagram() throws IOException {
    Map<String, String> files = Map.of(
        "service/user/UserController.java", "@RestController public class UserController {}",
        "service/admin/AdminService.java", "@Service public class AdminService {}",
        "service/mapper/MapperModel.java", "class MapperModel {}"
    );

    List<ServiceBoundary> boundaries = utils.extractFiles(files);
    ArchitectureMap map = utils.fallback(boundaries);


    map.serviceCalls.put("user", Set.of("admin"));
    map.serviceCalls.put("admin", Set.of("mapper"));

    String diagram = utils.generateLayeredDiagramWithMultiArrowsCompact(map);

    System.out.println("Diagram with relations:\n" + diagram);

 
    assertTrue( "Expected relation user -> admin", diagram.contains("user ---> admin"));
    assertTrue("Expected relation admin -> mapper", diagram.contains("admin ---> mapper"));
}

@Test
public void testBoundary() throws IOException {
    Map<String, String> files = Map.of(
        "service/user/UserController.java", "@RestController public class UserController {}",
        "service/user/UserService.java", "@Service public class UserService {}",
        "service/order/OrderController.java", "@RestController public class OrderController { UserService userService; }",
        "common/Logger.java", "public class Logger {}"
    );

    List<ServiceBoundary> boundaries = utils.extractFiles(files);
    ArchitectureMap map = utils.fallback(boundaries);

    System.out.println("Service Calls:");
map.serviceCalls.forEach((k, v) -> System.out.println(k + " -> " + v));



    map.serviceCalls = utils.inferServiceRelations(map);

    String asciiDiagram = utils.generateBoundaryContextDiagram(map);
    String asciiDiagram2 = utils.generateLayeredDiagramWithMultiArrowsCompact(map);
    String asciiDiagram3 = utils.generateFlatDiagramWithClearArrows(map);
    String asciiDiagram4 = utils.generateDiagramWithMultiArrowsBetweenBoxes(map);
    String asciiDiagram5 = utils.generateFlatDiagramWithMultiArrows(map);

    System.out.println("Ascii (Context): \n" + asciiDiagram);
    System.out.println("Ascii (Layered): \n" + asciiDiagram2);
    System.out.println("Ascii (Levels): \n" + asciiDiagram3);
    System.out.println("Ascii (Arrows): \n" + asciiDiagram4);
    System.out.println("Ascii (Best): \n" + asciiDiagram5);
    System.out.println("Ascii with Anaylze & print: \n");
    utils.analyzeAndPrint(files);

    assertTrue(map.services.containsKey("user"));
    System.out.println("Artifacts in user: " + map.services.get("user").business);
    
    System.out.println("Snippets in order: ");
    map.services.get("order").api.forEach(System.out::println);
    
    assertTrue(map.serviceCalls.containsKey("order") && map.serviceCalls.get("order").contains("user"));


    
}



}
