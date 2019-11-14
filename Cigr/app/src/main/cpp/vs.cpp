#include <opencv2/opencv.hpp>

#include "Decoder.h"
#include "Encoder.h"

#include "PartialStabilizer.h"

#define MOTION_MODEL MM_HOMOGRAPHY
//#define WHERE_FILE "/home/maxx/mobile/trunk/VID_20171024_153728.mp4"
//#define WHERE_FILE "/home/maxx/mobile/trunk/VID_20171218_044421.mp4"

#define WHERE_FILE "/home/maxx/mobile/trunk/cigra.mp4"
#define OUT_FILE "/home/maxx/mobile/trunk/VID.mp4"
#define FIRST_FRAME_FILE "/home/maxx/mobile/trunk/VID.png"
#define LOG_FILE "/home/maxx/mobile/trunk/VID.log"
#define FROM_VIDEO_FILE "/home/maxx/mobile/trunk/cigr_stab.mp4"
#define FROM_FRAME_FILE "/home/maxx/mobile/trunk/cigr_first_frame.png"
#define FROM_MASK_FILE "/home/maxx/mobile/trunk/cigr_first_frame_mask.png"
#define RESULT_VIDEO_FILE "/home/maxx/mobile/trunk/cigr_result.mp4"

using namespace std;
using namespace cv;
using namespace cv::videostab;

int g_CountFrames = 1;
int g_CurrentFrame = 0;
bool g_bFinished = true;
bool g_bCannotStabilize = false;

bool g_bResultVideoFinished = true;

void run2(std::string from, std::string to, std::string to_first_frame, std::string log) {
    g_bCannotStabilize = false;
    g_CurrentFrame = 0;

    Decoder decoder(from, false, 1);
    g_CountFrames = decoder.count();
    CGlobalBuffer glob_buffer(11, 15, decoder.getIWidth(), decoder.getIHeight(), AV_PIX_FMT_BGR24,
                              decoder.getBigWidth(), decoder.getBigHeight(), decoder.GetROI(),
                              decoder.GetFinishROI());
    decoder.nextFrame(glob_buffer.GetBaseFrame());
    glob_buffer.GetBaseFrame()->MakeGray();

    std::thread decoderThread(Decoder::Run, &decoder, &glob_buffer);
    remove(log.c_str());
    std::thread logBufferThread(CGlobalBuffer::func_log_sizes, &glob_buffer, log);

    PartialStabilizer stab;
    std::vector<Point2f> vecKeyPointsFirstFrame = stab.GetKeyPointsFromImageAndSetHere(
            glob_buffer.GetBaseFrame());
    std::thread stabThread(PartialStabilizer::Run, &stab, &glob_buffer);
    PartialStabilizer stab2;
    stab2.SetKeyPointsBegImage(vecKeyPointsFirstFrame);
    std::thread stabThread2(PartialStabilizer::Run, &stab2, &glob_buffer);
    PartialStabilizer stab3;
    stab3.SetKeyPointsBegImage(vecKeyPointsFirstFrame);
    std::thread stabThread3(PartialStabilizer::Run, &stab3, &glob_buffer);
    PartialStabilizer stab4;
    stab4.SetKeyPointsBegImage(vecKeyPointsFirstFrame);
    std::thread stabThread4(PartialStabilizer::Run, &stab4, &glob_buffer);
    PartialStabilizer stab5;
    stab5.SetKeyPointsBegImage(vecKeyPointsFirstFrame);
    std::thread stabThread5(PartialStabilizer::Run, &stab5, &glob_buffer);
    PartialStabilizer stab6;
    stab6.SetKeyPointsBegImage(vecKeyPointsFirstFrame);
    std::thread stabThread6(PartialStabilizer::Run, &stab6, &glob_buffer);
    PartialStabilizer stab7;
    stab7.SetKeyPointsBegImage(vecKeyPointsFirstFrame);
    std::thread stabThread7(PartialStabilizer::Run, &stab7, &glob_buffer);
    PartialStabilizer stab8;
    stab8.SetKeyPointsBegImage(vecKeyPointsFirstFrame);
    std::thread stabThread8(PartialStabilizer::Run, &stab8, &glob_buffer);

    Encoder encoder(to);
    encoder.Init(decoder.getFinishWidth(), decoder.getFinishHeight(), decoder.GetTimeBase(),
                 decoder.getRotate(), 3);
    Mat tmpFinishROIMap = glob_buffer.GetBaseFrame()->getMatROI()(decoder.GetFinishROI());
    encoder.write(tmpFinishROIMap, glob_buffer.GetBaseFrame()->getPts());
    std::thread encoderThread(Encoder::Run, &encoder, &glob_buffer);

    vector<int> compression_params;
    compression_params.push_back((int) IMWRITE_PNG_COMPRESSION);
    compression_params.push_back(0);
    try {
        Mat tmpRotatedMap;
        switch (decoder.getRotate()) {
            case 90:
                rotate(tmpFinishROIMap, tmpRotatedMap, ROTATE_90_CLOCKWISE);
                break;
            case 180:
                rotate(tmpFinishROIMap, tmpRotatedMap, ROTATE_180);
                break;
            case 270:
                rotate(tmpFinishROIMap, tmpRotatedMap, ROTATE_90_COUNTERCLOCKWISE);
                break;
            default:
                tmpRotatedMap = tmpFinishROIMap;
                break;
        }
        imwrite(to_first_frame, tmpRotatedMap, compression_params);
    }
    catch (cv::Exception &ex) {
        throw std::runtime_error(
                MyFormatter() << "Exception converting image to PNG format: " << ex.what());
    }


    decoderThread.join();
    stabThread.join();
    stabThread2.join();
    stabThread3.join();
    stabThread4.join();
    stabThread5.join();
    stabThread6.join();
    stabThread7.join();
    stabThread8.join();
    encoderThread.join();

    logBufferThread.join();
    g_bFinished = true;
    return;
}

