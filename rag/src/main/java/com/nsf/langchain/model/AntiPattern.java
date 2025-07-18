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
    private String guidance;

    private transient Pattern detectionRegex;
    private boolean requiresNegativeMatch;
    private String negativeMatchTerm; 

    public void initDetectionRegex() {
        switch (name.toLowerCase()) {
            case "esb usage":
                detectionRegex = Pattern.compile("\\b(enterprise service bus|esb|enterprise service bus|esb|mule|wso2|tibco|oracle service bus|sap pi|integration bus\n" + //
                                        ")\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "too many standards":
                detectionRegex = Pattern.compile("\\b(multiple frameworks|inconsistent standards| duplicate stack|tech sprawl|mixed libraries\n" + //
                                        ")\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "wrong cuts":
                detectionRegex = Pattern.compile("\\b(technical layers|data/business/presentation|layered service split|non-domain split|controller-service-repo|vertical slicing error\n" + //
                                        ")\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "hard-coded endpoints":
                detectionRegex = Pattern.compile("\\b(hardcoded ip|hard-coded ip|hardcoded port|hard-coded port|static ip|static port|hardcoded ip|hard-coded ip|hardcoded port|static ip|static port|inline address|localhost with port\n" + //
                                        ")\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "api versioning":
                detectionRegex = Pattern.compile("\\b(api versioning|backward compatibility|api backward compatibility|no versioning|missing version|v1 hardcoded|breaking changes api|backward incompatibility\n" + //
                                        ")\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "microservice greedy":
                detectionRegex = Pattern.compile("\\b(microservice greedy|excessive fragmentation|too many microservices|microservice overhead)\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "shared persistency":
                detectionRegex = Pattern.compile("\\b(shared database|shared persistency|shared persistence|shared datastore|data coupling|violation of service boundaries)\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "inappropriate service intimacy":
                detectionRegex = Pattern.compile("\\b(service intimacy|private data access|breaking encapsulation|tight coupling|common lib|shared lib|tight coupling via code|core-utils|framework inheritance|monolithic library\n" + //
                                        ")\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "shared libraries":
                detectionRegex = Pattern.compile("\\b(shared libraries|common libraries|library coupling|low modularity|common lib|shared lib|tight coupling via code|core-utils|framework inheritance|monolithic library\n" + //
                                        ")\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "cyclic dependency":
                detectionRegex = Pattern.compile("\\b(cyclic dependency|cyclic calls|circular dependency|complex call chains|dependency cycle|cyclic call|circular service reference|a -> b -> c -> a|dependency loop|circular import\n" + //
                                        ")\\b", Pattern.CASE_INSENSITIVE);
                break;
                case "distributed monolith":
                detectionRegex = Pattern.compile("\\b(distributed monolith|tight synchronous communication|mandatory cross-service calls|interdependent services|services fail together)\\b", Pattern.CASE_INSENSITIVE);
                break;
            
            case "over-microservices":
                detectionRegex = Pattern.compile("\\b(over-microservices|too many services|service explosion|fragmented system|coordination cost)\\b", Pattern.CASE_INSENSITIVE);
                break;
            
            case "violating single responsibility":
                detectionRegex = Pattern.compile("\\b(multiple concerns|entangled logic|poor separation|unrelated concerns in one service|violates SRP)\\b", Pattern.CASE_INSENSITIVE);
                break;
            
            case "spaghetti architecture":
                detectionRegex = Pattern.compile("\\b(spaghetti architecture|tangled dependencies|unmanaged interactions|dense dependency graph|no clear boundaries)\\b", Pattern.CASE_INSENSITIVE);
                break;
            
            case "distributed data inconsistency":
                detectionRegex = Pattern.compile("\\b(distributed data inconsistency|eventual consistency issue|data integrity problem|redundant data|sync issue)\\b", Pattern.CASE_INSENSITIVE);
                break;
            
            case "tight coupling":
                detectionRegex = Pattern.compile("\\b(tight coupling|must deploy together|shared implementation|coordinated updates|low flexibility)\\b", Pattern.CASE_INSENSITIVE);
                break;
            
            case "ignoring human costs":
                detectionRegex = Pattern.compile("\\b(human cost|team burnout|cognitive load|overwhelming complexity|team skill mismatch)\\b", Pattern.CASE_INSENSITIVE);
                break;
            
            case "monolithic mindset in microservices":
                detectionRegex = Pattern.compile("\\b(monolithic mindset|monolith in disguise|centralized database|shared state|monolithic thinking)\\b", Pattern.CASE_INSENSITIVE);
                break;
            
            case "data monolith":
                detectionRegex = Pattern.compile("\\b(data monolith|centralized database|tight data coupling|shared schema|db dependency)\\b", Pattern.CASE_INSENSITIVE);
                break;
            
            case "chatty communication":
                detectionRegex = Pattern.compile("\\b(chatty communication|high traffic between services|fine-grained messages|frequent calls|message overload)\\b", Pattern.CASE_INSENSITIVE);
                break;
            
            case "inadequate service boundaries":
                detectionRegex = Pattern.compile("\\b(inadequate boundaries|unclear responsibilities|overlapping concerns|modularity issues|wrong granularity)\\b", Pattern.CASE_INSENSITIVE);
                break;
            
            case "service sprawl":
                detectionRegex = Pattern.compile("\\b(service sprawl|excessive microservices|too many services|uncontrolled growth|complexity overload)\\b", Pattern.CASE_INSENSITIVE);
                break;
            
            case "over-reliance on orchestration":
                detectionRegex = Pattern.compile("\\b(over-reliance on orchestration|tied to orchestrator|platform dependency|limited portability|orchestration bottleneck)\\b", Pattern.CASE_INSENSITIVE);
                break;
            
            case "ignoring observability":
                requiresNegativeMatch = true;
                negativeMatchTerm = "distributed tracing|jaeger|zipkin|opentelemetry|x-ray";
                detectionRegex = Pattern.compile("\\b(ignoring observability|no monitoring|missing logs|no tracing|lack of metrics|debugging blind)\\b", Pattern.CASE_INSENSITIVE);
                break;
            
            case "not having an api gateway":
                requiresNegativeMatch = true;
                negativeMatchTerm = "api gateway|gateway|api-gateway|api_gateway";
                detectionRegex = Pattern.compile("\\b(direct communication among services|no gateway|client-to-service calls|missing ingress controller\n" + //
                                        ")\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "missing distributed tracing":
                requiresNegativeMatch = true;
                negativeMatchTerm = "distributed tracing|jaeger|zipkin|opentelemetry|x-ray|micrometer|otel";
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
                ", guidance='" + guidance + '\'' +
                ", requiresNegativeMatch=" + requiresNegativeMatch +
                ", negativeMatchTerm='" + negativeMatchTerm + '\'' +
                
                '}';
    }

}
