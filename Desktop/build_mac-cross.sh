#!/bin/bash
#
# EIM - Email Instant Messaging build script for Mac OS X
#
# by Denis Meyer
#

set -o errexit
set -o pipefail

#############
# Variables #
#############

SYSTEM_NAME=$(uname -s)
SCRIPT_VERSION="1.1.0"
OPERATING_SYSTEM_OK=0
FOLDER_NAME_APP="EIM_v0-13-2_beta_build-1_mac"
FOLDER_NAME_CROSS="EIM_v0-13-2_beta_build-1_cross"
APP_VOLUME_NAME="EIM v0.13.2 beta build 1"

#############
# Functions #
#############

function print_logo()
{
	echo ""
	echo "###########################################################"
	echo "#                                                         #"
	echo "# EIM - Email Instant Messaging build script for Mac OS X #"
	echo "#                                                         #"
	echo "###########################################################"
	echo "Script version $SCRIPT_VERSION by Denis Meyer"
	echo ""
}

function check_operatingSystem()
{
	case $SYSTEM_NAME in
	  Darwin)
		echo "1"
	    ;;
	  *)
		echo "0"
	    ;;
	esac
}

function exit_error()
{
	exit 2
}

##########
# Script #
##########

print_logo

# check the operating system
echo "Checking the operating system..."
OPERATING_SYSTEM_OK=$(check_operatingSystem)
if [[ $OPERATING_SYSTEM_OK == 1 ]];
then
	echo "Operating system supported."
else
	echo "Error: Unsupported platform: $SYSTEM_NAME" >&2
	exit_error
fi

rm -rf store/
ant clean
ant compile
ant jar
ant package-for-store
ant mac-bundle-EIM
ant clean
cp bundling-resources/mac/Info.plist store/EIM.app/Contents/
cp bundling-resources/mac/GenericApp.icns store/EIM.app/Contents/Resources/
mkdir store/$FOLDER_NAME_APP
mkdir store/$FOLDER_NAME_CROSS
cp -R bundling-resources/Files/ store/$FOLDER_NAME_APP/Files
cp -R bundling-resources/Files/ store/$FOLDER_NAME_CROSS/Files
mv store/EIM.app store/$FOLDER_NAME_APP/EIM.app
mv store/EIM.jar store/$FOLDER_NAME_CROSS/EIM.jar
cd store
hdiutil create -fs HFS+ -srcfolder $FOLDER_NAME_APP -volname "$APP_VOLUME_NAME" $FOLDER_NAME_APP.dmg
zip -r $FOLDER_NAME_CROSS.zip $FOLDER_NAME_CROSS -x ".*"
rm -rf $FOLDER_NAME_APP
rm -rf $FOLDER_NAME_CROSS
