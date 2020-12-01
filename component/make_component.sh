#!/usr/bin/env bash
#
# Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# The Universal Permissive License (UPL), Version 1.0
#
# Subject to the condition set forth below, permission is hereby granted to any
# person obtaining a copy of this software, associated documentation and/or
# data (collectively the "Software"), free of charge and under any and all
# copyright rights in the Software, and any and all patent rights owned or
# freely licensable by each licensor hereunder covering either (i) the
# unmodified Software as contributed to or provided by such licensor, or (ii)
# the Larger Works (as defined below), to deal in both
#
# (a) the Software, and
#
# (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
# one is included with the Software each a "Larger Work" to which the Software
# is contributed by such licensors),
#
# without restriction, including without limitation the rights to copy, create
# derivative works of, display, perform, and distribute the Software and make,
# use, sell, offer for sale, import, export, have made, and have sold the
# Software and the Larger Work(s), and to sublicense the foregoing rights on
# either these or other terms.
#
# This license is subject to the following condition:
#
# The above copyright notice and either this complete permission notice or at a
# minimum a reference to the UPL must be included in all copies or substantial
# portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.
#

declare -r JAVA_VERSION="${1:?First argument must be java version.}"
declare -r GRAALVM_VERSION="${2:?Second argument must be GraalVM version.}"
if [[ $JAVA_VERSION == 1.8* ]]; then
    JRE="jre/"
elif [[ $JAVA_VERSION == 11* ]]; then
    JRE=""
else
    echo "Unkown java version: $JAVA_VERSION"
    exit 1
fi
readonly COMPONENT_DIR="component_temp_dir"
readonly LANGUAGE_PATH="$COMPONENT_DIR/$JRE/languages/lazy"
if [[ -f ../native/lsnative ]]; then
    INCLUDE_LSNATIVE="TRUE"
fi

rm -rf COMPONENT_DIR

mkdir -p "$LANGUAGE_PATH"
cp ../language/target/lazyscript.jar "$LANGUAGE_PATH"

mkdir -p "$LANGUAGE_PATH/launcher"
cp ../launcher/target/ls-launcher.jar "$LANGUAGE_PATH/launcher/"

mkdir -p "$LANGUAGE_PATH/bin"
cp ../lazyscript$LANGUAGE_PATH/bin/
if [[ $INCLUDE_LSNATIVE = "TRUE" ]]; then
    cp ../native/lsnative $LANGUAGE_PATH/bin/
fi

touch "$LANGUAGE_PATH/native-image.properties"

mkdir -p "$COMPONENT_DIR/META-INF"
{
    echo "Bundle-Name: LazyScript";
    echo "Bundle-Symbolic-Name: com.guillermomolina.lazyscript";
    echo "Bundle-Version: $GRAALVM_VERSION";
    echo "Bundle-RequireCapability: org.graalvm; filter:=\"(&(graalvm_version=$GRAALVM_VERSION)(os_arch=amd64))\"";
    echo "x-GraalVM-Polyglot-Part: True"
} > "$COMPONENT_DIR/META-INF/MANIFEST.MF"

(
cd $COMPONENT_DIR || exit 1
jar cfm ../ls-component.jar META-INF/MANIFEST.MF .

echo "bin/lazy = ../$JRE/languages/lazyscript/bin/lazy" > META-INF/symlinks
if [[ $INCLUDE_LSNATIVE = "TRUE" ]]; then
    echo "bin/lsnative = ../$JRE/languages/lazyscript/bin/lsnative" >> META-INF/symlinks
fi
jar uf ../ls-component.jar META-INF/symlinks

{
    echo "$JRE"'languages/lazyscript/bin/lazy= rwxrwxr-x'
    echo "$JRE"'languages/lazyscript/bin/lsnative = rwxrwxr-x'
} > META-INF/permissions
jar uf ../ls-component.jar META-INF/permissions
)
rm -rf $COMPONENT_DIR
