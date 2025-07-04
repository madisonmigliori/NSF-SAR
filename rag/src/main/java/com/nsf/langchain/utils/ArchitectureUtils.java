package com.nsf.langchain.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;



import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nsf.langchain.git.BinaryTreeNode;
import com.nsf.langchain.git.GitHubApi;

@Component
public class ArchitectureUtils {
    private final GitHubApi gitHubApi;
    private final ObjectMapper mapper = new ObjectMapper();

    public ArchitectureUtils(GitHubApi gitHubApi){
        this.gitHubApi = gitHubApi;
    }


    public String getDependency(String user, String repo) throws Exception {
    List<String> allDependencies = new ArrayList<>();
    
    Set<String> filesToCheck = Set.of(
        "pom.xml", "build.gradle", "build.gradle.kts",
        "package.json", "yarn.lock", "pnpm-lock.yaml", "package-lock.json",
        "requirements.txt", "pyproject.toml", "Pipfile", "setup.py",
        "Cargo.toml", "go.mod", ".csproj", "Gemfile", "CMakeLists.txt", "Makefile"
    );
    
    String gitUrl = String.format("https://github.com/%s/%s.git", user, repo);
    List<String> allPaths = gitHubApi.listAllFilePaths(gitUrl);
    String repoPrefix = repo + "/";

    for (String path : allPaths) {
        String relativePath = path.startsWith(repoPrefix) ? path.substring(repoPrefix.length()) : path;
        String fileName = relativePath.substring(relativePath.lastIndexOf('/') + 1);
        if (filesToCheck.contains(fileName)) {
            try {
                System.out.println("Fetching and parsing: " + relativePath);
                String content = gitHubApi.fetchFileContent(user, repo, relativePath);
                if (content == null || content.isEmpty()) {
                    System.out.println("Empty content for file: " + relativePath);
                    continue;
                }

                switch (fileName) {
                    case "pom.xml":
                        allDependencies.addAll(parsePomXml(content));
                        break;
                    case "build.gradle":
                    case "build.gradle.kts":
                        allDependencies.addAll(parseGradle(content));
                        break;
                    case "package.json":
                        allDependencies.addAll(parsePackageJson(content));
                        break;
                    case "Gemfile":
                        allDependencies.addAll(parseGemfile(content));
                        break;
                    case ".csproj":
                        allDependencies.addAll(parseCsprojXml(content));
                        break;
                    case "pyproject.toml":
                        allDependencies.addAll(parsePyProjectToml(content));
                        break;
                    case "requirements.txt":
                        allDependencies.addAll(parseRequirementsTxt(content));
                        break;
                    case "setup.py":
                        allDependencies.addAll(parseSetupPy(content));
                        break;
                    // Add more parsers as needed
                    default:
                        System.out.println("No parser defined for file: " + fileName);
                        break;
                }
                System.out.println("Parsed " + path + ", found " + allDependencies.size() + " total dependencies so far.");
            } catch (Exception e) {
                System.err.println("Error parsing " + path + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    return allDependencies.isEmpty() ? "No build dependencies found." : String.join("\n", allDependencies);
}

    
       

    private List<String> parsePomXml(String pomXmlContent) throws Exception {
        List<String> dependencies = new ArrayList<>();
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new ByteArrayInputStream(pomXmlContent.getBytes()));

        NodeList dependencyNodes = doc.getElementsByTagName("dependency");
        for (int i = 0; i < dependencyNodes.getLength(); i++) {
            var dep = (org.w3c.dom.Element) dependencyNodes.item(i);
            String groupId = dep.getElementsByTagName("groupId").item(0).getTextContent();
            String artifactId = dep.getElementsByTagName("artifactId").item(0).getTextContent();
            dependencies.add(groupId + ":" + artifactId);
        }
        return dependencies;
    }
    
    private List<String> parseGradle(String gradleContent) {
        List<String> dependencies = new ArrayList<>();
        String[] lines = gradleContent.split("\n");
        for (String line : lines) {
            if (line.trim().startsWith("implementation") || line.trim().startsWith("api")) {
                dependencies.add(line.trim());
            }
        }
        return dependencies;
    }
    private List<String> parsePackageJson(String jsonContent) throws Exception {
        List<String> dependencies = new ArrayList<>();
        JsonNode rootNode = mapper.readTree(jsonContent);
        JsonNode depsNode = rootNode.get("dependencies");
        if (depsNode != null) {
            depsNode.fieldNames().forEachRemaining(dep -> dependencies.add(dep + ":" + depsNode.get(dep).asText()));
        }
        return dependencies;
    }

    private List<String> parseRequirementsTxt(String content) {
        List<String> dependencies = new ArrayList<>();
        String[] lines = content.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (!line.startsWith("#") && !line.isEmpty()) {
                dependencies.add(line);
            }
        }
        return dependencies;
    }

    private List<String> parsePyProjectToml(String content) {
        List<String> dependencies = new ArrayList<>();
        boolean inDeps = false;
        for (String line : content.split("\n")) {
            line = line.trim();
            if (line.startsWith("[tool.poetry.dependencies]")) {
                inDeps = true;
                continue;
            }
            if (line.startsWith("[")) {
                inDeps = false;
            }
            if (inDeps && !line.startsWith("#") && line.contains("=")) {
                String[] parts = line.split("=");
                if (parts.length >= 2) {
                    dependencies.add(parts[0].trim() + ":" + parts[1].trim());
                }
            }
        }
        return dependencies;
    }
    
    private List<String> parseGemfile(String content) {
        List<String> dependencies = new ArrayList<>();
        String[] lines = content.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("gem ")) {
                int start = line.indexOf("\"") + 1;
                int end = line.indexOf("\"", start);
                if (start > 0 && end > start) {
                    dependencies.add(line.substring(start, end));
                }
            }
        }
        return dependencies;
    }

