package updater.process;

import arc.files.*;
import arc.func.*;
import arc.struct.*;
import arc.util.*;
import org.apache.tools.ant.filters.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import updater.*;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import java.io.*;
import java.security.*;

public class ProjectProcessor{
    public static void process(String repoOwner,
                               String repoName,
                               ZipFi zip,
                               String repoVersion, Seq<Fi> filesToHash) throws IOException, InterruptedException, NoSuchAlgorithmException, SAXException{
        Fi innerFolder = zip.list()[0];
        Fi sourceFolder = Vars.sources.child(repoOwner).child(repoName).child("unzip-sources");
        sourceFolder.deleteDirectory();
        unzip:
        {
            String prefix = innerFolder.absolutePath() + "/";
            innerFolder.walk(it -> {
                it.copyTo(sourceFolder.child(it.absolutePath().substring(prefix.length())));
            });
        }
        Fi tmpRepository = Vars.sources.child("tmpRepository");
        tmpRepository.deleteDirectory();

        initBuildSrc:
        {
            Fi buildSrc = sourceFolder.child("buildSrc");
            String rawBuildGradle = null;
            if(buildSrc.child("build.gradle").exists()){
                rawBuildGradle = buildSrc.child("build.gradle").readString();
            }
            Vars.innerBuildSrc.copyFilesTo(buildSrc);
            if(rawBuildGradle != null){
                Fi myBuildGradle = Vars.innerBuildSrc.child("build.gradle");
                String[] parts = myBuildGradle.readString().split("//split");
                buildSrc.child("build.gradle").writeString(parts[0] + rawBuildGradle + parts[1]);
            }
            Vars.innerBuildSrc.child("gradlew.sh").copyTo(sourceFolder.child("gradlew.sh"));
            {
                Fi taskFile = buildSrc.child("src/main/java/maven2github/PublishToGithubTask.java");
                taskFile.writeString(taskFile.readString().replace("File targetFolder = new File(getProject().getRootProject().getBuildDir(), \"mavenLocal\");",
                    "File targetFolder = new File(\"" + tmpRepository.absolutePath() + "\");"

                ));
            }
            {
                Fi pluginFile = buildSrc.child("src/main/java/maven2github/PublishGithubPlugin.java");
                pluginFile.writeString(pluginFile.readString().replace("String strictMavenLocal = null;",
                    "String strictMavenLocal = \"" + tmpRepository.absolutePath() + "\";"

                ));
            }
        }

        setupPlugin:
        {
            Fi buildFile = sourceFolder.child("build.gradle");
            String string = buildFile.readString();
            int index = string.indexOf("allprojects");
            //language=TEXT
            String s1 = ("apply plugin: maven2github.PublishGithubPlugin\n" +
                         "publishConfig{\n" +
                         "    repoAuthor=\"{0}\"\n" +
                         "    repoName=\"{1}\"\n" +
                         "    version=\"{2}\"\n" +
                         "}\n")
                .replace("{0}", repoOwner)
                .replace("{1}", repoName)
                .replace("{2}", repoVersion);

            String newString = (string.substring(0, index) + s1 + string.substring(index))
                .replace("withJavadocJar()", "//withJavadocJar()");
            buildFile.writeString(newString);


            String readString = sourceFolder.child("gradle.properties").readString();
            int index1 = readString.indexOf("archash");
            if(index1 >= 0){
                int index2 = readString.indexOf("\n", index1);
                if(index2 < 0) index2 = readString.length();
                String s = readString.substring(0, index1) + "archash=" + repoVersion + readString.substring(index2);
                sourceFolder.child("gradle.properties").writeString(s);
            }
        }
        System.out.println("gradlew publishFolder");
        ProcessBuilder pb;
        String taskName = "publishToMavenLocal";// "publishFolder";
        if(OS.isLinux){
            Runtime.getRuntime()
                .exec("chmod +x gradlew", null, sourceFolder.file());
            pb = new ProcessBuilder(sourceFolder.absolutePath() + "/gradlew", taskName, "--stacktrace");
        }else{
            pb = new ProcessBuilder(sourceFolder.absolutePath() + "/gradlew.bat", taskName, "--stacktrace");
        }
        pb.directory(sourceFolder.file());
        Fi buildLogFile = Vars.sources.child("build.log");
        pb.redirectError(ProcessBuilder.Redirect.to(buildLogFile.file()));
        pb.redirectOutput(ProcessBuilder.Redirect.to(buildLogFile.file()));
        Process p = pb.start();
        p.waitFor();
        if(buildLogFile.readString().contains("BUILD FAILED")){
            throw new RuntimeException("exception happened: \n" + buildLogFile.readString());
        }
        System.out.println("Processing maven repo");
        RenameLocalPom.process(tmpRepository);


        {
            String prefix = tmpRepository.absolutePath() + "/";
            tmpRepository.walk(it -> {
                String path = it.absolutePath();
                System.out.println(path);
                Fi child = Vars.repository.child(path.substring(prefix.length()));
                try{
                    boolean shouldGenerateHash = !child.exists();
                    if(!it.name().equals("maven-metadata.xml") || !child.exists()){
                        it.copyTo(child);
                    }else{
                        child.writeString(mergeMavenMetadata(child.readString(), repoVersion));
                        shouldGenerateHash = true;
                    }
                    if(shouldGenerateHash){
                        filesToHash.add(child);
                    }
                }catch(Exception e){
                    throw new RuntimeException(e);
                }
            });
        }
    }

    static String mergeMavenMetadata(String current, String newTag) throws SAXException, IOException, ParserConfigurationException, TransformerException{
        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = documentBuilder.parse(new StringInputStream(current));

        org.w3c.dom.Node root = document.getDocumentElement();

        for(Node rootChild = root.getFirstChild(); rootChild != null; rootChild = rootChild.getNextSibling()){
            if(!rootChild.getNodeName().equals("versioning")) continue;

            for(Node versioningChild = rootChild.getFirstChild(); versioningChild != null; versioningChild = versioningChild.getNextSibling()){

                if(versioningChild.getNodeName().matches("latest|release")){
                    versioningChild.setTextContent(newTag);
                    versioningChild.setNodeValue(newTag);
                    continue;
                }
                if(versioningChild.getNodeName().equals("versions")){
                    OrderedSet<String> set = new OrderedSet<>();
                    for(Node versionNode = versioningChild.getFirstChild(); versionNode != null; versionNode = versionNode.getNextSibling()){
                        if(versionNode.getNodeName().equals("version") && !set.add(versionNode.getTextContent().trim())){
                            versioningChild.removeChild(versionNode);
                        }
                    }
                    if(set.add(newTag)){
                        Element version = document.createElement("version");
                        version.setTextContent(newTag);
                        versioningChild.appendChild(version);
                    }
                }
            }

        }

        Cons<Node>[] cons = new Cons[]{null};
        cons[0] = it -> {
            it.setNodeValue(it.getTextContent().trim());
            NodeList childNodes = it.getChildNodes();
            for(int i = 0; i < childNodes.getLength(); i++){
                cons[0].get(childNodes.item(i));
            }
        };
        cons[0].get(root);
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
//        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
//        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
//        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
//        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        DOMSource source = new DOMSource(document);
        StringWriter writer = new StringWriter();
        transformer.transform(source, new StreamResult(writer));
        return writer.toString();
    }
}
