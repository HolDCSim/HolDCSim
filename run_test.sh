#!/bin/bash

passed=0
failed=0;

function mytest {
    bash tests/"$@".sh $@
    local status=$?
    if [ $status -ne 0 ]; then
	    passed=$(($passed + 1));
    else
	    failed=$(($failed + 1));
    fi

    return $status
}

mytest 1_check_network_no_sleep 
mytest 2_network_sleep
mytest 3_check_server_no_sleep
mytest 4_server_sleep


echo "---------------------------------------------------------"
echo "Passed test=$passed; Failed=$failed\n"
echo "---------------------------------------------------------"
