package my.mma.api.global.fcm;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import my.mma.api.exception.ErrorCode;
import my.mma.api.exception.CustomException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

//@Service
@Slf4j
public class FirebaseInitialization {

    @Value("${firebase.credential-path}")
    private String credentialPath;

    @PostConstruct
    public void initialize() {
        try {

            FileInputStream serviceAccount = new FileInputStream(credentialPath);
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            FirebaseApp.initializeApp(options);
        } catch (FileNotFoundException e) {
            log.error("e=",e);
            log.error("firebase credentials path = {}", credentialPath);
            throw new CustomException(ErrorCode.SERVER_ERROR_500, "firebase admin file not found");
        } catch (IOException e) {
            throw new CustomException(ErrorCode.SERVER_ERROR_500, "firebase set Credentials error");
        }
    }

}
