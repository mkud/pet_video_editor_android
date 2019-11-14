#include <jni.h>
#include <string>
#include <opencv2/core.hpp>
#include "Encoder.h"

bool g_bFFMpegInited = false;
bool g_EncoderOneFrameSended = false;
Encoder *g_Encoder = 0;
int g_encoderWidth = 0, g_encoderHeight = 0;
std::string g_szDirectory;

extern double
convert_from_to(std::string from, std::string to, std::string to_first_frame, std::string log);

extern void make_result_video(std::string from_png, std::string from_mask, std::string from_mp4,
                              std::string logo, std::string to);

std::thread *g_StabilizerThread = 0;
std::thread *g_FinishVideoThread = 0;

extern void run2(std::string from, std::string to, std::string to_first_frame, std::string log);

extern int g_CountFrames;
extern int g_CurrentFrame;
extern bool g_bFinished;
extern bool g_bCannotStabilize;
bool g_bStopStabilizer = false;
extern bool g_bResultVideoFinished;

void GetJStringContent(JNIEnv *AEnv, jstring AStr, std::string &ARes) {
    if (!AStr) {
        ARes.clear();
        return;
    }

    const char *s = AEnv->GetStringUTFChars(AStr, NULL);
    ARes = s;
    AEnv->ReleaseStringUTFChars(AStr, s);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_smadec_maxx_cigr_JNIMainLib_EncoderInit(
        JNIEnv *env,
        jobject /* this */,
        jint width, jint height, jint rotation, jstring directory) {

    GetJStringContent(env, directory, g_szDirectory);

    if (!g_bFFMpegInited) {
        Decoder::InitFFMPEG();
        g_bFFMpegInited = true;
    }
    if (g_Encoder) {
        if ((width == g_encoderWidth) && (height == g_encoderHeight))
            return;
        delete g_Encoder;
    }
    g_encoderWidth = width;
    g_encoderHeight = height;
    g_Encoder = new Encoder(g_szDirectory + "/cigr_tmp.mp4");
    g_Encoder->Init(width, height, {1, 1000}, rotation, 4);
    g_EncoderOneFrameSended = false;
    return;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_smadec_maxx_cigr_JNIMainLib_EncoderUninit(
        JNIEnv *env,
        jobject /* this */,
        jboolean bMakeStabilize) {
    if (g_Encoder) {
        if (g_EncoderOneFrameSended) {
            g_Encoder->release();
            if (bMakeStabilize) {
                g_bStopStabilizer = false;
                rename((g_szDirectory + "/cigr_tmp.mp4").c_str(),
                       (g_szDirectory + "/cigr.mp4").c_str());
                g_bFinished = false;
                g_StabilizerThread = new std::thread(run2, g_szDirectory + "/cigr.mp4",
                                                     g_szDirectory + "/cigr_stab.mp4",
                                                     g_szDirectory + "/cigr_first_frame.png",
                                                     g_szDirectory + "/cigr_stab.log");
            }
        }
        delete g_Encoder;
        g_Encoder = 0;
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_smadec_maxx_cigr_JNIMainLib_EncoderSendFrame(
        JNIEnv *env,
        jobject /* this */,
        jbyteArray data,
        jlong msec) {
    if (!g_Encoder)
        return;
    g_EncoderOneFrameSended = true;
    jsize len = env->GetArrayLength(data);
    jbyte *body = env->GetByteArrayElements(data, 0);
    g_Encoder->write2((uint8_t *) body, msec);
    env->ReleaseByteArrayElements(data, body, 0);
    return;
}

extern "C"
JNIEXPORT jdouble JNICALL
Java_com_smadec_maxx_cigr_JNIMainLib_StabilizerCheckSyncFile(
        JNIEnv *env,
        jobject /* this */) {
    if (g_bCannotStabilize) {
        if (g_StabilizerThread && !g_bStopStabilizer) {
            g_StabilizerThread->join();
            delete g_StabilizerThread;
            g_StabilizerThread = 0;
        }
        return -2;
    }
    if (g_bFinished) {
        if (g_StabilizerThread) {
            g_StabilizerThread->join();
            delete g_StabilizerThread;
            g_StabilizerThread = 0;
        }
        return -1;
    }

    return double(g_CurrentFrame) / g_CountFrames;
}

extern "C"
JNIEXPORT jdouble JNICALL
Java_com_smadec_maxx_cigr_JNIMainLib_StabilizerStartSyncFileSyncronious(JNIEnv *env, jclass type,
                                                                        jstring from_file_,
                                                                        jstring dest_dir_) {
    const char *from_file = env->GetStringUTFChars(from_file_, 0);
    const char *dest_dir = env->GetStringUTFChars(dest_dir_, 0);

    double ret = convert_from_to(from_file, string(dest_dir) + "/cigr_stab.mp4",
                                 string(dest_dir) + "/cigr_first_frame.png",
                                 string(dest_dir) + "/cigr_stab.log");

    env->ReleaseStringUTFChars(from_file_, from_file);
    env->ReleaseStringUTFChars(dest_dir_, dest_dir);

    return ret;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_smadec_maxx_cigr_JNIMainLib_MakeResultVideo(JNIEnv *env, jclass type,
                                                     jstring from_file_png_,
                                                     jstring from_file_mask_,
                                                     jstring from_file_mp4_,
                                                     jstring logo_file_, jstring dest_dir_) {
    g_bResultVideoFinished = false;
    const char *from_file_png = env->GetStringUTFChars(from_file_png_, 0);
    const char *from_file_mask = env->GetStringUTFChars(from_file_mask_, 0);
    const char *from_file_mp4 = env->GetStringUTFChars(from_file_mp4_, 0);
    const char *logo_file = env->GetStringUTFChars(logo_file_, 0);
    const char *dest_dir = env->GetStringUTFChars(dest_dir_, 0);

    if (!g_bFFMpegInited) {
        Decoder::InitFFMPEG();
        g_bFFMpegInited = true;
    }

    g_FinishVideoThread = new std::thread(make_result_video, string(from_file_png),
                                          string(from_file_mask),
                                          string(from_file_mp4),
                                          string(logo_file), string(dest_dir));

    env->ReleaseStringUTFChars(from_file_png_, from_file_png);
    env->ReleaseStringUTFChars(from_file_mask_, from_file_mask);
    env->ReleaseStringUTFChars(from_file_mp4_, from_file_mp4);
    env->ReleaseStringUTFChars(logo_file_, logo_file);
    env->ReleaseStringUTFChars(dest_dir_, dest_dir);

    return;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_smadec_maxx_cigr_JNIMainLib_StabilizerSyncStopStabilize(JNIEnv *env, jclass type) {
    if (g_StabilizerThread) {
        g_bCannotStabilize = true;
        g_bStopStabilizer = true;
        g_StabilizerThread->join();
        delete g_StabilizerThread;
        g_StabilizerThread = 0;
    }
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_smadec_maxx_cigr_JNIMainLib_CheckResultVideo(JNIEnv *env, jclass type) {

    if (g_bResultVideoFinished) {
        if (g_FinishVideoThread) {
            g_FinishVideoThread->join();
            delete g_FinishVideoThread;
            g_FinishVideoThread = 0;
        }
        return 0;
    }
    return 1;
}