package updater.process;

import arc.files.Fi;

public class RenameLocalPom {
    public static void process(Fi folder) {
        folder.walk(it -> {
            if (it.name().equals("maven-metadata-local.xml")) {
                it.copyTo(it.sibling("maven-metadata.xml"));
                it.delete();
            }
        });
    }
}
