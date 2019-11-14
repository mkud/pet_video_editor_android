//
// Created by maxx on 11.10.17.
//

#include "Encoder.h"

extern int g_CountFrames;
extern int g_CurrentFrame;
extern bool g_bCannotStabilize;

void Encoder::Init(int width, int height, AVRational TimeBase, int iRotation, int iCountThread) {
    int err;
    AVCodec *codec;
    AVDictionary *fmt_opts = NULL;
    avformat_alloc_output_context2(&fmt_ctx, NULL, NULL, m_szTarget.c_str());
    if (fmt_ctx == NULL) {
        throw std::runtime_error(MyFormatter() << "can not alloc av context");
    }

    err = av_dict_set(&fmt_opts, "movflags", "faststart", 0);
    if (err < 0) {
        //LOGW("Error : av_dict_set movflags %d", err);
    }
    //default brand is "isom", which fails on some devices
    av_dict_set(&fmt_opts, "brand", "mp42", 0);
    if (err < 0) {
        //LOGW("Error : av_dict_set brand %d", err);
    }
    codec = avcodec_find_encoder(OUTPUT_CODEC);
    //codec = avcodec_find_encoder(fmt->video_codec);
    if (!codec) {
        throw std::runtime_error(MyFormatter() << "can't find encoder");
    }
    if (!(st = avformat_new_stream(fmt_ctx, codec))) {
        throw std::runtime_error(MyFormatter() << "can't create new stream");
    }
    //set stream time_base
    /* frames per second FIXME use input fps? */
    st->time_base = TimeBase;

    avctx = avcodec_alloc_context3(codec);
    if (!avctx) {
        throw std::runtime_error(MyFormatter() << "Could not allocate an encoding context");
    }

    //Set codec_ctx to stream's codec structure
    /* put sample parameters */
    //For audio
    //avctx->sample_fmt = (AVSampleFormat) (codec->sample_fmts ? codec->sample_fmts[0] : AV_SAMPLE_FMT_S16);
    avctx->width = width;
    avctx->height = height;
    avctx->time_base = st->time_base;
    avctx->pix_fmt = OUTPUT_PIX_FMT;
    avctx->bit_rate = 20000000;
    //H.264 specific options
    //avctx->gop_size = 5;
    //avctx->level = 31;
    //avctx->profile = FF_PROFILE_H264_BASELINE;
    avctx->thread_count = iCountThread;
    //avctx->thread_type = FF_THREAD_SLICE;
    /* Apparently it's in the example in master but does not work in V11
     if (o_format_ctx->oformat->flags & AVFMT_GLOBALHEADER)
     codec_ctx->flags |= AV_CODEC_FLAG_GLOBAL_HEADER;
     */
    /*    err = av_opt_set(codec_ctx->priv_data, "crf", "12", 0);
     if (err < 0) {
     LOGW("Error : av_opt_set crf %d", err);
     }
     err = av_opt_set(codec_ctx->priv_data, "profile", "main", 0);
     if (err < 0) {
     LOGW("Error : av_opt_set profile %d", err);
     }
     err = av_opt_set(codec_ctx->priv_data, "preset", "slow", 0);
     if (err < 0) {
     LOGW("Error : av_opt_set preset %d", err);
     }
     // disable b-pyramid. CLI options for this is "-b-pyramid 0"
     //Because Quicktime (ie. iOS) doesn't support this option
     err = av_opt_set(codec_ctx->priv_data, "b-pyramid", "0", 0);
     if (err < 0) {
     LOGW("Error : av_opt_set b-pyramid %d", err);
     }*/

    /* Third parameter can be used to pass settings to encoder */
    err = avcodec_open2(avctx, codec, NULL);
    if (err < 0) {
        throw std::runtime_error(
                MyFormatter() << "avcodec_open2 Cannot open video encoder for stream - " << err);
    }

    //It's necessary to open stream codec to link it to "codec" (the encoder).
    err = avcodec_parameters_from_context(st->codecpar, avctx);
    if (err < 0) {
        throw std::runtime_error(MyFormatter() << "avcodec_parameters_to_context");
    }

    switch (iRotation) {
        case 90:
            av_dict_set(&(st->metadata), "rotate", "90", 0);
            break;
        case 180:
            av_dict_set(&(st->metadata), "rotate", "180", 0);
            break;
        case 270:
            av_dict_set(&(st->metadata), "rotate", "270", 0);
            break;
        default:
            break;
    }

    fmt_ctx->oformat->audio_codec = AV_CODEC_ID_NONE;

    //* dump av format informations
    av_dump_format(fmt_ctx, 0, m_szTarget.c_str(), 1);
    //*/
    err = avio_open(&fmt_ctx->pb, m_szTarget.c_str(), AVIO_FLAG_WRITE);
    if (err < 0) {
        throw std::runtime_error(MyFormatter() << "avio_open");
    }
    //Write file header if necessary
    err = avformat_write_header(fmt_ctx, &fmt_opts);
    if (err < 0) {
        throw std::runtime_error(MyFormatter() << "avformat_write_header");
    }

///////////////////////////////

    sndFrame = av_frame_alloc();
    sndFrame->width = avctx->width;
    sndFrame->height = avctx->height;
    sndFrame->format = AV_PIX_FMT_YUV420P;
    /* Alloc tmp frame once and for all*/
    err = av_frame_get_buffer(sndFrame, 1);

    if (err < 0) {
        throw std::runtime_error(MyFormatter() << "can't alloc output frame buffer - " << err);
    }
    ctx = sws_getContext(avctx->width, avctx->height, AV_PIX_FMT_BGR24, avctx->width, avctx->height,
                         AV_PIX_FMT_YUV420P,
                         SWS_BILINEAR, 0, 0, 0);
    if (!ctx) {
        throw std::runtime_error(MyFormatter() << "can't alloc sws_getContext");
    }

    _m_width_mul_height = (unsigned int) avctx->width * avctx->height;
    _m_width_mul_height_div_4 = _m_width_mul_height / 4;

    pkt = av_packet_alloc();
}

