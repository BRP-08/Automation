import com.google.api.services.docs.v1.Docs;
import com.google.api.services.docs.v1.model.Document;
import com.google.api.services.slides.v1.Slides;
import com.google.api.services.slides.v1.model.BatchUpdatePresentationRequest;
import com.google.api.services.slides.v1.model.BatchUpdatePresentationResponse;
import com.google.api.services.slides.v1.model.CreateSlideRequest;
import com.google.api.services.slides.v1.model.InsertTextRequest;
import com.google.api.services.slides.v1.model.Request;
import com.google.api.services.slides.v1.model.TextContent;
import com.google.api.services.slides.v1.model.WriteControl;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.common.collect.Lists;

import java.io.FileInputStream;
import java.util.Collections;
import java.util.List;

public class GoogleSlidesAutomation {
    private static final String CREDENTIALS_FILE_PATH = "path/to/credentials.json";
    private static final String SOURCE_DOCUMENT_ID = "source-doc-id";
    private static final String SLIDES_PRESENTATION_ID = "slides-presentation-id";

    public static void main(String[] args) {
        try {
            // Load the credentials file
            GoogleCredentials credentials = ServiceAccountCredentials
                    .fromStream(new FileInputStream(CREDENTIALS_FILE_PATH))
                    .createScoped(Collections.singleton("https://www.googleapis.com/auth/presentations"));

            // Create a Google Docs service
            Docs docsService = new Docs.Builder(credentials).build();

            // Retrieve the source document content
            Document document = docsService.documents().get(SOURCE_DOCUMENT_ID).execute();

            // Extract the content from the source document
            String content = document.getBody().getContent().stream()
                    .filter(element -> element.getParagraph() != null && element.getParagraph().getElements() != null)
                    .map(element -> element.getParagraph().getElements().get(0).getTextRun().getContent())
                    .reduce("", String::concat);

            // Create a Google Slides service
            Slides slidesService = new Slides.Builder(credentials).build();

            // Create a new slide in the target presentation
            CreateSlideRequest createSlideRequest = new CreateSlideRequest();
            Request createSlideReq = new Request();
            createSlideReq.setCreateSlide(createSlideRequest);
            List<Request> requests = Lists.newArrayList(createSlideReq);

            BatchUpdatePresentationRequest batchUpdateRequest = new BatchUpdatePresentationRequest();
            batchUpdateRequest.setRequests(requests);
            BatchUpdatePresentationResponse batchUpdateResponse = slidesService.presentations()
                    .batchUpdate(SLIDES_PRESENTATION_ID, batchUpdateRequest)
                    .execute();

            // Get the ID of the newly created slide
            String slideId = batchUpdateResponse.getReplies().get(0).getCreateSlide().getObjectId();

            // Insert the content into the slide
            InsertTextRequest insertTextRequest = new InsertTextRequest();
            insertTextRequest.setObjectId(slideId);
            insertTextRequest.setText(content);
            insertTextRequest.setInsertionIndex(0);

            Request insertTextReq = new Request();
            insertTextReq.setInsertText(insertTextRequest);
            requests = Lists.newArrayList(insertTextReq);

            batchUpdateRequest = new BatchUpdatePresentationRequest();
            batchUpdateRequest.setRequests(requests);

            slidesService.presentations().batchUpdate(SLIDES_PRESENTATION_ID, batchUpdateRequest).execute();

            System.out.println("Content copied from Google Docs and pasted into Google Slides successfully.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
