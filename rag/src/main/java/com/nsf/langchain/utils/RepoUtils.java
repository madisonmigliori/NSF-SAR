package com.nsf.langchain.utils;

//Extracts the URL to get information about the repo for the LLM
public class RepoUtils {

    public static String extractRepoId(String url) {
        try {
            // Regex to match GitHub URLs (HTTPS, SSH, git@, with or without .git)
            // Examples:
            // https://github.com/user/repo.git
            // git@github.com:user/repo.git
            // https://github.com/user/repo
            // git@github.com:user/repo
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "(?:https://|http://|git@)([^/:]+)[:/]{1,2}([^/]+)/([^/]+?)(?:\\.git)?$"
            );
            java.util.regex.Matcher matcher = pattern.matcher(url.trim());
            if (matcher.find()) {
                String host = matcher.group(1);
                String user = matcher.group(2);
                String repo = matcher.group(3);
                // Only process github.com for now, but can be extended for other hosts
                if (host.contains("github.com")) {
                    return user + "-" + repo;
                }
            }
        } catch (Exception e) {
            return "Sorry, I couldn't process github URL right now.";
        }
        return "default";
    }
}
