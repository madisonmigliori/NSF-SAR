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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.testcontainers.shaded.org.apache.commons.lang3.arch.Processor.Arch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.regex.Pattern;

@Component
public class ServiceBoundaryUtils {

    public record ServiceBoundary(String filePath, String language,String layer, String indicator, String snippet){}

    private static final Pattern API_PATTERN = Pattern.compile(
    "(?i)\\b(@RestController|@Controller|@RequestMapping|@GetMapping|@PostMapping|@app\\.route\\(|@blueprint\\.route\\(|routes\\.draw" +
    "|express\\.Router|router\\.|http\\.HandleFunc|mux\\.HandleFunc|gin\\.Engine|#\\[get\\(|#\\[post\\(|Controller\\b|route\\b|endpoint\\b|handler\\b)\\b"
);

private static final Pattern BUSINESS_PATTERN = Pattern.compile(
    "(?i)\\b(@Service|@Component|Service\\b|useCase\\b|useCases\\b|BusinessLogic\\b|Manager\\b|Handler\\b|Interactor\\b" +
    "|\\w+Service\\b|\\w+Manager\\b|\\w+Interactor\\b|\\w+Handler\\b)\\b"
);

private static final Pattern DATA_PATTERN = Pattern.compile(
    "(?i)\\b(@Repository|Repository\\b|@Entity|Entity\\b|Model\\b|Schema\\b|DAO\\b|DB\\b|database\\b|CrudRepository\\b|JpaRepository\\b" +
    "|sql\\b|ORM\\b|sqlalchemy|diesel::|Mongoose|ActiveRecord|GORM|pydantic|Prisma)\\b"
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
            public List<String> data = new ArrayList<>();
            public List<String> shared = new ArrayList<>();
        }
        
        public Map<String, LayeredService> services = new HashMap<>();

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
                service.data = extractList(serviceNode, "data");
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
            if (!layers.data.isEmpty())
                System.out.println("Data: " + layers.data);
            if (!layers.shared.isEmpty())
                System.out.println("Shared: " + layers.shared);
    
