package com.nsf.langchain.git.token;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import java.io.File;
import java.nio.file.Files;
import java.security.interfaces.RSAPrivateKey;
import java.security.KeyFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;

public class GitHubAppAuth {
    public static String generateJWT(String appId, File privateKeyPem) throws Exception {
        String privateKeyContent = new String(Files.readAllBytes(privateKeyPem.toPath()))
                .replaceAll("-----BEGIN PRIVATE KEY-----", "")
                .replaceAll("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");

        byte[] keyBytes = Base64.getDecoder().decode(privateKeyContent);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        RSAPrivateKey privateKey = (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(keySpec);

        Instant now = Instant.now();
        return GitHubTokenFetcher.getInstallationToken( JWT.create()
                .withIssuer(appId)
                .withIssuedAt(now)
                .withExpiresAt(now.plusSeconds(540)) // JWT valid for 9 minutes
                .sign(Algorithm.RSA256(null, privateKey)));
    }
}
