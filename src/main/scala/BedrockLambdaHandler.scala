import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.slf4j.LoggerFactory
import com.typesafe.config.{Config, ConfigFactory}
import scala.jdk.CollectionConverters._

class APIResponse(val body: String, val statusCode: Int, val headers: Map[String, String]) {
  def toApiGatewayResponse: java.util.Map[String, Any] = {
    Map(
      "statusCode" -> statusCode,
      "headers" -> headers.asJava,
      "body" -> body
    ).asJava
  }

  override def toString: String = s"APIResponse(body=$body, statusCode=$statusCode, headers=$headers)"
}

class BedrockLambdaHandler extends RequestHandler[APIGatewayProxyRequestEvent, java.util.Map[String, Any]] {

  // Load configuration and initialize logging
  val config: Config = ConfigFactory.load("application.conf")
  val log = LoggerFactory.getLogger(this.getClass)

  override def handleRequest(input: APIGatewayProxyRequestEvent, context: Context): java.util.Map[String, Any] = {
    // Parse the JSON body and extract the "prompt" field
    val prompt = extractPrompt(input.getBody, context)
    log.info(s"Received prompt: $prompt")

    // Build the response
    val apiResponse = buildResponse(prompt, context)
    log.info("Sending response back to API Gateway")
    apiResponse.toApiGatewayResponse
  }

  private def extractPrompt(body: String, context: Context): String = {
    val defaultPrompt = ""
    if (body == null || body.trim.isEmpty) {
      log.warn("Request body is null or empty. Defaulting to the default prompt.")
      return defaultPrompt
    }

    val mapper = new ObjectMapper().registerModule(DefaultScalaModule)
    try {
      val parsedBody = mapper.readValue(body, classOf[Map[String, String]])
      parsedBody.getOrElse("prompt", {
        log.warn("Prompt field not found in the request body. Defaulting to the default prompt.")
        defaultPrompt
      })
    } catch {
      case e: Exception =>
        context.getLogger.log(s"Error parsing JSON body: ${e.getMessage}")
        log.warn("Failed to parse request body as JSON. Defaulting to the default prompt.")
        defaultPrompt
    }
  }

  private def buildResponse(prompt: String, context: Context): APIResponse = {
    val headers: Map[String, String] = Map(
      "X-amazon-author" -> config.getString("app.response.header.X-amazon-author"),
      "X-amazon-apiVersion" -> config.getString("app.response.header.X-amazon-apiVersion"),
      "Access-Control-Allow-Origin" -> "*", // CORS support
      "Access-Control-Allow-Methods" -> "POST, GET, OPTIONS"
    )

    val body: String = try {
      if (prompt == "") {
        log.info("Prompt is empty! Return default string");
        new ObjectMapper()
          .registerModule(DefaultScalaModule)
          .writeValueAsString(Map("generatedText" -> "Query is empty"))
      } else {
        log.info("Calling BedrockLambda.generateText...")
        val generatedText = BedrockLambda.generateText(prompt)
        new ObjectMapper()
          .registerModule(DefaultScalaModule)
          .writeValueAsString(Map("generatedText" -> generatedText))
      }
    } catch {
      case e: Exception =>
        val errorMessage = s"Error processing the request: ${e.getMessage}"
        context.getLogger.log(errorMessage)
        log.warn("Failed to generate text. Returning an error response.")
        new ObjectMapper()
          .registerModule(DefaultScalaModule)
          .writeValueAsString(Map("error" -> errorMessage))
    }

    log.info("Response generated successfully.")
    new APIResponse(body, 200, headers)
  }
}
