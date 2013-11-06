# This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of
# the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
# Copyright (c) 2013 Digi International Inc., All Rights Reserved.
'''
Basic git bindings.

Requires:
(Requires git on the system path.)
'''

import subprocess


_has_git_command = "git".split()
_sha_command = "git rev-parse --short HEAD".split()
_tag_command = "git describe --abbrev=0 --tags".split()
_clean_command = "git status -suno".split()


def has_git():
    try:
        subprocess.Popen(_has_git_command, stdout=subprocess.PIPE)
        return True
    except OSError:  # git not found
        return False


def current_sha():
    ''' returns the current checked out sha as a string '''
    return subprocess.Popen(_sha_command, stdout=subprocess.PIPE).\
        communicate()[0].strip()


def current_tag():
    return subprocess.Popen(_tag_command, stdout=subprocess.PIPE).\
        communicate()[0].strip()


def working_directory_clean():
    ''' returns true if the current working directory is clean '''
    return len(subprocess.Popen(_clean_command, stdout=subprocess.PIPE).
               communicate()[0].strip()) == 0
