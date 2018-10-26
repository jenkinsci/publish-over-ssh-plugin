package jenkins.plugins.publish_over_ssh;

import com.jcraft.jsch.Session;
import jenkins.plugins.publish_over.BPBuildInfo;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import jenkins.plugins.publish_over_ssh.helper.BapSshTestHelper;
import org.junit.Test;
import java.util.ArrayList;
import java.util.Arrays;
import static org.junit.Assert.assertEquals;


public class SftpForExecTest {

private final IMocksControl mockControl = EasyMock.createStrictControl();
private final Session mockSession = mockControl.createMock(Session.class);
private final BPBuildInfo buildInfo = BapSshTestHelper.createEmpty(true);
private final BapSshClient bapSshClient = new BapSshClient(buildInfo, mockSession);

@Test public void testSftpExecCommandsParsing() throws Exception {
        final String inputCommands = "  ln -s ./mainFile ./symlinkFile    \n"
            + "\n"
            + " ln ./mainFile ./hardlinkFile \n"
            + "         \n"
            + "  mkdir testDirectory \n cd testDirectory \n"
            + "rmdir testAnotherDirectory";

        final String[] expectedInputCommands = {"ln", "-s", "./mainFile", "./symlinkFile", "ln", "./mainFile", "./hardlinkFile",
            "mkdir", "testDirectory", "cd", "testDirectory", "rmdir", "testAnotherDirectory"};

        BapSshTransfer transfer = new BapSshTransfer("", "", "", false, false, inputCommands, 20000);

        ArrayList<String> expectedInputCommandList = new ArrayList<>(Arrays.asList(expectedInputCommands));
        ArrayList<String> parsedCommand = new ArrayList<>();

        for (String command:bapSshClient.parseAllCommands(transfer)) {
            for (String commandPart:bapSshClient.parseCommand(command)) {
                parsedCommand.add(commandPart);
            }
        }

        assertEquals(expectedInputCommandList, parsedCommand);
    }
}
