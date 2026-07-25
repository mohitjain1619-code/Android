const { RekognitionClient, DetectFacesCommand } = require("@aws-sdk/client-rekognition");

// Check if AWS credentials are configured
if (!process.env.AWS_REGION || !process.env.AWS_ACCESS_KEY_ID || !process.env.AWS_SECRET_ACCESS_KEY) {
  console.warn("WARNING: AWS credentials not fully configured. Rekognition will not work.");
}

const rekognition = new RekognitionClient({
  region: process.env.AWS_REGION || "us-east-1",
  credentials: {
    accessKeyId: process.env.AWS_ACCESS_KEY_ID,
    secretAccessKey: process.env.AWS_SECRET_ACCESS_KEY,
  },
});

const detectGenderFromImage = async (imageBuffer) => {
  try {
    if (!process.env.AWS_ACCESS_KEY_ID || !process.env.AWS_SECRET_ACCESS_KEY) {
      return { error: "AWS credentials not configured on server" };
    }

    const command = new DetectFacesCommand({
      Image: { Bytes: imageBuffer },
      Attributes: ["ALL"],
    });

    const response = await rekognition.send(command);

    if (!response.FaceDetails || response.FaceDetails.length === 0) {
      return { error: "No face detected" };
    }

    const gender = response.FaceDetails[0].Gender;

    return {
      gender: gender?.Value,
      confidence: gender?.Confidence,
    };
  } catch (error) {
    console.error("Rekognition error:", error);
    return { error: "Rekognition service error: " + error.message };
  }
};

module.exports = {
  detectGenderFromImage,
};
