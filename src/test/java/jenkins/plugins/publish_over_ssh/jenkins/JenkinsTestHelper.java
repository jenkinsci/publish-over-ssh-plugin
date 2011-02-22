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

package jenkins.plugins.publish_over_ssh.jenkins;

import hudson.util.CopyOnWriteList;
import jenkins.plugins.publish_over.BPPluginDescriptor;
import jenkins.plugins.publish_over_ssh.BapSshCommonConfiguration;
import jenkins.plugins.publish_over_ssh.BapSshHostConfiguration;
import jenkins.plugins.publish_over_ssh.BapSshPublisherPlugin;

import java.lang.reflect.Field;

public class JenkinsTestHelper {

    public void setGlobalConfig(final BapSshCommonConfiguration commonConfig,
                                final BapSshHostConfiguration... newHostConfigurations) throws Exception {
        for (BapSshHostConfiguration hostConfig : newHostConfigurations) {
            hostConfig.setCommonConfig(commonConfig);
        }
        CopyOnWriteList<BapSshHostConfiguration> hostConfigurations = getHostConfigurations();
        hostConfigurations.replaceBy(newHostConfigurations);
        BapSshPublisherPlugin.DESCRIPTOR.setCommonConfig(commonConfig);
    }

    public CopyOnWriteList<BapSshHostConfiguration> getHostConfigurations() throws Exception {
        Field hostConfig = BPPluginDescriptor.class.getDeclaredField("hostConfigurations");
        hostConfig.setAccessible(true);
        return (CopyOnWriteList) hostConfig.get(BapSshPublisherPlugin.DESCRIPTOR);
    }

}
