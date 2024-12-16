import org.json.JSONObject
import scala.collection.mutable
import scala.jdk.CollectionConverters._


object BedrockRequestBody {

  def builder(): BedrockRequestBodyBuilder = new BedrockRequestBodyBuilder()

  class BedrockRequestBodyBuilder {
    private var modelId: Option[String] = None
    private var prompt: Option[String] = None
    private val inferenceParameters: mutable.Map[String, Any] = mutable.Map()

    def withModelId(modelId: String): BedrockRequestBodyBuilder = {
      this.modelId = Some(modelId)
      this
    }

    def withPrompt(prompt: String): BedrockRequestBodyBuilder = {
      this.prompt = Some(prompt)
      this
    }

    def withInferenceParameter(paramName: String, paramValue: Any): BedrockRequestBodyBuilder = {
      inferenceParameters(paramName) = paramValue
      this
    }

    def build(): String = {
      if (modelId.isEmpty) throw new IllegalArgumentException("'modelId' is a required parameter")
      if (prompt.isEmpty) throw new IllegalArgumentException("'prompt' is a required parameter")

      val bedrockBodyCommand = modelId.get match {
        case "cohere.command-text-v14" =>
          new CohereCommand(prompt.get, inferenceParameters.toMap)
        case _ =>
          throw new IllegalArgumentException(s"Unsupported modelId: ${modelId.get}")
      }

      bedrockBodyCommand.execute()
    }
  }
}

abstract class BedrockBodyCommand(protected val prompt: String, protected val inferenceParameters: Map[String, Any]) {

  // use mutable here to update the parameter maps // won't have any side effect
  protected def updateMap(existingMap: mutable.Map[String, Any], newEntries: Map[String, Any]): Unit = {
    newEntries.foreach { case (key, value) =>
      if (existingMap.contains(key)) {
        existingMap.update(key, value)
      } else {
        existingMap.values.foreach {
          case nestedMap: mutable.Map[String, Any] =>
            updateMap(nestedMap, Map(key -> value))
          case _ => // Do nothing
        }
      }
    }
  }

  def execute(): String
}

class CohereCommand(prompt: String, inferenceParameters: Map[String, Any])
  extends BedrockBodyCommand(prompt, inferenceParameters) {

  override def execute(): String = {
    val jsonMap = mutable.Map[String, Any](
      "prompt" -> prompt,
      "max_tokens" -> 200,
      "temperature" -> 0.0,
      "p" -> 0.01,
      "k" -> 0,
      "stop_sequences" -> Array.empty[String],
      "return_likelihoods" -> "NONE"
    )

    if (inferenceParameters.nonEmpty) {
      updateMap(jsonMap, inferenceParameters)
    }

    new JSONObject(jsonMap.toMap.asJava).toString()
  }
}
