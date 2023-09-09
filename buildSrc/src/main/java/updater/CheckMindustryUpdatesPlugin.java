package updater;

import org.gradle.api.*;

public class CheckMindustryUpdatesPlugin implements Plugin<Project>{
    @Override
    public void apply(Project project){
        project.getExtensions().create("supportedRepos", SupportedRepos.class);
        project.getTasks().create("checkUpdates", CheckMindustryUpdates.class);
    }
}
