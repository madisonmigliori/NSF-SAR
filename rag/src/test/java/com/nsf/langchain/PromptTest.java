package com.nsf.langchain;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoBeans;

import com.nsf.langchain.git.GitHubApi;
import com.nsf.langchain.service.RagService;
import com.nsf.langchain.utils.ServiceBoundaryUtils;
import com.nsf.langchain.utils.ServiceBoundaryUtils.ArchitectureMap;
import com.nsf.langchain.utils.ServiceBoundaryUtils.ServiceBoundary;

@SpringBootTest
public class PromptTest {

    @Autowired
    RagService ragService;

    @Autowired
    ServiceBoundaryUtils utils;

    @MockitoBean
    private GitHubApi gitHubApi;

  




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


    String asciiDiagram2 = utils.generateVerticalDiagramWithLevelsAndArrows2(map);

    System.out.println("Map: " + map.services.entrySet());
    System.out.println("Service keys: " + map.services.keySet());
    System.out.println("User API: " + map.services.get("user"));
    System.out.println("Ascii (layered compact): \n" + asciiDiagram2);

    assertTrue(map.services.containsKey("user"));
    assertFalse(map.services.get("user").api.isEmpty());
    assertTrue(map.services.get("user").api.stream()
        .anyMatch(snippet -> snippet.contains("UserController")));
    assertTrue(map.services.get("user").business.stream()
        .anyMatch(snippet -> snippet.contains("UserService")));



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

    String diagram = utils.generateVerticalDiagramWithLevelsAndArrows2(map);

    System.out.println("Diagram with relations:\n" + diagram);

 

}

@Test
public void testBoundary() throws IOException {
    Map<String, String> files = Map.of(
        "service/user/UserController.java", "@RestController public class UserController {}",
        "service/user/UserService.java", "@Service public class UserService {}",
        "service/order/OrderController.java", "@RestController public class OrderController { UserService userService; }",
        "common/Logger.java", "public class Logger {}",
    "service/auth/AuthController.java", "@RestController public class AuthController {}",
    "service/auth/AuthService.java", "@Service public class AuthService {}",
    "service/a/AController.java", "@RestController public class AController { BService bService; }",
    "service/a/AService.java", "@Service public class AService { BService bService; }",
    "service/b/BController.java", "@RestController public class BController { AService aService; }",
    "service/b/BService.java", "@Service public class BService { AService aService; }"

    );

    List<ServiceBoundary> boundaries = utils.extractFiles(files);
    ArchitectureMap map = utils.fallback(boundaries);

    System.out.println("Service Calls:");
map.serviceCalls.forEach((k, v) -> System.out.println(k + " -> " + v));



    map.serviceCalls = utils.inferServiceRelations(map);


    System.out.println("Ascii with Anaylze & print: \n");
    utils.analyzeAndPrint(files);

    assertTrue(map.services.containsKey("user"));
    System.out.println("Artifacts in user: " + map.services.get("user").business);
    
    System.out.println("Snippets in order: ");
    map.services.get("order").api.forEach(System.out::println);
    
    assertTrue(map.serviceCalls.containsKey("order") && map.serviceCalls.get("order").contains("user"));


    
}



}
