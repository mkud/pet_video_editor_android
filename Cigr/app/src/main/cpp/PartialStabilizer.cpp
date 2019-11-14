/*
 * Stabilizer.cpp
 *
 *  Created on: 7 нояб. 2017 г.
 *      Author: maxx
 */

#include "PartialStabilizer.h"

extern bool g_bCannotStabilize;

PartialStabilizer::PartialStabilizer() {
    // TODO Auto-generated constructor stub
    buildMotionEstimator();

}

PartialStabilizer::~PartialStabilizer() {
    // TODO Auto-generated destructor stub
}

void PartialStabilizer::buildMotionEstimator() {
    est = makePtr<MotionEstimatorRansacL2>(MOTION_MODEL);

    RansacParams ransac = est->ransacParams();
    ransac.eps = .005;
    est->setRansacParams(ransac);

    est->setMinInlierRatio(0.001); //was 0.1. But less is better - more accurate

    Ptr<IOutlierRejector> outlierRejector = makePtr<NullOutlierRejector>();
    kbest = makePtr<KeypointBasedMotionEstimator>(est);
    //kbest->setDetector(ORB::create(500, 1.2, 8)); //was 1000. Quility quality has declined, but is faster for 3 seconds on the laptop
    kbest->setDetector(AgastFeatureDetector::create(
            5)); //was 1000. Quility quality has declined, but is faster for 3 seconds on the laptop
    kbest->setOutlierRejector(outlierRejector);
}

std::vector<Point2f> PartialStabilizer::GetKeyPointsFromImageAndSetHere(CMatFrame *firstFrame) {
    std::vector<KeyPoint> keypointsPrev_;
    kbest->detector()->detect(firstFrame->getMatGray(), keypointsPrev_);
    if (keypointsPrev_.empty())
        return pointsPrev_;

    // extract points from keypoints
    for (unsigned int j = 0, k = 0; j < keypointsPrev_.size(); j++) {
        for (k = 0; k < pointsPrev_.size(); k++)
            if (QuadOfDistanceBetweenPoints(pointsPrev_[k], keypointsPrev_[j].pt) < 2500)
                break;
        if (k == pointsPrev_.size())
            pointsPrev_.push_back(keypointsPrev_[j].pt);
    }
    return pointsPrev_;
}

Mat PartialStabilizer::GetMotionEstimate(CMatFrame *firstFrame, CMatFrame *nextFrame) {

    //Mat prevGray, nextGray;
    //cv::cvtColor(m_prevFrame, prevGray, cv::COLOR_BGR2GRAY);
    //cv::cvtColor(nextFrame, nextGray, cv::COLOR_BGR2GRAY);

    return kbest->estimate(firstFrame->getMatGray(), nextFrame->getMatGray());
}

Mat PartialStabilizer::GetMotionEstimate2(CMatFrame *firstFrame, CMatFrame *nextFrame) {

    // find correspondences
    kbest->opticalFlowEstimator()->run(firstFrame->getMatGray(), nextFrame->getMatGray(),
                                       pointsPrev_, points_, status_,
                                       noArray());

    // leave good correspondences only

    pointsPrevGood_.clear();
    pointsPrevGood_.reserve(points_.size());
    pointsGood_.clear();
    pointsGood_.reserve(points_.size());

    for (size_t i = 0; i < points_.size(); ++i) {
        if (status_[i]) {
            pointsPrevGood_.push_back(pointsPrev_[i]);
            pointsGood_.push_back(points_[i]);
        }
    }

    return est->estimate(pointsPrevGood_, pointsGood_/*, ok*/);

}

bool PartialStabilizer::FinishStabilizeFrame(CMatFrame *pFrame, CMatFrame *modifiedFrame, Mat &stab,
                                             Size frameSize_) {
    warpPerspective(pFrame->getMatROI(), modifiedFrame->getMat(), stab, frameSize_, INTER_LINEAR);
    return true;
}

//TODO: Is the last frame processed normally - otherwise it can be thrown out ....
void PartialStabilizer::Run(PartialStabilizer *obj, CGlobalBuffer *pGlobalBuffer) {
    pair<CMatFrame *, CMatFrame *> pairFrames;
    double dMax, dMin;
    while (1) {
        try {
            pairFrames = pGlobalBuffer->S2GET_FROMS1_FROMS3_Stabilize();
            if (!pairFrames.first || g_bCannotStabilize) {
                pGlobalBuffer->S2_FINISH_OTHER_S2_StabilizerStopAnotherStabilizers(); //tell the rest of the threads to stop
                if (g_bCannotStabilize)
                    pGlobalBuffer->S3_FULL_FINISH();
                return;    //so neighboring threads end
            }
            Mat stab = obj->GetMotionEstimate2(pairFrames.first, pGlobalBuffer->GetBaseFrame());
            obj->FinishStabilizeFrame(pairFrames.first, pairFrames.second, stab,
                                      pGlobalBuffer->GetSizeBaseFrame());
            pairFrames.second->setIFrameNum(pairFrames.first->getIFrameNum());
            pairFrames.second->setPts(pairFrames.first->getPts());

            cv::minMaxLoc(stab, &dMin, &dMax);
            pairFrames.second->SetDiff(dMin - dMax);

            pGlobalBuffer->S2PUT_TO_S1_StabilizerReturnFrameToFree(pairFrames.first);
            pGlobalBuffer->S2PUT_TO_S3_StabilizerSendForEncoder(pairFrames.second);
        } catch (std::runtime_error &err) {
            pGlobalBuffer->S2PUT_TO_S1_StabilizerReturnFrameToFree(pairFrames.first);
            pGlobalBuffer->S3PUT_TO_S2_EncoderReturnUnusedFrame(pairFrames.second);
        } catch (...) {
            pGlobalBuffer->S2PUT_TO_S1_StabilizerReturnFrameToFree(pairFrames.first);
            pGlobalBuffer->S3PUT_TO_S2_EncoderReturnUnusedFrame(pairFrames.second);
        }

    }
}
