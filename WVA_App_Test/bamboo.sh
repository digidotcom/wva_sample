#!/bin/bash

kill_all_emulators() {
    if [ "$(adb devices | grep 'emulator-' | wc -l)" -ge 1 ]; then
        adb devices | grep 'emulator-' | awk '{print $1}' | xargs -n1 sh -c 'adb -s $0 emu kill'
    else
        echo "No emulators are running..."
    fi
}

wait_for_start() {
    boot_ok=0
    find_elapsed=0
    wait_elapsed=0
    max_find=30
    max_wait=60
    while [ $boot_ok -lt 1 ]; do
        if [ $(adb devices | grep emulator | wc -l) -lt 1 ]; then
            echo "No emulator found yet..."
            sleep 3
            find_elapsed=`expr $find_elapsed + 3`
            if [ $find_elapsed -gt $max_find ]; then
                echo "Waited too long to find an emulator; exiting..."
                exit 1
            fi
        fi
        # Look for online emulators
        boot_ok=`adb devices | grep -e 'emulator-.*device' | wc -l`
        if [ $boot_ok -ge 1 ]; then
            echo "An emulator is online. Finishing wait..."
            return 0
        else
            sleep 3
            wait_elapsed=`expr $wait_elapsed + 3`
            if [ $wait_elapsed -gt $max_wait ]; then
                echo "Waited too long for emulator to come online; exiting..."
                exit 1
            fi
        fi
    done
}

wait_for_pm() {
    ok=1
    while [ $ok -gt 0 ]; do
        echo "Package Manager not there yet"
        sleep 1
        pmcmd="pm path com.android.launcher"
        ok=`adb shell $pmcmd | grep "system running?" | wc -l`
    done
    sleep 2
    if [ "$(adb shell $pmcmd | grep "system running?" | wc -l)" -gt 0 ]; then
        echo "Package Manager seemed to be online, but now it isn't..."
        return 1
    fi
    echo "Package Manager is online"
}

wait_for_no_emu() {
    while [ "$(adb devices | grep emulator | wc -l)" -ge 1 ]; do
        echo "There is still an emulator running..."
        sleep 1
    done
    echo "Emulator has stopped"
}

fail_kill_emu() {
    echo -e "\n\nStopping script!: $1"
    kill_all_emulators
    exit 1
}

main() {
case $1 in
    killall|kill|k)
        echo "Killing all current emulators..."
        kill_all_emulators
        return 0
        ;;
    emulator|emu|e)
        echo "Starting bamboo emulator..."
        nohup ant emu_bamboo >emulator.log 2>&1 </dev/null &
        return 0
        ;;
    wait|w)
        echo "Waiting for emulator to come online..."
        wait_for_start || fail_kill_emu "Some error occurred waiting for boot"
        echo "Calling \`adb wait-for-device'"
        adb wait-for-device || fail_kill_emu "Some error occurred waiting for device"
        echo "Waiting for emulator Package Manager to come up"
        wait_for_pm || fail_kill_emu "Package Manager never came up"
        return 0
        ;;
    *)
        echo "Usage: $0 [killall|emulator|wait]"
        ;;
esac
}

main $@
