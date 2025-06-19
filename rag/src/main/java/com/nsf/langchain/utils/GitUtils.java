
package com.nsf.langchain.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

public class GitUtils {

    public static void clone(String url, Path baseDir, String repoId) throws IOException, InterruptedException {
        runGit(baseDir, "clone", url, repoId);
    }

    public static void pull(Path repoDir) throws IOException, InterruptedException {
        runGit(repoDir, "pull");
    }

    private static void runGit(Path dir, String... args) throws IOException, InterruptedException {
        String[] cmd = new String[args.length + 1];
        cmd[0] = "git";
        System.arraycopy(args, 0, cmd, 1, args.length);
        var pb = new ProcessBuilder(cmd)
                   .directory(dir.toFile())
                   .redirectErrorStream(true);
        var p = pb.start();
        try (var rdr = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = rdr.readLine()) != null) {
                System.out.println("[git] " + line);
            }
        }
        if (!p.waitFor(3, TimeUnit.MINUTES)) {
            p.destroyForcibly();
            throw new RuntimeException("git timed out: " + String.join(" ", cmd));
        }
        if (p.exitValue() != 0) {
            throw new RuntimeException("git failed (" + p.exitValue() + ")");
        }
    }
}
