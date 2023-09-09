package updater.process;

import arc.files.*;
import arc.util.*;
import org.apache.commons.io.*;
import updater.Vars;

import java.io.*;
import java.net.*;

public class LibrariesDownloader {
    public static Fi core() {
        return Vars.sources.child("compDownloader").child("sources.zip");
    }

    public static Fi arc() {
        return Vars.sources.child("compDownloader").child("arcSources.zip");
    }

    public static ZipFi mindustryZip() {
        return new ZipFi(core());
    }

    public static ZipFi arcZip() {
        return new ZipFi(arc());
    }

    public static Fi coreZipRoot() {
        return new ZipFi(core()).list()[0];
    }

    public static Fi arcZipRoot() {
        return new ZipFi(arc()).list()[0];
    }


    public static void download(String mindustryVersion, String arcVersion) {
        boolean downloadNew = false;
        try {

            Fi version = Vars.sources.child("compDownloader").child("version.txt");
            Fi sourcesFi = core();
            Fi arcFi = arc();
            if (!version.exists() || !version.readString().equals(mindustryVersion)) {
                downloadNew = true;
                version.writeString(mindustryVersion);
            }
            boolean errorInSources = false, errorInArc = false;
            try {
                mindustryZip();
            } catch (Exception e) {
                errorInSources = true;
            }
            try {
                arcZip();
            } catch (Exception e) {
                errorInArc = true;
            }
            if (downloadNew || !sourcesFi.exists() || errorInSources) {
                Log.info("Downloading new core version");
                Time.mark();
                FileUtils.copyURLToFile(new URL("https://codeload.github.com/Anuken/Mindustry/zip/refs/tags/" + mindustryVersion), sourcesFi.file(), 10_000, 10_000);
                Log.info("Time to download: @ms", Time.elapsed());
            } else {
                Log.info("Game version and core version are the same");
            }
            if (downloadNew || !arcFi.exists() || errorInArc) {
                Log.info("Downloading new arc version");
                Time.mark();
                FileUtils.copyURLToFile(new URL("https://codeload.github.com/Anuken/Arc/zip/refs/tags/" + arcVersion), arcFi.file(), 10_000, 10_000);
                Log.info("Time to download: @ms", Time.elapsed());
            } else {
                Log.info("Game version and arc version are the same");
            }
        } catch (IOException e) {
            Log.err(e);
        }
    }

}
