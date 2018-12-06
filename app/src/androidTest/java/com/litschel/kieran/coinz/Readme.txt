Some important things to note before running the tests:

1) I tested this app in the emulator using the Nexus 5X emulator with android Oreo at API level 27.
   There is no reason why the app shouldn't work on other phones and versions of android, but I haven't
   tested this, so I would recommend running the tests on an emulator in this configuration to be safe.

2) If you've logged into an account in the app prior to testing, log out of it before running the tests.
   This is necessary as each test assumes it starts on the login screen, which won't happen if a user
   was logged in last time the app was run, as one of the features of the app is users stay logged in
   until logged out.

3) If you've been using the emulator prior, I'd recommend powering off the emulator and forcing a cold boot
   (cold boot now) via the AVD manager before running tests. This is because I've noticed the emulator
   can get quite laggy if it has been running for a while, which can intefere with the timing of the
   espresso tests causing them to fail.

4) These tests rely on an uninterrupted internet connection and Firebase executing queries and updates
   within 5 seconds. Naturally in rare cases one of these assumptions could be false, which would cause the
   test to fail. I've run the tests repeatedly and found no issues with them, so if a test does fail
   it is probably down to one of the two assumptions not being true. So if a test fails I'd recommend
   rerunning the failed test at least once, and you should see on following runs it does not fail.