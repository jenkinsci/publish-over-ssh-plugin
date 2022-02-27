# Changelog

For changes since 1.14 please go to the releases section in [Github](https://github.com/jenkinsci/publish-over-ssh-plugin/releases)

## 1.14 (24/03/2016)

-   [JENKINS-29360](https://issues.jenkins-ci.org/browse/JENKINS-29360) Bumped
    the version of jsch to overcome algorithm issue
-   Add support for proxies ([PR
    \#10](https://github.com/jenkinsci/publish-over-ssh-plugin/pull/10))

## 1.13 (19/05/2015)

-   Implement methods for adding and removing host configurations ([PR
    \#5](https://github.com/jenkinsci/publish-over-ssh-plugin/pull/5))

## 1.12 (16/10/2014)

-   Don't blow up errors when interacting with Item.EXTENDED\_READ ([PR
    \#8](https://github.com/jenkinsci/publish-over-ssh-plugin/pull/8))

## 1.11 (30/12/2013)

-   [JENKINS-17058](https://issues.jenkins-ci.org/browse/JENKINS-17058)
    Publish over SSH plugin XML configuration cannot be read on Jenkins
    start up.

## 1.10 (03/03/2013)

-   [JENKINS-16681](https://issues.jenkins-ci.org/browse/JENKINS-16681)
    Allow source file names and paths to contain whitespace
    -   Add Advanced Transfer Set option "Pattern separator"

## 1.9 (22/10/2012)

-   [JENKINS-13831](https://issues.jenkins-ci.org/browse/JENKINS-13831)
    Option to create empty directories
-   No default excludes option now available for all versions of Jenkins
-   Exclude files pattern now available for all versions of Jenkins

## 1.8 (10/09/2012)

-   [JENKINS-13693](https://issues.jenkins-ci.org/browse/JENKINS-13693)
    Add option to disable default excludes
-   Prefix Publish over to the global config section title
-   Move the defaults configuration in the global config to an Advanced
    section

## 1.7 (08/05/2012)

-   Fixed
    [JENKINS-13714](https://issues.jenkins-ci.org/browse/JENKINS-13714)
    Drag and drop handle missing from transfer sets

## 1.6 (06/02/2012)

-   Upgrade JSch from 0.1.44 to 0.1.45
-   Remove gssapi-with-mic as a preferred auth method

## 1.5 (10/11/2011)

-   Enable the server credentials to be specified/overriden when
    configuring the publisher in a job

## 1.4 (11/09/2011)

-   [JENKINS-10965](https://issues.jenkins-ci.org/browse/JENKINS-10965)
    Enable exec command to be run in a pseudo TTY

## 1.3 (05/08/2011)

-   [JENKINS-10599](https://issues.jenkins-ci.org/browse/JENKINS-10599)
    When using the Flatten files option, do not create the Remote
    directory if there are no files to transfer

## 1.2 (21/07/2011)

-   [JENKINS-10315](https://issues.jenkins-ci.org/browse/JENKINS-10315)
    Only open an SFTP connection if any of the transfer sets have source
    files configured
    -   Saves time and resources on client and server
    -   Enables the plugin to run commands on servers that do not
        support SFTP
-   [JENKINS-10363](https://issues.jenkins-ci.org/browse/JENKINS-10363)
    Allow the publisher default values to be changed in Manage Jenkins
    (on Jenkins and Hudson 1.391 - 1.395)

## 1.1 (09/07/2011)

-   Fixed
    [JENKINS-10268](https://issues.jenkins-ci.org/browse/JENKINS-10268)

## 1.0 (08/07/2011)

-   Add [Parameterized
    publishing](https://wiki.jenkins.io/display/JENKINS/Publish+Over#PublishOver-parampub)
    [JENKINS-10006](https://issues.jenkins-ci.org/browse/JENKINS-10006)
-   Add ability to
    [retry](https://wiki.jenkins.io/display/JENKINS/Publish+Over#PublishOver-retry)
    the publish
    [JENKINS-10094](https://issues.jenkins-ci.org/browse/JENKINS-10094)
-   Moved the "Verbose output in console" option to the new Advanced
    section containing the other new Server options

## 0.14 (06/06/2011)

-   Fixed
    [JENKINS-9878](https://issues.jenkins-ci.org/browse/JENKINS-9878)
    where the password/passphrase for an individual configuration was
    ignored when saving the global config (Manage Jenkins)

## 0.13 (20/05/2011)

-   Remove "Give the master a NODE\_NAME" option when running on Jenkins
    1.414 or later
-   Default the "Give the master a NODE\_NAME" option to 'master' when
    run on a Jenkins older than 1.414

## 0.12 (09/05/2001)

-   Advanced Transfer Set option to Exclude files  
    (option only available with Jenkins 1.407 and later)
-   [JENKINS-9480](https://issues.jenkins-ci.org/browse/JENKINS-9480)
    Exec command is now an expandable textarea

## 0.11 (15/04/2011)

-   Fixed form validation issue
    ([JENKINS-9376](https://issues.jenkins-ci.org/browse/JENKINS-9376))
    when selected configuration name contains non ASCII characters

## 0.10 (14/04/2011)

-   Fix potential NPE when performing ajax form validation

## 0.9 (10/04/2011)

-   Reduce horizontal space taken up by labels in the configuration
    views

## 0.8 (10/04/2011)

-   Add options to disable exec for individual configurations, or for
    the whole plugin

## 0.7 (01/04/2011)

-   Enable form validation for SSH key file location for Jenkins 1.399
    and later

## 0.6 (07/03/2011)

-   Fixed
    [JENKINS-8982](https://issues.jenkins-ci.org/browse/JENKINS-8982)
    where configuration for the builder was not populated when re
    configuring a job

## 0.5 (18/02/2011)

-   Passwords/passphrases encrypted in config files and in UI (now
    requires Hudson \> 1.384 or any Jenkins)
-   Environment variables for substitution now include build variables
    (including matrix build axis)
-   Added build wrapper to enable SSH before a (maven) project build, or
    to run after a build whether the build was successful or not

## 0.4 (16/02/2011)

-   Added form validation
-   Removed debug logs for new configurations

## 0.3 (11/02/2011)

-   2 new configuration options when in promotion
    -   Use the workspace when selecting "Source files"
    -   Use the time of the promotion when using "Remote directory is a
        date format"

## 0.2 (10/02/2011)

-   Stop the builder from showing up in promotion actions as publisher
    already included

## 0.1 (08/02/2011)

-   Initial release

Questions, Comments, Bugs and Feature Requests

Please post questions or comments about this plugin to the [Jenkins User
mailing list](http://jenkins-ci.org/content/mailing-lists).  
To report a bug or request an enhancement to this plugin please [create
a ticket in
JIRA](http://issues.jenkins-ci.org/browse/JENKINS/component/15792).
