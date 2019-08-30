package com.gluonhq.substrate;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class NativeImage {

    public int compile(String graalVMRoot, String classPath, String mainClass) {
        ProcessBuilder pb = new ProcessBuilder();
        List<String> command = pb.command();
        command.add(getNativeImageExecutable(graalVMRoot).toString());
        command.add("-cp");
        command.add(classPath);
        command.add(mainClass);
        int exitStatus = 1;
        try {
            Process p = pb.inheritIO().start();
            exitStatus = p.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return exitStatus;

    }

    Path getNativeImageExecutable (String graalVMRoot) {
        Path nativeImage = Path.of(graalVMRoot).resolve("bin").resolve("native-image");
        return nativeImage;
    }

}
