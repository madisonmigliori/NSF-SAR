package com.nsf.langchain.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Provider.Service;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.testcontainers.shaded.org.apache.commons.lang3.arch.Processor.Arch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class ServiceBoundaryUtils {

    public record ServiceBoundary(String filePath, String language,String layer, String indicator, String snippet){}

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
            System.out.println("Service: "+ service);
            System.out.println("Presentation: " + layers.presentation);
            System.out.println("API: " + layers.api);
            System.out.println("Business: " + layers.business);
            System.out.println("Data: " + layers.data);
            System.out.println("Shared: " + layers.shared);
            System.out.println();

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
        if (indicator.matches("(?i).*(@RestController|express\\.Router|router\\.|@app\\.route|Routes\\.draw).*")) return "api";
        if (indicator.matches("(?i).*(@Service|Service|impl|useCases|business|Manager|Handler).*")) return "business";
        if (indicator.matches("(?i).*(@Repository|ActiveRecord|Repository|sql|ORM|DB|Model|DAO).*")) return "data";
        if (indicator.matches("(?i).*(function .*Component|React|Page|useEffect|useState|\\.jsx|\\.tsx).*")) return "presentation";
        if (indicator.matches("(?i).*(Logger|Validator|Utils|Helper|Exception|Config|Constants|Settings|Util|Common|Middleware|Filter|Interceptor|Adapter|Transformer).*")) 
            return "shared";
    
        return "unknown";
    }
    

    private List<ServiceBoundary> extractJava(String path, String content){
        return matchPatterns(path, content, "Java", List.of(
            "@RestController", "@Service", "@Respository", 
            "public class \\w+", "interface \\w+"
        ));
    }

    private List<ServiceBoundary> extractPy(String path, String content){
        return matchPatterns(path, content, "Python", List.of(
            "@app.router\\([^]+\\)", "class \\w+", "def \\w+\\(", 
            "sqlalchemy", "pydantic"
        ));
    }

    private List<ServiceBoundary> extractJs(String path, String content){
        return matchPatterns(path, content, "Javascript", List.of(
            "express\\.Router\\(\\)", "app\\.get\\(", "router\\.\\w+\\(", 
            "function \\w+", "class \\w+"
        ));
    }

    private List<ServiceBoundary> extractGo(String path, String content){
        return matchPatterns(path, content, "Go", List.of(
            "http\\.HandleFunc", "mux\\.HandleFunc", "func \\w+\\(", 
            "package \\w+", "sql\\.Open"
        ));
    }
    private List<ServiceBoundary> extractRs(String path, String content){
        return matchPatterns(path, content, "Rust", List.of(
            "use actix_web::", "use rocket::", "impl \\w+", 
            "structure \\w+", "mod \\w+"
        ));
    }
    private List<ServiceBoundary> extractCpp(String path, String content){
        return matchPatterns(path, content, "C++", List.of(
            "class \\w+", "void \\w+\\(", "namespace \\w+"
        ));
    }

    private List<ServiceBoundary> extractRuby(String path, String content){
        return matchPatterns(path, content, "Ruby", List.of(
            "class \\w+", "def \\w+", "module \\w+", 
            "Rails\\.application.routes.draw", "ActiveRecord::Base"
        ));
    }

    private List<ServiceBoundary> extractReact(String path, String content){
        return matchPatterns(path, content, "React", List.of(
            "function \\w+\\(.*\\) \\{", "const \\w+ = \\(.*\\) => \\{",
            "export default \\w+", "useEffect\\(", "useState\\("
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

    // public String extractJson(String text) {
    //     int start = text.indexOf("{");
    //     int end = text.lastIndexOf("}");
    //     if (start == -1 || end == -1 || end <= start) {
    //         return null; 
    //     }
    //     return text.substring(start, end + 1);
    // }

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
    
    

    
    public String generateSBDiagram(Map<String, ArchitectureMap.LayeredService> services) {
        StringBuilder sb = new StringBuilder();
        final int width = 26;
        final String horizontalBorder = "+" + "-".repeat(width) + "+";
    
        for (var entry : services.entrySet()) {
            String serviceName = entry.getKey();
            ArchitectureMap.LayeredService layers = entry.getValue();
    
            // Print top border and service name centered-ish
            sb.append(horizontalBorder).append("\n");
            sb.append("| ").append(padCenter(serviceName, width - 2)).append(" |\n");
            sb.append(horizontalBorder).append("\n");
    
            boolean hasContent = false;
    
            hasContent |= printLayer(sb, "[Presentation]", layers.presentation, width);
            hasContent |= printLayer(sb, "[API]", layers.api, width);
            hasContent |= printLayer(sb, "[Business]", layers.business, width);
            hasContent |= printLayer(sb, "[Data]", layers.data, width);
            hasContent |= printLayer(sb, "[Shared/Utils]", layers.shared, width);
    
            if (!hasContent) {
                sb.append("| ").append(padCenter("(no roles detected)", width - 2)).append(" |\n");
            }
    
            sb.append(horizontalBorder).append("\n\n");
        }
        return sb.toString();
    }
    
    private boolean printLayer(StringBuilder sb, String label, List<String> items, int width) {
        if (items.isEmpty()) return false;
        List<String> uniqueItems = items.stream().distinct().toList();
        String joined = String.join(", ", uniqueItems);
        if (joined.length() > width - label.length() - 3) { 
            joined = joined.substring(0, width - label.length() - 6) + "...";
        }
        sb.append("| ").append(label).append(" ").append(joined).append("\n");
        return true;
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
