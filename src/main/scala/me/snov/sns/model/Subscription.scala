package me.snov.sns.model

import spray.json._

case class Subscription(
                         arn: String,
                         owner: String,
                         topicArn: String,
                         protocol: String,
                         endpoint: String,
                         subscriptionAttributes: Map[String, String] = Map.empty
                       ) {

  def isRawMessageDelivery: Boolean = {
    subscriptionAttributes
      .get("RawMessageDelivery")
      .map(java.lang.Boolean.parseBoolean(_))
      .getOrElse(false)
  }
}

object Subscription extends DefaultJsonProtocol {
  implicit val format = jsonFormat6(Subscription.apply)
}
