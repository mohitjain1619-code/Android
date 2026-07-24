# AWS Rekognition Integration for Gender Verification

## Architecture
- **Single Image Capture**: User captures 1 live FRONT image
- **Image Compression**: Compress to low res (max 500x500) before sending
- **AWS Rekognition**: Detects gender + face liveness
- **Conditional Verification**:
  - Males: Auto-verified (skip liveness check)
  - Females: Full liveness + gender match verification

---

## Android Setup

### 1. Add AWS SDK Dependencies
Edit `app/build.gradle`:
```gradle
dependencies {
    // AWS Rekognition
    implementation 'com.amazonaws:aws-android-sdk-rekognition:2.+'
    implementation 'com.amazonaws:aws-android-sdk-core:2.+'
}
```

### 2. Android Manifest Permissions
Add to `AndroidManifest.xml`:
```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.INTERNET" />
```

### 3. AndroidAWSConfig Class (Create New)
```java
package com.mohitt.camverz;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClient;
import android.content.Context;

public class AWSConfig {
    private static AmazonRekognition rekognitionClient;
    private static final String AWS_REGION = "us-east-1";
    private static final String ACCESS_KEY = "YOUR_AWS_ACCESS_KEY";
    private static final String SECRET_KEY = "YOUR_AWS_SECRET_KEY";

    public static AmazonRekognition getRekognitionClient() {
        if (rekognitionClient == null) {
            BasicAWSCredentials credentials = new BasicAWSCredentials(ACCESS_KEY, SECRET_KEY);
            rekognitionClient = new AmazonRekognitionClient(credentials);
            rekognitionClient.setRegion(Region.getRegion(Regions.fromName(AWS_REGION)));
        }
        return rekognitionClient;
    }
}
```

### 4. Image Compression Helper
Add to `VerificationActivity.java`:
```java
// Compress bitmap to max 500x500 and return as ByteBuffer
private ByteBuffer compressImage(Bitmap bitmap) {
    // Scale down to max 500x500
    int maxSize = 500;
    int width = bitmap.getWidth();
    int height = bitmap.getHeight();
    float scale = Math.min((float) maxSize / width, (float) maxSize / height);
    
    int newWidth = (int) (width * scale);
    int newHeight = (int) (height * scale);
    
    Bitmap compressed = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    
    // Convert to JPEG ByteBuffer
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    compressed.compress(Bitmap.CompressFormat.JPEG, 70, stream); // 70% quality
    byte[] imageBytes = stream.toByteArray();
    
    ByteBuffer buffer = ByteBuffer.wrap(imageBytes);
    buffer.rewind();
    return buffer;
}
```

### 5. AWS Rekognition Call
```java
private void detectGenderWithAWS(Bitmap bitmap) {
    new Thread(() -> {
        try {
            ByteBuffer imageBytes = compressImage(bitmap);
            
            DetectFacesRequest request = new DetectFacesRequest()
                .withImage(new Image().withBytes(imageBytes))
                .withAttributes(Attribute.ALL);
            
            AmazonRekognition rekognition = AWSConfig.getRekognitionClient();
            DetectFacesResult result = rekognition.detectFaces(request);
            
            // Check gender from AWS
            boolean faceFound = !result.getFaceDetails().isEmpty();
            if (!faceFound) {
                runOnUiThread(() -> Toast.makeText(this, "No face detected", Toast.LENGTH_SHORT).show());
                return;
            }
            
            // Get detected gender
            FaceDetail face = result.getFaceDetails().get(0);
            Gender gender = face.getGender();
            String detectedGender = gender.getValue().toLowerCase(); // "Male" or "Female"
            float confidence = gender.getConfidence();
            
            Log.d(TAG, "AWS Detected: " + detectedGender + " (" + confidence + "%)");
            
            // Verify gender matches user's profile
            boolean genderMatch = detectedGender.equals(selectedGender.toLowerCase());
            
            if (genderMatch && confidence > 80) {
                isGenderVerified = true;
                runOnUiThread(this::submitVerification);
            } else {
                runOnUiThread(() -> {
                    String msg = genderMatch ? "Low confidence. Try again." : "Gender mismatch. Expected: " + selectedGender;
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                    restartVerification();
                });
            }
            
        } catch (Exception e) {
            Log.e(TAG, "AWS Rekognition error: " + e.getMessage());
            runOnUiThread(() -> Toast.makeText(this, "Verification failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }).start();
}
```

---

## AWS Console Setup

### 1. Create IAM User
1. Go to AWS IAM Console
2. Create new user with permissions:
   ```json
   {
     "Version": "2012-10-17",
     "Statement": [
       {
         "Effect": "Allow",
         "Action": [
           "rekognition:DetectFaces",
           "rekognition:DetectLabels"
         ],
         "Resource": "*"
       }
     ]
   }
   ```
3. Generate Access Key + Secret Key
4. Add to `AWSConfig.java`

### 2. Enable Rekognition
- AWS Console → Rekognition
- Enable in region (us-east-1 recommended)

### 3. Cost Optimization
- DetectFaces: $0.00125 per image (first 5M/month)
- Estimate: 10,000 verifications = ~$12.50/month

---

## Flow Changes in VerificationActivity

### Old Flow (3 positions + TFLite)
```
FRONT → LEFT → RIGHT → Gender Check (Local TFLite) → Submit
```

### New Flow (1 image + AWS)
```
Check User Gender (Firebase)
  ├─ If MALE → Auto-verify ✅ (no verification needed)
  └─ If FEMALE → 
      ├─ Show "Capture your face" instruction
      ├─ Capture 1 FRONT image (live)
      ├─ Compress image
      ├─ Send to AWS Rekognition
      ├─ AWS returns: Gender + Confidence + Liveness
      ├─ If gender match + confidence > 80% → Verified ✅
      └─ If mismatch → Ask to retake
```

---

## Code Changes Summary

**Files to Modify:**
1. `VerificationActivity.java` - Remove LEFT/RIGHT, add AWS integration
2. `build.gradle` - Add AWS SDK
3. `AndroidManifest.xml` - Add permissions
4. Create `AWSConfig.java` - AWS client setup

**Files to Delete:**
1. `GenderModelManager.java` - No longer needed (AWS handles it)
2. `gender_model.tflite` - No longer needed

---

## Testing

### Test Case 1: Female User
1. Onboard as Female
2. Click Verify
3. Capture 1 image from camera
4. AWS detects "Female" with 90%+ confidence
5. Get verified ✅

### Test Case 2: Male User
1. Onboard as Male
2. Click Verify
3. Gets auto-verified immediately ✅

### Test Case 3: Gender Mismatch
1. Female user captures image but uploaded a male image
2. AWS detects "Male" 
3. Shows "Gender mismatch" error
4. Ask to retake ❌

---

## Next Steps
1. Add AWS SDK to gradle
2. Create AWSConfig.java
3. Update VerificationActivity with new logic
4. Test with real AWS account
5. Monitor RecognitionAPI costs
