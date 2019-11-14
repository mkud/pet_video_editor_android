//
// Created by maxx on 24.10.17.
//

#include "Decoder.h"

extern bool g_bCannotStabilize;

Decoder::Decoder(std::string szSource, bool bStaySize, int iThreadCount) :
		m_szSource(szSource), fmt_ctx(NULL), video_stream_index(-1), m_Rotate(0) {
	Init(iThreadCount, bStaySize);
}

bool Decoder::nextFrame(CMatFrame *matFrame) {
	/* read all packets */
	while (1) {	//first cycle - make packet
		if (SendPacket() || GetFrame(matFrame))
			return true;
		if (m_CurStage == FINISH_STAGE)
			return false;
	}
}

bool Decoder::SendPacket() {
	if (m_CurStage != SEND_PACKET_STAGE)
		return false;
	int ret;
	if (av_read_frame(fmt_ctx, &packet) < 0) {
		av_packet_unref(&packet);
		//here is end
		m_CurStage = FINISH_STAGE;
		return false;
	}

	if (packet.stream_index == video_stream_index) {

		ret = avcodec_send_packet(dec_ctx, &packet);
		if (ret < 0) {
			av_packet_unref(&packet);
			throw std::runtime_error(MyFormatter() << "Error while sending a packet to the decoder - " << ret);
			//here is and end error
		}
		m_CurStage = GET_FRAME_STAGE;
	}
	return false;
}

bool Decoder::GetFrame(CMatFrame *matFrame) {
	if (m_CurStage != GET_FRAME_STAGE)
		return false;
	int ret;
	ret = avcodec_receive_frame(dec_ctx, frame);
	if (ret == AVERROR(EAGAIN)) {
		av_frame_unref(frame);
		av_packet_unref(&packet);
		m_CurStage = SEND_PACKET_STAGE;
		return false;	//here is need another packet
	} else if (ret == AVERROR_EOF) {
		av_frame_unref(frame);
		av_packet_unref(&packet);
		m_CurStage = FINISH_STAGE;
		return false;	//here is finish
	} else if (ret < 0) {
		av_frame_unref(frame);
		av_packet_unref(&packet);
		throw std::runtime_error(MyFormatter() << "Error while receiving a frame from the decoder - " << ret);
		//here is and end error
	}
	sws_scale(swsctx, (const uint8_t * const *) frame->data, frame->linesize, 0, dec_ctx->height, matFrame->getFrame(),
			matFrame->getLineSize());
	matFrame->setPts(av_rescale_q(frame->pts, m_TimeBase2, m_TimeBase));
	av_frame_unref(frame);
	av_packet_unref(&packet);
	return true;
}

Decoder::~Decoder() {
	Close();
}

void Decoder::Close() {
	avcodec_free_context(&dec_ctx);
	avformat_close_input(&fmt_ctx);
	sws_freeContext(swsctx);
	av_frame_free(&frame);
}

