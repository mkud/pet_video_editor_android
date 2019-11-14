/*
 * CGlobalBuffer.h
 *
 *  Created on: 8 нояб. 2017 г.
 *      Author: maxx
 */

#ifndef CGLOBALBUFFER_H_
#define CGLOBALBUFFER_H_

#include <pthread.h>
#include "CMatFrame.h"
#include "internal_concurent.h"

class CGlobalBuffer {
    CMatFrame *m_pBaseFrame;

    std::vector<CMatFrame *> m_queue;
    concurent_list<CMatFrame *> m_S1TO_S2FROM_FreeBuffer;
    concurent_list<CMatFrame *> m_S2TO_S1FROM_StabilizerBuffer;

    std::vector<CMatFrame *> m_queue2;
    concurent_list<CMatFrame *> m_S2TO_S3FROM_queue2FreeBuffer;
    concurent_map2<int, CMatFrame *> m_S3TO_S2FROM_queue2EncoderBuffer;

    cv::Size m_sizeBaseFrame;

public:
    CGlobalBuffer(unsigned int iSizeBuffer, unsigned int iSizeBuffer2, int width, int height,
                  AVPixelFormat dst_pix_fmt, int width_big,
                  int height_big, cv::Rect rectROI, cv::Rect rectFinishROI);

    CMatFrame *GetBaseFrame() {
        return m_pBaseFrame;
    }

    cv::Size GetSizeBaseFrame() {
        return m_sizeBaseFrame;
    }

    CMatFrame *S1GET_DecoderGetFrameForDecoding();

    void S1PUT_TOS2_DecoderPushFrameToNext(CMatFrame *frame);

    std::pair<CMatFrame *, CMatFrame *> S2GET_FROMS1_FROMS3_Stabilize();

    void S2PUT_TO_S3_StabilizerSendForEncoder(CMatFrame *frame);

    void S2_FINISH_OTHER_S2_StabilizerStopAnotherStabilizers();

    void S2PUT_TO_S1_StabilizerReturnFrameToFree(CMatFrame *frame);

    void S3_FINISH(int iPos);

    void S3_FULL_FINISH();

    CMatFrame *S3GET_EncoderGetFrameForEncoding();

    void S3PUT_TO_S2_EncoderReturnUnusedFrame(CMatFrame *frame);

    virtual ~CGlobalBuffer();

    static void func_log_sizes(CGlobalBuffer *pBuffer, std::string szNameFile);
};

#endif /* CGLOBALBUFFER_H_ */
