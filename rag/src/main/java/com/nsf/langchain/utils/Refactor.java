package com.nsf.langchain.utils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;

import com.nsf.langchain.git.GitHubApi;
import com.nsf.langchain.model.Scoring;

import org.springframework.ai.chat.model.ChatResponse;


public class Refactor {
    @Autowired
    private ChatModel chatModel;

    @Autowired
    public Refactor(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    private static final Logger log = LoggerFactory.getLogger(Refactor.class);

    public String generateCurrentState(String repoId, String dependencies, String boundaries, String archDiagram) {
        StringBuilder sb = new StringBuilder();
        sb.append("Repository: ").append(repoId).append("\n\n");
        
        sb.append("--- Dependencies Summary ---\n");
        if (dependencies == null || dependencies.isEmpty()) {
            sb.append("No dependencies detected.\n");
        } else {
            sb.append(dependencies.trim()).append("\n");
        }
    
        sb.append("\n--- Service Boundaries ---\n");
        if (boundaries == null || boundaries.isEmpty()) {
            sb.append("No explicit service boundaries identified.\n");
        } else {
            sb.append(boundaries.trim()).append("\n");
        }
    
        sb.append("\n--- Architecture Diagram ---\n");
        if (archDiagram == null || archDiagram.isEmpty()) {
            sb.append("No architecture diagram available.\n");
        } else {
            sb.append(archDiagram.trim()).append("\n");
        }
    
        return sb.toString();
    }
    

    public String generatePainPoints(String analysisText,
                                 List<Pattern> antiPatterns,
                                 Map<String, Set<String>> dependencyGraph,
                                 List<String> configs,
                                 String fullSourceCode,
                                 List<String> requiredConfigFeatures) {

    StringBuilder painPoints = new StringBuilder("--- Identified Pain Points ---\n");

    boolean found = false;


    if (analysisText != null && antiPatterns != null) {
        for (Pattern pattern : antiPatterns) {
            var matcher = pattern.matcher(analysisText);
            while (matcher.find()) {
                found = true;
                painPoints.append("- ").append(matcher.group()).append("\n");
            }
        }
    }


    List<String> depIssues = null;
    try {
        depIssues = ServiceBoundaryUtils.analyzeDependencies(dependencyGraph);
    } catch (Exception e) {

        e.printStackTrace();
    }
    if (!depIssues.isEmpty()) {
        found = true;
        depIssues.forEach(issue -> painPoints.append("- ").append(issue).append("\n"));
    }


    List<String> configIssues = GitHubApi.checkConfigAndInfra(configs, requiredConfigFeatures);
    if (!configIssues.isEmpty()) {
        found = true;
        configIssues.forEach(miss -> painPoints.append("- ").append(miss).append("\n"));
    }


    List<String> codeWarnings = null;
    try {
        codeWarnings = GitHubApi.scanCommentsForWarnings(fullSourceCode);
    } catch (Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
    if (!codeWarnings.isEmpty()) {
        found = true;
        codeWarnings.forEach(warning -> painPoints.append("- ").append(warning).append("\n"));
    }

    if (!found) return "No pain points identified.";
    return painPoints.toString();
}

public String buildArchitectureReport(
    Set<String> matchedPatternNames,
    Set<String> matchedAntiPatternNames,
    Map<String, Double> scoringResults,
    List<Scoring> criteriaList
) {
    StringBuilder report = new StringBuilder();

    report.append("--- Architecture Analysis ---\n");
    report.append("Based on the analysis, your microservices architecture follows several architectural patterns.\n\n");

    // Matched Patterns
    if (matchedPatternNames.isEmpty()) {
        report.append("Matched Architectural Patterns: None detected.\n");
    } else {
        report.append("Matched Architectural Patterns:\n");
        matchedPatternNames.stream()
            .sorted()
            .forEach(p -> report.append("- ").append(p).append("\n"));
    }

    report.append("\n");

    // Matched Anti-Patterns
    if (matchedAntiPatternNames.isEmpty()) {
        report.append("Matched Architectural Anti-Patterns: None detected.\n");
    } else {
        report.append("Matched Architectural Anti-Patterns:\n");
        matchedAntiPatternNames.stream()
            .sorted()
            .forEach(ap -> report.append("- ").append(ap).append("\n"));
    }

    report.append("\nEvaluation Criteria and Scores:\n");

    double totalWeight = criteriaList.stream().mapToDouble(Scoring::getWeight).sum();
    double weightedTotal = 0.0;

    for (Scoring criterion : criteriaList) {
        String name = criterion.getName();
        double weight = criterion.getWeight();
        double score = scoringResults.getOrDefault(name, 0.0);
        double weighted = (weight / totalWeight) * score;
        weightedTotal += weighted;

        report.append(String.format("- %s (%.1f): %.1f/10\n", name, weight, score));
    }

    report.append(String.format("\n**Overall Score:** %.2f/10\n", weightedTotal));

    report.append("""
        
        Recommendations:
        - Increase reusability by extracting shared modules or services.
        - Improve agility via API simplification or async messaging.
        - Evaluate technology gaps and diversify where beneficial.
        - Optimize cost by consolidating infra and removing redundant pieces.

        """);

    return report.toString();
}

    

    public String generateTargetArchitecture(String painPoints, String currentStateSummary, List<Pattern> recommendedPatterns) {
        if ((painPoints == null || painPoints.isEmpty()) && (recommendedPatterns == null || recommendedPatterns.isEmpty())) {
            return currentStateSummary != null ? currentStateSummary : "No target architecture changes suggested.";
        }
    
        StringBuilder targetArch = new StringBuilder("--- Target Architecture Recommendations ---\n");
    
        if (painPoints != null && !painPoints.isEmpty()) {
            targetArch.append("Pain Points to Address:\n").append(painPoints).append("\n");
        }
    
        if (recommendedPatterns != null && !recommendedPatterns.isEmpty()) {
            targetArch.append("Recommended Architectural Patterns:\n");
            for (Pattern pattern : recommendedPatterns) {
                targetArch.append("- ").append(pattern.pattern()).append("\n");
            }
        }
    
        if (currentStateSummary != null && !currentStateSummary.isEmpty()) {
            targetArch.append("\nCurrent Architecture Summary:\n").append(currentStateSummary);
        }
    
        return targetArch.toString();
    }
    

    public String generateRefactor(String currentArchitecture, String targetArchitectureDescription) {
        String promptText = """
            You are a senior microservices architect.

            Your job is to generate a **step-by-step refactoring plan** based on the comparison of current and target architectures.

            Please include:
            1. **Service modularization** recommendations.
            2. **New components** to introduce (e.g., API Gateway, Event Bus, Config Server).
            3. Changes to **communication styles** (REST, message bus, gRPC).
            4. Suggestions for **observability**, **testing**, and **deployment** improvements.
            5. Suggested technologies (e.g., Spring Cloud Gateway, Kafka, Micrometer).

            ---
            Current Architecture:
            %s

            Target Architecture:
            %s

            Return your response as a clear, bulleted list of changes, grouped by category.
            """.formatted(currentArchitecture, targetArchitectureDescription);

        Prompt prompt = new Prompt(List.of(
            new SystemMessage("You are a helpful assistant."),
            new UserMessage(promptText)
        ));

        try {
ChatResponse response = chatModel.call(prompt);
            String result = response.getResult().getOutput().getText();

            if (result == null || result.trim().isEmpty()) {
                return fallbackRefactor(targetArchitectureDescription);
            }

            return result;
        } catch (Exception e) {
            log.warn("generateRefactor failed: {}", e.getMessage());
            return fallbackRefactor(targetArchitectureDescription);
        }
    }

    private String fallbackRefactor(String targetArchitectureDescription) {
        return """
            --- Suggested Refactor Plan (Fallback) ---

            • Introduce an API Gateway (e.g., Spring Cloud Gateway) to route external traffic.
            • Add a Config Server for centralized configuration.
            • Use an Event Bus (e.g., Kafka) for decoupled communication between services.
            • Modularize shared concerns like logging, error handling, and metrics into common libraries.
            • Apply resilience patterns (Circuit Breaker, Retry, Timeout) using Resilience4j.
            • Add distributed tracing with Micrometer + Zipkin/Prometheus.
            • Separate database schemas per service if not already done.
            • Move toward container-based deployment with Docker + Kubernetes.
            • Ensure each service has a clear domain responsibility (bounded context).

            (This fallback assumes modern Spring microservices architecture.)
            """;
    }

    public String generateTestImprovement(String currentStateSummary, String painPoints, String techStack) {
        StringBuilder sb = new StringBuilder();
        sb.append("--- Comprehensive Test Strategy Improvement Plan ---\n\n");
    
        String lowerSummary = currentStateSummary != null ? currentStateSummary.toLowerCase() : "";
        String lowerPain = painPoints != null ? painPoints.toLowerCase() : "";
        String lowerStack = techStack != null ? techStack.toLowerCase() : "";
    
        // Basic test types
        if (lowerSummary.contains("no unit test") || lowerPain.contains("missing unit test")) {
            sb.append("- Introduce **unit tests** to validate core logic isolated from external dependencies.\n");
            sb.append("  Use mocking frameworks to simulate dependencies and ensure fast feedback.\n\n");
        }
    
        if (lowerSummary.contains("no integration test") || lowerPain.contains("no service interaction")) {
            sb.append("- Add **integration tests** to verify interactions between modules and external systems.\n");
            sb.append("  Focus on contracts between services and database integration.\n\n");
        }
    
        if (lowerSummary.contains("no e2e") || lowerPain.contains("missing e2e") || lowerPain.contains("broken flows")) {
            sb.append("- Implement **end-to-end (E2E) tests** covering critical user journeys and workflows.\n");
            sb.append("  Automate using tools like Selenium, Cypress, or Playwright depending on your stack.\n\n");
        }
    
        // Test architecture and design improvements
        if (lowerPain.contains("tight coupling") || lowerPain.contains("fragile")) {
            sb.append("- Refactor to decouple components and increase test isolation.\n");
            sb.append("  Design for testability using dependency injection and interfaces.\n\n");
        }
    
        if (lowerPain.contains("slow ci") || lowerPain.contains("slow feedback")) {
            sb.append("- Optimize CI pipelines with:\n");
            sb.append("  • Test parallelization\n");
            sb.append("  • Test impact analysis to run only affected tests\n");
            sb.append("  • Lightweight smoke tests on each commit\n\n");
        }
    
     
        // ... inside your generateTestImprovement method, replace or append to the tech stack block:

if (lowerStack.contains("java") || lowerStack.contains("spring")) {
    sb.append("- Use **JUnit 5** for unit/integration tests, **Mockito** for mocks, **Testcontainers** for containers.\n");
    sb.append("- Coverage with **JaCoCo**.\n");
    sb.append("- Consider **Spring Cloud Contract** for contract testing.\n\n");
} else if (lowerStack.contains("node") || lowerStack.contains("react") || lowerStack.contains("angular") || lowerStack.contains("vue")) {
    sb.append("- Use **Jest** or **Mocha** with **Sinon** for unit testing, **Supertest** for API tests, **Cypress** or **Playwright** for E2E.\n");
    sb.append("- Coverage via **nyc (Istanbul)**.\n");
    sb.append("- Use **ESLint** and **Prettier** for static analysis.\n\n");
} else if (lowerStack.contains("python") || lowerStack.contains("flask") || lowerStack.contains("django") || lowerStack.contains("fastapi")) {
    sb.append("- Use **Pytest** for testing, **tox** for environments, **coverage.py** for coverage.\n");
    sb.append("- Use **pytest-mock**, **factory_boy**, and **hypothesis** for property-based testing.\n\n");
} else if (lowerStack.contains("go") || lowerStack.contains("golang")) {
    sb.append("- Use Go’s built-in **testing** package, **Testify** for assertions.\n");
    sb.append("- Enhance CI reports with **GoConvey** or **gotestsum**.\n\n");
} else if (lowerStack.contains("ruby") || lowerStack.contains("rails")) {
    sb.append("- Use **RSpec** for unit and integration tests, **Capybara** for acceptance tests.\n");
    sb.append("- Use **SimpleCov** for coverage.\n\n");
} else if (lowerStack.contains("c++") || lowerStack.contains("cpp")) {
    sb.append("- Use **GoogleTest (gtest)** or **Catch2** for unit testing.\n");
    sb.append("- Use **lcov** and **gcov** for coverage metrics.\n\n");
} else if (lowerStack.contains("c#") || lowerStack.contains(".net")) {
    sb.append("- Use **xUnit.net** or **NUnit** for tests, **Moq** for mocking.\n");
    sb.append("- Use **Coverlet** or **dotCover** for coverage.\n\n");
} else if (lowerStack.contains("swift") || lowerStack.contains("ios")) {
    sb.append("- Use **XCTest** framework for unit and UI testing.\n");
    sb.append("- Use **Fastlane** for automating tests and deployment.\n\n");
} else if (lowerStack.contains("kotlin")) {
    sb.append("- Use **JUnit 5**, **MockK** for mocking.\n");
    sb.append("- Use **Spek** or **KotlinTest** for BDD style testing.\n\n");
} else if (lowerStack.contains("rust")) {
    sb.append("- Use Rust’s built-in **cargo test** for unit/integration tests.\n");
    sb.append("- Use **tarpaulin** for coverage.\n\n");
} else if (lowerStack.contains("php") || lowerStack.contains("laravel") || lowerStack.contains("symfony")) {
    sb.append("- Use **PHPUnit** for testing.\n");
    sb.append("- Use **Behat** for behavior-driven development.\n\n");
} else if (lowerStack.contains("scala")) {
    sb.append("- Use **ScalaTest** or **Specs2**.\n\n");
} else {
    sb.append("- Choose modern, community-supported testing frameworks tailored to your language/framework.\n\n");
}

    

        sb.append("- Include **performance and load testing** to identify bottlenecks early.\n");
        sb.append("- Integrate **security testing** with static code analysis and vulnerability scanning.\n");
        sb.append("- Use **mutation testing** to assess test suite effectiveness.\n");
        sb.append("- Implement **continuous testing** to automatically run relevant tests on every change.\n");
        sb.append("- Monitor flakiness and failures using dashboards and alerts to maintain test reliability.\n\n");
    
  
        sb.append("- Promote a **testing culture** where developers write and maintain tests as part of the Definition of Done.\n");
        sb.append("- Train teams on best practices, test design patterns, and CI/CD pipeline optimization.\n\n");
    
        sb.append("- **Integrate tests fully into CI/CD workflows** with clear feedback loops.\n");
        sb.append("- Use test reports and metrics to drive continuous improvement.\n");
    
        return sb.toString();
    }
    

    public String generateDocument(
        String currentState,
        String painPoints,
        String targetArchitecture,
        String refactorPlan,
        String testImprovement
) {
    StringBuilder sb = new StringBuilder();

    sb.append("# Microservices Architecture Report\n\n");

    sb.append("## 1. Current Architecture State\n");
    sb.append(currentState != null && !currentState.isBlank() ? currentState : "No current state available.");
    sb.append("\n\n---\n\n");

    sb.append("## 2. Pain Points & Anti-Patterns\n");
    sb.append(painPoints != null && !painPoints.isBlank() ? painPoints : "No pain points detected.");
    sb.append("\n\n---\n\n");

    sb.append("## 3. Target Architecture Vision\n");
    sb.append(targetArchitecture != null && !targetArchitecture.isBlank() ? targetArchitecture : "Target architecture not yet defined.");
    sb.append("\n\n---\n\n");

    sb.append("## 4. Refactoring Recommendations\n");
    sb.append(refactorPlan != null && !refactorPlan.isBlank() ? refactorPlan : "No refactoring plan available.");
    sb.append("\n\n---\n\n");

    sb.append("## 5. Test Strategy Improvements\n");
    sb.append(testImprovement != null && !testImprovement.isBlank() ? testImprovement : "No test improvements identified.");
    sb.append("\n\n---\n\n");

    return sb.toString();
}

}
