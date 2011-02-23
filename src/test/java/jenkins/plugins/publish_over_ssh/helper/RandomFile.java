/*
 * The MIT License
 *
 * Copyright (C) 2010-2011 by Anthony Robinson
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package jenkins.plugins.publish_over_ssh.helper;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

public class RandomFile {

    private static final int DEFAULT_FILE_SIZE = 200;
    private static Random random = new Random();

    private final File file;
    private final byte[] contents;

    public RandomFile(final File directory, final String filename) {
        this(new File(directory, filename));
    }

    public RandomFile(final File file) {
        this(file, DEFAULT_FILE_SIZE);
    }

    public RandomFile(final File file, final int size) {
        this.file = file;

        contents = new byte[size];
        random.nextBytes(contents);
        final File parent = file.getParentFile();
        if (!parent.exists() && !parent.mkdirs())
            throw new RuntimeException("Failed to make parent directory [" + parent.getAbsolutePath() + "]");
        try {
            FileUtils.writeByteArrayToFile(file, contents);
        } catch (IOException ioe) {
            throw new RuntimeException("Failed to create file [" + file.getAbsolutePath()  + "]", ioe);
        }
    }

    public File getFile() {
        return file;
    }

    public byte[] getContents() {
        return Arrays.copyOf(contents, contents.length);
    }

    public String getFileName() {
        return file.getName();
    }

}
