//
// Created by maxx on 11.10.17.
//

#ifndef CINEMA2_ENCODER_H
#define CINEMA2_ENCODER_H

#include <iostream>

extern "C" {
#include <libavformat/avformat.h>
#include <libavformat/avio.h>
#include  <libavcodec/avcodec.h>
#include  <libavformat/avformat.h>
#include <libavutil/imgutils.h> //for av_image_alloc only
#include <libavutil/opt.h>
#include <libswscale/swscale.h>
}

#include <opencv2/core/core.hpp>
#include "CGlobalBuffer.h"
#include "PartialStabilizer.h"

//#define OUTPUT_CODEC AV_CODEC_ID_H264
#define OUTPUT_CODEC AV_CODEC_ID_MPEG4
//Input pix fmt is set to BGR24
#define OUTPUT_PIX_FMT AV_PIX_FMT_YUV420P

class PartialStabilizer;

class Encoder {
private:
	AVFormatContext *fmt_ctx;
	AVCodecContext * avctx;
	AVStream *st;

	AVFrame *sndFrame;

	SwsContext * ctx;
	AVPacket *pkt;

	int SendFrame(AVFrame *in_frame, int pts);

	std::string m_szTarget;

	size_t _m_width_mul_height, _m_width_mul_height_div_4;
public:
	Encoder(std::string szTarget) :
			fmt_ctx(NULL), m_szTarget(szTarget) {
	}

	void Init(int width, int height, AVRational TimeBase, int iRotate, int iCountThread);

	~Encoder() {
	}

	int release();

	void write(cv::Mat &tmp_mat, int iPtsMat);

	void write2(uint8_t *in_frame, int pts);

	static void Run(Encoder *obj, CGlobalBuffer* globalBuffer);
};

#endif //CINEMA2_ENCODER_H
