#!/usr/bin/env python
# This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
# the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
# If it is not possible or desirable to put the notice in a particular file, then You may include 
# the notice in a location (such as a LICENSE file in a relevant directory) where a recipient 
# would be likely to look for such a notice.
#
# You may add additional accurate notices of copyright ownership.
#
# Copyright (c) 2013 Digi International Inc., All Rights Reserved.

import git
import glob
import os
import re
import sys
import zipfile

IGNORE_DIRS = ["AppDoc", "gradle", "WVA_App_Test", "WVALib_Test",
               "doc", "bin", "gen", "test", "out"]
IGNORE_FILE_NAMES = ["gradlew(|.bat)", ".*\.py", ".*\.pyc", "REVFILE",
                    ".*\.zip", ".*\.sh", "local.properties", ".*\.gradle",
                    ".*\.iml", "ant.properties", "build.xml", "Makefile",
                    "README", "local.properties", "javadoc.xml"]
PERMITTED_DOT_FILES = [".project", ".classpath"]

def scandirs(path='.'):
    result = []
    current_files = glob.glob(os.path.join(path, '*'))
    dotfiles = filter(lambda n: os.path.basename(n) in PERMITTED_DOT_FILES,
                        glob.glob(os.path.join(path, '.*')))
    current_files.extend(dotfiles)

    for f in current_files:
        if os.path.isdir(f):
            if os.path.basename(f) not in IGNORE_DIRS and \
                f not in IGNORE_DIRS:
                result.extend(scandirs(f))
        else:
            basename = os.path.basename(f)
            for r in IGNORE_FILE_NAMES:
                if re.match(r, basename) is not None:
                    # file name matches some ignore_file_names entry
                    break
            else:  # filename not in ignore_file_names
                result.append(f)
    return result

def makezip(rev):
    INFO_FILE = "info.txt"
    with open(INFO_FILE, 'w') as f:
        f.write("Digi Wireless Vehicle Bus Adapter Sample Android Application\n")
        f.write("Source code for Eclipse project\n\n")
        f.write("Digi Part Number: 82003335_%s\n" % rev)
        if git.has_git():
            f.write("Commit: %s\n" % git.current_sha())
        else:
            print "GIT UNAVAILABLE; COULDN'T WRITE CURRENT COMMIT TO", INFO_FILE
            f.write("Commit information unknown.")

    files = scandirs('.')

    zipname = "82003335_%s.zip" % rev

    if os.path.exists(zipname):
        print zipname, "already exists! Remove it first"
        return 1

    zf = zipfile.ZipFile(zipname, 'w')
    for f in files:
        f = os.path.normpath(f)
        print "Processing file:", f
        zf.write(f)
    print "Done. Zipfile can be found at", zipname
    return 0

if __name__ == "__main__":
    if len(sys.argv) < 2:
        # run 'python makezip.py <rev_letter>'
        print "Usage: python makezip.py <REV_LETTER>"
    else:
        makezip(sys.argv[1])
