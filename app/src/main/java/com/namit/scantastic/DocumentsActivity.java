package com.namit.scantastic;

import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.namit.scantastic.models.Document;
import com.namit.scantastic.models.ScannedImage;
import com.namit.scantastic.utils.AppConstants;
import com.namit.scantastic.vision.OnDocumentsReadyListener;
import com.namit.scantastic.vision.VisionManager;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory;
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject;
import com.tom_roush.pdfbox.util.PDFBoxResourceLoader;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.namit.scantastic.utils.AppConstants.IMG_STORAGE_DIR;

public class DocumentsActivity extends AppCompatActivity implements OnDocumentsReadyListener {

    @BindView(R.id.recycler_view) RecyclerView recyclerView;
    @BindView(R.id.progress_bar) ProgressBar progressBar;

    private static final int MARGIN = 5;
    private static final int DOCS_PER_COLUMN = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_documents);
        ButterKnife.bind(this);

        toggleProgressbar(View.VISIBLE, View.GONE);
        createBottomBar();

        if(AppConstants.capturedImages != null) {
            VisionManager visionManager = new VisionManager(this, this);
            visionManager.readImages(AppConstants.capturedImages);
        }
    }


    @Override
    public void documentsReady(List<Document> documents) {
        recyclerView.setLayoutManager(new GridLayoutManager(this, DOCS_PER_COLUMN));
        recyclerView.setAdapter(new DocumentsAdapter(this, documents));
        PDFBoxResourceLoader.init(getApplicationContext());
        for (int i=0;i<documents.size();i++){
            String filename =  new Date().getTime() + "_" + i + ".pdf";
            File storageDirectory = new File(Environment.getExternalStorageDirectory(), IMG_STORAGE_DIR);
            File path = new File(storageDirectory, filename);
            Log.d(getLocalClassName(), "PDF location: " + path.getAbsolutePath());
            List<ScannedImage> pages = documents.get(i).getPages();
            PDDocument document = new PDDocument();
            for(int j=0; j<pages.size();j++){
                PDPage pdPage = new PDPage();
                document.addPage(pdPage);
                PDPageContentStream contentStream;
                try {
                    contentStream = new PDPageContentStream(document, pdPage);
                    PDImageXObject alphaXimage = LosslessFactory.createFromImage(document, pages.get(j).getBitmap());
                    PDRectangle cropbBox = pdPage.getCropBox();
                    float widthPt = cropbBox.getWidth();
                    float imageWidth = alphaXimage.getWidth() - MARGIN;
                    float imageHeight = alphaXimage.getHeight() - MARGIN;
                    if(imageWidth > widthPt){
                        contentStream.drawImage(alphaXimage, MARGIN, MARGIN, widthPt- MARGIN, (imageHeight * (widthPt/imageWidth))- MARGIN);
                    }else{
                        contentStream.drawImage(alphaXimage, MARGIN, MARGIN, imageWidth, imageHeight);
                    }
                    contentStream.close();
                }catch (IOException e){
                    Log.e(getLocalClassName(), e.getMessage(), e);
                }
            }
            try{
                document.save(path.getAbsolutePath());
                document.close();
            } catch (IOException e) {
                Log.e(getLocalClassName(), e.getMessage(), e);
            }
        }
        toggleProgressbar(View.GONE, View.VISIBLE);
    }


    private void toggleProgressbar(int progressBarVisibility, int listVisibility) {
        progressBar.setVisibility(progressBarVisibility);
        recyclerView.setVisibility(listVisibility);
    }

    private void createBottomBar() {
        View.OnClickListener bottomBarListener = view -> {
            switch (view.getId()){
                case R.id.nav_done:

                    break;
                case R.id.nav_share:
//                    uploadToDrive();
                    break;
            }
        };
        findViewById(R.id.nav_done).setOnClickListener(bottomBarListener);
        findViewById(R.id.nav_share).setOnClickListener(bottomBarListener);
    }

    private void uploadToDrive() throws GeneralSecurityException, IOException {
        // Build a new authorized API client service.
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Drive service = new Drive.Builder(HTTP_TRANSPORT, JacksonFactory.getDefaultInstance()
//                ,getCredentials(HTTP_TRANSPORT)
                ,null)
                .setApplicationName(getString(R.string.app_name))
                .build();
        // Print the names and IDs for up to 10 files.

        File storageDirectory = new File(Environment.getExternalStorageDirectory(), IMG_STORAGE_DIR);
        File[] files = storageDirectory.listFiles();
        for(File doc : files){
            if(!doc.getName().endsWith("pdf")){
                return;
            }
            com.google.api.services.drive.model.File driveFile = new com.google.api.services.drive.model.File();
            driveFile.setName(doc.getName());
            FileContent mediaContent = new FileContent("application/pdf", doc.getAbsoluteFile());
            com.google.api.services.drive.model.File file = service.files().create(driveFile, mediaContent)
                    .setFields("id")
                    .execute();
            System.out.println("File ID: " + file.getId());
        }

    }

    //TODO finish implementing drive connection
//    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
//        // Load client secrets.
//        InputStream in = DriveQuickstart.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
//        if (in == null) {
//            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
//        }
//        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
//
//        // Build flow and trigger user authorization request.
//        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
//                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
//                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
//                .setAccessType("offline")
//                .build();
//        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
//        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
//    }
}
