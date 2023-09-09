package updater;

import arc.files.*;
import arc.struct.*;
import arc.util.*;
import arc.util.serialization.*;
import org.apache.commons.io.*;
import org.gradle.api.*;
import org.gradle.api.tasks.*;
import org.xml.sax.*;
import updater.SupportedRepos.*;
import updater.process.*;

import java.io.*;
import java.net.*;
import java.security.*;
import java.util.*;

public class CheckMindustryUpdates extends DefaultTask{
    public CheckMindustryUpdates(){
        getOutputs().upToDateWhen(task -> false);
    }

    static String readLast(Reader reader) throws IOException{
        // read everything into a buffer
        int n;
        char[] part = new char[(1/*1 << 13*/)];
        StringBuilder sb = new StringBuilder();
        char openChar = '{';
        char closeChar = '}';
        int deep = 0;
        while((n = reader.read(part, 0, part.length)) != -1){
            if(deep == 0 && part[0] == '[') continue;
            if(deep == 0 && part[0] == ']') break;
            if(closeChar == part[0]){
                deep--;
            }
            if(openChar == part[0]){
                deep++;
            }
            sb.append(part, 0, n);
            if(deep == 0) break;
        }
        return sb.toString().trim();
    }

    @TaskAction
    public void run() throws IOException, NoSuchAlgorithmException, InterruptedException, SAXException{
        Vars.root = Fi.get(getProject().getBuildDir().getAbsolutePath()).parent();
        ArrayList<SupportedRepo> supportedRepos = getProject().getExtensions().getByType(SupportedRepos.class).repos;
        Vars.innerBuildSrc = Vars.root.child("innerBuildSrc");
        Vars.repository = Vars.root.child("repository");
        Vars.sources = Vars.root.child("sources");
        for(SupportedRepo repo : supportedRepos){
            String repoAuthor = repo.author();
            String repoName = repo.name();
            Log.info("Processing @/@",repoAuthor,repoName);
            Fi supportedVersionsFile = Vars.root.child("supportedVersions").child(repoAuthor).child(repoName).child("supported-versions.txt");
            ObjectSet<String> supportedVersions = new ObjectSet<>();
            if(supportedVersionsFile.exists()){
                for(String str : supportedVersionsFile.readString().split("\n")){
                    if(str.matches("((#|//|\\\\).*|/\\*\\*?[^*]*\\*/)")) continue;
                    supportedVersions.add(str.trim());
                }
            }

            Jval lastJson;
            {
                URL url = new URL("https://api.github.com/repos/" + repoAuthor + "/" + repoName + "/releases");

                try(InputStream stream = url.openStream()){
                    InputStreamReader reader = new InputStreamReader(stream);
                    String text = readLast(reader);
                    lastJson = Jval.read(text);
                }
            }
            String lastTag = lastJson.getString("tag_name",null);
            if(lastTag==null){
                Log.warn("Has no version for @/@",repoAuthor,repoName);
                return;
            }
            if(!supportedVersions.add(lastTag)){
                System.out.println("No updates found");
                return;
            }
            System.out.println("Found new tag " + lastTag + "!!!");
            processRepo(repoAuthor, repoName, lastTag);

            System.out.println("Saving tag");
            supportedVersionsFile.writeString("\n" + lastTag, true);
        }


        System.out.println("Creating .md5 and .sha1");
        GenerateHashes.process(Vars.repository);



        System.out.println("Done.");
//        Vars.sources.child("tmp.lastJson").writeString(lastJson.toString(Jval.Jformat.formatted));


    }

    private void processRepo(String author, String repoName, String version){
        try{
            Fi sourcesFi = Vars.sources.child(author).child(repoName).child("sources.zip");
            Log.info("Downloading " + author + "/" + repoName);
            Time.mark();
            FileUtils.copyURLToFile(new URL("https://codeload.github.com/" + author + "/" + repoName + "/zip/refs/tags/" + version), sourcesFi.file(), 10_000, 10_000);
            Log.info("Time to download: @ms", Time.elapsed());
            ProjectProcessor.process(author, repoName, new ZipFi(sourcesFi), version);
        }catch(IOException | NoSuchAlgorithmException | InterruptedException | SAXException e){
            throw new RuntimeException(e);
        }
    }
}