void Decoder::Init(int iThreadCount, bool bStaySize) {

	frame = av_frame_alloc();
	if (!frame)
		throw std::runtime_error(MyFormatter() << "Cannot get mem for frame 1");

	int ret;
	AVCodec *dec;

	if ((ret = avformat_open_input(&fmt_ctx, m_szSource.c_str(), NULL, NULL)) < 0) {
		char d[255];
		av_strerror(ret, d, 255);
		throw std::runtime_error(MyFormatter() << "Cannot open input file - " << ret << " " << d);
	}

	if ((ret = avformat_find_stream_info(fmt_ctx, NULL)) < 0) {
		throw std::runtime_error(MyFormatter() << "Cannot find stream information - " << ret);
	}

	/* select the video stream */
	ret = av_find_best_stream(fmt_ctx, AVMEDIA_TYPE_VIDEO, -1, -1, &dec, 0);
	if (ret < 0) {
		throw std::runtime_error(MyFormatter() << "Cannot find a video stream in the input file - " << ret);
	}
	video_stream_index = ret;

	/* create decoding context */
	dec_ctx = avcodec_alloc_context3(dec);
	if (!dec_ctx)
		throw std::runtime_error(MyFormatter() << "ENOMEM");
	m_TimeBase = fmt_ctx->streams[video_stream_index]->codec->time_base;
	if (m_TimeBase.den > 65000)
		m_TimeBase = {1, 1000};

	m_TimeBase2 = fmt_ctx->streams[video_stream_index]->time_base;

	AVDictionaryEntry *tag = av_dict_get(fmt_ctx->streams[video_stream_index]->metadata, "rotate",
	NULL, 0);
	if (tag) {
		sscanf(tag->value, "%d", &m_Rotate);
	}

	dec_ctx->thread_count = iThreadCount;

	avcodec_parameters_to_context(dec_ctx, fmt_ctx->streams[video_stream_index]->codecpar);
	av_opt_set_int(dec_ctx, "refcounted_frames", 1, 0);

	/* init the video decoder */
	if ((ret = avcodec_open2(dec_ctx, dec, NULL)) < 0) {
		throw std::runtime_error(MyFormatter() << "Cannot open video decoder - " << ret);
	}

	if (bStaySize) {
		m_iWidth = dec_ctx->width;
		m_iHeight = dec_ctx->height;
		rectRoi = cv::Rect(0, 0, dec_ctx->width, dec_ctx->height);
		rectFinishROI = rectRoi;
	} else {
		// (m_Rotate % 180) ? need to turn: no
		m_iHeightRotated = (m_Rotate % 180) ? dec_ctx->width : dec_ctx->height;
		m_iWidthRotated = (m_Rotate % 180) ? dec_ctx->height : dec_ctx->width;

		if (m_iHeightRotated * 4 <= m_iWidthRotated * 5) {
			//only square
			m_iHeight = (m_Rotate % 180) ? (800 * dec_ctx->height) / dec_ctx->width : 800;
			m_iWidth = (m_Rotate % 180) ? 800 : (800 * dec_ctx->width) / dec_ctx->height;
			rectRoi = cv::Rect((m_Rotate % 180) ? 0 : (m_iWidth - 800) / 2,
					(m_Rotate % 180) ? (m_iHeight - 800) / 2 : 0, 800, 800);
			rectFinishROI = cv::Rect(100, 100, 600, 600);
		} else {
			//square + vertical
			m_iHeight = (m_Rotate % 180) ? 800 : (800 * dec_ctx->height) / dec_ctx->width;
			m_iWidth = (m_Rotate % 180) ? (800 * dec_ctx->width) / dec_ctx->height : 800;
			rectRoi = cv::Rect((m_iWidth - ((m_Rotate % 180) ? 1000 : 800)) / 2,
					(m_iHeight - ((m_Rotate % 180) ? 800 : 1000)) / 2, (m_Rotate % 180) ? 1000 : 800,
					(m_Rotate % 180) ? 800 : 1000);
			rectFinishROI = cv::Rect(100, 100, (m_Rotate % 180) ? 800 : 600, (m_Rotate % 180) ? 600 : 800);
		}
	}

	const AVPixelFormat dst_pix_fmt = AV_PIX_FMT_BGR24;
	swsctx = sws_getContext(dec_ctx->width, dec_ctx->height, dec_ctx->pix_fmt, m_iWidth, m_iHeight, dst_pix_fmt,
			SWS_BILINEAR, NULL, NULL, NULL);
	if (!swsctx) {
		throw std::runtime_error(MyFormatter() << "fail to sws_getCachedContext");
	}

	m_CurStage = SEND_PACKET_STAGE;
}

void Decoder::InitFFMPEG() {
	// initialize FFmpeg library
	av_register_all();
}

void Decoder::Run(Decoder * obj, CGlobalBuffer* globalBuffer) {
	for (int i = 0;; i++) {
		CMatFrame *pCurMat = 0;
		try {
			pCurMat = globalBuffer->S1GET_DecoderGetFrameForDecoding();
			if (!pCurMat || g_bCannotStabilize)
				return;	//if NULL flew to us, we exit
			if (!obj->nextFrame(pCurMat) || (i >= 150)) {
				globalBuffer->S1PUT_TOS2_DecoderPushFrameToNext(NULL);
				globalBuffer->S2PUT_TO_S1_StabilizerReturnFrameToFree(pCurMat);
				globalBuffer->S3_FINISH(i); //from here we will complete S3
				return; //If we did not read a single frame, send the end of the stream and exit.
			}
			pCurMat->MakeGray();
			pCurMat->setIFrameNum(i);
			globalBuffer->S1PUT_TOS2_DecoderPushFrameToNext(pCurMat);
		} catch (std::runtime_error &err) {
			globalBuffer->S2PUT_TO_S1_StabilizerReturnFrameToFree(pCurMat);
		} catch (...) {
			globalBuffer->S2PUT_TO_S1_StabilizerReturnFrameToFree(pCurMat);
		}

	}
	//globalBuffer->S1PUT_TOS2_DecoderPushFrameToNext(NULL);    //After enumerating all the planned frames, send the end of the stream.
}
