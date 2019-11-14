/*
 * CMatFrame.cpp
 *
 *  Created on: 8 нояб. 2017 г.
 *      Author: maxx
 */

#include "CMatFrame.h"

CMatFrame::CMatFrame(int width, int height, AVPixelFormat dst_pix_fmt) : m_iFrameNum(0) {
	int ret = av_image_alloc(decframe, decframe_linesize, width, height, dst_pix_fmt, 1);
	if (ret < 0)
		throw std::runtime_error(MyFormatter() << "cannot alloc memory for decframe - " << ret);
	m_MatView = cv::Mat(height, width, CV_8UC3, decframe[0], decframe_linesize[0]);
}

CMatFrame::~CMatFrame() {
	if (!m_MatView.empty())
		av_freep(&decframe[0]);
}
