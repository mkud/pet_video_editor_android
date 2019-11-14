#!/bin/bash

NDK=/home/maxx/android-ndk-r15c
SYSROOT=$NDK/platforms/android-26/arch-arm/
TOOLCHAIN=$NDK/toolchains/arm-linux-androideabi-4.9/prebuilt/linux-x86_64
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
    --cross-prefix=$TOOLCHAIN/bin/arm-linux-androideabi- \
    --target-os=linux \
    --arch=armv7-a \
    --enable-cross-compile \
    --sysroot=$SYSROOT \
    --extra-cflags="-Os -fpic -march=armv7-a -mfloat-abi=softfp -mfpu=vfpv3-d16" \
    --extra-ldflags="-march=armv7-a -Wl,--fix-cortex-a8" \
    $ADDITIONAL_CONFIGURE_FLAG
make clean
make
make install
}
CPU=armv7-a
PREFIX=$(pwd)/android/$CPU
build_one
