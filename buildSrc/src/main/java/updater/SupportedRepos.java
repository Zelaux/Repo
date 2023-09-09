package updater;

import org.gradle.api.provider.*;

import java.util.*;

public  class SupportedRepos{

     ArrayList<SupportedRepo> repos=new ArrayList<>();

    public ArrayList<SupportedRepo> getRepos(){
        return repos;
    }

    public void setRepos(ArrayList<SupportedRepo> repos){
        this.repos = repos;
    }

    public SupportedRepo repo(String author, String repo){
        return new SupportedRepo(author, repo);
    }

    public record SupportedRepo(String author, String name){
    }
}
