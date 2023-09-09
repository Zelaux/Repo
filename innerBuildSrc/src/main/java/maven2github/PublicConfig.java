package maven2github;

public class PublicConfig{
     String version;
    String repoAuthor, repoName;

    public String getVersion(){
        return version;
    }

    public void setVersion(String version){
        this.version = version;
    }

    public String getRepoAuthor(){
        return repoAuthor;
    }

    public void setRepoAuthor(String repoAuthor){
        this.repoAuthor = repoAuthor;
    }

    public String getRepoName(){
        return repoName;
    }

    public void setRepoName(String repoName){
        this.repoName = repoName;
    }
}
