package maven2github;

import groovy.lang.GroovyObject;
import groovy.lang.MetaClass;
import groovy.lang.MetaMethod;
import groovy.lang.MetaProperty;
import org.gradle.api.DefaultTask;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Task;
import org.gradle.api.UnknownTaskException;
import org.gradle.api.internal.artifacts.mvnsettings.LocalMavenRepositoryLocator;
import org.gradle.api.internal.artifacts.repositories.transport.RepositoryTransportFactory;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.publish.maven.internal.publication.MavenPublicationInternal;
import org.gradle.api.publish.maven.internal.publisher.MavenNormalizedPublication;
import org.gradle.api.publish.maven.internal.publisher.MavenPublishers;
import org.gradle.api.publish.maven.tasks.PublishToMavenLocal;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import org.gradle.internal.serialization.Cached;

import javax.inject.Inject;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

import static org.gradle.api.publish.plugins.PublishingPlugin.PUBLISH_TASK_GROUP;

public class PublishToGithubTask extends DefaultTask {
    private final Cached<MavenNormalizedPublication> normalizedPublication = Cached.of(this::computeNormalizedPublication);

    {
        setGroup(PUBLISH_TASK_GROUP);
    }

    private static MavenPublicationInternal toPublicationInternal(MavenPublication publication) {
        if (publication == null) {
            return null;
        } else if (publication instanceof MavenPublicationInternal) {
            return (MavenPublicationInternal) publication;
        } else {
            throw new InvalidUserDataException(
                    String.format(
                            "publication objects must implement the '%s' interface, implementation '%s' does not",
                            MavenPublicationInternal.class.getName(),
                            publication.getClass().getName()
                    )
            );
        }
    }

    private MavenNormalizedPublication computeNormalizedPublication() {
        MavenPublicationInternal publicationInternal = getPublicationInternal();
        if (publicationInternal == null) {
            throw new InvalidUserDataException("The 'publication' property is required");
        }

//        getDuplicatePublicationTracker().checkCanPublishToMavenLocal(publicationInternal);
        return publicationInternal==null?null: publicationInternal.asNormalisedPublication();
    }

    @Internal
    protected MavenPublicationInternal getPublicationInternal() {
        return toPublicationInternal(getPublication());
    }

    @Inject
    protected MavenPublishers getMavenPublishers() {
        throw new UnsupportedOperationException();
    }


    @TaskAction
    protected void publish() {
        try {
            MavenNormalizedPublication mavenNormalizedPublication = normalizedPublication.get();
            if(mavenNormalizedPublication==null)return;

            MavenPublishers publishers = PublishToGithubTask.this.getMavenPublishers();
            Field __repositoryTransportFactory = MavenPublishers.class.getDeclaredField("repositoryTransportFactory");
            __repositoryTransportFactory.setAccessible(true);
            RepositoryTransportFactory repositoryTransportFactory = (RepositoryTransportFactory) __repositoryTransportFactory.get(publishers);
            Field __mavenRepositoryLocator = MavenPublishers.class.getDeclaredField("mavenRepositoryLocator");
            __mavenRepositoryLocator.setAccessible(true);
            LocalMavenRepositoryLocator mavenRepositoryLocator = (LocalMavenRepositoryLocator) __mavenRepositoryLocator.get(publishers);


            File targetFolder = new File(getProject().getRootProject().getBuildDir(), "mavenLocal");
            MavenGithubRepository mavenLocal = new MavenGithubRepository(PublishToGithubTask.this.getTemporaryDirFactory(), repositoryTransportFactory, mavenRepositoryLocator, targetFolder.toURI());

            mavenLocal.publish(mavenNormalizedPublication, null);
//            FileUtils.copyDirectory(mavenRepositoryLocator.getLocalMavenRepository().toPath().toFile(), new File(getProject().getBuildDir(),"mavenLocal"));
//            /*mavenRepositoryLocator.getLocalMavenRepository().toURI()*/
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private MavenPublication getPublication() {

        for (PublishToMavenLocal publish : getProject().getTasks().withType(PublishToMavenLocal.class)) {
            System.out.println("publish: " + publish);
        }

        Task publishToMavenLocal = null;
        try {
            publishToMavenLocal = getProject().getTasks().getByName("publishMavenPublicationToMavenLocal");
        } catch (UnknownTaskException e) {
            return null;
        }
        return ((PublishToMavenLocal)publishToMavenLocal).getPublication();
    }
}
