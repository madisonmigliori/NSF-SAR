package com.nsf.langchain.utils;
public class RepoUtils {

    public static String[] extractRepoId(String url) {
        try {

            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "(?:https://|http://|git@)([^/:]+)[:/]{1,2}([^/]+)/([^/]+?)(?:\\.git)?$"
            );
            java.util.regex.Matcher matcher = pattern.matcher(url.trim());
            if (matcher.find()) {
                String host = matcher.group(1);
                String user = matcher.group(2);
                String repo = matcher.group(3);
                if (host.contains("github.com")) {
                    String[] repoInfo = {user, repo};
                    return repoInfo;
                }
            }
        } catch (Exception e) {
            String[] notFound = {"Sorry, I couldn't process github URL right now."};
            return notFound;
            // return "Sorry, I couldn't process github URL right now.";
        }
        return new String[]{"default"};
    }
}