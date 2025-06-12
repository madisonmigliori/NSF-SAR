package com.nsf.langchain.ParsingCode.GitToken;

import java.io.File;

public class GetToken {
    private String token;
    
    public GetToken(){
        try {
            this.token = GitHubAppAuth.generateJWT("1393268", new File("langchain/src/main/resources/nsf-sar.2025-06-12.pkcs8.pem"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getToken(){
        return this.token;
    }

}
