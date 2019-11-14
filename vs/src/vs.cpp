#include <opencv2/videostab.hpp>
#include <opencv2/imgcodecs.hpp>

#include "Decoder.h"
#include "Encoder.h"

#include "PartialStabilizer.h"

#define MOTION_MODEL MM_HOMOGRAPHY
//#define WHERE_FILE "/home/maxx/mobile/trunk/VID_20171024_153728.mp4"
//#define WHERE_FILE "/home/maxx/mobile/trunk/VID_20171218_044421.mp4"

#define WHERE_FILE "/home/maxx/mobile/trunk/cigra(4).mp4"
#define OUT_FILE "/home/maxx/mobile/trunk/VID.mp4"
#define FIRST_FRAME_FILE "/home/maxx/mobile/trunk/VID.png"
#define LOG_FILE "/home/maxx/mobile/trunk/VID.log"
#define FROM_VIDEO_FILE "/home/maxx/mobile/trunk/cigra_stab.mp4"
#define FROM_FRAME_FILE "/home/maxx/mobile/trunk/cigra_first_frame.png"
#define FROM_MASK_FILE "/home/maxx/mobile/trunk/cigra_first_frame_mask.png"
#define RESULT_VIDEO_FILE "/home/maxx/mobile/trunk/cigra_result.mp4"

using namespace std;
using namespace cv;
using namespace cv::videostab;

int g_CountFrames = 1;
int g_CurrentFrame = 0;
bool g_bFinished = true;
bool g_bCannotStabilize = false;

void run2(std::string from, std::string to, std::string to_first_frame, std::string log) {
	g_bCannotStabilize = false;
	g_CurrentFrame = 0;

	Decoder decoder(from, false, 1);
	g_CountFrames = decoder.count();
	CGlobalBuffer glob_buffer(11, 15, decoder.getIWidth(), decoder.getIHeight(), AV_PIX_FMT_BGR24,
			decoder.getBigWidth(), decoder.getBigHeight(), decoder.GetROI(), decoder.GetFinishROI());
	decoder.nextFrame(glob_buffer.GetBaseFrame());
	glob_buffer.GetBaseFrame()->MakeGray();

	vector<int> compression_params;
	compression_params.push_back((int) cv::IMWRITE_PNG_COMPRESSION);
	compression_params.push_back(0);
	PartialStabilizer stab;
	std::vector<Point2f> vecKeyPointsFirstFrame = stab.GetKeyPointsFromImageAndSetHere(glob_buffer.GetBaseFrame());
	try {
		Mat tmpRotatedMap;
		Mat tmpFinishROIMap = glob_buffer.GetBaseFrame()->getMatROI()(decoder.GetFinishROI());
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
		//for (int i = 0; i < vecKeyPointsFirstFrame.size(); i++)
		//	cv::circle(glob_buffer.GetBaseFrame()->getMatROI(), vecKeyPointsFirstFrame[i], 5, Scalar(0,0,0), 2);
		//cv::imwrite(to_first_frame, glob_buffer.GetBaseFrame()->getMatROI(), compression_params);
		cv::imwrite(to_first_frame, tmpRotatedMap, compression_params);
	} catch (cv::Exception &ex) {
		throw std::runtime_error(MyFormatter() << "Exception converting image to PNG format: " << ex.what());
	}

	std::thread decoderThread(Decoder::Run, &decoder, &glob_buffer);

	std::thread logBufferThread(CGlobalBuffer::func_log_sizes, &glob_buffer, log);

	//PartialStabilizer stab;
	//std::vector<Point2f> vecKeyPointsFirstFrame = stab.GetKeyPointsFromImageAndSetHere(glob_buffer.GetBaseFrame());
	std::thread stabThread(PartialStabilizer::Run, &stab, &glob_buffer);
	/*PartialStabilizer stab2;
	 std::thread stabThread2(PartialStabilizer::Run, &stab2, &glob_buffer);
	 PartialStabilizer stab3;
	 std::thread stabThread3(PartialStabilizer::Run, &stab3, &glob_buffer);
	 PartialStabilizer stab4;
	 std::thread stabThread4(PartialStabilizer::Run, &stab4, &glob_buffer);
	 PartialStabilizer stab5;
	 std::thread stabThread5(PartialStabilizer::Run, &stab5, &glob_buffer);
	 PartialStabilizer stab6;
	 std::thread stabThread6(PartialStabilizer::Run, &stab6, &glob_buffer);
	 PartialStabilizer stab7;
	 std::thread stabThread7(PartialStabilizer::Run, &stab7, &glob_buffer);
	 PartialStabilizer stab8;
	 std::thread stabThread8(PartialStabilizer::Run, &stab8, &glob_buffer);
	 */
	Encoder encoder(to);
	encoder.Init(decoder.getFinishWidth(), decoder.getFinishHeight(), decoder.GetTimeBase(), decoder.getRotate(), 3);
	std::thread encoderThread(Encoder::Run, &encoder, &glob_buffer);
	decoderThread.join();
	stabThread.join();
	/*stabThread2.join();
	 stabThread3.join();
	 stabThread4.join();
	 stabThread5.join();
	 stabThread6.join();
	 stabThread7.join();
	 stabThread8.join();*/
	encoderThread.join();

	logBufferThread.join();
	g_bFinished = true;
	return;
}

