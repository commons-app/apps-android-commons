#! /bin/sh
# Spot malformed string replacement patterns in Android localization files.
# First install Lint from the Android SDK

grep -R "%1$ s" commons/res/values*
grep -R "%1$ d" commons/res/values*
grep -R "%1" commons/res/values* | grep -v "%1\\$"

grep -RH '%' commons/res/values* | 
 sed -e 's/%/\n%/g' | # Split lines that contain several expressions
 grep '%'           | # Filter out lines that do not contain expressions
 grep -v ' % '      | # Lone % character, not a variable
 grep -v '%<'       | # Same, at the end of the string
 #grep -v '% '       | # Same, at the beginning of the string
 grep -v '%で'      | # Same, no spaces in Japanese
 grep -v '%s'       | # Single string variable
 grep -v '%d'       | # Single decimal variable
 grep -v '%[0-9][0-9]\?$s' | # Multiple string variable
 grep -v '%[0-9][0-9]\?$d' |  # Multiple decimal variable
 grep -v '%1$.1f'   | # ?
 grep -v '%.1f'     |
 grep -v '%\\n'     |
 grep -v '%20'        # Ignore URL whitespace
exit
# Double-width percent sign
grep -R '％' commons/res/values*

# Broken CDATA syntax
grep -R "CDATA " commons/res/values*

# Android SDK Lint (does not detect most syntax errors)
lint --check StringFormatInvalid commons
