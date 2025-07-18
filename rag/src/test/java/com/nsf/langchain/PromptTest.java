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

  

     

    private void assertLayerContains(List<String> layer, String expected, String layerName) {
        assertTrue(layerName + " should contain: " + expected + "\nActual: " + layer,
            layer.stream().anyMatch(s -> s.contains(expected)));
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

    System.out.println("Services: " + map.services.keySet());
    System.out.println("Full Map:\n" + map.services);

    String asciiDiagram = utils.generateServiceBoundary(map);
    System.out.println("Generated ASCII Diagram:\n" + asciiDiagram);


    assertTrue("Expected 'user' service to be present", map.services.containsKey("user"));


    List<String> apiLayer = map.services.get("user").api;
    List<String> businessLayer = map.services.get("user").business;

    System.out.println("API Layer: " + apiLayer);
    System.out.println("Business Layer: " + businessLayer);

 
    assertLayerContains(apiLayer, "UserController", "API Layer");
    assertLayerContains(businessLayer, "UserService", "Business Layer");


    assertNotNull("Diagram should not be null", asciiDiagram);
    assertFalse("Diagram should not be blank", asciiDiagram.isBlank());
    assertTrue("Diagram should include boundary box", asciiDiagram.contains("+---"));
    assertTrue("Diagram should include user service", asciiDiagram.contains(" user "));
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

    String diagram = utils.generateServiceBoundary(map);

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


    System.out.println("Initial Service Calls:");
    map.serviceCalls.forEach((k, v) -> System.out.println(k + " -> " + v));


    map.serviceCalls = utils.inferServiceRelations(map);

    System.out.println("\nInferred Service Calls:");
    map.serviceCalls.forEach((k, v) -> System.out.println(k + " -> " + v));


    System.out.println("\n--- Boundary Context Diagram ---");
    utils.printServiceBoundary(files);


    assertTrue(map.services.containsKey("user"));
    assertTrue(map.services.get("user").business.stream()
        .anyMatch(snippet -> snippet.contains("UserService")));

    assertTrue(map.services.containsKey("order"));
    assertTrue(map.services.get("order").api.stream()
        .anyMatch(snippet -> snippet.contains("OrderController")));


    assertTrue(
        map.serviceCalls.containsKey("order") && 
        map.serviceCalls.get("order").contains("user")
    );
}


}