int Encoder::release() {
    SendFrame(NULL, 0);
    //Write file trailer before exit
    av_write_trailer(fmt_ctx);
    //close file
    avio_close(fmt_ctx->pb);
    try {
        avcodec_free_context(&avctx);
    }
    catch (...) {
        return 0;
    }
    avformat_free_context(fmt_ctx);

    sws_freeContext(ctx);

    av_frame_free(&sndFrame);
    av_packet_free(&pkt);
    return 0;
}

void Encoder::write(Mat &tmp_mat, int iPtsMat) {
    //create a AVPicture frame from the opencv Mat input image
    const int stride[] = {static_cast<int>(tmp_mat.step[0])};
    int err = sws_scale(ctx, (const uint8_t *const *) &tmp_mat.data, stride, 0, tmp_mat.rows,
                        sndFrame->data,
                        sndFrame->linesize);

    if (err < 0)
        return;

    SendFrame(sndFrame, iPtsMat);
    return;
}

void Encoder::write2(uint8_t *tmp_mat, int pts) {
    //create a AVPicture frame from the opencv Mat input image
    /*av_image_fill_arrays(sndFrame->data, sndFrame->linesize, tmp_mat,
                         (AVPixelFormat) sndFrame->format, sndFrame->width, sndFrame->height, 1);*/
    /*sndFrame->linesize[0] = avctx->width;
    sndFrame->linesize[1] = avctx->width / 2;
    sndFrame->linesize[2] = avctx->width / 2;*/
    memcpy(sndFrame->data[0], tmp_mat, _m_width_mul_height);
    memcpy(sndFrame->data[1], tmp_mat + _m_width_mul_height + _m_width_mul_height_div_4,
           _m_width_mul_height_div_4);
    memcpy(sndFrame->data[2], tmp_mat + _m_width_mul_height,
           _m_width_mul_height_div_4);
    //memcpy(sndFrame->data[0], tmp_mat, len);
    SendFrame(sndFrame, pts);
    return;
}

