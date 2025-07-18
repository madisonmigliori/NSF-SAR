package com.nsf.langchain.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Provider.Service;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.testcontainers.shaded.org.apache.commons.lang3.arch.Processor.Arch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nsf.langchain.service.RagService;
import com.nsf.langchain.utils.ServiceBoundaryUtils.ArchitectureMap;
import com.nsf.langchain.utils.ServiceBoundaryUtils.ArchitectureMap.LayeredService;
import com.nsf.langchain.utils.ServiceBoundaryUtils.BoundedContext;

import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class ServiceBoundaryUtils {

    private static final Logger log = LoggerFactory.getLogger(ServiceBoundaryUtils.class);

    private static final int MAX_ITEM_LENGTH = 25;
    private static final int TRUNCATE_AT = 22;

    public record ServiceBoundary(String filePath, String language,String layer, String indicator, String snippet){

       }
    
       private static final List<String> LAYER_PRIORITY = List.of(
        "entity", "repository", "business", "api", "presentation", "shared", "apiGateway"
    );
    
    private static final Map<String, String> LAYER_LABELS = Map.of(
        "entity", "Entity",
        "repository", "Repository",
        "business", "Business",
        "api", "API",
        "presentation", "Presentation",
        "shared", "Shared",
        "apiGateway", "API Gateway"
    );
    


    public static final List<String> DDD_LAYER_ORDER = List.of("domain", "application", "infrastructure", "interface");

    private static final Pattern API_PATTERN = Pattern.compile(
        "(?i)(@RestController|@Controller|@RequestMapping|@GetMapping|@PostMapping" +
        "|@app\\.route\\(|@blueprint\\.route\\(|routes\\.draw" +
        "|express\\.Router\\(\\)|router\\.|http\\.HandleFunc|mux\\.HandleFunc|gin\\.Engine" +
        "|#\\[get\\(|#\\[post\\(|\\bController\\b|\\broute\\b|\\bendpoint\\b|\\bhandler\\b)"
    );
    
    


private static final Pattern BUSINESS_PATTERN = Pattern.compile(
    "(?i)\\b(@Service|@Component|UseCase|UseCases|BusinessLogic|\\w+Service|\\w+Manager|\\w+Interactor|\\w+Handler)\\b"
);



private static final Pattern ENTITY_PATTERN = Pattern.compile(
    "(?i)\\b(@Entity|Entity|Model|Schema|DAO|sql|ORM|sqlalchemy|diesel::|Mongoose|ActiveRecord|GORM|pydantic|Prisma)\\b"
);



private static final Pattern REPOSITORY_PATTERN = Pattern.compile(
    "(?i)\\b(@Repository|Repository|CrudRepository|JpaRepository)\\b"
);


private static final Pattern API_GATEWAY_PATTERN = Pattern.compile(
    "(?i)\\b(ApiGateway|Gateway|Proxy|Filter|Router|LoadBalancer|Zuul|SpringCloudGateway)\\b"
);


private static final Pattern PRESENTATION_PATTERN = Pattern.compile(
    "(?i)\\b(View|Page|Component|template|render|React|Angular|Vue|useEffect|useState|Fragment|Screen|FlaskForm|yew::)\\b" +
"(?i)(export default|\\.jsx|\\.tsx|\\.vue)"

);


private static final Pattern SHARED_PATTERN = Pattern.compile(
    "(?i)\\b(Logger|Validator|Utils?|Helper|Exception|Error|Config|Constants|Settings" +
"|Common|Middleware|Filter|Interceptor|Adapter|Transformer|Bridge|Base|Shared|Wrapper|Factory|Strategy)\\b"

);


public static class BoundedContext {
    public String name;
    public Map<String, List<String>> domainLayers = new LinkedHashMap<>();
    
    
    
    
        public BoundedContext() {
            for (String layer : DDD_LAYER_ORDER) {
                domainLayers.put(layer, new ArrayList<>());
            }
        }
        
        
        
            public BoundedContext(String serviceName) {
              this.name = serviceName;
        }
    
    
    }
    
    
    

    
        public static class ArchitectureMap{
            public static class LayeredService {
                public List<String> presentation = new ArrayList<>();
                public List<String> api = new ArrayList<>();
                public List<String> business = new ArrayList<>();
                public List<String> entity = new ArrayList<>();
                public List<String> repository = new ArrayList<>();
                public List<String> apiGateway = new ArrayList<>();
                public List<String> shared = new ArrayList<>();
                public Map<String, Set<String>> layerToClasses = new HashMap<>();
            }
    
            public Map<String, BoundedContext> boundedContexts = new LinkedHashMap<>();
            public Map<String, LayeredService> services = new HashMap<>();
            public Map<String, Set<String>> serviceCalls = new LinkedHashMap<>();
    
            
            public static List<String> extractList(JsonNode node, String fieldName){
                List<String> list = new ArrayList<>();
                if (node.has(fieldName)) {
                    for(JsonNode item : node.get(fieldName)) {
                        list.add(item.asText());
                    }
                }
                return list;
            }
    
            public static void elevateAndDeduplicateSharedAndEntityLayers(ArchitectureMap map) {
                Set<String> globalEntities = new HashSet<>();
                Set<String> globalShared = new HashSet<>();
            
                for (Map.Entry<String, ArchitectureMap.LayeredService> entry : map.services.entrySet()) {
                    ArchitectureMap.LayeredService svc = entry.getValue();
            
                    Map<String, Set<String>> cleanedLayers = assignClassesUniqueLayer(svc.layerToClasses);
            
                    Set<String> entities = cleanedLayers.remove("entity");
                    if (entities != null) globalEntities.addAll(entities);
            
                    Set<String> shared = cleanedLayers.remove("shared");
                    if (shared != null) globalShared.addAll(shared);
            
                    svc.presentation = new ArrayList<>(cleanedLayers.getOrDefault("presentation", Set.of()));
                    svc.api = new ArrayList<>(cleanedLayers.getOrDefault("api", Set.of()));
                    svc.business = new ArrayList<>(cleanedLayers.getOrDefault("business", Set.of()));
                    svc.repository = new ArrayList<>(cleanedLayers.getOrDefault("repository", Set.of()));
                    svc.apiGateway = new ArrayList<>(cleanedLayers.getOrDefault("apiGateway", Set.of()));
                    svc.shared = new ArrayList<>();
                    svc.entity = new ArrayList<>();
                    svc.layerToClasses= cleanedLayers;
                }
            
                
                ArchitectureMap.LayeredService modelService = new ArchitectureMap.LayeredService();
                modelService.entity = new ArrayList<>(globalEntities);
                map.services.put("model", modelService);
    
            
    
            
                ArchitectureMap.LayeredService sharedService = new ArchitectureMap.LayeredService();
                sharedService.shared = new ArrayList<>(globalShared);
                map.services.put("shared", sharedService);
            }
            
    
    
        public void printLayers() {
            services.forEach((service, layers) -> {
                System.out.println("Service: " + service);
        
                if (!layers.presentation.isEmpty())
                    System.out.println("Presentation: " + layers.presentation);
                if (!layers.api.isEmpty())
                    System.out.println("API: " + layers.api);
                if (!layers.business.isEmpty())
                    System.out.println("Business: " + layers.business);
                if (!layers.entity.isEmpty())
                    System.out.println("Entity: " + layers.entity);
                if (!layers.apiGateway.isEmpty())
                    System.out.println("Api Gateway: " + layers.apiGateway);
                if (!layers.repository.isEmpty())
                    System.out.println("Repository: " + layers.repository);
                if (!layers.shared.isEmpty())
                    System.out.println("Shared: " + layers.shared);
        
                System.out.println(); 
            });
        }
        
        
        
        }
    
       
    
    
    
    
        
        public Map<String, BoundedContext> boundedContexts = new LinkedHashMap<>();
        
    
    
        public List<ServiceBoundary> extractFiles(Map<String, String> files) {
            List<ServiceBoundary> artifacts = new ArrayList<>();
        
            for (Map.Entry<String, String> entry : files.entrySet()) {
                String fileName = entry.getKey();
                String content = entry.getValue();
        
                if (fileName.endsWith(".java")) {
                    artifacts.addAll(extractJava(fileName, content));
                } else if (fileName.endsWith(".py")) {
                    artifacts.addAll(extractPy(fileName, content));
                } else if (fileName.endsWith(".js") || fileName.endsWith(".ts")) {
                    artifacts.addAll(extractJs(fileName, content));
                } else if (fileName.endsWith(".go")) {
                    artifacts.addAll(extractGo(fileName, content));
                } else if (fileName.endsWith(".rs")) {
                    artifacts.addAll(extractRs(fileName, content));
                } else if (fileName.endsWith(".cpp") || fileName.endsWith(".h") || fileName.endsWith(".hpp")) {
                    artifacts.addAll(extractCpp(fileName, content));
                } else if (fileName.endsWith(".rb")) {
                    artifacts.addAll(extractRuby(fileName, content));
                } else if (fileName.endsWith(".jsx") || fileName.endsWith(".tsx")) {
                    artifacts.addAll(extractReact(fileName, content));
                }
            }
        
            return artifacts;
        }
        
        
    
        
    
        
    private String inferLayer(String indicator) {
        if (indicator == null || indicator.isBlank()) return "unknown";
        String trimmed = indicator.trim();
    
        if (API_PATTERN.matcher(trimmed).find()) return "api";
        if (BUSINESS_PATTERN.matcher(trimmed).find()){
            System.out.println("Matched BUSINESS: " + trimmed);
            return "business";
        }
        if (ENTITY_PATTERN.matcher(trimmed).find()) return "entity";
        if (API_GATEWAY_PATTERN.matcher(trimmed).find()) return "apiGateway";
        if (REPOSITORY_PATTERN.matcher(trimmed).find()) return "repository";
        if (PRESENTATION_PATTERN.matcher(trimmed).find()) return "presentation";
        if (SHARED_PATTERN.matcher(trimmed).find()) return "shared";
    
        return "unknown";
    }
    
        
    
        private List<ServiceBoundary> extractJava(String path, String content){
            return matchPatterns(path, content, "Java", List.of(
                // API Layer
                "@RestController", "@Controller", "@RequestMapping", "@GetMapping", "@PostMapping",
                // Business Layer
                "@Service", "@Component", "Manager", "Handler", "UseCase", "BusinessLogic",
                // Data Layer
                "@Repository", "Entity", "JpaRepository", "CrudRepository", "DAO", "Model", "Schema",
                // Presentation Layer
                "public class \\w+Controller", "public class \\w+View", "public class \\w+Page",
                // Shared/Common Utilities
                "Logger", "Validator", "Utils", "Helper", "Exception", "Config", "Constants", "Settings",
                // General Java
                "public class \\w+", "interface \\w+"
            ));
        }
        
        private List<ServiceBoundary> extractPy(String path, String content){
            return matchPatterns(path, content, "Python", List.of(
                // API Layer
                "@app\\.route\\(", "@blueprint\\.route\\(", "Flask", "FastAPI",
                // Business Layer
                "class \\w+Service", "class \\w+Manager", "def \\w+_service", "def \\w+_handler",
                // Data Layer
                "class \\w+Model", "class \\w+Schema", "sqlalchemy", "pydantic", "BaseModel",
                // Presentation Layer
                "def \\w+_view", "render_template", "FlaskForm",
                // Shared
                "Logger", "Validator", "Utils", "Helper", "Exception", "Config", "Constants", "Settings",
                // General Python
                "class \\w+", "def \\w+\\("
            ));
        }
        
        private List<ServiceBoundary> extractJs(String path, String content){
            return matchPatterns(path, content, "Javascript", List.of(
                // API Layer
                "\"express\\\\.Router\\\\(\\\\)\"\n" + //
                                        "", "app\\.get\\(", "app\\.post\\(", "router\\.\\w+\\(", 
                // Business Layer
                "class \\w+Service", "function \\w+Service\\(", "const \\w+Service =",
                // Data Layer
                "class \\w+Model", "function \\w+Model\\(", "const \\w+Model =",
                // Presentation Layer (React)
                "function \\w+\\(", "class \\w+ extends React\\.Component", "useEffect\\(", "useState\\(",
                "render\\(", "React\\.createElement", "export default \\w+",
                // Shared
                "Logger", "Validator", "Utils", "Helper", "Exception", "Config", "Constants", "Settings",
                // General JS
                "class \\w+", "function \\w+\\("
            ));
        }
        
        private List<ServiceBoundary> extractGo(String path, String content){
            return matchPatterns(path, content, "Go", List.of(
                // API Layer
                "http\\.HandleFunc", "mux\\.HandleFunc", "gin\\.Engine", 
                // Business Layer
                "type \\w+Service struct", "func \\(.*\\) \\w+Service\\.", 
                // Data Layer
                "type \\w+Model struct", "sql\\.Open", "db\\.Query", "gorm\\.",
                // Presentation Layer
                "func \\w+Handler\\(", "html/template", 
                // Shared
                "Logger", "Validator", "Utils", "Helper", "Exception", "Config", "Constants", "Settings",
                // General Go
                "func \\w+\\(", "type \\w+ struct"
            ));
        }
        
        private List<ServiceBoundary> extractRs(String path, String content){
            return matchPatterns(path, content, "Rust", List.of(
                // API Layer
                "use actix_web::", "use rocket::", "#\\[get\\(", "#\\[post\\(",
                // Business Layer
                "struct \\w+Service", "impl \\w+Service",
                // Data Layer
                "struct \\w+Model", "diesel::", "schema::", "Queryable", "Insertable",
                // Presentation Layer
                "struct \\w+View", "impl \\w+View", "yew::", 
                // Shared
                "Logger", "Validator", "Utils", "Helper", "Error", "Config", "Constants", "Settings",
                // General Rust
                "fn \\w+\\(", "mod \\w+"
            ));
        }
        
        private List<ServiceBoundary> extractCpp(String path, String content){
            return matchPatterns(path, content, "C++", List.of(
                // API Layer
                "class \\w+Controller", "void \\w+Handler\\(",
                // Business Layer
                "class \\w+Service", "class \\w+Manager",
                // Data Layer
                "class \\w+Model", "struct \\w+Model", "namespace ORM", 
                // Presentation Layer
                "class \\w+View", "class \\w+Page",
                // Shared
                "Logger", "Validator", "Utils", "Helper", "Exception", "Config", "Constants", "Settings",
                // General C++
                "class \\w+", "void \\w+\\("
            ));
        }
        
        private List<ServiceBoundary> extractRuby(String path, String content){
            return matchPatterns(path, content, "Ruby", List.of(
                // API Layer
                "Rails\\.application\\.routes\\.draw", "class \\w+Controller",
                // Business Layer
                "class \\w+Service", "module \\w+Service", "def \\w+_service",
                // Data Layer
                "class \\w+Model", "ActiveRecord::Base", "class \\w+Serializer",
                // Presentation Layer
                "class \\w+View", "class \\w+Helper", "module \\w+Helper",
                // Shared
                "Logger", "Validator", "Utils", "Helper", "Exception", "Config", "Constants", "Settings",
                // General Ruby
                "class \\w+", "def \\w+"
            ));
        }
        
        private List<ServiceBoundary> extractReact(String path, String content){
            return matchPatterns(path, content, "React", List.of(
                // Presentation Layer
                "function \\w+\\(.*\\) \\{", "const \\w+ = \\(.*\\) => \\{",
                "useEffect\\(", "useState\\(", "render\\(", "React\\.createElement",
                "export default \\w+",
                // Shared
                "Logger", "Validator", "Utils", "Helper", "Exception", "Config", "Constants", "Settings"
            ));
        }
        
    
        private List<ServiceBoundary> matchPatterns(String path, String content, String language, List<String> patterns) {
            List<ServiceBoundary> results = new ArrayList<>();
        
            for (String regex : patterns) {
                Matcher matcher = Pattern.compile(regex).matcher(content);
                while (matcher.find()) {
                    String match = matcher.group(); 
                    String layer = inferLayer(match);
        
                    results.add(new ServiceBoundary(path, language, layer, match, content));
                }
            }
        
            return results;
        }
        
        
        
        public String format(List<ServiceBoundary> artifacts){
            return artifacts.stream()
            .map(a -> String.format("[Service Boundary Identification]\nFile: %s\nType: %s\nIndicator: %s\nCode Snippet:\n%s\n---", 
            a.filePath(), a.language(), a.layer(), a.indicator(), a.snippet()))
            .collect(Collectors.joining("\n"));
    
        }
    
        public Map<String, Set<String>> buildDependencyGraph(List<ServiceBoundary> services) {
            Map<String, Set<String>> graph = new HashMap<>();
        
            Map<String, String> serviceLayers = new HashMap<>();
            for (ServiceBoundary service : services) {
                String serviceName = inferServiceName(service.filePath()).toLowerCase().trim();
                serviceLayers.put(serviceName, service.layer().toLowerCase());
            }
        
            Set<String> allServiceNames = serviceLayers.keySet();
        
            for (ServiceBoundary service : services) {
                String serviceName = inferServiceName(service.filePath()).toLowerCase().trim();
                String serviceLayer = serviceLayers.getOrDefault(serviceName, "unknown");
                System.out.println("Processing service: " + serviceName + ", layer: " + serviceLayer);
            
                Set<String> dependencies = graph.computeIfAbsent(serviceName, k -> new HashSet<>());
        
              
                switch (serviceLayer) {
                    case "api":
                        if (allServiceNames.contains("config")) dependencies.add("config");
                        break;
                    case "apigateway":
                        if (allServiceNames.contains("shared")) dependencies.add("shared");
                        break;
                    case "presentation":
                        if (allServiceNames.contains("api")) dependencies.add("api");
                        if (allServiceNames.contains("shared")) dependencies.add("shared");
                        break;
                    case "business":
                        if (allServiceNames.contains("repository")) dependencies.add("repository");
                        if (allServiceNames.contains("shared")) dependencies.add("shared");
                        break;
                    case "repository":
                        if (allServiceNames.contains("entity")) dependencies.add("entity");
                        if (allServiceNames.contains("shared")) dependencies.add("shared");
                        break;
                    case "admin":
                        if (allServiceNames.contains("business")) dependencies.add("business");
                        if (allServiceNames.contains("repository")) dependencies.add("repository");
                        break;
                    default:
                        log.warn("Unknown service layer '{}' for service '{}'", serviceLayer, serviceName);
                        break;
                }
        
            

                System.out.println("Snippet for " + serviceName + ": " + service.snippet());

               
                for (String otherServiceName : allServiceNames) {
                    if (serviceName.equals(otherServiceName)) continue;
            
                    if (service.snippet().toLowerCase().matches(".*\\b" + Pattern.quote(otherServiceName) + "\\b.*")) {
                        System.out.println("Adding dependency from " + serviceName + " to " + otherServiceName + " based on snippet match.");
                        dependencies.add(otherServiceName);
                    }
                }
        
                dependencies.remove(serviceName); 
            }
        
            return graph;
        }
        
    
    
        
        public String generateServiceBoundary(ArchitectureMap map) {
            Map<String, Set<String>> edges = map.serviceCalls;
            Map<String, Integer> inDegree = new HashMap<>();
            for (String s : map.services.keySet()) inDegree.put(s, 0);
            for (Set<String> tos : edges.values()) {
                for (String to : tos) {
                    inDegree.put(to, inDegree.getOrDefault(to, 0) + 1);
                }
            }
        
            Queue<String> queue = new LinkedList<>();
            Map<String, Integer> levels = new HashMap<>();
            for (String s : map.services.keySet()) {
                if (inDegree.get(s) == 0) {
                    queue.add(s);
                    levels.put(s, 0);
                }
            }
        
            while (!queue.isEmpty()) {
                String cur = queue.poll();
                int level = levels.get(cur);
                for (String nxt : edges.getOrDefault(cur, Set.of())) {
                    if (levels.getOrDefault(nxt, -1) < level + 1) {
                        levels.put(nxt, level + 1);
                    }
                    inDegree.put(nxt, inDegree.get(nxt) - 1);
                    if (inDegree.get(nxt) == 0) queue.add(nxt);
                }
            }
        
            for (String s : map.services.keySet()) levels.putIfAbsent(s, 0);
        
            Map<Integer, List<String>> servicesByLevel = new TreeMap<>();
            for (Map.Entry<String, Integer> entry : levels.entrySet()) {
                servicesByLevel.computeIfAbsent(entry.getValue(), k -> new ArrayList<>()).add(entry.getKey());
            }
        
            final int H_SPACING = 6;
            final int V_SPACING = 3;
            List<StringBuilder> lines = new ArrayList<>();
        
            class BoxMetadata {
                int top, left, width, height;
            }
            Map<String, BoxMetadata> boxPositions = new HashMap<>();
        
            int currentRow = 0;
        
            for (List<String> levelServices : servicesByLevel.values()) {
                int maxHeight = 0;
                List<String[]> renderedBoxes = new ArrayList<>();
                List<Integer> boxWidths = new ArrayList<>();
        
                for (String svc : levelServices) {
                    ArchitectureMap.LayeredService layers = map.services.get(svc);
   
        
                    List<List<Object>> layersList = List.of(
                        List.of("Domain", layers.entity, layers.business),
                        List.of("Application", List.of()), 
                        List.of("Infrastructure", layers.repository, layers.shared),
                        List.of("Interface", layers.api, layers.presentation, layers.apiGateway)
                    );
        
                    List<String[]> layerBoxes = new ArrayList<>();

                    for (List<Object> layerInfo : layersList) {
                        String title = (String) layerInfo.get(0);
                        List<String> combined = new ArrayList<>();
                        for (int i = 1; i < layerInfo.size(); i++) {
                            
                            combined.addAll((List<String>)layerInfo.get(i));
                        }
                        layerBoxes.add(generateLayerBox(title, combined).split("\n"));
                    }
        
                    int totalHeight = layerBoxes.stream().mapToInt(b -> b.length).sum() + (layerBoxes.size() - 1);
                    int boxWidth = layerBoxes.stream().mapToInt(b -> b[0].length()).max().orElse(0);
        
                    int innerWidth = layerBoxes.stream().mapToInt(b -> b[0].length()).max().orElse(0);
                    int paddedWidth = Math.min(Math.max(innerWidth + 4, 20), 60); // outer box width
                    
                    List<String> fullStackList = new ArrayList<>();
                    String serviceTitle = svc.length() > paddedWidth - 4 ? svc.substring(0, paddedWidth - 4) : svc;
                    
                    fullStackList.add(drawBoxTop(paddedWidth));
                    fullStackList.add(drawBoxLine(" " + serviceTitle + " ", paddedWidth));
                    
                    for (int i = 0; i < layerBoxes.size(); i++) {
                        String[] layer = layerBoxes.get(i);
                        for (String line : layer) {
                            fullStackList.add(drawBoxLine(line, paddedWidth));
                        }
                    }
                    fullStackList.add(drawBoxBottom(paddedWidth));
                    
                    String[] fullStack = fullStackList.toArray(new String[0]);
                    
        
                    renderedBoxes.add(fullStack);
                    boxWidths.add(boxWidth);
                    maxHeight = Math.max(maxHeight, totalHeight);
                }
        
                while (lines.size() < currentRow + maxHeight + V_SPACING) {
                    lines.add(new StringBuilder());
                }
        
                int currentCol = 0;
                for (int i = 0; i < levelServices.size(); i++) {
                    String svc = levelServices.get(i);
                    String[] box = renderedBoxes.get(i);
                    int boxWidth = boxWidths.get(i);
        
                    for (int j = 0; j < box.length; j++) {
                        StringBuilder line = lines.get(currentRow + j);
                        while (line.length() < currentCol) line.append(' ');
                        line.append(" ".repeat(Math.max(0, currentCol - line.length())));
                        line.append(box[j]);
                    }
        
                    BoxMetadata meta = new BoxMetadata();
                    meta.top = currentRow;
                    meta.left = currentCol;
                    meta.width = boxWidth;
                    meta.height = box.length;
                    boxPositions.put(svc, meta);
        
                    currentCol += boxWidth + H_SPACING;
                }
        
                currentRow += maxHeight + V_SPACING;
            }
        
            BiConsumer<Integer, Integer> ensureLineLength = (row, length) -> {
                while (lines.size() <= row) lines.add(new StringBuilder());
                StringBuilder line = lines.get(row);
                while (line.length() < length) line.append(' ');
            };
        
            Set<String> drawnPairs = new HashSet<>();
            for (Map.Entry<String, Set<String>> edge : edges.entrySet()) {
                String from = edge.getKey();
                for (String to : edge.getValue()) {
                    if (!boxPositions.containsKey(from) || !boxPositions.containsKey(to)) continue;
        
                    BoxMetadata fromBox = boxPositions.get(from);
                    BoxMetadata toBox = boxPositions.get(to);
        
                    int fromMidRow = fromBox.top + fromBox.height / 2;
                    int toMidRow = toBox.top + toBox.height / 2;
                    int fromMidCol = fromBox.left + fromBox.width / 2;
                    int toMidCol = toBox.left + toBox.width / 2;
        
                    String pairKey = from + "->" + to;
                    String revPairKey = to + "->" + from;
        
                    if (levels.get(from).equals(levels.get(to))) {
                        int arrowRow = fromMidRow - 1;
                        if (arrowRow < 0) arrowRow = fromBox.top + fromBox.height;
        
                        ensureLineLength.accept(arrowRow, Math.max(fromMidCol, toMidCol) + 20);
                        StringBuilder line = lines.get(arrowRow);
        
                        int start = Math.min(fromMidCol, toMidCol);
                        int end = Math.max(fromMidCol, toMidCol);
        
                        for (int c = start + 1; c < end; c++) {
                            if (line.charAt(c) == ' ') line.setCharAt(c, '-');
                        }
        
                        if (fromMidCol < toMidCol) {
                            line.setCharAt(toMidCol, '>');
                            line.append(" ").append(from).append(" --> ").append(to);
                        } else {
                            line.setCharAt(toMidCol, '<');
                            line.append(" ").append(to).append(" <-- ").append(from);
                        }
        
                        if (edges.getOrDefault(to, Set.of()).contains(from) && !drawnPairs.contains(revPairKey)) {
                            int arrowRow2 = fromMidRow + fromBox.height;
                            ensureLineLength.accept(arrowRow2, Math.max(fromMidCol, toMidCol) + 20);
                            StringBuilder line2 = lines.get(arrowRow2);
        
                            for (int c = start + 1; c < end; c++) {
                                if (line2.charAt(c) == ' ') line2.setCharAt(c, '-');
                            }
        
                            if (toMidCol < fromMidCol) {
                                line2.setCharAt(fromMidCol, '>');
                                line2.append(" ").append(to).append(" --> ").append(from);
                            } else {
                                line2.setCharAt(fromMidCol, '<');
                                line2.append(" ").append(from).append(" <-- ").append(to);
                            }
                            drawnPairs.add(pairKey);
                            drawnPairs.add(revPairKey);
                        }
        
                    } else {
                        int startRow = fromBox.top + fromBox.height;
                        int endRow = toBox.top - 1;
                        int arrowCol = fromMidCol;
        
                        if (startRow > endRow) {
                            int temp = startRow;
                            startRow = endRow;
                            endRow = temp;
                        }
        
                        for (int r = startRow; r <= endRow; r++) {
                            ensureLineLength.accept(r, arrowCol + 1);
                            StringBuilder line = lines.get(r);
                            line.setCharAt(arrowCol, '|');
                        }
        
                        ensureLineLength.accept(endRow, arrowCol + 20);
                        StringBuilder arrowLine = lines.get(endRow);
                        arrowLine.setCharAt(arrowCol, 'v');
                        arrowLine.append(" ").append(from).append(" --> ").append(to);
                    }
                }
            }
        
            return lines.stream().map(StringBuilder::toString).collect(Collectors.joining("\n"));
        }
        
    
        private String generateLayerBox(String title, List<String> items) {
            List<String> lines = new ArrayList<>();
            lines.add("[" + title + "]");
        
            Set<String> seen = new HashSet<>();
            List<String> cleaned = items.stream()
                .map(String::trim)
                .filter(i -> !i.isEmpty() && !i.contains("package") && !i.contains("Copyright"))
                .filter(seen::add)
                .map(i -> i.length() > MAX_ITEM_LENGTH ? i.substring(0, TRUNCATE_AT) + "..." : i)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .map(i -> "  - " + i)
                .collect(Collectors.toList());
        
            lines.addAll(cleaned);
        
            int maxWidth = lines.stream().mapToInt(String::length).max().orElse(0);
            maxWidth = Math.min(Math.max(maxWidth + 4, 20), 40); // reasonable bounds
        
            StringBuilder sb = new StringBuilder();
            sb.append(drawBoxTop(maxWidth)).append("\n");
            for (String line : lines) {
                sb.append(drawBoxLine(line, maxWidth)).append("\n");
            }
            sb.append(drawBoxBottom(maxWidth));
            return sb.toString();
        }

        
       
        
        private String generateCompactBox(String service, ArchitectureMap.LayeredService layers) {
            List<String> lines = new ArrayList<>();
            lines.add(service);
        
            for (String layer : LAYER_PRIORITY) {
                List<String> items = switch (layer) {
                    case "entity" -> layers.entity;
                    case "repository" -> layers.repository;
                    case "business" -> layers.business;
                    case "api" -> layers.api;
                    case "presentation" -> layers.presentation;
                    case "shared" -> layers.shared;
                    case "apiGateway" -> layers.apiGateway;
                    default -> List.of();
                };
                addLayerLinesCompact(lines, LAYER_LABELS.getOrDefault(layer, layer), items);
            }
            
        
            int maxWidth = lines.stream().mapToInt(String::length).max().orElse(0);
            maxWidth = Math.min(Math.max(maxWidth + 4, 20), 30); 
        
            StringBuilder sb = new StringBuilder();
            sb.append(drawBoxTop(maxWidth)).append("\n");
            for (String line : lines) {
                sb.append(drawBoxLine(line, maxWidth)).append("\n");
            }
            sb.append(drawBoxBottom(maxWidth)).append("\n");
            return sb.toString();
        }
    
        
        public String generateCompactBoxBounded(String contextName, BoundedContext bc) {
            List<String> lines = new ArrayList<>();
            lines.add(contextName);
    
            for (String layer : DDD_LAYER_ORDER) {
                List<String> classes = bc.domainLayers.getOrDefault(layer, List.of());
                addLayerLinesCompact(lines, capitalize(layer), classes);
            }
    
            int maxWidth = lines.stream().mapToInt(String::length).max().orElse(0);
            maxWidth = Math.min(Math.max(maxWidth + 4, 20), 30); 
        
            StringBuilder sb = new StringBuilder();
            sb.append(drawBoxTop(maxWidth)).append("\n");
            for (String line : lines) {
                sb.append(drawBoxLine(line, maxWidth)).append("\n");
            }
            sb.append(drawBoxBottom(maxWidth)).append("\n");
            return sb.toString();
        }
        
        public static Map<String, BoundedContext> convertToBoundedContexts(ArchitectureMap map) {
            Map<String, BoundedContext> result = new LinkedHashMap<>();
            for (var entry : map.services.entrySet()) {
                String serviceName = entry.getKey();
                ArchitectureMap.LayeredService layers = entry.getValue();
                BoundedContext bc = toBoundedContext(serviceName, layers);
                result.put(serviceName, bc);
            }
            return result;
        }
    
    
        public static BoundedContext toBoundedContext(String serviceName, ArchitectureMap.LayeredService svc) {
            BoundedContext bc = new BoundedContext(serviceName);
        List<String> domain = new ArrayList<>();
        domain.addAll(svc.entity);
        domain.addAll(svc.business);
        bc.domainLayers.put("domain", domain);


        bc.domainLayers.put("application", List.of()); 


        List<String> infra = new ArrayList<>();
        infra.addAll(svc.repository);
        infra.addAll(svc.shared);
        bc.domainLayers.put("infrastructure", infra);


        List<String> iface = new ArrayList<>();
        iface.addAll(svc.api);
        iface.addAll(svc.presentation);
        iface.addAll(svc.apiGateway);
        bc.domainLayers.put("interface", iface);

        return bc;
    }





    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0,1).toUpperCase() + s.substring(1);
    }

    private String truncate(String s, int maxLength) {
        return s.length() <= maxLength ? s : s.substring(0, maxLength - 3) + "...";
    }
    
 


