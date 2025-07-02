package com.nsf.langchain.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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


    public String getDependency(String user, String repo) {
        List<String> allDependencies = new ArrayList<>();
        List<String> filesToCheck = List.of("pom.xml", "build.gradle", "package.json");

        for (String file : filesToCheck) {
            try {
                String content = gitHubApi.fetchFileContent(user, repo, file);
                if (file.endsWith("pom.xml")) {
                    allDependencies.addAll(parsePomXml(content));
                } else if (file.endsWith("build.gradle")) {
                    allDependencies.addAll(parseGradle(content));
                } else if (file.endsWith("package.json")) {
                    allDependencies.addAll(parsePackageJson(content));
                }
            } catch (Exception e) {
                continue;
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
