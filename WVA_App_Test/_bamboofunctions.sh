# Functions defined for use in bamboo.sh

function kill_all_emulators() {
    if [ "$(adb devices | grep 'emulator-' | wc -l)" -ge 1 ]; then
        adb devices | grep 'emulator-' | awk '{print $1}' | xargs -n1 sh -c 'adb -s $0 emu kill'
    else
        echo "No emulators are running..."
    fi
}

function wait_for_start() {
    boot_ok=0
    find_elapsed=0
    wait_elapsed=0
    max_find=30
    max_wait=60
    while [ $boot_ok -lt 1 ]; do
        if [ $(adb devices | grep emulator | wc -l) -lt 1 ]; then
            echo "No emulator found yet..."
            sleep 3
            (( find_elapsed += 3 ))
            if [[ $find_elapsed -gt $max_find ]]; then
                echo "Waited too long to find an emulator; exiting..."
                exit 1
            fi
        fi
        # Look for online emulators
        boot_ok=`adb devices | grep -e 'emulator-.*device' | wc -l`
        if [[ $boot_ok -ge 1 ]]; then
            echo "An emulator is online. Finishing wait..."
            return 0
        else
            sleep 3
            (( wait_elapsed += 3 ))
            if [[ $wait_elapsed -gt $max_wait ]]; then
                echo "Waited too long for emulator to come online; exiting..."
                exit 1
            fi
        fi
    done
}

function wait_for_pm() {
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

function wait_for_no_emu() {
    while [ "$(adb devices | grep emulator | wc -l)" -ge 1 ]; do
        echo "There is still an emulator running..."
        sleep 1
    done
    echo "Emulator has stopped"
}

function fail_kill_emu() {
    echo "\n\nStopping script!: $1"
    kill_all_emulators
    exit 1
}
