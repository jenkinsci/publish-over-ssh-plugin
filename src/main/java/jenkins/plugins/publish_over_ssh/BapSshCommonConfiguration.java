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

import java.io.Serializable;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import hudson.model.Describable;
import jenkins.model.Jenkins;
import jenkins.plugins.publish_over_ssh.descriptor.BapSshCommonConfigurationDescriptor;

public class BapSshCommonConfiguration implements Describable<BapSshCommonConfiguration>, Serializable {

	private static final long serialVersionUID = 1L;

	private final boolean disableAllExec;

	private String generalCredentialsId;

	@DataBoundConstructor
	public BapSshCommonConfiguration(final String generalCredentialsId) {
		this(generalCredentialsId, false);
	}
	
	@DataBoundConstructor
	public BapSshCommonConfiguration(final String generalCredentialsId, final boolean disableAllExec) {
		this.disableAllExec = disableAllExec;
		this.generalCredentialsId = generalCredentialsId;
	}

	public boolean isDisableAllExec() {
		return disableAllExec;
	}

	public BapSshCommonConfigurationDescriptor getDescriptor() {
		return Jenkins.get().getDescriptorByType(BapSshCommonConfigurationDescriptor.class);
	}

	public String getGeneralCredentialsId() {
		return generalCredentialsId;
	}

	@DataBoundSetter
	public void setGeneralCredentialsId(String credentialsId) {
		this.generalCredentialsId = credentialsId;
	}

}
