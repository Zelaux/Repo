package maven2github;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;

import java.util.ArrayList;
import java.util.List;

public class PublishGithubPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        System.out.println("Hello world: " + project);
        PublicConfig config = project.getExtensions().create("publishConfig", PublicConfig.class);
        project.afterEvaluate(_o -> {

            registerProject(_o,config.repoAuthor,config.repoName,config.version);
            /*project.allprojects(it -> {
                TaskContainer tasks = it.getTasks();
                PublishToGithubTask folder = tasks.register("publishFolder", PublishToGithubTask.class).get();
                folder.dependsOn(
                        it.getTasksByName("publishToMavenLocal", true),


                        );
            });*/
        });
    }

    private TaskProvider<PublishToGithubTask> registerProject(Project project, String repoAuthor, String repoName, String version) {
        TaskContainer tasks = project.getTasks();
        List<Object> dependencies = new ArrayList<>();
        project.setGroup("com.github."+repoAuthor+"."+repoName);
        project.setVersion(version);
        project.subprojects(sub -> {
            TaskProvider<PublishToGithubTask> publish = registerProject(sub, repoAuthor, repoName, version);
           if(publish!=null) dependencies.add(publish);
        });


        TaskProvider<PublishToGithubTask> register = null;
        try {
            register = tasks.register("publishFolder", PublishToGithubTask.class, task -> {
                task.dependsOn(project.getTasksByName("publishToMavenLocal", true), dependencies);
            });
            System.out.println(project);
        } catch (Exception e) {
            return null;
//            return (TaskProvider<PublishToGithubTask>) tasks.getByName("publishFolder");
        }


        return register;
    }

}
