package com.nsf.langchain.git;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nsf.langchain.git.token.GetToken;
import com.nsf.langchain.model.AntiPattern;
import com.nsf.langchain.model.MSAPattern;
import com.nsf.langchain.model.Scoring;
import com.nsf.langchain.utils.ArchitectureUtils;
import com.nsf.langchain.utils.JsonUtils;
import com.nsf.langchain.utils.ServiceBoundaryUtils;
import com.nsf.langchain.utils.ServiceBoundaryUtils.ArchitectureMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;



@Service
public class GitHubApi {

    private static final Map<String, Double> SEVERITY_WEIGHTS = Map.of(
    "high", 3.0,
    "medium", 2.0,
    "low", 1.0
);


    private static final Logger log = LoggerFactory.getLogger(GitHubApi.class);

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    
    // @Autowired
    // private ArchitectureUtils architectureUtils;


    // private final String token;
  
   // The above Java code defines a class `GitHubApi` with two constructors - one default constructor that initializes the class with a GitHub Personal Access Token (PAT) retrieved from the system environment variable "GITHUB_PAT", and another constructor that takes a token as a parameter. If the token is null or empty, it throws an IllegalStateException.
    public GitHubApi() {
        this.token = "";
        this.architectureUtils = null;
        this.serviceBoundaryUtils = null;
    }

//     @Autowired
// public GitHubApi(ArchitectureUtils architectureUtils) {
//     this.architectureUtils = architectureUtils;
// }

// public GitHubApi(String token) {
//     this.architectureUtils = null; 
//     this.token = token;
// }



private final String token;
private final ArchitectureUtils architectureUtils;
private final ServiceBoundaryUtils serviceBoundaryUtils;

@Autowired
public GitHubApi(ArchitectureUtils architectureUtils, ServiceBoundaryUtils serviceBoundaryUtils) {
    String token = System.getenv("GITHUB_PAT");
    if (token == null || token.isBlank()) {
        throw new IllegalStateException("GITHUB_PAT token not provided");
    }
    this.token = token;
    this.architectureUtils = architectureUtils;
    this.serviceBoundaryUtils = serviceBoundaryUtils;
}



    private List<AntiPattern> antiPatterns;
    private List<MSAPattern> patterns;
    private List<Scoring> scoringCriteria;
    

    private List<AntiPattern> getAntiPatterns() {
    if (antiPatterns == null) {
        Path path = Path.of("doc/msa-anti-patterns.json");
        antiPatterns = JsonUtils.loadAntiPatternsJson(path);
        }
    return antiPatterns;
    }

    private List<MSAPattern> getPatterns() {
        if (patterns == null) {
            Path path = Path.of("doc/msa-patterns.json");
            patterns = JsonUtils.loadPatternsJson(path);
            }
        return patterns;
        }

    public List<Scoring> loadScoringCriteria(Path scoringJsonPath) {
            scoringCriteria = JsonUtils.loadScoringJson(scoringJsonPath);
            return scoringCriteria;
        }


    public static void initAllRegexAnti(List<AntiPattern> antiPatterns) {
            if (antiPatterns == null) return;
            for (AntiPattern ap : antiPatterns) {
                ap.initDetectionRegex();
            }
     }

     public static void initAllRegex(List<MSAPattern> patterns) {
        if (patterns == null) return;
        for (MSAPattern p : patterns) {
            p.initDetectionRegex();
        }
 }
        

    /**
     * The `inspectRepo` function fetches and analyzes files from a GitHub repository, identifying anti-patterns, patterns, warnings, and configuration issues.
     * 
     * @param gitUrl The `inspectRepo` method you provided seems to be inspecting a GitHub repository by making API requests to fetch its contents and analyzing the files within it for anti-patterns, patterns, warnings, and configurations. It constructs a tree structure representing the repository contents and their relationships.
     * @return The method `inspectRepo` returns a `BinaryTreeNode` object, which represents the root node of a binary tree structure containing information about the files and directories in a GitHub repository.
     */
  


