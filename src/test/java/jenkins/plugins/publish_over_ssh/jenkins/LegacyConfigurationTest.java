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

import hudson.model.Project;
import jenkins.plugins.publish_over_ssh.BapSshCommonConfiguration;
import jenkins.plugins.publish_over_ssh.BapSshHostConfiguration;
import jenkins.plugins.publish_over_ssh.BapSshPublisher;
import jenkins.plugins.publish_over_ssh.BapSshPublisherPlugin;
import jenkins.plugins.publish_over_ssh.BapSshTransfer;
import org.junit.Test;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.hudson.test.recipes.LocalData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;

@SuppressWarnings("PMD.SignatureDeclareThrowsException")
public class LegacyConfigurationTest extends HudsonTestCase {

    @LocalData
    @Test
    public void testLoadR0x1Minimal() throws Exception {
        final List<BapSshHostConfiguration> configurations = BapSshPublisherPlugin.DESCRIPTOR.getHostConfigurations();
        assertEquals(1, configurations.size());
        final int expectedPort = 22;
        final int expectedTimeout = 300000;
        final BapSshHostConfiguration expected = new BapSshHostConfiguration("default", "hostname", "username", "password", "",
                                                                       expectedPort, expectedTimeout, true, "", "");
        expected.setCommonConfig(new BapSshCommonConfiguration("", "", ""));
        assertEquals(expected, configurations.get(0));

        final int expectedExecTimeout = 120000;
        final List<BapSshTransfer> transfers = Collections.singletonList(
                    new BapSshTransfer("**/*", "", "", false, false, "", expectedExecTimeout));
        final BapSshPublisher publisher = new BapSshPublisher("default", false, new ArrayList<BapSshTransfer>(transfers));
        final ArrayList<BapSshPublisher> publishers = new ArrayList<BapSshPublisher>();
        publishers.add(publisher);
        final BapSshPublisherPlugin expectedPlugin = new BapSshPublisherPlugin(publishers, false, false, false, "");
        assertEquals(expectedPlugin, getConfiguredPlugin());
    }

    @LocalData
    @Test
    public void testLoadR0x1() throws Exception {
        final int defaultPort = 22;
        final int defaultTimeout = 300000;
        final int configDPort = 8022;
        final int configDTimeout = 10000;
        final BapSshHostConfiguration[] expectedConfig = new BapSshHostConfiguration[] {
            new BapSshHostConfiguration("config a", "hostname.a", "username.a", "password.a", "remoteDirectory.a",
                                        defaultPort, defaultTimeout, false, "", ""),
            new BapSshHostConfiguration("config b", "hostname.b", "username.b", "", "",
                                        defaultPort, defaultTimeout, true, "/an/unencrypted/key", ""),
            new BapSshHostConfiguration("config c", "hostname.c", "username.c", "", "",
                                        defaultPort, defaultTimeout, true, "", KEY_2),
            new BapSshHostConfiguration("config d", "hostname.d", "username.d", "passphrase", "remoteDirectory.d",
                                        configDPort, configDTimeout, true, "path/to/key", KEY_2)
        };
        final BapSshCommonConfiguration common = new BapSshCommonConfiguration("hello", COMMON_KEY, "/this/will/be/ignored");
        for (BapSshHostConfiguration hostConfig : expectedConfig) {
            hostConfig.setCommonConfig(common);
        }
        assertEquals(common, BapSshPublisherPlugin.DESCRIPTOR.getCommonConfig());
        assertEquals(expectedConfig.length, BapSshPublisherPlugin.DESCRIPTOR.getHostConfigurations().size());
        final BapSshHostConfiguration[] actualHostConfigurations = BapSshPublisherPlugin.DESCRIPTOR.getHostConfigurations()
                    .toArray(new BapSshHostConfiguration[expectedConfig.length]);
        assertArrayEquals(expectedConfig, actualHostConfigurations);

        final int tarnsfer11Timeout = 120000;
        final int transfer12Timeout = 15000;
        final BapSshTransfer transfer11 = new BapSshTransfer("", "", "", false, false, "date", tarnsfer11Timeout);
        final BapSshTransfer transfer12 = new BapSshTransfer("target/*.jar", "'builds/'yyyy_MM_dd/'build-${BUILD_NUMBER}'", "target",
                                                       true, true, "ls -la /tmp", transfer12Timeout);
        final ArrayList<BapSshTransfer> transfers1 = new ArrayList<BapSshTransfer>();
        transfers1.add(transfer11);
        transfers1.add(transfer12);
        final BapSshPublisher publisher1 = new BapSshPublisher("config a", true, transfers1);
        final int transfer21Timeout = 10000;
        final BapSshTransfer transfer21 = new BapSshTransfer("out\\dist\\**\\*", "", "out\\dist", false, false, "", transfer21Timeout);
        final ArrayList<BapSshTransfer> transfers2 = new ArrayList<BapSshTransfer>();
        transfers2.add(transfer21);
        final BapSshPublisher publisher2 = new BapSshPublisher("config c", false, transfers2);
        final ArrayList<BapSshPublisher> publishers = new ArrayList<BapSshPublisher>();
        publishers.add(publisher1);
        publishers.add(publisher2);
        final BapSshPublisherPlugin expectedPlugin = new BapSshPublisherPlugin(publishers, true, true, true, "essien");
        assertEquals(expectedPlugin, getConfiguredPlugin());
    }