void make_result_video(std::string from_png, std::string from_mask, std::string from_mp4,
                       std::string logo, std::string to) {
    try {
        Decoder decoder(from_mp4, true, 4);
        CMatFrame matFrame_video = CMatFrame(decoder.getBigWidth(), decoder.getBigHeight(),
                                             AV_PIX_FMT_BGR24);
        Mat matFrame_first_frame = imread(from_png);
        Mat matFrame_first_frame_mask = imread(from_mask, IMREAD_UNCHANGED);

        Mat matFrame_logo = imread(logo);
        Mat matFrame_logo2 = imread(logo, IMREAD_GRAYSCALE);
        Rect rectCopyLogo(590 - matFrame_logo.cols, matFrame_first_frame.rows > 600 ? 110 : 10,
                          matFrame_logo.cols, matFrame_logo.rows);
        matFrame_logo.copyTo(matFrame_first_frame(rectCopyLogo), matFrame_logo);
        try {
            matFrame_first_frame_mask(rectCopyLogo).setTo(Scalar(255, 255, 255, 255),
                                                          matFrame_logo2);
        } catch (cv::Exception e) {
            cout << e.what();
        }

        vector<Mat> chanels;
        split(matFrame_first_frame_mask, chanels);
        Mat mask_rotated_one_chanell;

        Mat matFrame_first_frame_rotated;
        switch (decoder.getRotate()) {
            case 90:
                rotate(matFrame_first_frame, matFrame_first_frame_rotated,
                       ROTATE_90_COUNTERCLOCKWISE);
                rotate(chanels[3], mask_rotated_one_chanell, ROTATE_90_COUNTERCLOCKWISE);
                break;
            case 180:
                rotate(matFrame_first_frame, matFrame_first_frame_rotated, ROTATE_180);
                rotate(chanels[3], mask_rotated_one_chanell, ROTATE_180);
                break;
            case 270:
                rotate(matFrame_first_frame, matFrame_first_frame_rotated, ROTATE_90_CLOCKWISE);
                rotate(chanels[3], mask_rotated_one_chanell, ROTATE_90_CLOCKWISE);
                break;
            default:
                matFrame_first_frame_rotated = matFrame_first_frame;
                mask_rotated_one_chanell = chanels[3];
                break;
        }
        mask_rotated_one_chanell /= 255;

        Encoder encoder(to);
        encoder.Init(decoder.getBigWidth(), decoder.getBigHeight(), decoder.GetTimeBase(),
                     decoder.getRotate(), 4);

        while (decoder.nextFrame(&matFrame_video)) {
            matFrame_first_frame_rotated.copyTo(matFrame_video.getMat(), mask_rotated_one_chanell);
            encoder.write(matFrame_video.getMatLink(), matFrame_video.getPts());
        }
        encoder.release();
    } catch (const exception &e) {
    }
    g_bResultVideoFinished = true;
}

double
convert_from_to(std::string from, std::string to, std::string to_first_frame, std::string log) {
    struct timeval tv_beg, tv_end;
    gettimeofday(&tv_beg, NULL);
    try {
        Decoder::InitFFMPEG();
        run2(from, to, to_first_frame, log);
    } catch (const exception &e) {
        return -1;
    }
    gettimeofday(&tv_end, NULL);
    return (tv_end.tv_sec - tv_beg.tv_sec) - double(tv_end.tv_usec - tv_beg.tv_usec) / 1000000;
}

int main() {
    //convert_from_to(WHERE_FILE, OUT_FILE, FIRST_FRAME_FILE, LOG_FILE);
    //make_result_video(FROM_FRAME_FILE, FROM_MASK_FILE, FROM_VIDEO_FILE, RESULT_VIDEO_FILE);
}