    public BinaryTreeNode inspectRepo(String gitUrl) throws Exception {
        List<String> allWarnings = new ArrayList<>();
        List<AntiPattern> antiPatternsList = getAntiPatterns();
        List<MSAPattern> patternsList = getPatterns();
        initAllRegexAnti(antiPatternsList);
        initAllRegex(patternsList);
        String[] parts = gitUrl.replace(".git", "").split("/");

        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid GitHub repo URL: " + gitUrl);
        }

        String user = parts[parts.length - 2];
        String repo = parts[parts.length - 1];

        String apiUrl = String.format("https://api.github.com/repos/%s/%s/contents/", user, repo);
        BinaryTreeNode root = new BinaryTreeNode(repo, "repo", apiUrl);

        Stack<BinaryTreeNode> toVisit = new Stack<>();
        toVisit.push(root);
        List<BinaryTreeNode> fileNodes = new ArrayList<>();
        log.info("ArchitectureUtils initialized? {}", architectureUtils != null);


       
        



        while (!toVisit.isEmpty()) {
            BinaryTreeNode current = toVisit.pop();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(current.url))
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/vnd.github+json")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Failed to fetch {}: HTTP {}", current.url, response.statusCode());
                log.error("Response body: {}", response.body());
                continue;
            }

            JsonNode jsonNode = mapper.readTree(response.body());

            if (jsonNode.isArray()) {
                for (JsonNode node : jsonNode) {
                    String name = node.get("name").asText();
                    String type = node.get("type").asText();
                    String url = node.get("url").asText();

                    if ("dir".equals(type)) {
                        BinaryTreeNode dirNode = new BinaryTreeNode(name, type, url);
                        current.children.add(dirNode);
                        toVisit.push(dirNode);

                    } else if ("file".equals(type)) {
                        String path = node.get("path").asText();
                        String fileContent = fetchFileContent(user, repo, path);
                        BinaryTreeNode fileNode = new BinaryTreeNode(name, type, url, fileContent != null ? fileContent : "");

                        fileNodes.add(fileNode);
                        current.children.add(fileNode);
                    }
                }
            }
        }

        String fullProjectText = fileNodes.stream().map(n -> n.content).collect(Collectors.joining("\n"));

        Map<String, String> fileMap = new HashMap<>();
        for (BinaryTreeNode node : fileNodes) {
        fileMap.put(node.name, node.content);
        }

        
        ArchitectureMap map = null;
