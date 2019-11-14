/*
 * CGlobalBuffer.cpp
 *
 *  Created on: 8 нояб. 2017 г.
 *      Author: maxx
 */

#include "CGlobalBuffer.h"
#include <chrono>
#include <thread>
#include <fstream>
#include <iostream>

CGlobalBuffer::CGlobalBuffer(unsigned int iSizeBuffer, unsigned int iSizeBuffer2, int width, int height,
		AVPixelFormat dst_pix_fmt, int width_big, int height_big, cv::Rect rectROI, cv::Rect rectFinishROI) {
	// TODO Auto-generated constructor stub
	m_queue.resize(iSizeBuffer);
	for (int i = 0; i < iSizeBuffer; i++) {
		m_queue[i] = new CMatFrame(width_big, height_big, dst_pix_fmt);
		m_queue[i]->SetROI(rectROI);
		m_S1TO_S2FROM_FreeBuffer.put_init(m_queue[i]);
	}

	m_queue2.resize(iSizeBuffer2);
	for (int i = 0; i < iSizeBuffer2; i++) {
		m_queue2[i] = new CMatFrame(width, height, dst_pix_fmt);
		m_queue2[i]->SetROI(rectFinishROI);
		m_S2TO_S3FROM_queue2FreeBuffer.put_init(m_queue2[i]);
	}

	m_pBaseFrame = new CMatFrame(width_big, height_big, dst_pix_fmt);
	m_pBaseFrame->SetROI(rectROI);
	m_sizeBaseFrame = m_pBaseFrame->getMatROI().size();
}

CMatFrame* CGlobalBuffer::S1GET_DecoderGetFrameForDecoding() {
	return m_S1TO_S2FROM_FreeBuffer.get();
}

void CGlobalBuffer::S1PUT_TOS2_DecoderPushFrameToNext(CMatFrame* frame) {
	m_S2TO_S1FROM_StabilizerBuffer.put(frame);
}

std::pair<CMatFrame*, CMatFrame*> CGlobalBuffer::S2GET_FROMS1_FROMS3_Stabilize() {
	return std::pair<CMatFrame *, CMatFrame *>(m_S2TO_S1FROM_StabilizerBuffer.get(),
			m_S2TO_S3FROM_queue2FreeBuffer.get());
}

void CGlobalBuffer::S2_FINISH_OTHER_S2_StabilizerStopAnotherStabilizers() {
	m_S2TO_S1FROM_StabilizerBuffer.send_finish();
	m_S2TO_S3FROM_queue2FreeBuffer.send_finish();
}

void CGlobalBuffer::S3_FULL_FINISH() {
	m_S3TO_S2FROM_queue2EncoderBuffer.send_finish();
}

void CGlobalBuffer::S3_FINISH(int iPos) {
	m_S3TO_S2FROM_queue2EncoderBuffer.put(iPos, NULL);
}

CMatFrame* CGlobalBuffer::S3GET_EncoderGetFrameForEncoding() {
	return m_S3TO_S2FROM_queue2EncoderBuffer.getElement();
}

void CGlobalBuffer::S3PUT_TO_S2_EncoderReturnUnusedFrame(CMatFrame* frame) {
	m_S2TO_S3FROM_queue2FreeBuffer.put(frame);
}

CGlobalBuffer::~CGlobalBuffer() {
	// TODO Auto-generated destructor stub
	for (unsigned int i = 0; i < m_queue.size(); i++)
		delete m_queue[i];

	for (unsigned int i = 0; i < m_queue2.size(); i++)
		delete m_queue2[i];

	delete m_pBaseFrame;
}

void CGlobalBuffer::S2PUT_TO_S3_StabilizerSendForEncoder(CMatFrame* frame) {
	m_S3TO_S2FROM_queue2EncoderBuffer.put(frame->getIFrameNum(), frame);
}

void CGlobalBuffer::S2PUT_TO_S1_StabilizerReturnFrameToFree(CMatFrame* frame) {
	m_S1TO_S2FROM_FreeBuffer.put(frame);
}

extern bool g_bCannotStabilize;

void CGlobalBuffer::func_log_sizes(CGlobalBuffer* pBuffer, std::string szNameFile) {
	std::this_thread::sleep_for(std::chrono::milliseconds(500));
	while (pBuffer->m_S1TO_S2FROM_FreeBuffer.UnsyncGetCount() != pBuffer->m_queue.size()) {
		if (g_bCannotStabilize) {
			pBuffer->S3_FULL_FINISH();
			return;
		}
		std::this_thread::sleep_for(std::chrono::milliseconds(500));
		std::ofstream log(szNameFile, std::ofstream::out | std::ofstream::app);
		log << "free buf - " << pBuffer->m_S1TO_S2FROM_FreeBuffer.UnsyncGetCount() << " stabbuf - "
				<< pBuffer->m_S2TO_S1FROM_StabilizerBuffer.UnsyncGetCount() << " encoderbuf - "
				<< pBuffer->m_S3TO_S2FROM_queue2EncoderBuffer.UnsyncGetCount() << " freebuff2 - "
				<< pBuffer->m_S2TO_S3FROM_queue2FreeBuffer.UnsyncGetCount() << std::endl;
		log.close();
	}

}
