import java.nio.charset.{Charset, StandardCharsets}
import org.json.JSONObject
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import org.slf4j.LoggerFactory
import com.typesafe.config.{Config, ConfigFactory}




object BedrockLambda {

  val config = ConfigFactory.load("application.conf")
  val log = LoggerFactory.getLogger(this.getClass)
  private val MODEL_ID = config.getString("app.default_model")
  private val accessKeyId = config.getString("app.aws.accessKeyId")
  private val secretAccessKey = config.getString("app.aws.secretAccessKey")
  private val region = config.getString("app.aws.region")

  def generateText(prompt: String): String = {
    log.info("Start text generation process")

    val credentialsProvider = StaticCredentialsProvider.create(
      AwsBasicCredentials.create(accessKeyId, secretAccessKey)
    )

    val bedrockClient = BedrockRuntimeClient.builder()
      .region(Region.of(region))
      .credentialsProvider(credentialsProvider)
      .build()

    try   {
      val bedrockBody = BedrockRequestBody.builder()
        .withModelId(MODEL_ID)
        .withPrompt(prompt)
        .withInferenceParameter("temperature", config.getInt("app.model.temperature"))
        .withInferenceParameter("p", config.getInt("app.model.p"))
        .withInferenceParameter("k", config.getInt("app.model.k"))
        .withInferenceParameter("max_tokens", config.getInt("app.model.max_tokens"))
        .build()

      log.info("Built request body successfully")

      val invokeModelRequest = InvokeModelRequest.builder()
        .modelId(MODEL_ID)
        .body(SdkBytes.fromString(bedrockBody, StandardCharsets.UTF_8))
        .build()

      val invokeModelResponse: InvokeModelResponse = bedrockClient.invokeModel(invokeModelRequest)
      val responseAsJson = new JSONObject(invokeModelResponse.body().asUtf8String())

      log.info("Got response from bedrock")
      responseAsJson.getJSONArray("generations").getJSONObject(0).getString("text")


    } finally {
      bedrockClient.close()
    }
  }
}