int Encoder::SendFrame(AVFrame *in_frame, int pts) {
    int err;
    //convert the frame to the color space and pixel format specified in the sws context
    if (in_frame)
        in_frame->pts = pts;
    err = avcodec_send_frame(avctx, in_frame);
    if (err < 0) {
        return err;
    }
    while (1) {
        err = avcodec_receive_packet(avctx, pkt);

        if (err == AVERROR(EAGAIN)) {
            return 1;
        }
        if (err == AVERROR_EOF) {
            return 1;
        }
        av_packet_rescale_ts(pkt, avctx->time_base, st->time_base);
        pkt->stream_index = st->index;

        err = av_write_frame(fmt_ctx, pkt);
        if (err < 0) {
            throw std::runtime_error(MyFormatter() << "write frame  - " << err);
        }
    }

}

void Encoder::Run(Encoder *obj, CGlobalBuffer *globalBuffer) {
    CMatFrame *pCurMat = 0;
    int wrong_cntr = 0;
    CMatFrame *pPrevMat = globalBuffer->S3GET_EncoderGetFrameForEncoding();    //Get first frame.
    if (g_bCannotStabilize || !pPrevMat) {
        //quit if nothing
        obj->release();
        return;
    }
    Mat MatBaseFrame = globalBuffer->GetBaseFrame()->getMatROI()(pPrevMat->GetRectROI());
    obj->write(MatBaseFrame, globalBuffer->GetBaseFrame()->getPts());

    obj->write(pPrevMat->getMatROI(), pPrevMat->getPts());       //let's draw
    for (int i = 3;; i++) {
        try {
            pCurMat = globalBuffer->S3GET_EncoderGetFrameForEncoding();
            if (g_bCannotStabilize) {
                obj->release();
                return;
            }
            if (!pCurMat) {    //if got stream finish then finish
                globalBuffer->S3PUT_TO_S2_EncoderReturnUnusedFrame(pPrevMat);
                obj->release();
                return;
            }
            g_CurrentFrame = pCurMat->getIFrameNum(); // put progress

            //if big difference between 2 frames 
            if (abs(pPrevMat->GetDiff() - pCurMat->GetDiff()) > 45) {
                //and if difference 3 frames, so don't work...
                if (wrong_cntr > 2) {
                    g_bCannotStabilize = true;
                    obj->release();
                    return;
                }

                wrong_cntr++; //Calculating this different frame
                pPrevMat->setPts(pCurMat->getPts());    //Mask old frame to new
                pPrevMat->setIFrameNum(pCurMat->getIFrameNum()); //Strong masking)))

                //write that (Yes-Yes this is terrible, we write old frame 2 times and throw wrong frame)
                if ((g_CountFrames - i) < 4) {
                    Mat xxx = (pPrevMat->getMatROI() * (float(g_CountFrames - i) / 4))
                              + (MatBaseFrame * (float(1) - (float(g_CountFrames - i) / 4)));
                    obj->write(xxx, pPrevMat->getPts());
                } else
                    obj->write(pPrevMat->getMatROI(), pPrevMat->getPts());

                //and this frame became new.
                //So new need to return
                globalBuffer->S3PUT_TO_S2_EncoderReturnUnusedFrame(pCurMat);
            } else {
                wrong_cntr = 0; //counter of wrong frames
                if ((g_CountFrames - i) < 4) {
                    Mat xxx = (pCurMat->getMatROI() * (float(g_CountFrames - i) / 4))
                              + (MatBaseFrame * (float(1) - (float(g_CountFrames - i) / 4)));
                    obj->write(xxx, pCurMat->getPts());
                } else
                    obj->write(pCurMat->getMatROI(), pCurMat->getPts());    //write new frame

                //return old frame to the buffer
                globalBuffer->S3PUT_TO_S2_EncoderReturnUnusedFrame(pPrevMat);

                pPrevMat = pCurMat; //Storing new frame as old
            }
        } catch (std::runtime_error &err) {
            globalBuffer->S3PUT_TO_S2_EncoderReturnUnusedFrame(pCurMat);
        } catch (...) {
            globalBuffer->S3PUT_TO_S2_EncoderReturnUnusedFrame(pCurMat);
        }
    }
    // obj->release();    //If got all planned frames - quit.
}