List<String> architectureWarnings = new ArrayList<>();
try {
    map = serviceBoundaryUtils.fallback(serviceBoundaryUtils.extractFiles(fileMap));
    map.serviceCalls = serviceBoundaryUtils.inferServiceRelations(map);
    architectureWarnings = serviceBoundaryUtils.detectAntiPatterns(map);
    log.info("Architecture Anti-patterns found: {}", architectureWarnings);
} catch (Exception e) {
    log.error("Failed to perform architecture-level analysis: {}", e.getMessage());
}

    Map<String, List<String>> fileToWarnings = new HashMap<>();
    for (String warning : architectureWarnings) {
    for (String file : fileMap.keySet()) {
        if (warning.toLowerCase().contains(file.toLowerCase().replace(".java", ""))) {
            fileToWarnings.computeIfAbsent(file, k -> new ArrayList<>()).add(warning);
        }
    }
}
        

        String dependencies = "";
    

        for (BinaryTreeNode fileNode : fileNodes) {
            String content = fileNode.content;
            String name = fileNode.name;

      
            try {
                dependencies = architectureUtils.getDependencyFile(fileNodes);
                if (dependencies == null || dependencies.isBlank()) {
                log.warn("No dependencies extracted from repo '{}'", repo);
            } else {
            log.info("Dependencies extracted successfully:\n{}", dependencies);
            }
        } catch (Exception e) {
            log.warn("Failed to extract dependencies from repo: {}", e.getMessage());
        }


            for (AntiPattern ap : antiPatternsList) {
                String snippet = ap.getMatchSnippet(content, fullProjectText);
                if (snippet != null) {
                    fileNode.antiPatterns.add("Anti-pattern found in file: " + ap.getName());
                    fileNode.severityScore += SEVERITY_WEIGHTS.getOrDefault(ap.getSeverity().toLowerCase(), 1.0);
                    fileNode.antiPatternCounts.merge(ap.getName(), 1, Integer::sum);
                    log.info("Anti-pattern '{}' matched in file '{}'. Snippet: '{}'", ap.getName(), name, snippet);
                }
            }

            if (fileToWarnings.containsKey(name)) {
                for (String warning : fileToWarnings.get(name)) {
                    fileNode.antiPatterns.add(warning);
                    allWarnings.add(warning);
                }
            }

                

            

            for (MSAPattern p : patternsList) {
                String snippet = p.getMatchSnippetDep(content, dependencies);
                if (snippet != null) {
                    fileNode.patterns.add("Pattern found in file: " + p.getName());
                    fileNode.patternCounts.merge(p.getName(), 1, Integer::sum);
                    log.info("Pattern '{}' matched in file '{}'. Snippet: '{}'", p.getName(), name, snippet);
                }
            }
        }

        log.info("Dependencies results: {}", dependencies);

        aggregateCounts(root);
        log.info("Final total severity score: {}", root.severityScore);
        log.info("Total anti-patterns found: {}", root.antiPatternCounts);
        log.info("Total patterns found: {}", root.patternCounts);
        log.info("Aggregated counts and severity scores for repo '{}'", repo);


        if (scoringCriteria == null) {
            scoringCriteria = loadScoringCriteria(Path.of("doc/msa-scoring.json")); 
        }
        Map<String, Double> scores = calculateScoring(scoringCriteria, fileNodes);
        log.info("Scoring results: {}", scores);
    

        root.scoringResults = scores;
        root.dependenciesSummary = dependencies;

    
        return root;
    }

    public void aggregateCounts(BinaryTreeNode node) {
        if (node.children == null || node.children.isEmpty()) return;
    
        for (BinaryTreeNode child : node.children) {
            aggregateCounts(child);
            child.patternCounts.forEach(
                (k, v) -> node.patternCounts.merge(k, v, Integer::sum));
            child.antiPatternCounts.forEach(
                (k, v) -> node.antiPatternCounts.merge(k, v, Integer::sum));
            node.severityScore += child.severityScore;
        }
       

    }

    public Map<String, Double> calculateScoring(List<Scoring> scoringCriteria, List<BinaryTreeNode> fileNodes) {
        Map<String, Integer> matchesPerCriteria = new HashMap<>();
        Map<String, Double> scorePerCriteria = new HashMap<>();
    
        Set<String> detectedPatterns = new HashSet<>();
        for (BinaryTreeNode fileNode : fileNodes) {
            fileNode.patternCounts.keySet().forEach(p -> detectedPatterns.add(p.toLowerCase()));
            fileNode.antiPatternCounts.keySet().forEach(p -> detectedPatterns.add(p.toLowerCase()));
        }
    
        for (Scoring criteria : scoringCriteria) {
            int total = criteria.getPatterns().size();
            int matched = 0;
    
            for (String pattern : criteria.getPatterns()) {
                if (detectedPatterns.contains(pattern.toLowerCase())) {
                    matched++;
                }
            }
    
            double matchPercent = (total == 0) ? 0.0 : (double) matched / total;
            double baseScore;
            int percent = (int)(matchPercent * 100);

            if (percent >= 70) {
                baseScore = 10.0;
            } else if (percent >= 50) {
                baseScore = 7.0;
            } else if (percent >= 30) {
                baseScore = 5.0;
            } else if (percent >= 10) {
                baseScore = 3.0;
            } else {
                baseScore = 1.0;
            }

    
            double weighted = baseScore * criteria.getWeight();
    
            scorePerCriteria.put(criteria.getName(), weighted);
    
            System.out.printf(" %s: matched %d/%d (%.0f%%) → base: %.1f → weighted: %.2f%n",
                    criteria.getName(), matched, total, matchPercent * 100, baseScore, weighted);
        }
    
        return scorePerCriteria;
    }
    


    public String fetchFileContent(String user, String repo, String path) throws Exception {
        String apiUrl = String.format("https://api.github.com/repos/%s/%s/contents/%s", user, repo, path);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/vnd.github+json")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.warn("Failed to fetch file content for {}: HTTP {}", path, response.statusCode());
            log.warn("Response body: {}", response.body());
            return null;
        }

        JsonNode jsonNode = mapper.readTree(response.body());
        if (jsonNode.has("content")) {
            String encodedContent = jsonNode.get("content").asText().replaceAll("\\s", "");
            byte[] decodedBytes = Base64.getDecoder().decode(encodedContent);
            return new String(decodedBytes, StandardCharsets.UTF_8);
        } else {
            log.warn("No content found for file: {}", path);
            return null;
        }
    }

    public List<String> flattenFilePaths(BinaryTreeNode root) {
        List<String> paths = new ArrayList<>();
        flatten(root, "", paths);
        return paths;
    }

    private void flatten(BinaryTreeNode node, String currentPath, List<String> paths) {
        if (node == null) return;

        String newPath = currentPath.isEmpty() ? node.name : currentPath + "/" + node.name;

        if ("file".equals(node.type)) {
            paths.add(newPath);
            
        }

        if (node.children != null) {
            for (BinaryTreeNode child : node.children) {
                flatten(child, newPath, paths);
            }
        }
    }


  
