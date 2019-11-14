//
// Created by maxx on 24.10.17.
//

#ifndef MOVIETO_VIDEOSOURCE_APP_H
#define MOVIETO_VIDEOSOURCE_APP_H

#include <iostream>
#include "internal_exception.h"

// FFmpeg
extern "C" {
#include <libavformat/avformat.h>
#include <libavcodec/avcodec.h>
#include <libavutil/avutil.h>
#include <libavutil/pixdesc.h>
#include <libswscale/swscale.h>
#include <libavutil/opt.h>
#include <libavutil/imgutils.h>
}
// OpenCV
#include <opencv2/core.hpp>
#include "CMatFrame.h"
#include "CGlobalBuffer.h"

class Decoder {
	std::string m_szSource;
	AVFormatContext *fmt_ctx;
	int video_stream_index;
	SwsContext* swsctx;
	AVCodecContext *dec_ctx;
	AVFrame *frame;

	int m_iWidth, m_iHeight;
	int m_iWidthRotated, m_iHeightRotated;
	cv::Rect rectRoi, rectFinishROI;

	AVPacket packet;
	enum stage {
		SEND_PACKET_STAGE, GET_FRAME_STAGE, FINISH_STAGE
	} m_CurStage;

    void Init(int iThreadCount, bool bStaySize);

	void Close();

	bool GetFrame(CMatFrame *matFrame);

	bool SendPacket();

	AVRational m_TimeBase, m_TimeBase2;

	int m_Rotate;

public:
	Decoder(std::string szSource, bool bStaySize, int iThreadCount);

	~Decoder();

	bool nextFrame(CMatFrame *matFrame);

	static void InitFFMPEG();

	double fps() {
		return av_q2d(fmt_ctx->streams[video_stream_index]->r_frame_rate);
	}

	int count() {
		return fmt_ctx->streams[video_stream_index]->nb_frames;
	}

	int getRotate() {
		return m_Rotate;
	}

	int getIHeight() const {
		return rectRoi.height;
	}

	int getIWidth() const {
		return rectRoi.width;
	}

	int getFinishHeight() const {
		return rectFinishROI.height;
	}

	int getFinishWidth() const {
		return rectFinishROI.width;
	}

	int getBigWidth() {
		return m_iWidth;
	}

	int getBigHeight() {
		return m_iHeight;
	}

	cv::Rect GetROI() {
		return rectRoi;
	}

	cv::Rect GetFinishROI() {
		return rectFinishROI;
	}

	AVRational GetTimeBase() {
		return m_TimeBase;
	}

	static void Run(Decoder * obj, CGlobalBuffer* globalBuffer);
};

#endif //MOVIETO_VIDEOSOURCE_APP_H
