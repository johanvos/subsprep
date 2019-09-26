package com.gluonhq.entry;

import com.gluonhq.substrate.NativeImage;

public class Main {

    public static void main(String[] args) {
        System.err.println("Hello, main");
        NativeImage nativeImage = new NativeImage();
        String gr = "/home/johan/graal/github/fork/graal/vm/mxbuild/linux-amd64/GRAALVM_UNKNOWN/graalvm-unknown-19.3.0-dev";
        int ret = nativeImage.compile(gr, "/tmp", "HelloWorld");
        if (ret == 0) {
            System.err.println("TEST SUCCEEDED");
        } else {
            System.err.println("TEST FAILED");
        }
    }

}
