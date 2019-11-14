package com.smadec.maxx.cigr;

public class JNIMainLib {

    static {
        System.loadLibrary("native-lib"); // libnative-lib.so (Unixes)
    }

    // Native method declaration
    public native static void EncoderInit(int iWidth, int iHeight, int iRotation, String szDirectory);

    public native static void EncoderUninit(boolean bMakeStabilize);

    public native static void EncoderSendFrame(byte[] data, long lMsec);

    public native static double StabilizerCheckSyncFile();

    public native static double StabilizerStartSyncFileSyncronious(String from_file, String dest_dir);

    public native static void MakeResultVideo(String from_file_png, String from_file_mask, String from_file_mp4, String logo_file, String dest_dir);

    public native static int CheckResultVideo();

    public native static void StabilizerSyncStopStabilize();
}
