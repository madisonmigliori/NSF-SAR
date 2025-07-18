package com.nsf.langchain.service;

import java.util.List;
import java.util.Set;

import com.nsf.langchain.git.BinaryTreeNode;
import com.nsf.langchain.utils.ArchitectureUtils;
import com.nsf.langchain.utils.ServiceBoundaryUtils;

public class EmbeddingUtils {
    private String dependencies;

    private static final Set<String> SKIPPED_EXTENSIONS = Set.of(
        ".jar", ".class", ".exe", ".png", ".jpg", ".jpeg", ".gif",
        ".zip", ".tar", ".gz", ".min.js", ".map"
    );

    private static final Set<String> TEXTUAL_EXTENSIONS = Set.of(
        ".java", ".py", ".js", ".ts", ".cpp", ".c", ".go", ".rs",
        ".html", ".css", ".md", ".txt", ".xml", ".yml", ".yaml", ".json", ""
    );

    private static final Set<String> SERVICE_BOUNDARY = Set.of(
        ".java", ".py", ".js", ".go", ".rs", ".cpp",
        ".rb", ".jsx"
    );

    private static final Set<String> DEPENDENCY = Set.of(
        "pom.xml", "build.gradle", "build.gradle.kts",
        "package.json", "package-lock.json",
        "requirements.txt", "pyproject.toml", "Pipfile", "setup.py",
        "Cargo.toml", "go.mod", "Gopkg.toml", ".csproj", "Gemfile", "CMakeLists.txt", "Makefile"
    );

    public enum EmbedDecision {
        EMBED_DIRECTLY,
        EMBED_CHUNKED,
        SKIP_FILE,
        SUMMARIZE_ONLY
    }

    public static EmbedDecision shouldEmbed(BinaryTreeNode node, ChromaVectorStoreManager vectorManager) throws Exception {
        String name = node.getName();
        int contentLength = Integer.valueOf(node.getSize());

        for (String ext : SKIPPED_EXTENSIONS) {
            if (name.endsWith(ext)) return EmbedDecision.SKIP_FILE;
        }

        if (DEPENDENCY.contains(name)) {
            ArchitectureUtils architectureUtils = new ArchitectureUtils();
            String dependencies = architectureUtils.getDependency(name, node.getContent());
            vectorManager.setDependencies(dependencies, name);
        }

        for (String boundary : SERVICE_BOUNDARY) {
            if (name.endsWith(boundary)){
                ServiceBoundaryUtils serviceBoundary = new ServiceBoundaryUtils();
                List<ServiceBoundaryUtils.ServiceBoundary> artifacts = serviceBoundary.extractCode(node);
                ServiceBoundaryUtils.ArchitectureMap archMap = serviceBoundary.fallback(artifacts);
                vectorManager.setServiceBoundary(artifacts, name);
                vectorManager.setArchMap(archMap, name);
            }
        }

        // if (contentLength > 10000000) {
        //     // return EmbedDecision.SKIP_FILE; // too large to handle safely
        //     return EmbedDecision.SUMMARIZE_ONLY;
        // } else if (contentLength > 10000) {
        //     return EmbedDecision.EMBED_CHUNKED;
        // } else {
        //     return EmbedDecision.EMBED_DIRECTLY;
        // }
        return EmbedDecision.EMBED_DIRECTLY;
    }
}