public List<String> listAllFilePaths(String gitUrl) throws Exception {
    BinaryTreeNode root = inspectRepo(gitUrl);
    aggregateCounts(root);
    GitHubApi.printTree(root, "");
    return flattenFilePaths(root);
}

public Map<String, String> extractCode(BinaryTreeNode root){
    Map<String, String> codeFiles = new HashMap<>();
    collectCodeFiles(root,"", codeFiles);
    return codeFiles;
}

private void collectCodeFiles(BinaryTreeNode node, String path, Map<String,String> codeFiles){
    if(node == null){
        return;
    }

    String fullPath = path.isEmpty() ? node.name : path + "/" + node.name;

    if("file".equals(node.type) && isCodeFile(node.name)){
        codeFiles.put(fullPath, node.content);
    }
    if(node.children != null){
        for (BinaryTreeNode child : node.children){
            collectCodeFiles(child, fullPath, codeFiles);
        }
    }
}

private boolean isCodeFile(String fileName) {
    return fileName.endsWith(".java") || fileName.endsWith(".py") ||
           fileName.endsWith(".js") || fileName.endsWith(".ts") ||
           fileName.endsWith(".go") || fileName.endsWith(".rs") ||
           fileName.endsWith(".cpp") || fileName.endsWith(".hpp") ||
           fileName.endsWith(".h") || fileName.endsWith(".rb") ||
           fileName.endsWith(".jsx") || fileName.endsWith(".tsx");
}

private boolean isConfigFile(String fileName) {
    String lower = fileName.toLowerCase();
    return lower.equals("application.properties") || lower.equals("application.yml") ||
           lower.equals("config.json") || lower.endsWith(".yaml") || lower.endsWith(".toml") ||
           lower.equals("docker-compose.yml") || lower.equals("docker-compose.yaml");
}

