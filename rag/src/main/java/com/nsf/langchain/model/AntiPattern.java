package com.nsf.langchain.model;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AntiPattern {
    private String name;
    private String description;
    private List<String> impact;
    private String severity;

    private transient Pattern detectionRegex;
    private boolean requiresNegativeMatch;
    private String negativeMatchTerm; 

    public void initDetectionRegex() {
        switch (name.toLowerCase()) {
            case "esb usage":
                detectionRegex = Pattern.compile("\\b(enterprise service bus|esb)\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "too many standards":
                detectionRegex = Pattern.compile("\\b(multiple frameworks|inconsistent standards)\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "wrong cuts":
                detectionRegex = Pattern.compile("\\b(technical layers|data/business/presentation)\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "hard-coded endpoints":
                detectionRegex = Pattern.compile("\\b(hardcoded ip|hard-coded ip|hardcoded port|hard-coded port|static ip|static port)\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "api versioning":
                detectionRegex = Pattern.compile("\\b(api versioning|backward compatibility|api backward compatibility)\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "microservice greedy":
                detectionRegex = Pattern.compile("\\b(microservice greedy|excessive fragmentation|too many microservices|microservice overhead)\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "shared persistency":
                detectionRegex = Pattern.compile("\\b(shared database|shared persistency|shared persistence|shared datastore|data coupling|violation of service boundaries)\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "inappropriate service intimacy":
                detectionRegex = Pattern.compile("\\b(service intimacy|private data access|breaking encapsulation|tight coupling)\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "shared libraries":
                detectionRegex = Pattern.compile("\\b(shared libraries|common libraries|library coupling|low modularity)\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "cyclic dependency":
                detectionRegex = Pattern.compile("\\b(cyclic dependency|cyclic calls|circular dependency|complex call chains|dependency cycle)\\b", Pattern.CASE_INSENSITIVE);
                break;
         

            case "not having an api gateway":
                requiresNegativeMatch = true;
                negativeMatchTerm = "api gateway|gateway|api-gateway|api_gateway";
                detectionRegex = Pattern.compile("\\b(direct communication among services)\\b", Pattern.CASE_INSENSITIVE);
                break;
            
            case "missing distributed tracing":
                requiresNegativeMatch = true;
                negativeMatchTerm = "distributed tracing|jaeger|zipkin|opentelemetry|x-ray";
                break;
            case "no service discovery":
                requiresNegativeMatch = true;
                negativeMatchTerm = "eureka|consul|etcd|zookeeper|service registry";
                break;
            case "no health checks":
                requiresNegativeMatch = true;
                negativeMatchTerm = "actuator|liveness|readiness|kubernetes probe";
                break;
            default:
                detectionRegex = Pattern.compile("\\b" + Pattern.quote(name) + "\\b", Pattern.CASE_INSENSITIVE);
                break;
        }
    }
    

    // public boolean matches(String content) {
    //     if (detectionRegex == null) {
    //         initDetectionRegex(); 
    //     }
    //     return detectionRegex.matcher(content).find();
    // }

  
    public boolean matches(String content) {
        if (requiresNegativeMatch) {
            return negativeMatchTerm != null && !Pattern.compile(negativeMatchTerm, Pattern.CASE_INSENSITIVE).matcher(content).find();
        }
        return getMatchSnippet(content) != null;
    }
    
    public String getMatchSnippet(String content) {
        if (requiresNegativeMatch) {
            boolean missing = !Pattern.compile(negativeMatchTerm, Pattern.CASE_INSENSITIVE).matcher(content).find();
            return missing ? "(missing: " + negativeMatchTerm + ")" : null;
        }
    
        if (detectionRegex != null) {
            Matcher matcher = detectionRegex.matcher(content);
            if (matcher.find()) {
                return matcher.group();
            }
        }
        return null;
    }

    public boolean matches(String content, String fullProjectContent) {
        if (requiresNegativeMatch) {
            return negativeMatchTerm != null &&
                   !Pattern.compile(negativeMatchTerm, Pattern.CASE_INSENSITIVE)
                           .matcher(fullProjectContent).find();
        }
        return getMatchSnippet(content) != null;
    }

    public String getMatchSnippet(String content, String fullProjectContent) {
        if (requiresNegativeMatch) {
            boolean missing = !Pattern.compile(negativeMatchTerm, Pattern.CASE_INSENSITIVE)
                                      .matcher(fullProjectContent).find();
            return missing ? "(missing: " + negativeMatchTerm + ")" : null;
        }
        return getMatchSnippet(content); 
    }
    
    


    @Override
    public String toString() {
        return "AntiPattern{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", impact=" + impact +
                ", severity='" + severity + '\'' +
                ", requiresNegativeMatch=" + requiresNegativeMatch +
                ", negativeMatchTerm='" + negativeMatchTerm + '\'' +
                '}';
    }

}
