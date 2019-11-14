/*
 * Stabilizer.h
 *
 *  Created on: 7 нояб. 2017 г.
 *      Author: maxx
 */

#ifndef PARTIALSTABILIZER_H_
#define PARTIALSTABILIZER_H_

#include <opencv2/core/core.hpp>
#include <opencv2/videostab.hpp>
#include "Decoder.h"
#include "Encoder.h"

#define MOTION_MODEL MM_HOMOGRAPHY

class Encoder;

using namespace std;
using namespace cv;
using namespace cv::videostab;

class PartialStabilizer {
    void buildMotionEstimator();

    Ptr<KeypointBasedMotionEstimator> kbest;

    Ptr<MotionEstimatorRansacL2> est;
    std::vector<uchar> status_;
    std::vector<Point2f> pointsPrev_, points_;
    std::vector<Point2f> pointsPrevGood_, pointsGood_;

public:
    PartialStabilizer();

    std::vector<Point2f> GetKeyPointsFromImageAndSetHere(CMatFrame *firstFrame);

    float QuadOfDistanceBetweenPoints(Point2f &p, Point2f &q) {
        return (p.x - q.x) * (p.x - q.x) + (p.y - q.y) * (p.y - q.y);
    }

    void SetKeyPointsBegImage(std::vector<Point2f> keyPoints) {
        pointsPrev_ = keyPoints;
    }

    virtual ~PartialStabilizer();

    Mat GetMotionEstimate(CMatFrame *firstFrame, CMatFrame *nextFrame);

    Mat GetMotionEstimate2(CMatFrame *firstFrame, CMatFrame *nextFrame);

    bool
    FinishStabilizeFrame(CMatFrame *pFrame, CMatFrame *modifiedFrame, Mat &stab, Size frameSize_);

    static void Run(PartialStabilizer *obj, CGlobalBuffer *pGlobalBuffer);
};

#endif /* PartialStabilizer_H_ */