private void addLayerLinesCompact(List<String> lines, String layerName, List<String> items) {
    if (items == null || items.isEmpty()) return;

    Set<String> seen = new HashSet<>();
    List<String> cleanedItems = items.stream()
        .map(String::trim)
        .filter(item -> !(item.contains("Copyright") || item.contains("package ")))
        .filter(seen::add)
        .map(item -> item.length() > MAX_ITEM_LENGTH ? item.substring(0, TRUNCATE_AT) + "..." : item)
        .sorted(String.CASE_INSENSITIVE_ORDER)
        .map(item -> "  - " + item)
        .collect(Collectors.toList());

    if (!cleanedItems.isEmpty()) {
        lines.add(layerName + ":");
        lines.addAll(cleanedItems);
    }
}

public String generateSharedKernelBox(List<String> sharedItems) {
    List<String> lines = new ArrayList<>();
    lines.add("Shared Kernel");
    lines.add("+ Shared Definitions");

    for (String item : sharedItems) {
        lines.add("| - " + item);
    }

    int maxWidth = lines.stream().mapToInt(String::length).max().orElse(0);
    maxWidth = Math.min(Math.max(maxWidth + 4, 20), 40); 

    StringBuilder sb = new StringBuilder();
    sb.append(drawBoxTop(maxWidth)).append("\n");
    for (String line : lines) {
        sb.append(drawBoxLine(line, maxWidth)).append("\n");
    }
    sb.append(drawBoxBottom(maxWidth)).append("\n");
    return sb.toString();
}


    
    
    
    private String drawBoxTop(int width) {
        return "+" + "-".repeat(width - 2) + "+";
    }
    
    private String drawBoxBottom(int width) {
        return "+" + "-".repeat(width - 2) + "+";
    }
    
    private String drawBoxLine(String content, int width) {
        if (content == null) content = "";
        if (content.length() > width - 4) {
            content = content.substring(0, width - 7) + "...";
        }
        return "| " + padRight(content, width - 4) + " |";
    }
    

    private String padRight(String str, int width) {
        if (str.length() >= width) return str;
        return str + " ".repeat(width - str.length());
    }
    

    
    private String padCenter(String text, int width) {
        int padding = width - text.length();
        int padStart = padding / 2;
        int padEnd = padding - padStart;
        return " ".repeat(padStart) + text + " ".repeat(padEnd);
    }


    
    public List<String> detectAntiPatterns(ArchitectureMap map) {
        List<String> issues = new ArrayList<>();
    
        // 1. Wrong Cuts
        boolean mostlyLayered = true;
        for (String svc : map.services.keySet()) {
            ArchitectureMap.LayeredService layers = map.services.get(svc);
            if (!(layers.api != null && layers.business != null && layers.entity != null)) {
                mostlyLayered = false;
                break;
            }
        }
        if (mostlyLayered) {
            issues.add("Wrong Cuts");
        }
    
     // 2. Shared Persistency
        Map<String, Set<String>> repoToServices = new HashMap<>();
        for (Map.Entry<String, ArchitectureMap.LayeredService> entry : map.services.entrySet()) {
            ArchitectureMap.LayeredService layers = entry.getValue();
            if (layers.entity != null | layers.business != null) {
 
                for (String repo : layers.entity) {
                    repoToServices.computeIfAbsent(repo, k -> new HashSet<>()).add(entry.getKey());
                }

                for (String repo : layers.business) {
                    repoToServices.computeIfAbsent(repo, k -> new HashSet<>()).add(entry.getKey());
                }
            }
        }

        for (Map.Entry<String, Set<String>> e : repoToServices.entrySet()) {
            if (e.getValue().size() > 1) {
                issues.add("Shared Persistency'" + e.getKey() +
                           "' is used by multiple services: " + e.getValue());
            }
        }

        
    
        // 3. Inappropriate Service Intimacy
        for (Map.Entry<String, Set<String>> entry : map.serviceCalls.entrySet()) {
            String from = entry.getKey();
            ArchitectureMap.LayeredService fromLayers = map.services.get(from);
            for (String to : entry.getValue()) {
                ArchitectureMap.LayeredService toLayers = map.services.get(to);
                if (toLayers != null && (toLayers.business != null || toLayers.entity != null)) {
                    if (!from.equals(to)) {
                        issues.add("Inappropriate Service Intimacy'" + from +
                                   "' accesses internal layers of '" + to + "'");
                    }
                }
            }
        }

        int tightCouplings = 0;
        for (String s1 : map.serviceCalls.keySet()) {
            for (String s2 : map.serviceCalls.getOrDefault(s1, Set.of())) {
                if (map.serviceCalls.containsKey(s2) && map.serviceCalls.get(s2).contains(s1)) {
                    tightCouplings++;
                }
            }
        }

        if (tightCouplings > map.serviceCalls.size()) {
            issues.add("Distributed Monolith.");
        }


        if (hasCycles(map.serviceCalls)) {
            issues.add("Anti-Pattern: Cyclic Dependency — service calls form dependency loops.");
        }

        int totalEdges = map.serviceCalls.values().stream().mapToInt(Set::size).sum();
        int maxEdges = map.services.size() * (map.services.size() - 1);
        if (map.services.size() > 3 && totalEdges > 0.7 * maxEdges) {
            issues.add("Anti-Pattern: Spaghetti Architecture — services are excessively interconnected.");
        }

        long tinyServices = map.services.values().stream()
        .filter(svc -> (svc.api == null ? 0 : svc.api.size()) +
                       (svc.business == null ? 0 : svc.business.size()) +
                       (svc.entity == null ? 0 : svc.entity.size()) < 3)
        .count();
    if (tinyServices > (map.services.size() * 0.5)) {
    issues.add("Anti-Pattern: Over-Microservices — many services have too few responsibilities.");
}

for (Map.Entry<String, ArchitectureMap.LayeredService> entry : map.services.entrySet()) {
    Set<String> allClasses = new HashSet<>();
    if (entry.getValue().api != null) allClasses.addAll(entry.getValue().api);
    if (entry.getValue().business != null) allClasses.addAll(entry.getValue().business);
    if (entry.getValue().entity != null) allClasses.addAll(entry.getValue().entity);
    boolean mixedDomains = allClasses.stream().anyMatch(cls ->
            cls.toLowerCase().contains("user") &&
            allClasses.stream().anyMatch(c -> c.toLowerCase().contains("order") || c.toLowerCase().contains("payment")));
    if (mixedDomains) {
        issues.add("Violating Single Responsibility — service '" + entry.getKey() +
                "' mixes unrelated domain concepts.");
    }
}

for (Map.Entry<String, Set<String>> entry : map.serviceCalls.entrySet()) {
    long chats = entry.getValue().stream()
            .filter(target -> Collections.frequency(new ArrayList<>(entry.getValue()), target) > 3)
            .count();
    if (chats > 0) {
        issues.add("Anti-Pattern: Chatty Communication — service '" + entry.getKey() +
                "' has high-frequency calls to other services.");
    }
}

for (String service : map.services.keySet()) {
    String lower = service.toLowerCase();
    if (lower.contains("controller") || lower.contains("util") || lower.contains("helper")) {
        issues.add("Anti-Pattern: Inadequate Service Boundaries — service '" + service +
                "' reflects technical structure, not business domain.");
    }
}

    
        return issues;
    }
    
    

    
    public List<String> getAntiPatternWarnings(Map<String, String> files) throws IOException {
        List<ServiceBoundary> boundaries = extractFiles(files);
        ArchitectureMap map = fallback(boundaries);
        map.serviceCalls = inferServiceRelations(map);
        return detectAntiPatterns(map);
    }
    

    public ArchitectureMap fallback(List<ServiceBoundary> artifacts){
        ArchitectureMap map = new ArchitectureMap();
    
        for (ServiceBoundary artifact : artifacts){
            String serviceName = inferServiceName(artifact.filePath());
            ArchitectureMap.LayeredService layers = map.services.computeIfAbsent(serviceName, k -> new ArchitectureMap.LayeredService());
            

      
            System.out.println("File: " + artifact.filePath() + ", Layer: " + artifact.layer() + ", Snippet: " + artifact.snippet());
    
            String snippet = artifact.snippet();
String fileName = artifact.filePath()
    .substring(artifact.filePath().lastIndexOf("/") + 1)
    .replace(".java", "");

String entry = "[" + snippet + "]" + fileName;

switch (artifact.layer()) {
    case "presentation" -> {
        if (!layers.presentation.contains(entry)) layers.presentation.add(entry);
        if (!layers.presentation.contains(fileName)) layers.presentation.add(fileName);
    }
    case "api" -> {
        if (!layers.api.contains(entry)) layers.api.add(entry);
        if (!layers.api.contains(fileName)) layers.api.add(fileName);
    }
    case "business" -> {
        if (!layers.business.contains(entry)) layers.business.add(entry);
        if (!layers.business.contains(fileName)) layers.business.add(fileName);
    }
    case "entity" -> {
        if (!layers.entity.contains(entry)) layers.entity.add(entry);
        if (!layers.entity.contains(fileName)) layers.entity.add(fileName);
    }
    case "apiGateway" -> {
        if (!layers.apiGateway.contains(entry)) layers.apiGateway.add(entry);
        if (!layers.apiGateway.contains(fileName)) layers.apiGateway.add(fileName);
    }
    case "repository" -> {
        if (!layers.repository.contains(entry)) layers.repository.add(entry);
        if (!layers.repository.contains(fileName)) layers.repository.add(fileName);
    }
    case "shared" -> {
        if (!layers.shared.contains(entry)) layers.shared.add(entry);
        if (!layers.shared.contains(fileName)) layers.shared.add(fileName);
    }
}

            
        }
        
    
        System.out.println("Service Calls:");
        map.serviceCalls.forEach((k, v) -> System.out.println(k + " -> " + v));

        map.serviceCalls = inferServiceRelations(map);
    
        return map;
    }
    

   
    private String inferServiceName(String filePath) {
        String[] parts = filePath.replace("\\", "/").split("/");
        if (parts.length < 2) {
            return "UnknownService";
        }

        return parts[parts.length - 2];
    }
    

    

    public String printServiceBoundary(Map<String, String> files) throws IOException {
        StringBuilder sb = new StringBuilder();
        List<ServiceBoundary> boundaries = extractFiles(files);
        ArchitectureMap map = fallback(boundaries);
        Map<String, Set<String>> graph = buildDependencyGraph(boundaries);
        
        sb.append("\nDependency Graph:\n");
        graph.forEach((k, v) -> sb.append(k).append(" -> ").append(v).append("\n"));
    
        Map<String, Set<String>> inferredCalls = inferServiceRelations(map);
        map.serviceCalls = inferredCalls;
        
        sb.append("\nService Calls:\n");
        map.serviceCalls.forEach((k, v) -> sb.append(k).append(" -> ").append(v).append("\n"));

        List<String> antiPatterns = detectAntiPatterns(map);
        Set<String> uniqueAntiPatterns = new LinkedHashSet<>(antiPatterns); 

        sb.append("\nAnti-Patterns Detected:\n");
        if (uniqueAntiPatterns.isEmpty()) {
            sb.append("None! \n");
        } else {
            uniqueAntiPatterns.forEach(ap -> sb.append("Anti-Pattern: ").append(ap).append("\n"));
        }
    
    
        sb.append("\n--- Boundary Context Diagram ---");
        sb.append(generateServiceBoundary(map)).append("\n");;
    
        sb.append("\n--- Bounded Contexts (DDD-style) ---");
        sb.append(printBoundedContextDiagrams(map)).append("\n");;
     

        return sb.toString();
    }
    

    public String printBoundedContextDiagrams(ArchitectureMap map) {
        StringBuilder sb = new StringBuilder();
        Map<String, BoundedContext> contexts = convertToBoundedContexts(map);
        contexts.forEach((name, bc) -> {
           sb.append(generateCompactBoxBounded(name, bc));
        });

        return sb.toString();
    }
   

    public Map<String, Set<String>> inferServiceRelations(ArchitectureMap map) {
        Map<String, Set<String>> detectedCalls = new HashMap<>();
  
        Map<String, String> classToService = new HashMap<>();
        for (var entry : map.services.entrySet()) {
            String service = entry.getKey();
            ArchitectureMap.LayeredService layers = entry.getValue();
            List<String> allClasses = new ArrayList<>();
            allClasses.addAll(layers.api);
            allClasses.addAll(layers.business);
            allClasses.addAll(layers.presentation);
            allClasses.addAll(layers.entity);
            allClasses.addAll(layers.apiGateway);
            allClasses.addAll(layers.repository);
            allClasses.addAll(layers.shared);
            for (String cls : allClasses) {
                classToService.put(cls.toLowerCase(), service);
            }
        }
    
        for (var entry : map.services.entrySet()) {
            String serviceA = entry.getKey();
            ArchitectureMap.LayeredService layersA = entry.getValue();
    
            List<String> snippetsA = new ArrayList<>();
            snippetsA.addAll(layersA.api);
            snippetsA.addAll(layersA.business);
            snippetsA.addAll(layersA.presentation);
            snippetsA.addAll(layersA.entity);
            snippetsA.addAll(layersA.apiGateway);
            snippetsA.addAll(layersA.repository);
            snippetsA.addAll(layersA.shared);
    
            Set<String> calls = new HashSet<>();
    
            for (String snippet : snippetsA) {
                String snippetLower = snippet.toLowerCase();
                for (String cls : classToService.keySet()) {
                    if (snippetLower.contains(cls) && !classToService.get(cls).equals(serviceA)) {
                        calls.add(classToService.get(cls));
                    }
                }
            }
    
            if (!calls.isEmpty()) {
                detectedCalls.put(serviceA, calls);
            }
        }
    
        return detectedCalls;
    }
    
    public static Map<String, Set<String>> assignClassesUniqueLayer(Map<String, Set<String>> layered) {
        Map<String, Set<String>> result = new HashMap<>();
        Set<String> assignedClasses = new HashSet<>();
    
        for (String layer : LAYER_PRIORITY) {
            Set<String> classes = layered.getOrDefault(layer, Set.of());
            for (String cls : classes) {
                if (assignedClasses.add(cls)) {
                    result.computeIfAbsent(layer, k -> new HashSet<>()).add(cls);
                }
            }
        }
        return result;
    }
    
    private boolean dependsOnAny(Set<String> serviceClasses, Set<String> globalClasses) {
        if (serviceClasses == null) return false;
        for (String cls : serviceClasses) {
            if (globalClasses.contains(cls)) return true;
        }
        return false;
    }
    
    public static List<String> analyzeDependencies(Map<String, Set<String>> dependencyGraph) {
        List<String> issues = new ArrayList<>();
        try {
            if (hasCycles(dependencyGraph)) {
                issues.add("High Severity: Circular dependency detected among services.");
            }
        } catch (Exception e) {
            e.printStackTrace(); 
        }
    
        Map<String, Integer> inDegree = new HashMap<>();
        for (var entry : dependencyGraph.entrySet()) {
            for (String dep : entry.getValue()) {
                inDegree.put(dep, inDegree.getOrDefault(dep, 0) + 1);
            }
        }
    
        int bottleneckThreshold = 5;
        inDegree.forEach((service, count) -> {
            if (count > bottleneckThreshold) {
                issues.add("Medium Severity: Service '" + service + "' is a dependency bottleneck (depended on by " + count + " services).");
            }
        });
    
        return issues;
    }
    
    private static boolean hasCycles(Map<String, Set<String>> graph) {
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();
    
        for (String node : graph.keySet()) {
            if (detectCycleDFS(node, graph, visited, recursionStack)) {
                return true;
            }
        }
        return false;
    }
    
    private static boolean detectCycleDFS(String current, Map<String, Set<String>> graph, Set<String> visited, Set<String> stack) {
        if (stack.contains(current)) return true;
        if (visited.contains(current)) return false;
    
        visited.add(current);
        stack.add(current);
    
        for (String neighbor : graph.getOrDefault(current, Set.of())) {
            if (detectCycleDFS(neighbor, graph, visited, stack)) return true;
        }
    
        stack.remove(current);
        return false;
    }
    
}