private boolean isTextualFile(String fileName) {
    return isCodeFile(fileName) || isConfigFile(fileName) || fileName.endsWith(".md");
}



    public static void printTree(BinaryTreeNode node, String indent) {
        if (node == null) return;

        System.out.println(indent + node.type + " " + node.name);

        if (node.children != null) {
            for (BinaryTreeNode child : node.children) {
                printTree(child, indent + "    ");
            }
        }
    }
    public static List<String> checkConfigAndInfra(List<String> configs, List<String> requiredFeatures) {
        List<String> missing = new ArrayList<>();
        for (String feature : requiredFeatures) {
            if (configs.stream().noneMatch(c -> c.toLowerCase().contains(feature.toLowerCase()))) {
                missing.add("Missing configuration or implementation for: " + feature);
            }
        }
        return missing;
    }
    
    public static List<String> scanCommentsForWarnings(String sourceCode) {
        List<String> warnings = new ArrayList<>();
        Pattern todoPattern = Pattern.compile("//\\s*(TODO|FIXME|HACK|BUG)", Pattern.CASE_INSENSITIVE);
        try (java.util.Scanner scanner = new java.util.Scanner(sourceCode)) {
            int lineNum = 0;
            while (scanner.hasNextLine()) {
                lineNum++;
                String line = scanner.nextLine();
                if (todoPattern.matcher(line).find()) {
                    warnings.add("Comment warning at line " + lineNum + ": " + line.trim());
                }
            }
        }
        return warnings;
    }

    public List<String> detectAntiPatterns(String analysisText, List<Pattern> antiPatterns) {
                List<String> found = new ArrayList<>();
                for (Pattern pattern : antiPatterns) {
                    if (pattern.matcher(analysisText).find()) {
                        found.add("Anti-pattern detected: " + pattern.pattern());
                    }
                }
                return found;
            }

            public void matchGlobalArchitecturePatterns(BinaryTreeNode root, List<AntiPattern> antiPatterns, String deps, String arch, String boundaries) {
                if (root == null) return;
            
                String lowerDeps = deps.toLowerCase();
                String lowerArch = arch.toLowerCase();
                String lowerBoundary = boundaries.toLowerCase();
            
                Set<String> matched = matchAntiPatterns(antiPatterns, lowerDeps, lowerArch, lowerBoundary);
                            
                                Stack<BinaryTreeNode> stack = new Stack<>();
                                stack.push(root);
                            
                                while (!stack.isEmpty()) {
                                    BinaryTreeNode current = stack.pop();
                            
                                    if ("file".equals(current.type)) {
                                        matched.forEach(p -> current.warnings.add("Architecture-level anti-pattern detected: " + p));
                                    }
                            
                                    if (current.children != null) {
                                        current.children.forEach(stack::push);
                                    }
                                }
                            }
                
                
                        private Set<String> matchAntiPatterns(List<AntiPattern> antiPatterns, String lowerDeps, String lowerArch, String lowerBoundary) {
                                Set<String> matched = new HashSet<>();
                                for (AntiPattern ap : antiPatterns) {
                                    String name = ap.getName().toLowerCase();
                                    if (lowerDeps.contains(name) || lowerArch.contains(name) || lowerBoundary.contains(name)) {
                                        matched.add(ap.getName());
                                    }
                                }
                                return matched;
                            }
                            

        public List<BinaryTreeNode> collectRelevantFiles(BinaryTreeNode root) {
                                List<BinaryTreeNode> files = new ArrayList<>();
                                Set<String> filesToCheck = Set.of(
                                    "pom.xml", "build.gradle", "build.gradle.kts",
                                    "package.json", "package-lock.json",
                                    "requirements.txt", "pyproject.toml", "Pipfile", "setup.py",
                                    "Cargo.toml", "go.mod", "Gopkg.toml", ".csproj", "Gemfile", "CMakeLists.txt", "Makefile"
                                );
                            
                                traverseTree(root, files, filesToCheck);
                                return files;
                            }
                            
        private void traverseTree(BinaryTreeNode node, List<BinaryTreeNode> files, Set<String> filesToCheck) {
                                if (filesToCheck.contains(node.name)) {
                                    files.add(node);
                                }
                                for (BinaryTreeNode child : node.children) {
                                    traverseTree(child, files, filesToCheck);
                                }
                            }
                            


}
