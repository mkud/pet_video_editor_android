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
    --arch=arm \
    --enable-gpl        \
    --enable-libx264    \
    --enable-cross-compile \
    --sysroot=$SYSROOT \
    --extra-cflags="-O3 -fpic $ADDI_CFLAGS -I../x264/android/arm/include" \
    --extra-ldflags="$ADDI_LDFLAGS -L../x264/android/arm/lib" \
    $ADDITIONAL_CONFIGURE_FLAG
make clean
make
make install
}
CPU=arm
PREFIX=$(pwd)/android/$CPU
ADDI_CFLAGS="-marm"
build_one