    private static final String COMMON_KEY =
        "-----BEGIN RSA PRIVATE KEY-----\n"
            + "Proc-Type: 4,ENCRYPTED\n"
            + "DEK-Info: AES-128-CBC,8885F902F99146AA580E7A6D270020D4\n"
            + "\n"
            + "HFzZpIKKLghgZeDBXSFpgm3uoiMUSX/+zyNWRw0ifPXciPIz4DnFSkjVMTM+eCil\n"
            + "/Jo03n7rzh9bO2hbvTxAFa+LImbQcp8T3dRBfJFP8G01ZnjtSobTJ/ykB38kOChc\n"
            + "VyPF5uebIaKy56JECJ0AM7YI1+2VqKemaKNUwsWoAPG8efdK7w7v0x/loA6gZ8Fi\n"
            + "IGOL3nAeLx1u6jaHANRikUi8232KeEDbfM8NzbrlJho16yNTniiVAwhZ0G+E6/Ks\n"
            + "yA1779dDqoaYXgVtCZ04b8ZKC3VwMmzvQui6mONLvcuutX8wQ/zKOHaE8VrYWPx0\n"
            + "2LQ0YpUX7jR8k31KGSySgSB87h2Xil1DGg09KN2MsPIJV1KAXj5hk0Z96UyJAMT3\n"
            + "7kYMMcHUmGTxBbtAwAfp8xGfN+lepfwIgp1CtoW2jq/ZY0peao4FBSt3do2AlVu6\n"
            + "zjcM1q+Q6Oigu5oNpsIhuwfvhyrqxN1KxE4chtJ+mQFnXrBvtVZzaLoJ9U7PEa1x\n"
            + "Y1hFCjceovKZ+E9d0XNPTrjNVAa62oijCFF+bepMMzett9uKvJkOTugEwNqzUG+J\n"
            + "GGfxBvJohYmzGEXfW2T5eBFMYfvLa5YERub6PzTF2UHItm6fymsviEWW3+5k3ESa\n"
            + "RB2YVSXPkH2H0o7bg/tXdVk/uRj26q0FQahowW5TkKwVRKrqjlQY6jzRsYeY68aL\n"
            + "hzEG6fwvPmrH9Edpf0F8Xeha0PGdavyBIY5lMPy6t/dPDRDtmkZwZqLsTw0T/Zht\n"
            + "YeraJAOubc3OlYboZxWQ1ZpEEXha8UQj1/Tp4vh5f67+XVEjpsphZ7Ugey6n52bo\n"
            + "CXioB6719qL9cYNE/1QuUO3SoNppIttP2AkS00RDufRrOEzdQtoWJ7gLOS/r/UTX\n"
            + "C1XhFs0j69eJ6Kw/EUhj+ASiLz4XCzj9WGh5P0Sj1Y0YPI2XbqmoZnpPOnY+1rhl\n"
            + "xDb52PlLGGOmve3GadYz9+5yrsDD7IwRPyWWghuWWKoWYhRpXPoQBHPFEZi2uxw6\n"
            + "IjMBALDzPo+vHtGfD8OuGjUYBXGubo6MrUt7YspuzYGx/2RUwUM5H6jHj3XWNPNq\n"
            + "HQHTeLE35WOxE0K+h22XBC/hbBQF3RJDu7dtNoBVQRHcoY6c7H1SPwYMXCBzbuMS\n"
            + "odPv6W9UXPbUQAcSqknQ9D1pvjO5b+T3BCtIRQTSRIqGFKECeBO/cu5nPq8XGwtY\n"
            + "Yp3KgFdQJ4kh7G9Woehw1kDBdBLX+qnwa0DCNMVUPYu7mBMg/QUZAFIL9lUceR2g\n"
            + "ajIy+8Y6Xb0l91VEudC7tyPs7eet8aBorfjHPWkWePhCKtcrFmIr8wChV3qaQnvl\n"
            + "8PyRf/DgdqTXRz1NOas7vTJoMm/ji5zdBrdVzS1GLvUj6+1PUp7YyY3N/oJaPHqH\n"
            + "e8NAXq02aE3o0++kiQiK960/K5pJ2vRlNb/nYh0fsScJ0eID8Uoty/LZqvZNd6RI\n"
            + "2iwiddkujpk/x/xi+DgiiL171DsAPk6Kdjl7UD+l/OCp1PON2YZy84eaHVyOk7W+\n"
            + "dpXL/6h5McOOlG4Q8H6bpAvjcgtHlH99QVJIibI9pUAuzwaRPXtD6ivO+Xw2H3c7\n"
            + "-----END RSA PRIVATE KEY-----\n";