            System.out.println(); // Spacer between services
        });
    }
    
    }

    public List<ServiceBoundary> extractCode(Path rootPath) throws IOException{
        List<ServiceBoundary> artifacts = new ArrayList<>();

        Files.walk(rootPath)
        .filter(Files::isRegularFile)
        .forEach(file -> {
            try {
                String content = Files.readString(file);
                String fileName = file.toString();

                if(fileName.endsWith(".java")){
                    artifacts.addAll(extractJava(fileName, content));
                }

                if(fileName.endsWith(".py")){
                    artifacts.addAll(extractPy(fileName, content));
                }

                if(fileName.endsWith(".js")|| fileName.endsWith(".ts")) {
                    artifacts.addAll(extractJs(fileName, content));
                }
                if(fileName.endsWith(".go")){
                    artifacts.addAll(extractGo(fileName, content));
                }
                if(fileName.endsWith(".rs")){
                    artifacts.addAll(extractRs(fileName, content));
                }
                if(fileName.endsWith(".cpp")|| fileName.endsWith(".h") || fileName.endsWith(".hpp")){
                    artifacts.addAll(extractCpp(fileName, content));
                }
                if(fileName.endsWith(".rb")){
                    artifacts.addAll(extractRuby(fileName, content));
                }
                if(fileName.endsWith(".jsx")|| fileName.endsWith(".tsx")){
                    artifacts.addAll(extractReact(fileName, content));
                }
            
            }
            catch (IOException e){
                e.printStackTrace();

            }
        });
        return artifacts;
    }

    public List<ServiceBoundary> extractCode(Map<String, String> files) {
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
    if (DATA_PATTERN.matcher(trimmed).find()) return "data";
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
    

    private List<ServiceBoundary> matchPatterns(String path, String content, String language, List<String>patterns){
        List<ServiceBoundary> results = new ArrayList<>();
        for (String regex : patterns) {
            Matcher matcher = Pattern.compile(regex).matcher(content);
            while(matcher.find()){
                String match = matcher.group();;
                String layer = inferLayer(regex);
                results.add(new ServiceBoundary(path, language, layer, regex, match));
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

    public String extractJson(String text) {
        text = text.replace("```json", "").replace("```", "").trim();
    
        int braceCount = 0;
        int startIndex = -1;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '{') {
                if (startIndex == -1) startIndex = i;
                braceCount++;
            } else if (c == '}') {
                braceCount--;
                if (braceCount == 0 && startIndex != -1) {
                    return text.substring(startIndex, i + 1);
                }
            }
        }
        return null; 
    }
    

    public String generateAsciiDiagram(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(json);
        JsonNode services = root.get("Services");
        JsonNode borders = root.get("Borders");
    
        if (services == null || !services.isArray()) {
            System.out.println( "No services found in JSON.");
        }

        if (borders == null || !borders.isArray()) {
            System.out.println("No borders found in JSON.");
        }
    
        StringBuilder sb = new StringBuilder();
    
        for (JsonNode service : services) {
            String name = service.get("Name").asText();
            JsonNode responsibilities = service.get("Responsibilities");
    
            int maxLen = name.length();
            List<String> respList = new ArrayList<>();
            if (responsibilities != null && responsibilities.isArray()) {
                for (JsonNode resp : responsibilities) {
                    String r = resp.asText();
                    respList.add(r);
                    if (r.length() > maxLen) maxLen = r.length();
                }
            }
    
            int width = maxLen + 4; 
    
    
            sb.append("+").append("-".repeat(width)).append("+\n");
  
            sb.append("| ").append(padRight(name, width - 2)).append(" |\n");
  
            sb.append("+").append("-".repeat(width)).append("+\n");
    
       
            for (String resp : respList) {
                sb.append("| ").append(padRight(resp, width - 2)).append(" |\n");
            }
    
       
            sb.append("+").append("-".repeat(width)).append("+\n\n");
        }
    
        return sb.toString();
    }
    
    private String padRight(String text, int width) {
        return text + " ".repeat(Math.max(0, width - text.length()));
    }
    
    
    public String generateAsciiDiagramFromLLMJson(String json) {
        StringBuilder sb = new StringBuilder();
        final int width = 60;
        final String border = "+" + "-".repeat(width - 2) + "+";
    
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);

            String[] possibleKeys = {"serviceBoundaries", "service_boundaries", "boundaries", "services", "Services", "Boundaries"};

        for (String key : possibleKeys) {
        JsonNode boundaries = root.get(key);
         if (boundaries != null && boundaries.isArray()) {
            for (JsonNode service : boundaries) {
            String name = service.path("name").asText();
            List<String> responsibilities = extractList(service, "responsibilities");
            List<String> layers = extractList(service, "layers");

            sb.append(renderServiceBlock(name, responsibilities, null, null, layers, width, border));
             }
            return sb.toString();
            }
        }

    
        Iterator<Map.Entry<String, JsonNode>> fieldNames = root.fields();
        while (fieldNames.hasNext()) {
            Map.Entry<String, JsonNode> entry = fieldNames.next();
            String key = entry.getKey();
            JsonNode value = entry.getValue();

            if (!value.isArray()) continue;


            if (value.isArray()) {
                sb.append(border).append("\n");
                sb.append("| ").append(center(key, width - 4)).append(" |\n");
                sb.append(border).append("\n");

                for (JsonNode service : value) {
                    String name = service.path("name").asText();
                    List<String> responsibilities = extractList(service, "responsibilities");
                    List<String> types = extractList(service, "type");
                    List<String> borders = extractList(service, "borderIds");
                    List<String> layers = extractList(service, "layers");
                    sb.append(renderServiceBlock(name, responsibilities, types, borders, layers, width, border));
                }
            } else if (value.isObject()) {
    
                List<String> responsibilities = extractList(value, "responsibilities");
                sb.append(renderServiceBlock(key, responsibilities, null, null, null, width, border));
            }
        }
    


    
            return sb.toString();
    
        } catch (Exception e) {
            return "Error parsing JSON: " + e.getMessage();
        }
    }
    
    private List<String> extractList(JsonNode node, String field) {
        List<String> list = new ArrayList<>();
        if (node.has(field)) {
            for (JsonNode item : node.get(field)) {
                list.add(item.asText());
            }
        }
        return list;
    }
    
    private String renderServiceBlock(String name, List<String> responsibilities, List<String> types, List<String> borders, List<String> layers, int width, String border) {
        StringBuilder sb = new StringBuilder();
        sb.append(border).append("\n");
        sb.append(String.format("| %-"+(width-4)+"s |\n", center(name, width - 4)));
        sb.append(border).append("\n");

        if (responsibilities != null && !responsibilities.isEmpty()) {
            sb.append("| Responsibilities:").append(" ".repeat(width - 20)).append("|\n");
            for (String r : responsibilities) {
                sb.append("|   - ").append(padRight(r, width - 5)).append("|\n");
            }
        }

        if (types != null && !types.isEmpty()) {
            sb.append("| Types:").append(" ".repeat(width - 20)).append("|\n");
            for (String t : types) {
                sb.append("|   - ").append(padRight(t, width - 5)).append("|\n");
            }
        }
        if (borders != null && !borders.isEmpty()) {
            sb.append("| Linked Borders:").append(" ".repeat(width - 20)).append("|\n");
            for (String b : borders) {
                sb.append("|   - ").append(padRight(b, width - 5)).append("|\n");
            }
        }

        if (layers != null && !layers.isEmpty()) {
            sb.append("| Layers:").append(" ".repeat(width - 10)).append("|\n");
            for (String l : layers) {
                sb.append("|   * ").append(padRight(l, width - 6)).append("|\n");
            }
        }

        sb.append(border).append("\n\n");
        return sb.toString();
    }
    

    private String center(String text, int width) {
        int padding = Math.max(0, (width - text.length()) / 2);
        return " ".repeat(padding) + text + " ".repeat(width - text.length() - padding);
    }
    

    
    public String generateBoundaryContextDiagram(ArchitectureMap map) {
    Map<String, Set<String>> serviceToShared = new HashMap<>();
    Map<String, Set<String>> edges = new HashMap<>();


    for (Map.Entry<String, ArchitectureMap.LayeredService> entry : map.services.entrySet()) {
        String service = entry.getKey();
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
    Set<String> allServices = new HashSet<>(map.services.keySet());

    for (String service : allServices) {
        sb.append("+------------------+\n");
        sb.append("| ").append(padCenter(service, 18)).append(" |\n");
        sb.append("+------------------+\n");

        Set<String> targets = edges.getOrDefault(service, new HashSet<>());
        for (String target : targets) {
            sb.append("       uses -----> ").append(target).append("\n");
        }
        sb.append("\n");
    }

    return sb.toString();
}

  
    public ArchitectureMap parseResponsbilities(String ascii){
        ArchitectureMap map = new ArchitectureMap();

        String[] blocks = ascii.split("\\+[-]+\\+\\n");
        for(String block : blocks) {
            if(block.isBlank()) continue;

            String[] lines = block.split("\\n");
            String serviceName = null;
            List<String> responsbilities = new ArrayList<>();

            for(String line: lines){
                line = line.trim().replace("|", "").trim();
                if(line.isBlank()) continue;

                if (serviceName == null){
                    serviceName = line;
                }
                else if (line.startsWith("-")){
                    responsbilities.add(line.substring(1).trim());

                }
            }

            if(serviceName == null){
                ArchitectureMap.LayeredService layeredService = new ArchitectureMap.LayeredService();

                layeredService.shared.addAll(responsbilities);
                map.services.put(serviceName, layeredService);
            }
        }

       

        return map;
}

    
    
    private String padCenter(String text, int width) {
        int padding = width - text.length();
        int padStart = padding / 2;
        int padEnd = padding - padStart;
        return " ".repeat(padStart) + text + " ".repeat(padEnd);
    }
    

    public ArchitectureMap fallback(List<ServiceBoundary> artifacts){
        ArchitectureMap map = new ArchitectureMap();

        for (ServiceBoundary artifact : artifacts){
            String serviceName = inferServiceName(artifact.filePath());
            ArchitectureMap.LayeredService layers = map.services.computeIfAbsent(serviceName, k -> new ArchitectureMap.LayeredService());
            
            switch (artifact.layer()){
                case "presentation" ->  layers.presentation.add(artifact.snippet());
                case "api" ->  layers.api.add(artifact.snippet());
                case "business" ->  layers.business.add(artifact.snippet());
                case "data" ->  layers.data.add(artifact.snippet());
                case "shared" ->  layers.shared.add(artifact.snippet());
            }
        }
                return map;
    }

    private String inferServiceName(String filePath){
        String[] parts = filePath.split("[/\\\\]");
        if(parts.length >=2){
            return parts[parts.length - 2];
        }
        return "UnknownService";
    }
}
