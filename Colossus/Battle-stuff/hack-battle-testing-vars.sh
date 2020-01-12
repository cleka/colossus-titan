#! /bin/sh

#
# Replaces two variables in source files to false or true
#
# Set to true to suppress the flee and concede dialog, so I don't have to click
# them over and over again.
#
# In git they are false, for testing/developing battle temporarily set to true
#

if [ $# -ne 1 ]
then
    echo "1 Argument needed ('true' or 'false')"
    exit 1
fi

tcADF="public boolean testCaseAutoDontFlee"
sDEDMS="private boolean saveDuringEngagementDialogMessageShown"

DONT_FLEE_VAL=$1
DIALOG_SHOWN_VAL=$1

sed -i -e "s/$tcADF = \(.*\);/$tcADF = $DONT_FLEE_VAL;/" core/src/main/java/net/sf/colossus/client/Client.java

sed -i -e "s/$sDEDMS = \(.*\);/$sDEDMS = $DIALOG_SHOWN_VAL;/" core/src/main/java/net/sf/colossus/gui/MasterBoard.java
