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

package jenkins.plugins.publish_over_ssh.casc;

import hudson.Extension;
import io.jenkins.plugins.casc.impl.configurators.DataBoundConfigurator;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import jenkins.plugins.publish_over_ssh.BapSshHostConfiguration;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * JCasC configurator for {@link BapSshHostConfiguration}. Hides attributes that are public API but must not be part
 * of the YAML model: {@code commonConfig} (injected by the plugin descriptor, would otherwise be exported under
 * every host) and the deprecated plaintext {@code proxyPassword} accessor pair (use {@code secretProxyPassword}).
 */
@Extension(optional = true)
@Restricted(NoExternalUse.class)
public class BapSshHostConfigurationConfigurator extends DataBoundConfigurator<BapSshHostConfiguration> {

    public BapSshHostConfigurationConfigurator() {
        super(BapSshHostConfiguration.class);
    }

    @Override
    protected Set<String> exclusions() {
        return new HashSet<>(Arrays.asList("commonConfig", "proxyPassword"));
    }
}
