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

import com.jcraft.jsch.UserInfo;
import org.easymock.IArgumentMatcher;
import org.easymock.classextension.EasyMock;

public class UserInfoPasswordMatcher implements IArgumentMatcher {

    public static UserInfo uiPassword(final String password) {
        EasyMock.reportMatcher(new UserInfoPasswordMatcher(password));
        return null;
    }

    private String expectedPassword;

    public UserInfoPasswordMatcher(final String expectedPassword) {
        this.expectedPassword = expectedPassword;
    }

    public boolean matches(final Object actual) {
        if (!(actual instanceof UserInfo)) {
            return false;
        }
        UserInfo actualUI = (UserInfo) actual;
        return (expectedPassword == actualUI.getPassword()) && (expectedPassword == actualUI.getPassphrase());
    }

    public void appendTo(final StringBuffer stringBuffer) {
        stringBuffer.append("uiPassword(\"")
            .append(expectedPassword)
            .append("\")");
    }
}
