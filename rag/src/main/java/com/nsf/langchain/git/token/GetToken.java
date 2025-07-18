package com.nsf.langchain.git.token;

import java.io.File;
import io.github.cdimascio.dotenv.Dotenv;

public class GetToken {
    private String token;
    
    public GetToken(){
        try {
            Dotenv dotenv = Dotenv.load();
            // String key = dotenv.get("GITHUB_API_TOKEN");
            this.token = GitHubAppAuth.generateJWT("1393268", new File("src/main/resources/nsf-sar.2025-06-24.pkcs8.pem"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getToken(){
        return this.token;
    }

}
