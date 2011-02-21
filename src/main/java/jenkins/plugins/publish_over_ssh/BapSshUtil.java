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

package jenkins.plugins.publish_over_ssh;

import jenkins.plugins.publish_over.BapPublisherException;

import java.io.UnsupportedEncodingException;

public class BapSshUtil {

    public static final int EXTENSION_ORDINAL_POST_BUILD_WRAPPER = 10;
    public static final int EXTENSION_ORDINAL_PRE_BUILD_WRAPPER = EXTENSION_ORDINAL_POST_BUILD_WRAPPER + 1;

    public static byte[] toBytes(final String string) {
        if (string == null)
            return null;
        try {
            return string.getBytes("UTF-8");
        } catch (UnsupportedEncodingException uee) {
            throw new BapPublisherException("Really? Really? You don't know what UTF-8 is? Really? ...", uee);
        }
    }

}
