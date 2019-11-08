#!/bin/bash
machineName=`uname -a`
VMVISUAL=""
if [[ "$machineName" == Darwin* ]]
then
   JAVA=`/usr/libexec/java_home`;
   VMVISUAL=${JAVA}/bin/jvisualvm; echo "$VMVISUAL"
   $VMVISUAL &
elif [[ "$machineName" == Linux* ]]
then
   if [ -n $JAVA_HOME ]
   then
      VMVISUAL=${JAVA_HOME}/bin/jvisualvm
      $VMVISUAL &
   else
      echo "could not find java vmvisual tool execute"
   fi
   
fi
