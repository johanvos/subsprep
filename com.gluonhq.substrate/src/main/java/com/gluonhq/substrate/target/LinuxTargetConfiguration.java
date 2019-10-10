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
package com.gluonhq.substrate.target;

import com.gluonhq.substrate.model.ProjectConfiguration;
import com.gluonhq.substrate.util.FileOps;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

public class LinuxTargetConfiguration extends AbstractTargetConfiguration {

    @Override
    public boolean compile(Path gvmPath, ProjectConfiguration config, String cp) throws IOException, InterruptedException {
        FileOps.rmdir(gvmPath.resolve("tmp"));
        String tmpDir = gvmPath.resolve("tmp").toFile().getAbsolutePath();
        String mainClassName = config.getMainClassName();
        if (mainClassName == null || mainClassName.isEmpty()) {
            throw new IllegalArgumentException("No main class is supplied. Cannot compile.");
        }
        if (cp == null || cp.isEmpty()) {
            throw new IllegalArgumentException("No classpath specified. Cannot compile");
        }
        String nativeImage = getNativeImagePath(config);
        ProcessBuilder compileBuilder = new ProcessBuilder(nativeImage);
        compileBuilder.command().add("-H:+ExitAfterRelocatableImageWrite");
        compileBuilder.command().add("-H:TempDirectory="+tmpDir);
        compileBuilder.command().add("-Dsvm.platform=org.graalvm.nativeimage.Platform$LINUX_AMD64");
        compileBuilder.command().add("-cp");
        compileBuilder.command().add(cp);
        compileBuilder.command().add(mainClassName);
        compileBuilder.redirectErrorStream(true);
        Process compileProcess = compileBuilder.start();
        InputStream inputStream = compileProcess.getInputStream();
        int result = compileProcess.waitFor();
        // we will print the output of the process only if we don't have the resulting objectfile

        boolean failure = result!=0;
        String extraMessage = null;
        if (!failure) {
            String nameSearch = mainClassName.toLowerCase()+".o";
            Path p = FileOps.findFile(gvmPath, nameSearch);
            if (p == null) {
                failure = true;
                extraMessage = "Objectfile should be called "+nameSearch+" but we didn't find that under "+gvmPath.toString();
            }
        }
        if (failure) {
            System.err.println("Compilation failed with result = " + result);
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
            String l = br.readLine();
            while (l != null) {
                System.err.println(l);
                l = br.readLine();
            }
            System.err.println("That was the error.");
            if (extraMessage!= null) {
                System.err.println("Additional information: "+extraMessage);
            }
        }
        return !failure;
    }

    @Override
    public void link(Path workDir, String appName, String target) throws Exception {
        System.err.println("LINUX LINK");
    }

    @Override
    public void run(Path workDir, String appName, String target) throws Exception {
        System.err.println("LINUX RUN");
    }

}
