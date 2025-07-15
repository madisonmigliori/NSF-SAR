package com.nsf.langchain.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MSAPattern {
    private String name;
    private String description;
    private String advantage;
    private String disadvantage;

    @JsonProperty("common implementations")
    private String common;

    private transient Pattern detectionRegex;

        
    public void initDetectionRegex() {
        switch (name.toLowerCase()) { 
            case "proxy":
            detectionRegex = Pattern.compile("\\b(proxy service|intermediary service|routing layer|consul|zookeeper|wso2)\\b", Pattern.CASE_INSENSITIVE);
            break;

            case "sidecar":
            detectionRegex = Pattern.compile("\\b(sidecar|envoy|istio|netflix prana|app mesh|sidecar proxy|sidecar container)\\b", Pattern.CASE_INSENSITIVE);
            break;

            case "bulkhead":
            detectionRegex = Pattern.compile("\\b(bulkhead|isolated thread pool|separate connection pool|hystrix)\\b", Pattern.CASE_INSENSITIVE);
            break;

            case "strangler fig":
            detectionRegex = Pattern.compile("\\b(strangler fig|incremental migration|conditional routing|legacy replacement|monolith decomposition)\\b", Pattern.CASE_INSENSITIVE);
            break;


            case "shadow deployment":
                detectionRegex = Pattern.compile("\\b(shadow deployment|traffic mirroring|load balancer|istio|route 53)\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "retry":
                detectionRegex = Pattern.compile("\\b(retry|exponential backoff|jitter|hystrix|spring cloud retry|polly)\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "message broker":
                detectionRegex = Pattern.compile("\\b(kafka|rabbitmq|\\bsns|\\bsqs|google pub/sub|message broker)\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "message queue":
                detectionRegex = Pattern.compile("\\b(rabbitmq|sqs|queue storage|message queue)\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "sequential convoy":
                detectionRegex = Pattern.compile("\\b(sequential convoy|fifo|partitioned kafka|service bus sessions|azure service bus sessions|qs fifo|kafka partition key|message ordering)\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "competing consumers":
                detectionRegex = Pattern.compile("\\b(competing consumers|consumer groups|rabbitmq|kafka)\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "scheduler agent supervisor":
                detectionRegex = Pattern.compile("\\b(scheduler agent|cronjob|quartz scheduler|eventbridge)\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "process manager":
                detectionRegex = Pattern.compile("\\b(process manager|saga|camunda|netflix conductor)\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "api gateway":
                detectionRegex = Pattern.compile("\\b(api gateway|netflix zuul|aws api gateway|azure api management|kong)\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "backends for frontends":
                detectionRegex = Pattern.compile("\\b(backends for frontends|bff|node\\.js|spring boot)\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "aggregator":
                detectionRegex = Pattern.compile("\\b(aggregator|api gateway|response aggregation|graphql)\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "client-side ui composition":
                detectionRegex = Pattern.compile("\\breact\\b|\\bangular\\b|\\bvue\\b|\\bgraphql\\b|\\brest\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "chain of responsibility":
                detectionRegex = Pattern.compile("\\b(chain of responsibility|workflow|pipeline|rest|grpc|messaging)\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "command query responsibility segregation":
            case "cqrs":
                detectionRegex = Pattern.compile("\\b(cqrs|command query responsibility segregation|event sourcing|denormalized|read model)\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "event sourcing":
                detectionRegex = Pattern.compile("\\b(event sourcing|eventstore|kafka|snapshots|event log)\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "saga":
                detectionRegex = Pattern.compile("\\b(saga|compensating transaction|netflix conductor|camunda|choreography|orchestration)\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "polyglot persistence":
                detectionRegex = Pattern.compile("\\b(polyglot persistence|multiple databases|mongodb|postgresql|redis|elasticsearch|cassandra)\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "data sharding":
                detectionRegex = Pattern.compile("\\b(sharding|partitioning|hotspot|resharding|twitter|instagram)\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "database per service":
                detectionRegex = Pattern.compile("\\b(database per service|service database|dedicated database|amazon|netflix|uber)\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "health check":
                detectionRegex = Pattern.compile("\\b(health check|actuator|liveness|readiness|kubernetes probe)\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "distributed tracing":
                detectionRegex = Pattern.compile("\\b(distributed tracing|opentelemetry|jaeger|zipkin|x-ray|sleuth)\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "log aggregation":
                detectionRegex = Pattern.compile("\\b(log aggregation|elk|logstash|kibana|fluentd|cloudwatch|loki)\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "performance metrics":
                detectionRegex = Pattern.compile("\\b(prometheus|metrics|new relic|appdynamics|datadog|micrometer|grafana)\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "circuit breaker":
                detectionRegex = Pattern.compile("\\b(circuit breaker|hystrix|resilience4j|polly|istio|spring cloud circuit breaker)\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "blue-green deployment":
                detectionRegex = Pattern.compile("\\b(blue-green deployment|kubernetes|spinnaker|aws elastic beanstalk|jenkins x)\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "service discovery":
                detectionRegex = Pattern.compile("\\b(service discovery|eureka|consul|etcd|cloud map|istio|zookeeper)\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "fail fast":
                detectionRegex = Pattern.compile("\\b(fail fast|fast fail|immediate termination)\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "fallback":
                detectionRegex = Pattern.compile("\\b(fallback|graceful degradation)\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "throttling":
                detectionRegex = Pattern.compile("\\b(throttling|rate limiting|request limiting)\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "load leveling":
                detectionRegex = Pattern.compile("\\b(load leveling|queue buffer|burst handling)\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "priority queue":
                detectionRegex = Pattern.compile("\\b(priority queue|priority levels|starvation)\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "timeout":
                detectionRegex = Pattern.compile("\\btimeout\\b|\\bdeadline\\b|\\bRestTemplate\\b|\\bretrofit\\b|\\bfeign\\b|\\bgrpc deadline\\b|\\bkubernetes probe\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "valet key":
                detectionRegex = Pattern.compile("\\b(valet key|shared access signature|sas|pre-signed url|google cloud signed url)\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "dead letter queue":
                detectionRegex = Pattern.compile("\\b(dead letter queue|dlq|dead letter exchange|dlx|fault isolation)\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "load balancer":
                detectionRegex = Pattern.compile("\\b(load balancer|elb|nginx|haproxy|azure load balancer)\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "cache aside":
                detectionRegex = Pattern.compile("\\b(cache aside|redis|memcached|application-managed cache)\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "autoscaling":
                detectionRegex = Pattern.compile("\\b(autoscaling|horizontal pod autoscaler|aws auto scaling)\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "event-driven autoscaling":
                detectionRegex = Pattern.compile("\\b(event-driven autoscaling|keda|lambda sqs trigger)\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "token-based authentication":
                detectionRegex = Pattern.compile("\\b(token-based authentication|jwt|oauth 2.0)\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "api gateway with security enforcement":
                detectionRegex = Pattern.compile("\\b(api gateway security|kong|apigee|aws api gateway)\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "claims-based authorization":
                detectionRegex = Pattern.compile("\\b(claims-based authorization|jwt claims|role based access)\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "metrics collection and monitoring":
                detectionRegex = Pattern.compile("\\b(metrics collection|prometheus|grafana|cloudwatch)\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "correlation ids":
                detectionRegex = Pattern.compile("\\b(correlation id|trace id|request id|opentelemetry context)\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "canary deployment":
                detectionRegex = Pattern.compile("\\b(canary deployment|istio|aws codedeploy|argo rollouts)\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "rolling deployment":
                detectionRegex = Pattern.compile("\\b(rolling deployment|kubernetes rolling update|spinnaker|helm)\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "synchronous communication":
                detectionRegex = Pattern.compile("\\b(synchronous communication|restful api|grpc)\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "asynchronous communication":
                detectionRegex = Pattern.compile("\\b(asynchronous communication|rabbitmq|kafka|aws sqs)\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "event-driven communication":
                detectionRegex = Pattern.compile("\\b(event-driven|event bus|kafka|eventbridge|event grid)\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "service mesh":
                detectionRegex = Pattern.compile("\\b(service mesh|istio|linkerd|consul connect|sidecar proxy)\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "rest api":
                detectionRegex = Pattern.compile("\\b(rest api|http api|spring boot rest|express\\.js|flask api)\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "static content hosting":
                detectionRegex = Pattern.compile("\\b(static content|s3|cloudfront|azure blob storage|azure cdn|google cloud storage|cloud cdn|netlify|vercel)\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "gateway routing":
                detectionRegex = Pattern.compile("\\b(api gateway|nginx|haproxy|istio ingress|azure front door|application gateway)\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "gateway aggregation":
                detectionRegex = Pattern.compile("\\b(aggregation|aggregator|graphql|api gateway aggregation|response aggregation)\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "publisher/subscriber":
                detectionRegex = Pattern.compile("\\b(kafka|rabbitmq|sns|sqs|pub/sub|event bus|event grid)\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "stateless services":
                detectionRegex = Pattern.compile("\\b(stateless service|stateless api|jwt|sessionless|idempotent|external cache|redis|memcached)\\b", Pattern.CASE_INSENSITIVE);
                break;

            case "domain-driven design":
                detectionRegex = Pattern.compile("\\b(domain[- ]?driven design|ddd|bounded context|aggregate root|ubiquitous language)\\b", Pattern.CASE_INSENSITIVE);
                break;
            case "consumer-driven contracts":
                detectionRegex = Pattern.compile("\\b(pact|spring cloud contract|hoverfly|wiremock|consumer[- ]driven contracts?)\\b", Pattern.CASE_INSENSITIVE);
                break;
            
            case "business capability":
                detectionRegex = Pattern.compile("\\b(business capability|domain service|sales service|marketing service|accounting service|capability aligned)\\b", Pattern.CASE_INSENSITIVE);
                break;
            
            case "anti-corruption layer":
                detectionRegex = Pattern.compile("\\b(anti[- ]?corruption layer|acl|dto|adapter pattern|translation layer|mapper service)\\b", Pattern.CASE_INSENSITIVE);
                break;
            
           

            case "transaction":
                detectionRegex = Pattern.compile("\\btransaction\\b", Pattern.CASE_INSENSITIVE);
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
        return getMatchSnippet(content) != null;
    }
    

    public String getMatchSnippet(String content) {
        if (detectionRegex != null) {
            Matcher matcher = detectionRegex.matcher(content);
            if (matcher.find()) {
                return matcher.group();
            }
        }
        return null;
    }
    
    public String getMatchSnippetDep(String content, String dep) {
        if (detectionRegex != null) {
    
            Matcher contentMatcher = detectionRegex.matcher(content);
            if (contentMatcher.find()) {
                return "Matched in content: " + contentMatcher.group();
            }
            Matcher depMatcher = detectionRegex.matcher(dep);
            if (depMatcher.find()) {
                return "Matched in dependencies: " + depMatcher.group();
            }
        }
        return null;
    }

    @Override
    public String toString(){
     return String.format("""
             === Patterns ===
             Name: %s

             Description: %s

             Advantage: %s

             Disadvantages: %s

             Common Implemenations: %s

             """, name, description, advantage, disadvantage, common);
    }
}


