package com.namit.scantastic;

import com.namit.scantastic.models.CapturedImage;

interface ImageAdapterListener {

    void imageClicked(CapturedImage image, int position);
}
