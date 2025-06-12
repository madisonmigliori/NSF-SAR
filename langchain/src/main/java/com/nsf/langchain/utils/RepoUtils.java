package com.nsf.langchain.utils;

//Extracts the URL to get information about the repo for the LLM
public class RepoUtils {

    public static String extractRepoId(String url) {
        try {
            String[] parts = url.replace("https://", "")
                                .replace("http://", "")
                                .split("/");
            if (parts.length >= 3 && parts[0].contains("github.com")) {
                String user = parts[1];
                String repo = parts[2].replace(".git", "");
                return user + "-" + repo; 
            }
        } catch (Exception e) {
            return "Sorry, I couldn't process github URL right now.";
        }
        return "default";
    }
}
