package jenkins.plugins.publish_over_ssh;

import com.jcraft.jsch.Session;
import jenkins.plugins.publish_over.BPBuildInfo;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.junit.jupiter.api.Test;
import jenkins.plugins.publish_over_ssh.helper.BapSshTestHelper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.ArrayList;
import java.util.Arrays;


class SftpForExecTest {

private final IMocksControl mockControl = EasyMock.createStrictControl();
private final Session mockSession = mockControl.createMock(Session.class);
private final BPBuildInfo buildInfo = BapSshTestHelper.createEmpty(true);
private final BapSshClient bapSshClient = new BapSshClient(buildInfo, mockSession);

    @Test
    void testSftpExecCommandsParsing() {
        final String inputCommands = """
            ln -s ./mainFile ./symlinkFile

            ln ./mainFile ./hardlinkFile

            mkdir testDirectory
            cd testDirectory
            rmdir testAnotherDirectory""";

        final String[] expectedInputCommands = {"ln", "-s", "./mainFile", "./symlinkFile", "ln", "./mainFile", "./hardlinkFile",
            "mkdir", "testDirectory", "cd", "testDirectory", "rmdir", "testAnotherDirectory"};

        BapSshTransfer transfer = new BapSshTransfer("", "", "", false, false, inputCommands, 20000);

        ArrayList<String> expectedInputCommandList = new ArrayList<>(Arrays.asList(expectedInputCommands));
        ArrayList<String> parsedCommand = new ArrayList<>();

        for (String command:bapSshClient.parseAllCommands(transfer)) {
            parsedCommand.addAll(Arrays.asList(bapSshClient.parseCommand(command)));
        }

        assertEquals(expectedInputCommandList, parsedCommand);
    }
}