#include <opencv2/highgui/highgui.hpp>
double make_result_video(std::string from_png, std::string from_mask, std::string from_mp4, std::string to) {
	struct timeval tv_beg, tv_end;
	gettimeofday(&tv_beg, NULL);
	try {
		Decoder::InitFFMPEG();
		Decoder decoder(from_mp4, true, 4);
		CMatFrame matFrame_video = CMatFrame(decoder.getBigWidth(), decoder.getBigHeight(), AV_PIX_FMT_BGR24);
		Mat matFrame_first_frame = imread(from_png);
		Mat matFrame_first_frame_mask = imread(from_mask, IMREAD_UNCHANGED);
		vector<Mat> chanels;
		split(matFrame_first_frame_mask, chanels);
		Mat mask_rotated_one_chanell;

		Mat matFrame_first_frame_rotated;
		switch (decoder.getRotate()) {
		case 90:
			rotate(matFrame_first_frame, matFrame_first_frame_rotated, ROTATE_90_COUNTERCLOCKWISE);
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
		namedWindow("123", WINDOW_AUTOSIZE);
		mask_rotated_one_chanell /= 255;
		//mask_rotated_one_chanell *= 255;
		//cv::imshow("123", mask_rotated_one_chanell);
		//cv::waitKey();

		Mat mask_rotated_inverted_one_chanel;
		bitwise_not(mask_rotated_one_chanell, mask_rotated_inverted_one_chanel);
		//mask_rotated_one_chanell /= 2;
		//mask_rotated_inverted_one_chanel /= 2;
		Mat mask_rotated_inverted_, mask_rotated_;
		vector<Mat> chanels2;
		chanels2.push_back(mask_rotated_inverted_one_chanel);
		chanels2.push_back(mask_rotated_inverted_one_chanel);
		chanels2.push_back(mask_rotated_inverted_one_chanel);
		merge(chanels2, mask_rotated_inverted_);

		chanels2.clear();
		chanels2.push_back(mask_rotated_one_chanell);
		chanels2.push_back(mask_rotated_one_chanell);
		chanels2.push_back(mask_rotated_one_chanell);
		merge(chanels2, mask_rotated_);

		//Mat matFrame_first_frame_;
		//bitwise_and(matFrame_first_frame_rotated, mask_rotated_, matFrame_first_frame_rotated);
		//matFrame_first_frame_rotated -= mask_rotated_inverted_;
		//cv::imshow("123", matFrame_first_frame_rotated);
		//cv::waitKey();

		/*Mat mask2;
		 vector<Mat> chanels2;
		 chanels2.push_back(mask);
		 chanels2.push_back(mask);
		 chanels2.push_back(mask);
		 merge(chanels2, mask2);*/
		Encoder encoder(to);
		encoder.Init(decoder.getBigWidth(), decoder.getBigHeight(), decoder.GetTimeBase(), decoder.getRotate(), 4);

		Mat mask3;
		while (decoder.nextFrame(&matFrame_video)) {
			//bitwise_and(matFrame_video.getMat(), mask_rotated_inverted_, matFrame_video.getMat());
			//bitwise_or(matFrame_video.getMat(), matFrame_first_frame_rotated, matFrame_video.getMat());
			//matFrame_video.getMat() -= mask_rotated_;
			//matFrame_video.getMat() += matFrame_first_frame_rotated;
			matFrame_first_frame_rotated.copyTo(matFrame_video.getMat(), mask_rotated_one_chanell);
			//cv::imshow("123", matFrame_video.getMat());
			//cv::waitKey();
			encoder.write(matFrame_video.getMatLink(), matFrame_video.getPts());
		}
		encoder.release();
	} catch (const exception &e) {
		return -1;
	}
	gettimeofday(&tv_end, NULL);
	return (tv_end.tv_sec - tv_beg.tv_sec) - double(tv_end.tv_usec - tv_beg.tv_usec) / 1000000;
}

double convert_from_to(std::string from, std::string to, std::string to_first_frame, std::string log) {
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
	cout << convert_from_to(WHERE_FILE, OUT_FILE, FIRST_FRAME_FILE, LOG_FILE) << endl;
	//make_result_video(FROM_FRAME_FILE, FROM_MASK_FILE, FROM_VIDEO_FILE, RESULT_VIDEO_FILE);
}
