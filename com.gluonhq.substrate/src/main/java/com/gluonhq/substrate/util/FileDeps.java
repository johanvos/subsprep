/*
 * Copyright (c) 2019, Gluon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL GLUON BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.gluonhq.substrate.util;


import com.gluonhq.substrate.Constants;
import com.gluonhq.substrate.model.ProjectConfiguration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FileDeps {

    private static final String URL_GRAAL_LIBS = "http://download2.gluonhq.com/omega/graallibs/graalvm-svm-${host}-${version}.zip";
    private static final String URL_JAVA_STATIC_SDK = "http://download2.gluonhq.com/substrate/staticjdk/labs-staticjdk-${target}-gvm-${version}.zip";
    private static final String URL_JAVAFX_STATIC_SDK = "http://download2.gluonhq.com/omega/javafxstaticsdk/${target}-libsfx-${version}.zip";

    private static final List<String> JAVA_FILES = Arrays.asList(
            "libjava.a", "libnet.a", "libnio.a", "libzip.a"
    );

    private static final List<String> JAVAFX_FILES = Arrays.asList(
            "javafx.base.jar", "javafx.controls.jar", "javafx.graphics.jar",
            "javafx.fxml.jar", "javafx.media.jar", "javafx.web.jar",
            "libglass.a"
    );

    public static void setupDependencies(ProjectConfiguration configuration) throws IOException {
        String target = configuration.getTargetTriplet().getOs();

        if (! Files.isDirectory(Constants.USER_SUBSTRATE_PATH)) {
            Files.createDirectories(Constants.USER_SUBSTRATE_PATH);
        }

        boolean downloadGraalLibs = false, downloadJavaStatic = false, downloadJavaFXStatic = false;


        // Java Static

        Path javaStatic = Path.of(configuration.getStaticRoot());
        Logger.logDebug("Processing JavaStatic dependencies at " + javaStatic.toString());

        if (configuration.isUseJNI()) {
            if (! Files.isDirectory(javaStatic)) {
                downloadJavaStatic = true;
            } else {
                String path = javaStatic.toString();
                if (JAVA_FILES.stream()
                        .map(s -> new File(path, s))
                        .anyMatch(f -> !f.exists())) {
                    Logger.logDebug("jar file not found");
                    downloadJavaStatic = true;
                } else if (configuration.isEnableCheckHash()) {
                    Logger.logDebug("Checking java static sdk hashes");
                    Map<String, String> hashes = getHashMap(javaStatic.getParent().toString() + File.separator + "javaStaticSdk-" + target + ".md5");
                    if (hashes == null) {
                        Logger.logDebug("javaStaticSdk/" + configuration.getJavaStaticSdkVersion() + "/javaStaticSdk-" + target + ".md5 not found");
                        downloadJavaStatic = true;
                    } else if (JAVA_FILES.stream()
                            .map(s -> new File(path, s))
                            .anyMatch(f -> !hashes.get(f.getName()).equals(calculateCheckSum(f)))) {
                        Logger.logDebug("jar file has invalid hashcode");
                        downloadJavaStatic = true;
                    }
                }
            }
        }

        // JavaFX Static
        if (configuration.isUseJavaFX()) {
            Path javafxStatic = Path.of(configuration.getJavaFXRoot()).resolve("lib");
            Logger.logDebug("Processing JavaFXStatic dependencies at " + javafxStatic.toString());

            if (! Files.isDirectory(javafxStatic)) {
                Logger.logDebug("javafxStaticSdk/" + configuration.getJavafxStaticSdkVersion() + "/" + target + "-sdk/lib folder not found");
                downloadJavaFXStatic = true;
            } else {
                String path = javafxStatic.toString();
                if (JAVAFX_FILES.stream()
                        .map(s -> new File(path, s))
                        .anyMatch(f -> !f.exists())) {
                    Logger.logDebug("jar file not found");
                    downloadJavaFXStatic = true;
                } else if (configuration.isEnableCheckHash()) {
                    Logger.logDebug("Checking javafx static sdk hashes");
                    Map<String, String> hashes = getHashMap(javafxStatic.getParent().getParent().toString() + File.separator + "javafxStaticSdk-" + target + ".md5");
                    if (hashes == null) {
                        Logger.logDebug("javafxStaticSdk/" + configuration.getJavafxStaticSdkVersion() + "/javafxStaticSdk-" + target + ".md5 not found");
                        downloadJavaFXStatic = true;
                    } else if (JAVAFX_FILES.stream()
                            .map(s -> new File(path, s))
                            .anyMatch(f -> !hashes.get(f.getName()).equals(calculateCheckSum(f)))) {
                        Logger.logDebug("jar file has invalid hashcode");
                        downloadJavaFXStatic = true;
                    }
                }
            }
        }
        try {
//            if (downloadGraalLibs) {
//                downloadGraalZip(SVMBridge.USER_OMEGA_PATH, configuration);
//            }

            if (downloadJavaStatic) {
                downloadJavaZip(target, Constants.USER_SUBSTRATE_PATH, configuration);
            }

            if (downloadJavaFXStatic) {
                downloadJavaFXZip(target, Constants.USER_SUBSTRATE_PATH, configuration);
            }

        } catch (IOException e) {
            throw new RuntimeException("Error downloading zips: " + e.getMessage());
        }
        Logger.logDebug("Setup dependencies done");
    }

    private static Map<String, String> getHashMap(String nameFile) {
        Map<String, String> hashes = null;
        try (FileInputStream fis = new FileInputStream(new File(nameFile));
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            hashes = (Map<String, String>) ois.readObject();
        } catch (ClassNotFoundException | IOException e) {}
        return hashes;
    }


    private static void downloadJavaZip(String target, Path omegaPath, ProjectConfiguration configuration) throws IOException {
        Logger.logDebug("Process zip javaStaticSdk");
        processZip(URL_JAVA_STATIC_SDK
                        .replace("${version}", configuration.getJavaStaticSdkVersion())
                        .replace("${target}", target),
                omegaPath.resolve("${target}-libs-${version}.zip"
                        .replace("${version}", configuration.getJavaStaticSdkVersion())
                        .replace("${target}", target)),
                "javaStaticSdk", configuration.getJavaStaticSdkVersion(), "javaStaticSdk-" + target + ".md5");
        Logger.logDebug("Processing zip java done");
    }

    private static void downloadJavaFXZip(String target, Path omegaPath, ProjectConfiguration configuration) throws IOException {
        Logger.logDebug("Process zip javafxStaticSdk");
        processZip(URL_JAVAFX_STATIC_SDK
                        .replace("${version}", configuration.getJavafxStaticSdkVersion())
                        .replace("${target}", target),
                omegaPath.resolve("${target}-libsfx-${version}.zip"
                        .replace("${version}", configuration.getJavafxStaticSdkVersion())
                        .replace("${target}", target)),
                "javafxStaticSdk", configuration.getJavafxStaticSdkVersion(), "javafxStaticSdk-" + target + ".md5");

        Logger.logDebug("Process zip javafx done");
    }

    private static void processZip(String urlZip, Path zipPath, String folder, String version, String name) throws IOException {
        URL url = new URL(urlZip);
        url.openConnection();
        try (InputStream reader = url.openStream();
             FileOutputStream writer = new FileOutputStream(zipPath.toFile())) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = reader.read(buffer)) > 0) {
                writer.write(buffer, 0, bytesRead);
                buffer = new byte[8192];
            }
        }
        Path zipDir = zipPath.getParent().resolve(folder).resolve(version);
        if (! zipPath.toFile().isDirectory()) {
            Files.createDirectories(zipDir);
        }
        Map<String, String> hashes = new HashMap<>();
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipPath.toFile()))) {
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                File destFile = new File(zipDir.toFile(), zipEntry.getName());
                if (zipEntry.isDirectory()) {
                    if (! destFile.exists()) {
                        Files.createDirectories(destFile.toPath());
                    }
                } else {
                    byte[] buffer = new byte[1024];
                    try (FileOutputStream fos = new FileOutputStream(destFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                    String sum = calculateCheckSum(destFile);
                    hashes.put(destFile.getName(), sum);
                }
                zipEntry = zis.getNextEntry();
            }
            zis.closeEntry();
        }

        try (FileOutputStream fos =
                      new FileOutputStream(zipDir.toString() + File.separator + name);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(hashes);
        }
    }

    private static String calculateCheckSum(File file) {
        try {
            // not looking for security, just a checksum. MD5 should be faster than SHA
            try (final InputStream stream = new FileInputStream(file);
                 final DigestInputStream dis = new DigestInputStream(stream, MessageDigest.getInstance("MD5"))) {
                dis.getMessageDigest().reset();
                byte[] buffer = new byte[4096];
                while (dis.read(buffer) != -1) { /* empty loop body is intentional */ }
                return Arrays.toString(dis.getMessageDigest().digest());
            }

        } catch (IllegalArgumentException | NoSuchAlgorithmException | IOException | SecurityException e) {
        }
        return Arrays.toString(new byte[0]);
    }

}
