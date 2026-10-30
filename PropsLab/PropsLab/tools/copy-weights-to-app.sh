#!/system/bin/sh
# Copies weights from Termux's private storage into PropsLab's, on the phone.
#
# The two apps cannot read each other's files, so the bytes are piped through the shell
# user: run-as exposes one app's directory to the reader and the other's to the writer.
# Nothing leaves the device.
set -e

DEST_PKG=com.propslab
SRC_PKG=com.termux
SRC_DIR=files/home/models
DEST_DIR=files/models

run-as $DEST_PKG mkdir -p $DEST_DIR

for name in "$@"; do
  echo "copying $name"
  run-as $SRC_PKG cat "$SRC_DIR/$name" |
    run-as $DEST_PKG sh -c "cat > '$DEST_DIR/$name'"
done

echo "--- destination ---"
run-as $DEST_PKG ls -la $DEST_DIR
