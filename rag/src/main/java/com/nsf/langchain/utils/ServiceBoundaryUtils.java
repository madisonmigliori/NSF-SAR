package com.nsf.langchain.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Provider.Service;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.testcontainers.shaded.org.apache.commons.lang3.arch.Processor.Arch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class ServiceBoundaryUtils {

    public record ServiceBoundary(String filePath, String language,String layer, String indicator, String snippet){}

    private static final Pattern API_PATTERN = Pattern.compile(
        "(?i)(@RestController|@Controller|@RequestMapping|@GetMapping|@PostMapping" +
        "|@app\\.route\\(|@blueprint\\.route\\(|routes\\.draw" +
        "|express\\.Router|router\\.|http\\.HandleFunc|mux\\.HandleFunc|gin\\.Engine" +
        "|#\\[get\\(|#\\[post\\(|Controller\\b|route\\b|endpoint\\b|handler\\b)"
    );
    

    private static final Pattern BUSINESS_PATTERN = Pattern.compile(
        "(?i)\\b(@Service|@Component|Service|UseCase|UseCases|BusinessLogic|Manager|Handler|Interactor|\\w+Service|\\w+Manager|\\w+Interactor|\\w+Handler)\\b"
    );
    

private static final Pattern ENTITY_PATTERN = Pattern.compile(
    "(?i)\\b(@Entity|Entity\\b|Model\\b|Schema\\b|DAO\\b|sql\\b|ORM\\b|sqlalchemy|diesel::|Mongoose|ActiveRecord|GORM|pydantic|Prisma)\\b"
);

private static final Pattern REPOSITORY_PATTERN = Pattern.compile(
    "(?i)\\b(@Repository|Repository\\b|CrudRepository\\b|JpaRepository\\b)\\b"
);

private static final Pattern API_GATEWAY_PATTERN = Pattern.compile(
    "(?i)\\b(ApiGateway|Gateway|Proxy|Filter|Router|LoadBalancer|Zuul|SpringCloudGateway)\\b"
);


private static final Pattern PRESENTATION_PATTERN = Pattern.compile(
    "(?i)\\b(View\\b|Page\\b|Component\\b|template\\b|render\\b|React\\b|Angular\\b|Vue\\b|useEffect\\b|useState\\b|Fragment\\b" +
    "|createElement|export default\\b|FlaskForm|yew::|\\.jsx|\\.tsx|\\.vue|Screen\\b)\\b"
);

private static final Pattern SHARED_PATTERN = Pattern.compile(
    "(?i)\\b(Logger\\b|Validator\\b|Utils?\\b|Helper\\b|Exception\\b|Error\\b|Config\\b|Constants\\b|Settings\\b" +
    "|Common\\b|Middleware\\b|Filter\\b|Interceptor\\b|Adapter\\b|Transformer\\b|Bridge\\b|Base\\b|Shared\\b|Wrapper\\b|Factory\\b|Strategy\\b)\\b"
);

    


    public static class ArchitectureMap{
        public static class LayeredService {
            public List<String> presentation = new ArrayList<>();
            public List<String> api = new ArrayList<>();
            public List<String> business = new ArrayList<>();
            public List<String> entity = new ArrayList<>();
            public List<String> repository = new ArrayList<>();
            public List<String> apiGateway = new ArrayList<>();
            public List<String> shared = new ArrayList<>();
        }

        
        public Map<String, LayeredService> services = new HashMap<>();
        public Map<String, Set<String>> serviceCalls = new LinkedHashMap<>();

        public static ArchitectureMap fromJson(String json) throws IOException {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);
            ArchitectureMap map = new ArchitectureMap();

            Iterator<String> fieldNames = root.fieldNames();
            while(fieldNames.hasNext()){
                String serviceName = fieldNames.next();
                JsonNode serviceNode = root.get(serviceName);

                LayeredService service = new LayeredService();
                service.presentation = extractList(serviceNode, "presentation");
                service.api = extractList(serviceNode, "api");
                service.business = extractList(serviceNode, "business");
                service.entity = extractList(serviceNode, "entity");
                service.repository = extractList(serviceNode, "repository");
                service.apiGateway = extractList(serviceNode, "apiGateway");
                service.shared = extractList(serviceNode, "shared");


                map.services.put(serviceName, service);


            }
            return map;
        }

        public static List<String> extractList(JsonNode node, String fieldName){
            List<String> list = new ArrayList<>();
            if (node.has(fieldName)) {
                for(JsonNode item : node.get(fieldName)) {
                    list.add(item.asText());
                }
            }
            return list;
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
    if (BUSINESS_PATTERN.matcher(trimmed).find()) return "business";
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
            "express\\.Router\\(\\)", "app\\.get\\(", "app\\.post\\(", "router\\.\\w+\\(", 
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




    
    public String generateBoundaryContextDiagram(ArchitectureMap map) {
        Set<String> includedServices = map.services.keySet();
    
        Map<String, Set<String>> serviceToShared = new HashMap<>();
        Map<String, Set<String>> edges = new HashMap<>();
    
  
        for (Map.Entry<String, ArchitectureMap.LayeredService> entry : map.services.entrySet()) {
            String service = entry.getKey();
            if (!includedServices.contains(service)) continue;
    
            Set<String> sharedItems = new HashSet<>(entry.getValue().shared);
            serviceToShared.put(service, sharedItems);
        }
    
    
        for (Map.Entry<String, Set<String>> entryA : serviceToShared.entrySet()) {
            String serviceA = entryA.getKey();
            Set<String> sharedA = entryA.getValue();
    
            for (Map.Entry<String, Set<String>> entryB : serviceToShared.entrySet()) {
                String serviceB = entryB.getKey();
                if (serviceA.equals(serviceB)) continue;
    
                Set<String> sharedB = entryB.getValue();
                Set<String> intersection = new HashSet<>(sharedA);
                intersection.retainAll(sharedB);
    
                if (!intersection.isEmpty()) {
                    edges.computeIfAbsent(serviceA, k -> new HashSet<>()).add(serviceB);
                }
            }
        }
    
        StringBuilder sb = new StringBuilder();
        for (String service : includedServices) {
            sb.append("+------------------+\n");
            sb.append("| ").append(padCenter(service, 18)).append(" |\n");
            sb.append("+------------------+\n");
    
            ArchitectureMap.LayeredService layers = map.services.get(service);
            if (layers != null) {
                if (!layers.api.isEmpty()) {
                    sb.append("api:\n");
                    for (String snippet : layers.api) {
                        sb.append("  - ").append(snippet).append("\n");
                    }
                }
                if (!layers.business.isEmpty()) {
                    sb.append("business:\n");
                    for (String snippet : layers.business) {
                        sb.append("  - ").append(snippet).append("\n");
                    }
                }
                if (!layers.presentation.isEmpty()) {
                    sb.append("presentation:\n");
                    for (String snippet : layers.presentation) {
                        sb.append("  - ").append(snippet).append("\n");
                    }
                }
                if (!layers.entity.isEmpty()) {
                    sb.append("entity:\n");
                    for (String snippet : layers.entity) {
                        sb.append("  - ").append(snippet).append("\n");
                    }
                }
                if (!layers.apiGateway.isEmpty()) {
                    sb.append("apiGateway:\n");
                    for (String snippet : layers.apiGateway) {
                        sb.append("  - ").append(snippet).append("\n");
                    }
                }
                if (!layers.repository.isEmpty()) {
                    sb.append("repository:\n");
                    for (String snippet : layers.repository) {
                        sb.append("  - ").append(snippet).append("\n");
                    }
                }
                if (!layers.shared.isEmpty()) {
                    sb.append("shared:\n");
                    for (String snippet : layers.shared) {
                        sb.append("  - ").append(snippet).append("\n");
                    }
                }
            }
    
            Set<String> targets = edges.getOrDefault(service, new HashSet<>());
            if (!targets.isEmpty()) {
                sb.append("uses:\n");
                for (String target : targets) {
                    sb.append("  -> ").append(target).append("\n");
                }
            }
            sb.append("\n");
        }
    
        return sb.toString();
    }
    

    public String generateLayeredDiagramWithMultiArrowsCompact(ArchitectureMap map) {
        Set<String> services = map.services.keySet();
    
        Map<String, Set<String>> edges = new LinkedHashMap<>();
        System.out.println("Service Calls:");
map.serviceCalls.forEach((k, v) -> System.out.println(k + " -> " + v));

        if (map.serviceCalls != null && !map.serviceCalls.isEmpty()) {
            for (Map.Entry<String, Set<String>> entry : map.serviceCalls.entrySet()) {
                String from = entry.getKey();
                for (String to : entry.getValue()) {
                    if (!from.equals(to)) {
                        edges.computeIfAbsent(from, k -> new HashSet<>()).add(to);
                    }
                }
            }
        } else {
            Map<String, Set<String>> serviceToShared = new HashMap<>();
            for (String service : services) {
                serviceToShared.put(service, new HashSet<>(map.services.get(service).shared));
            }
            for (String a : services) {
                for (String b : services) {
                    if (a.equals(b)) continue;
                    Set<String> sharedA = serviceToShared.getOrDefault(a, Set.of());
                    Set<String> sharedB = serviceToShared.getOrDefault(b, Set.of());
                    Set<String> intersection = new HashSet<>(sharedA);
                    intersection.retainAll(sharedB);
                    if (!intersection.isEmpty()) {
                        edges.computeIfAbsent(a, k -> new HashSet<>()).add(b);
                    }
                }
            }
        }
    
    
        Map<String, Integer> inDegree = new HashMap<>();
        for (String s : services) inDegree.put(s, 0);
        for (Set<String> tos : edges.values()) {
            for (String to : tos) {
                inDegree.put(to, inDegree.getOrDefault(to, 0) + 1);
            }
        }
    
        Queue<String> queue = new LinkedList<>();
        Map<String, Integer> levels = new HashMap<>();
    
        for (String s : services) {
            if (inDegree.get(s) == 0) {
                queue.add(s);
                levels.put(s, 0);
            }
        }
    
        while (!queue.isEmpty()) {
            String cur = queue.poll();
            int level = levels.get(cur);
            for (String nxt : edges.getOrDefault(cur, Set.of())) {
                int prev = levels.getOrDefault(nxt, -1);
                if (level + 1 > prev) {
                    levels.put(nxt, level + 1);
                }
                inDegree.put(nxt, inDegree.get(nxt) - 1);
                if (inDegree.get(nxt) == 0) queue.add(nxt);
            }
        }
    
        for (String s : services) {
            levels.putIfAbsent(s, 0);
        }
    
        Map<Integer, List<String>> servicesByLevel = new TreeMap<>();
        for (Map.Entry<String, Integer> entry : levels.entrySet()) {
            servicesByLevel.computeIfAbsent(entry.getValue(), k -> new ArrayList<>()).add(entry.getKey());
        }
    
        StringBuilder sb = new StringBuilder();
    
        for (Map.Entry<Integer, List<String>> levelEntry : servicesByLevel.entrySet()) {
            int level = levelEntry.getKey();
            List<String> svcAtLevel = levelEntry.getValue();
    
            sb.append("Level ").append(level).append(":\n");
            for (String svc : svcAtLevel) {
                ArchitectureMap.LayeredService layers = map.services.get(svc);
                String box = generateCompactBox(svc, layers);
                String indent = " ".repeat(level * 4);
                for (String line : box.split("\n")) {
                    sb.append(indent).append(line).append("\n");
                }
                sb.append("\n");
            }
        }
    
        sb.append("Relations:\n");
        for (Map.Entry<String, Set<String>> edge : edges.entrySet()) {
            int fromLevel = levels.getOrDefault(edge.getKey(), 0);
            String indent = " ".repeat(fromLevel * 4);
            for (String to : edge.getValue()) {
                sb.append(indent).append(edge.getKey()).append(" ---> ").append(to).append("\n");
            }
        }
    
        return sb.toString();
    }

    public String generateFlatDiagramWithClearArrows(ArchitectureMap map) {
        Set<String> services = map.services.keySet();
        Map<String, Set<String>> edges = map.serviceCalls;
    
        // Generate boxes & record their positions
        Map<String, String[]> boxLines = new LinkedHashMap<>();
        Map<String, Integer> boxWidths = new LinkedHashMap<>();
        int boxHeight = 0;
    
        for (String svc : services) {
            ArchitectureMap.LayeredService layers = map.services.get(svc);
            String box = generateCompactBox(svc, layers);
            String[] lines = box.split("\n");
            boxLines.put(svc, lines);
            boxWidths.put(svc, lines[0].length());
            boxHeight = Math.max(boxHeight, lines.length);
        }
    
        // Spacing between boxes
        int spacing = 4;
    
        // Calculate total width and each box's start position
        Map<String, Integer> positions = new LinkedHashMap<>();
        int currentPos = 0;
        for (String svc : services) {
            positions.put(svc, currentPos);
            currentPos += boxWidths.get(svc) + spacing;
        }
    
        // Build output line by line for boxes
        StringBuilder sb = new StringBuilder();
    
        for (int lineIndex = 0; lineIndex < boxHeight; lineIndex++) {
            StringBuilder line = new StringBuilder();
            for (String svc : services) {
                String[] lines = boxLines.get(svc);
                int width = boxWidths.get(svc);
                if (lineIndex < lines.length) {
                    line.append(lines[lineIndex]);
                } else {
                    line.append(" ".repeat(width));
                }
                line.append(" ".repeat(spacing));
            }
            sb.append(line.toString()).append("\n");
        }
    
        // Blank line between boxes and arrows
        sb.append("\n");
    
        // Draw arrows with labels below boxes
        for (String from : edges.keySet()) {
            for (String to : edges.get(from)) {
                int fromPos = positions.get(from);
                int toPos = positions.get(to);
    
                int fromCenter = fromPos + boxWidths.get(from) / 2;
                int toCenter = toPos + boxWidths.get(to) / 2;
    
                // Calculate arrow length and direction
                StringBuilder arrowLine = new StringBuilder(" ".repeat(currentPos));
    
                if (fromCenter < toCenter) {
                    // arrow right
                    for (int i = fromCenter; i < toCenter; i++) arrowLine.setCharAt(i, '-');
                    arrowLine.setCharAt(toCenter, '>');
                } else if (fromCenter > toCenter) {
                    // arrow left
                    for (int i = toCenter + 1; i < fromCenter; i++) arrowLine.setCharAt(i, '-');
                    arrowLine.setCharAt(toCenter, '<');
                } else {
                    // same center, just arrow head
                    arrowLine.setCharAt(toCenter, '*');
                }
    
                sb.append(arrowLine.toString()).append("\n");
    
                int labelStart = Math.min(fromCenter, toCenter);
                int labelEnd = Math.max(fromCenter, toCenter);
                int labelWidth = labelEnd - labelStart + 1;
                String label = from + " ---> " + to;
    
                if (label.length() > labelWidth) {
                    label = label.substring(0, labelWidth);
                }
    
                StringBuilder labelLine = new StringBuilder(" ".repeat(currentPos));
                int labelPos = labelStart + (labelWidth - label.length()) / 2;
    
                for (int i = 0; i < label.length(); i++) {
                    labelLine.setCharAt(labelPos + i, label.charAt(i));
                }
    
                sb.append(labelLine.toString()).append("\n\n");
            }
        }
    
        return sb.toString();
    }


    public String generateDiagramWithMultiArrowsBetweenBoxes(ArchitectureMap map) {
        List<String> svcList = new ArrayList<>(map.services.keySet());
        Map<String, Set<String>> edges = map.serviceCalls;
    
        Map<String, String[]> boxLines = new LinkedHashMap<>();
        Map<String, Integer> boxWidths = new LinkedHashMap<>();
        int maxHeight = 0;
    
        // Prepare boxes & track max height
        for (String svc : svcList) {
            ArchitectureMap.LayeredService layers = map.services.get(svc);
            String box = generateCompactBox(svc, layers);
            String[] lines = box.split("\n");
            boxLines.put(svc, lines);
            boxWidths.put(svc, lines[0].length());
            maxHeight = Math.max(maxHeight, lines.length);
        }
    
        // Normalize box heights
        for (String svc : svcList) {
            String[] lines = boxLines.get(svc);
            if (lines.length < maxHeight) {
                String[] padded = new String[maxHeight];
                int padTop = (maxHeight - lines.length) / 2;
                int padBottom = maxHeight - lines.length - padTop;
                int idx = 0;
                for (; idx < padTop; idx++) padded[idx] = " ".repeat(boxWidths.get(svc));
                System.arraycopy(lines, 0, padded, idx, lines.length);
                idx += lines.length;
                for (; idx < maxHeight; idx++) padded[idx] = " ".repeat(boxWidths.get(svc));
                boxLines.put(svc, padded);
            }
        }
    
        // Space between boxes horizontally
        int spacing = 6;
    
        // Arrow lines count between boxes (max number of arrows between those two boxes)
        int maxArrowLines = 3; // max vertical arrows you want to support, can adjust
    
        // Build diagram lines here
        List<StringBuilder> outputLines = new ArrayList<>();
    
        // For each box line + arrow lines except after last box line
        for (int lineIdx = 0; lineIdx < maxHeight; lineIdx++) {
            StringBuilder line = new StringBuilder();
    
            for (int i = 0; i < svcList.size(); i++) {
                String svc = svcList.get(i);
                line.append(boxLines.get(svc)[lineIdx]);
                if (i < svcList.size() - 1) {
                    // Add spacing for arrows area (arrows are on separate lines)
                    line.append(" ".repeat(spacing));
                }
            }
            outputLines.add(line);
            
            // Now add arrow lines **after** this box line if it’s the vertical middle of the box
            if (lineIdx == maxHeight / 2) {
                // We'll add multiple arrow lines vertically between boxes here
                for (int arrowLineIdx = 0; arrowLineIdx < maxArrowLines; arrowLineIdx++) {
                    StringBuilder arrowLine = new StringBuilder();
    
                    for (int i = 0; i < svcList.size(); i++) {
                        int boxWidth = boxWidths.get(svcList.get(i));
                        // For each box, add spaces for box width
                        arrowLine.append(" ".repeat(boxWidth));
                        if (i < svcList.size() - 1) {
                            // Calculate arrows between svc[i] and svc[i+1]
                            String from = svcList.get(i);
                            String to = svcList.get(i + 1);
    
                            Set<String> fromTo = edges.getOrDefault(from, Set.of());
                            Set<String> toFrom = edges.getOrDefault(to, Set.of());
    
                            // We'll draw one arrow per arrowLineIdx if available
                            // Collect all arrows as lists
                            List<String> rightArrows = new ArrayList<>();
                            List<String> leftArrows = new ArrayList<>();
    
                            // For simplicity, one arrow per line max
                            // But if you want multiple arrows between same services,
                            // you can store those in map and draw here accordingly
                            // For now: maxArrowLines = max arrows to draw
    
                            if (arrowLineIdx == 0) {
                                // For example, first arrow line: draw right arrow if exists
                                if (fromTo.contains(to)) rightArrows.add("-->");
                                else rightArrows.add("   ");
                                if (toFrom.contains(from)) leftArrows.add("<--");
                                else leftArrows.add("   ");
                            } else if (arrowLineIdx == 1) {
                                // second line: no arrow or optional extension if you track multiple arrows
                                rightArrows.add("   ");
                                leftArrows.add("   ");
                            } else {
                                rightArrows.add("   ");
                                leftArrows.add("   ");
                            }
    
                            // Compose arrow line content:
                            // If both directions, draw double arrow centered in spacing
                            String arrowStr;
                            if (rightArrows.get(0).equals("-->") && leftArrows.get(0).equals("<--")) {
                                // double arrow centered in spacing
                                int leftSpaces = spacing / 2 - 2;
                                int rightSpaces = spacing - leftSpaces - 4;
                                arrowStr = " ".repeat(leftSpaces) + "<->" + " ".repeat(rightSpaces);
                            } else if (rightArrows.get(0).equals("-->")) {
                                // right arrow aligned right side
                                arrowStr = "-".repeat(spacing - 1) + ">";
                            } else if (leftArrows.get(0).equals("<--")) {
                                // left arrow aligned left side
                                arrowStr = "<" + "-".repeat(spacing - 1);
                            } else {
                                arrowStr = " ".repeat(spacing);
                            }
                            arrowLine.append(arrowStr);
                        }
                    }
                    outputLines.add(arrowLine);
                }
            }
        }
    
    
        return outputLines.stream().map(StringBuilder::toString).collect(Collectors.joining("\n"));
    }
    
    public String generateFlatDiagramWithMultiArrows(ArchitectureMap map) {
        List<String> services = new ArrayList<>(map.services.keySet());
        Map<String, Set<String>> edges = map.serviceCalls;
        Map<String, String[]> boxLines = new LinkedHashMap<>();
        Map<String, Integer> boxWidths = new LinkedHashMap<>();
        int boxHeight = 0;
    
        for (String svc : services) {
            ArchitectureMap.LayeredService layers = map.services.get(svc);
            String box = generateCompactBox(svc, layers);
            String[] lines = box.split("\n");
            boxLines.put(svc, lines);
            boxWidths.put(svc, lines[0].length());
            boxHeight = Math.max(boxHeight, lines.length);
        }
    
        int spacing = 12; 
    
       
        Map<String, Integer> positions = new HashMap<>();
        int pos = 0;
        for (String svc : services) {
            positions.put(svc, pos);
            pos += boxWidths.get(svc) + spacing;
        }
        int totalWidth = pos - spacing; // total width of the whole line
    
        // Prepare output lines buffer
        List<StringBuilder> outputLines = new ArrayList<>();
        // Add boxHeight lines for boxes
        for (int i = 0; i < boxHeight; i++) {
            outputLines.add(new StringBuilder(" ".repeat(totalWidth)));
        }
    
        // Draw boxes side-by-side
        for (int i = 0; i < services.size(); i++) {
            String svc = services.get(i);
            String[] lines = boxLines.get(svc);
            int start = positions.get(svc);
            int width = boxWidths.get(svc);
    
            for (int lineIdx = 0; lineIdx < boxHeight; lineIdx++) {
                String content = lineIdx < lines.length ? lines[lineIdx] : " ".repeat(width);
                StringBuilder lineSb = outputLines.get(lineIdx);
                for (int c = 0; c < width; c++) {
                    lineSb.setCharAt(start + c, content.charAt(c));
                }
            }
        }
    
       
        List<StringBuilder> arrowLines = new ArrayList<>();

        class Arrow {
            int fromPos, toPos;
            boolean leftToRight;
            boolean bidirectional;
    
            Arrow(int fromPos, int toPos, boolean bidir) {
                this.fromPos = fromPos;
                this.toPos = toPos;
                this.leftToRight = fromPos < toPos;
                this.bidirectional = bidir;
            }
    
            int start() {
                return Math.min(fromPos, toPos);
            }
            int end() {
                return Math.max(fromPos, toPos);
            }
        }
    
        List<Arrow> arrows = new ArrayList<>();
    
        // Build list of arrows including bidirectional detection
        for (int i = 0; i < services.size(); i++) {
            String from = services.get(i);
            int fromCenter = positions.get(from) + boxWidths.get(from) / 2;
    
            for (int j = 0; j < services.size(); j++) {
                if (i == j) continue;
                String to = services.get(j);
                int toCenter = positions.get(to) + boxWidths.get(to) / 2;
    
                boolean fromTo = edges.getOrDefault(from, Set.of()).contains(to);
                boolean toFrom = edges.getOrDefault(to, Set.of()).contains(from);
    
                if (fromTo && toFrom && i < j) {
                    // Only add bidirectional once (when i < j)
                    arrows.add(new Arrow(fromCenter, toCenter, true));
                } else if (fromTo && !toFrom) {
                    arrows.add(new Arrow(fromCenter, toCenter, false));
                }
            }
        }
    
        // Function to find a free line index to place an arrow without overlapping
        List<int[]> occupied = new ArrayList<>(); // each int[] = {start, end} occupied per line
    
        for (Arrow arrow : arrows) {
            int start = arrow.start();
            int end = arrow.end();
            int lineIdx = 0;
    
            while (true) {
                boolean conflict = false;
                for (int[] occ : occupied) {
                    if (occ[2] == lineIdx) { // same line
                        // check overlap
                        if (!(end < occ[0] || start > occ[1])) {
                            conflict = true;
                            break;
                        }
                    }
                }
                if (!conflict) break;
                lineIdx++;
            }
            occupied.add(new int[]{start, end, lineIdx});
            // Ensure arrowLines has enough lines
            while (arrowLines.size() <= lineIdx) {
                arrowLines.add(new StringBuilder(" ".repeat(totalWidth)));
            }
    
            StringBuilder lineSb = arrowLines.get(lineIdx);
    
            // Draw arrow body
            for (int p = start + 1; p < end; p++) {
                lineSb.setCharAt(p, '-');
            }
    
            if (arrow.bidirectional) {
                lineSb.setCharAt(start, '<');
                lineSb.setCharAt(end, '>');
                // For bidir, add middle double arrow indicator
                int mid = (start + end) / 2;
                lineSb.setCharAt(mid, '=');
            } else if (arrow.leftToRight) {
                lineSb.setCharAt(end, '>');
            } else {
                lineSb.setCharAt(start, '<');
            }
        }
    
        // Append arrow lines after box lines
        StringBuilder result = new StringBuilder();
        for (StringBuilder boxLine : outputLines) {
            result.append(boxLine).append("\n");
        }
        for (StringBuilder arrowLine : arrowLines) {
            result.append(arrowLine).append("\n");
        }
    
        return result.toString();
    }
    
    

    
    
    private String generateCompactBox(String service, ArchitectureMap.LayeredService layers) {
        List<String> lines = new ArrayList<>();
        lines.add(service);
    
        addLayerLinesCompact(lines, "API", layers.api);
        addLayerLinesCompact(lines, "Business", layers.business);
        addLayerLinesCompact(lines, "Entity", layers.entity);
        addLayerLinesCompact(lines, "Api Gateway", layers.apiGateway);
        addLayerLinesCompact(lines, "Repository", layers.repository);
        addLayerLinesCompact(lines, "Presentation", layers.presentation);
        addLayerLinesCompact(lines, "Shared", layers.shared);
    
        int maxWidth = lines.stream().mapToInt(String::length).max().orElse(0);
        maxWidth = Math.min(Math.max(maxWidth + 4, 20), 30); // min 20, max 30
    
        StringBuilder sb = new StringBuilder();
        sb.append(drawBoxTop(maxWidth)).append("\n");
        for (String line : lines) {
            sb.append(drawBoxLine(line, maxWidth)).append("\n");
        }
        sb.append(drawBoxBottom(maxWidth)).append("\n");
        return sb.toString();
    }
    
   
    
    private void addLayerLinesCompact(List<String> lines, String layerName, List<String> items) {
        if (items == null || items.isEmpty()) return;
    
        Set<String> seen = new HashSet<>();
        List<String> cleanedItems = items.stream()
            .filter(item -> !item.contains("Copyright"))
            .filter(item -> !item.contains("package "))
            .map(item -> {
                String shortItem = item.length() > 25 ? item.substring(0, 22) + "..." : item;
                return shortItem;
            })
            .filter(seen::add) 
            .map(item -> "  - " + item)
            .collect(Collectors.toList());
    
        if (!cleanedItems.isEmpty()) {
            lines.add(layerName + ":");
            lines.addAll(cleanedItems);
        }
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
    
    private String padRight(String text, int width) {
        return text + " ".repeat(Math.max(0, width - text.length()));
    }
    
    private String center(String text, int width) {
        int padding = width - text.length();
        int padStart = padding / 2;
        int padEnd = padding - padStart;
        return " ".repeat(padStart) + text + " ".repeat(padEnd);
    }
    
    
    private String padCenter(String text, int width) {
        int padding = width - text.length();
        int padStart = padding / 2;
        int padEnd = padding - padStart;
        return " ".repeat(padStart) + text + " ".repeat(padEnd);
    }


    public ArchitectureMap parseResponsibilities(String ascii) {
        ArchitectureMap map = new ArchitectureMap();
    
        String[] blocks = ascii.split("\\+[-]+\\+\\n");
        for (String block : blocks) {
            if (block.isBlank()) continue;
    
            String[] lines = block.split("\\n");
            String serviceName = null;
            ArchitectureMap.LayeredService layeredService = new ArchitectureMap.LayeredService();
    
            String currentLayer = null;
    
            for (String line : lines) {
                line = line.trim().replace("|", "").trim();
                if (line.isBlank()) continue;
    
                if (serviceName == null) {
                    serviceName = line;
                    continue;
                }
    
                if (line.endsWith(":")) {
                    currentLayer = line.substring(0, line.length() - 1).toLowerCase();
                    continue;
                }
    
                if (line.startsWith("-")) {
                    String snippet = line.substring(1).trim();
                    if (currentLayer == null) {
                        layeredService.shared.add(snippet);
                    } else {
                        switch (currentLayer) {
                            case "api" -> layeredService.api.add(snippet);
                            case "business" -> layeredService.business.add(snippet);
                            case "repository" -> layeredService.repository.add(snippet);
                            case "entity" -> layeredService.entity.add(snippet);
                            case "apiGateway" -> layeredService.apiGateway.add(snippet);
                            case "presentation" -> layeredService.presentation.add(snippet);
                            case "shared" -> layeredService.shared.add(snippet);
                            default -> layeredService.shared.add(snippet);
                        }
                    }
                }
            }
    
            if (serviceName != null) {
                map.services.put(serviceName, layeredService);
            }
        }
        return map;
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
    

    public void analyzeAndPrint(Map<String, String> files) throws IOException {
        List<ServiceBoundary> boundaries = extractFiles(files);
        ArchitectureMap map = fallback(boundaries);
    
        
        Map<String, Set<String>> inferredCalls = inferServiceRelations(map);
        System.out.println("Service Calls:");
map.serviceCalls.forEach((k, v) -> System.out.println(k + " -> " + v));

        map.serviceCalls = inferredCalls;
    
        System.out.println("\n--- Boundary Context Diagram ---");
        System.out.println(generateBoundaryContextDiagram(map));  
    }
    
   

    public Map<String, Set<String>> inferServiceRelations(ArchitectureMap map) {
        Map<String, Set<String>> detectedCalls = new HashMap<>();
    
        for (String serviceA : map.services.keySet()) {
            Set<String> calls = new HashSet<>();
            ArchitectureMap.LayeredService layersA = map.services.get(serviceA);
    
           
            List<String> snippetsA = new ArrayList<>();
            snippetsA.addAll(layersA.api);
            snippetsA.addAll(layersA.business);
            snippetsA.addAll(layersA.presentation);
            snippetsA.addAll(layersA.entity);
            snippetsA.addAll(layersA.apiGateway);
            snippetsA.addAll(layersA.repository);
            snippetsA.addAll(layersA.shared);
    
            for (String serviceB : map.services.keySet()) {
                if (serviceA.equals(serviceB)) continue;
                ArchitectureMap.LayeredService layersB = map.services.get(serviceB);
    
           
                List<String> classNamesB = new ArrayList<>();
                classNamesB.addAll(layersB.api);
                classNamesB.addAll(layersB.business);
                classNamesB.addAll(layersB.presentation);
                classNamesB.addAll(layersB.entity);
                classNamesB.addAll(layersB.apiGateway);
                classNamesB.addAll(layersB.repository);
                classNamesB.addAll(layersB.shared);
    
                boolean depends = snippetsA.stream().anyMatch(snippetA ->
                    classNamesB.stream().anyMatch(classB ->
                        snippetA.toLowerCase().contains(classB.toLowerCase())
                    )
                );
    
                System.out.printf("Checking if %s uses %s -> %s\n", serviceA, serviceB, depends);
    
                if (depends) {
                    calls.add(serviceB);
                }
            }
    
            if (!calls.isEmpty()) {
                detectedCalls.put(serviceA, calls);
            }
        }
    
        return detectedCalls;
    }
    
    
}
