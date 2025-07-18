// package com.nsf.langchain;
// import com.nsf.langchain.git.BinaryTreeNode;
// import com.nsf.langchain.git.GitHubApi;
// import com.nsf.langchain.service.IngestionService;

// import org.junit.jupiter.api.Test;

// import java.util.List;

// import static org.junit.jupiter.api.Assertions.*;

// class BinaryTreeFlattenerTest {

//     private final GitHubApi gitHubApiHello = new GitHubApi(); // Replace with your actual service class that contains flattenFilePaths()

//     @Test
//     void testFlattenFilePaths() {

//         BinaryTreeNode root = new BinaryTreeNode("root", "directory", "url-root");

//         BinaryTreeNode src = new BinaryTreeNode("src", "directory", "url-src");
//         BinaryTreeNode mainJava = new BinaryTreeNode("Main.java", "file", "url-main", "class Main {}");
//         BinaryTreeNode utilsJava = new BinaryTreeNode("Utils.java", "file", "url-utils", "class Utils {}");

//         BinaryTreeNode docs = new BinaryTreeNode("docs", "directory", "url-docs");
//         BinaryTreeNode readme = new BinaryTreeNode("README.md", "file", "url-readme", "# Readme content");

//         src.children.add(mainJava);
//         src.children.add(utilsJava);
//         docs.children.add(readme);
//         root.children.add(src);
//         root.children.add(docs);

//         List<String> paths = gitHubApiHello.flattenFilePaths(root);

//         List<String> expectedPaths = List.of(
//                 "root/src/Main.java",
//                 "root/src/Utils.java",
//                 "root/docs/README.md"
//         );

//         assertEquals(expectedPaths.size(), paths.size(), "Mismatch in number of file paths returned");
//         assertTrue(paths.containsAll(expectedPaths), "Returned paths do not match expected file paths");

//         paths.forEach(System.out::println);
//     }

//     @Test
// void testRepoArchitectureTreeStructure() {

//     BinaryTreeNode root = new BinaryTreeNode("root", "directory", "url-root");

//     BinaryTreeNode src = new BinaryTreeNode("src", "directory", "url-src");
//     BinaryTreeNode mainJava = new BinaryTreeNode("Main.java", "file", "url-main", "class Main {}");

//     BinaryTreeNode readme = new BinaryTreeNode("README.md", "file", "url-readme", "# Readme content");

//     src.children.add(mainJava);
//     root.children.add(src);
//     root.children.add(readme);

//     List<String> paths = gitHubApiHello.flattenFilePaths(root);

//     List<String> expectedPaths = List.of(
//             "root/src/Main.java",
//             "root/README.md"
//     );

//     assertEquals(expectedPaths.size(), paths.size(), "Mismatch in number of files found");
//     assertTrue(paths.containsAll(expectedPaths), "Flattened paths don't match expected architecture");
// }


// }