    private static final String KEY_2 =
        "-----BEGIN DSA PRIVATE KEY-----\n"
            + "MIIBuwIBAAKBgQCCwrvPcyl0tJhXxbWGKgO966l/Vhxg8w+rlqVWXNqOsY+n7xdr\n"
            + "vMNnos0qXV9+iYXJ0mBLeTLOO1q/ezTDEGXDyGdf3ubouF64YaBu0VU/us2rviQb\n"
            + "quncWFbkmxiDJCPqeNMQBkIzDTwhDX4fFt8W5oiTupxNbUQLYEvhe49gVQIVAO23\n"
            + "v9XGw29rYSXMLIgQ5614gxqLAoGAYYUGCNCPNjMAn07gYBKkpkGp3oo/m0SKyyP9\n"
            + "tOX9XjNMFzEyPV/qgx5roky122OvHM85IDJJLLBPlFJzH3gXaEMqMdBrKgodF2B5\n"
            + "IoOYq8RvUmsgWYtUjQZTKQNrRdMo7SEzfhZY9VwkxI/DBGgX0JIacqER6ToGT50C\n"
            + "8MG/8u8CgYAbeOHfx0uAa26M0AhOc4neQs3rdgvTN3KZCBUhHHolRbj1HtAkLtcA\n"
            + "HDlOpzD+53tp0zdjCenB8agOZ8QNVTIHEFtJXidmznNISUBnjR8OmjQgSnFxv/WR\n"
            + "HDo8xcZZP8/VOhWx9vJRSD7Q68W9kIcrJqA9c+Al0hrEkuBDFkymlgIVALIwRRoV\n"
            + "4xWDtv+JzBP0SclOU6Fz\n"
            + "-----END DSA PRIVATE KEY-----\n";

    private BapSshPublisherPlugin getConfiguredPlugin() {
        for (Project project : hudson.getProjects()) {
            if (project.getPublisher(BapSshPublisherPlugin.DESCRIPTOR) != null)
                return (BapSshPublisherPlugin) project.getPublisher(BapSshPublisherPlugin.DESCRIPTOR);
        }
        fail();
        return null;
    }

}
