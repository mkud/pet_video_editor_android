#!/bin/bash

NDK=/home/maxx/android-ndk-r15c
SYSROOT=$NDK/platforms/android-26/arch-x86_64/
TOOLCHAIN=$NDK/toolchains/x86_64-4.9/prebuilt/linux-x86_64
function build_one
{
./configure \
    --prefix=$PREFIX \
    --enable-shared \
    --disable-static \
    --disable-doc \
    --disable-ffmpeg \
    --disable-ffplay \
    --disable-ffprobe \
    --disable-ffserver \
    --disable-avdevice \
    --disable-doc \
    --disable-symver \
    --cross-prefix=$TOOLCHAIN/bin/x86_64-linux-android- \
    --target-os=linux \
    --arch=x86_64 \
    --enable-cross-compile \
    --sysroot=$SYSROOT \
    --extra-cflags="-Os -fpic" \
    --extra-ldflags="" \
    $ADDITIONAL_CONFIGURE_FLAG
make clean
make
make install
}
CPU=x86_64
PREFIX=$(pwd)/android/$CPU
build_one
