#!/bin/bash
# Build script for Universal Pokemon Randomizer ZX (custom)
# Requires JDK 8 (1.8) and IntelliJ IDEA (for form compiler)

JAVA_HOME="/c/Program Files/Java/jdk1.8.0_202"
JAR="$JAVA_HOME/bin/jar"
JAVA="$JAVA_HOME/bin/java"

IDEA_HOME="/c/Program Files/JetBrains/IntelliJ IDEA 2025.3.4"
FORMS_RT="$IDEA_HOME/lib/forms_rt.jar"
JAVAC2="$IDEA_HOME/plugins/java/lib/javac2.jar"
ASM_JAR="$IDEA_HOME/lib/module-intellij.libraries.asm.jar"
JDOM_JAR="$IDEA_HOME/lib/util-8.jar"
ANT_DIR="$IDEA_HOME/plugins/gradle/lib/ant"

# Use a temp dir without spaces to avoid javac issues
BUILD_DIR="$(cygpath -u "$LOCALAPPDATA")/Temp/pkrandom_build"
CLASSES_DIR="$BUILD_DIR/classes"

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
SRC_DIR="$PROJECT_DIR/src"
JAR_NAME="PokeRandoZX.jar"
OUTPUT_JAR="$PROJECT_DIR/launcher/$JAR_NAME"

echo "=== Universal Pokemon Randomizer ZX - Build Script ==="
echo ""

# Verify paths exist
if [ ! -f "$JAVA_HOME/bin/javac" ]; then
    echo "ERROR: JDK 8 not found at $JAVA_HOME"
    exit 1
fi
if [ ! -f "$FORMS_RT" ]; then
    echo "ERROR: IntelliJ not found at $IDEA_HOME"
    exit 1
fi

# Clean previous build
echo "[1/4] Cleaning previous build..."
rm -rf "$BUILD_DIR"
mkdir -p "$CLASSES_DIR"
mkdir -p "$BUILD_DIR/src"

# Copy source to temp dir (avoids spaces in paths)
echo "[2/4] Copying source files..."
cp -r "$SRC_DIR"/* "$BUILD_DIR/src/"
SOURCE_COUNT=$(find "$BUILD_DIR/src" -name "*.java" | wc -l)
echo "       Found $SOURCE_COUNT Java files"

# Windows paths for Ant
W_CLASSES=$(cygpath -w "$CLASSES_DIR")
W_SRC=$(cygpath -w "$BUILD_DIR/src")
W_FORMS_RT=$(cygpath -w "$FORMS_RT")
W_JAVAC2=$(cygpath -w "$JAVAC2")
W_ASM=$(cygpath -w "$ASM_JAR")
W_JAVAHOME=$(cygpath -w "$JAVA_HOME")

# Create Ant build file using javac2 (compiles + instruments forms in one step)
cat > "$BUILD_DIR/build.xml" << ANTEOF
<project name="pkrandom" default="compile" basedir=".">
    <path id="javac2.cp">
        <pathelement location="$W_JAVAC2"/>
        <pathelement location="$W_FORMS_RT"/>
        <pathelement location="$W_ASM"/>
        <pathelement location="$(cygpath -w "$JDOM_JAR")"/>
    </path>
    <taskdef name="javac2" classname="com.intellij.ant.Javac2" classpathref="javac2.cp"/>

    <target name="compile">
        <javac2 srcdir="$W_SRC" destdir="$W_CLASSES"
                source="1.8" target="1.8" encoding="UTF-8"
                includeantruntime="false"
                fork="true" executable="$W_JAVAHOME\\bin\\javac">
            <classpath>
                <pathelement location="$W_FORMS_RT"/>
            </classpath>
        </javac2>
    </target>
</project>
ANTEOF

# Compile + instrument forms with Ant javac2
echo "[3/4] Compiling and instrumenting GUI forms..."
ANT_CP="$(cygpath -w "$ANT_DIR/ant-launcher.jar");$(cygpath -w "$ANT_DIR/ant.jar")"

"$JAVA" -cp "$ANT_CP" org.apache.tools.ant.launch.Launcher \
    -f "$(cygpath -w "$BUILD_DIR/build.xml")" \
    compile 2>&1

if [ $? -ne 0 ]; then
    echo ""
    echo "ERROR: Compilation/instrumentation failed. See errors above."
    exit 1
fi

# Copy non-Java resources (config files, images, patches, etc.)
echo "       Copying resources..."
find "$BUILD_DIR/src" -type f \
    ! -name "*.java" \
    ! -name "*.form" \
    | while read -r file; do
    rel="${file#$BUILD_DIR/src/}"
    dest="$CLASSES_DIR/$rel"
    mkdir -p "$(dirname "$dest")"
    cp "$file" "$dest"
done

# Create manifest
echo "Main-Class: com.dabomstew.pkrandom.newgui.NewRandomizerGUI" > "$BUILD_DIR/MANIFEST.MF"

# Build JAR (include forms_rt classes so it runs standalone)
echo "[4/4] Creating JAR..."
cd "$CLASSES_DIR"
"$JAR" xf "$FORMS_RT"
rm -rf META-INF

"$JAR" cfm "$OUTPUT_JAR" "$BUILD_DIR/MANIFEST.MF" .

if [ $? -ne 0 ]; then
    echo "ERROR: JAR creation failed."
    exit 1
fi

echo ""
echo "=== Build successful! ==="
echo "JAR: $OUTPUT_JAR"
echo ""
echo "To run, double-click: launcher/launcher_WINDOWS.bat"
echo "Or from bash:"
echo "  cd launcher && \"$JAVA_HOME/bin/java\" -Xmx4608M -jar $JAR_NAME please-use-the-launcher"