    private List<String> parseCsprojXml(String xmlContent) throws Exception {
        List<String> dependencies = new ArrayList<>();
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new ByteArrayInputStream(xmlContent.getBytes()));
    
        NodeList packageRefs = doc.getElementsByTagName("PackageReference");
        for (int i = 0; i < packageRefs.getLength(); i++) {
            var elem = (org.w3c.dom.Element) packageRefs.item(i);
            String name = elem.getAttribute("Include");
            String version = elem.getAttribute("Version");
            dependencies.add(name + ":" + version);
        }
        return dependencies;
    }
    
    private List<String> parseSetupPy(String content) {
        List<String> dependencies = new ArrayList<>();
        for (String line : content.split("\n")) {
            line = line.trim();
            if ((line.startsWith("install_requires") || line.contains("install_requires")) && line.contains("[")) {
 
                int start = line.indexOf("[");
                int end = line.indexOf("]");
                if (start != -1 && end != -1 && end > start) {
                    String[] deps = line.substring(start + 1, end).replace("\"", "").replace("'", "").split(",");
                    for (String dep : deps) {
                        if (!dep.trim().isEmpty()) {
                            dependencies.add(dep.trim());
                        }
                    }
                }
            }
        }
        return dependencies;
    }
    
    
    
    public String displayArchitecture(BinaryTreeNode node, String url, boolean isLast) {
        StringBuilder diagram = new StringBuilder();
        buildTreeDiagram(node, "", isLast, diagram);
        return diagram.toString();
    }

    private void buildTreeDiagram(BinaryTreeNode node, String prefix, boolean isLast, StringBuilder diagram) {
        if (node == null) return;

        diagram.append(prefix)
               .append(isLast ? "└── " : "├── ")
               .append(node.type)
               .append(": ")
               .append(node.name)
               .append("\n");

        if (node.children != null && !node.children.isEmpty()) {
            for (int i = 0; i < node.children.size(); i++) {
                boolean lastChild = (i == node.children.size() - 1);
                buildTreeDiagram(node.children.get(i), prefix + (isLast ? "    " : "│   "), lastChild, diagram);
            }
        }
    }

    public void buildTree(BinaryTreeNode node, Path path) throws IOException {
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
        for (Path entry : stream) {
            String type = Files.isDirectory(entry) ? "directory" : "file";
            BinaryTreeNode child = new BinaryTreeNode(entry.getFileName().toString(), type, entry.toString());
            node.children.add(child);
            if (Files.isDirectory(entry)) {
                buildTree(child, entry);
            }
        }
    }
}

}
