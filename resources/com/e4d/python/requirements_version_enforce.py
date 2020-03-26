#!/usr/bin/env python3

"""A script to enforce versioning requirements for python packages.

Parsing requirements.txt files for requirements is nontrivial, so we use the
python pip.req module to do it for us, then use this script to wrap around
that functionality.
"""
import glob
import sys
import pip.req
import traceback

# Scan all directories for files named requirements.txt
requirements_files = glob.glob('**/requirements.txt', recursive=True)

for req_file in requirements_files:
    try:
        for item in pip.req.parse_requirements(req_file, session="somesession"):
            if isinstance(item, pip.req.InstallRequirement):

                # To check whether or not we have versioning, we check if specifiers
                # exist for a given install requirement.
                if not item.specifier:
                    print('{} has no version specifiers.'.format(item))
                    sys.exit(1)
    except Exception as exc:
        print('Exception while trying to parse requirements file {}: {}'.format(
            req_file, exc))
        sys.exit(1)

sys.exit(0)
