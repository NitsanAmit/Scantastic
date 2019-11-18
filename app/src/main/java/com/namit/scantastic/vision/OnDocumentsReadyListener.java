package com.namit.scantastic.vision;

import com.namit.scantastic.models.Document;

import java.util.List;

public interface OnDocumentsReadyListener {

    void documentsReady(List<Document> documents);

}
