import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito._

class BedrockLambdaHandlerTesting extends AnyFlatSpec with MockitoSugar{
  "AWS Bedrock api" should "return a successful response" in {
    val prompt = "What is the capital of France?"
    val response = BedrockLambda.generateText(prompt)
    println(s"Response: $response")
    assert(response.length != 0)
  }
}
