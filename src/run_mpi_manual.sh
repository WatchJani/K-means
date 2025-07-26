#!/bin/bash

MPJ_JAR=../../../mpj-v0_44/lib/mpj.jar
JSON_JAR=../../../javax.json-1.0.4.jar
JAVAFX_LIB=/home/janko/89221073_k-means/openjfx-24.0.1_linux-x64_bin-sdk/javafx-sdk-24.0.1/lib
CLASS_DIR=.

for i in {0..3}; do
  echo "Starting process $i..."
  java \
    --module-path "$JAVAFX_LIB" \
    --add-modules javafx.controls,javafx.fxml,javafx.web \
    -cp "$CLASS_DIR:$MPJ_JAR:$JSON_JAR" \
    MPIMain $i &
done

wait
