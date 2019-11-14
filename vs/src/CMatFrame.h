/*
 * CMatFrame.h
 *
 *  Created on: 8 нояб. 2017 г.
 *      Author: maxx
 */

#ifndef CMATFRAME_H_
#define CMATFRAME_H_

#include <opencv2/core/core.hpp>
#include <opencv2/imgproc.hpp>

extern "C" {
#include <libavformat/avformat.h>
#include <libavutil/imgutils.h> //for av_image_alloc only
}

#include "internal_exception.h"

class CMatFrame {
	uint8_t *decframe[4];
	int decframe_linesize[4];
	cv::Mat m_MatView;
	int m_iFrameNum;

	cv::Mat m_MatGray;
	int m_iPts;
	double m_dDiff;

	cv::Mat m_ROIMatView;
	cv::Mat m_ROIMatViewGray;
	cv::Rect m_RectROI;
public:
	CMatFrame(int width, int height, AVPixelFormat dst_pix_fmt);

	cv::Rect GetRectROI() {
		return m_RectROI;
	}

	void SetROI(cv::Rect rectROI) {
		m_RectROI = rectROI;
		m_ROIMatView = m_MatView(rectROI);
	}

	virtual ~CMatFrame();

	uint8_t * const * getFrame() {
		return decframe;
	}

	const int * getLineSize() {
		return decframe_linesize;
	}

	cv::Mat getMat() {
		return m_MatView;
	}

	cv::Mat &getMatLink() {
		return m_MatView;
	}

	cv::Mat* getMatPointer() {
		return &m_MatView;
	}

	void MakeGray() {
		cv::cvtColor(m_MatView, m_MatGray, cv::COLOR_BGR2GRAY);
		m_ROIMatViewGray = m_MatGray(m_RectROI);
	}

	cv::Mat getMatGray() {
		return m_ROIMatViewGray;
	}

	cv::Mat &getMatROI() {
		return m_ROIMatView;
	}

	int getIFrameNum() const {
		return m_iFrameNum;
	}

	void setIFrameNum(int iFrameNum) {
		m_iFrameNum = iFrameNum;
	}

	int getPts() const {
		return m_iPts;
	}

	void setPts(int iPts) {
		m_iPts = iPts;
	}

	const uint8_t * const *GetDataForSwsScale() {
		return (const uint8_t * const *) &m_ROIMatView.data;
	}

	const uint8_t * const *GetDataForSwsScaleFull() {
		return (const uint8_t * const *) &m_MatView.data;
	}

	double GetDiff() {
		return m_dDiff;
	}

	void SetDiff(double dDiff) {
		m_dDiff = dDiff;
	}

};

#endif /* CMATFRAME_H_ */
