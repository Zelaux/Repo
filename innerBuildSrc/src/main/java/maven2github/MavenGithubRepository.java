package maven2github;

import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.internal.artifacts.mvnsettings.LocalMavenRepositoryLocator;
import org.gradle.api.internal.artifacts.repositories.transport.RepositoryTransport;
import org.gradle.api.internal.artifacts.repositories.transport.RepositoryTransportFactory;
import org.gradle.api.publish.maven.internal.publisher.MavenLocalPublisher;
import org.gradle.api.publish.maven.internal.publisher.MavenNormalizedPublication;
import org.gradle.internal.Factory;
import org.gradle.internal.resource.ExternalResourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.net.URI;

public class MavenGithubRepository extends MavenLocalPublisher {
    private static final Logger LOGGER = LoggerFactory.getLogger(MavenLocalPublisher.class);
    private final RepositoryTransportFactory repositoryTransportFactory;
    private final LocalMavenRepositoryLocator mavenRepositoryLocator;
    public final URI rootUri;
    public MavenGithubRepository(Factory<File> temporaryDirFactory, RepositoryTransportFactory repositoryTransportFactory, LocalMavenRepositoryLocator mavenRepositoryLocator, URI rootUri) {
        super(temporaryDirFactory, repositoryTransportFactory, mavenRepositoryLocator);
        this. repositoryTransportFactory=repositoryTransportFactory;
        this. mavenRepositoryLocator=mavenRepositoryLocator;
        this.rootUri = rootUri;
    }

    @Override
    public void publish(MavenNormalizedPublication publication, @Nullable MavenArtifactRepository artifactRepository) {
        LOGGER.info("Publishing to maven local repository");
//        URI rootUri = this.mavenRepositoryLocator.getLocalMavenRepository().toURI();
//        URI rootUri = new File(getB);
        RepositoryTransport transport = this.repositoryTransportFactory.createFileTransport("mavenLocal");
        ExternalResourceRepository repository = transport.getRepository();
        this.publish(publication, repository, rootUri, true);
    }
}
