package me.snov.sns.api

import java.util.UUID

import akka.actor.{ActorLogging, Actor, ActorRef, Props}
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import me.snov.sns.api.PublishActor.CmdPublish
import me.snov.sns.api.SubscribeActor.CmdFanOut
import me.snov.sns.model.Message

object PublishApi {
  private val arnPattern = """([\w+_:-]{1,512})""".r
  
  def route(actorRef: ActorRef)(implicit timeout: Timeout): Route = {
    pathSingleSlash {
      formField('Action ! "Publish") {
        formFields('TopicArn, 'Message) { (topicArn, message) =>
          topicArn match {
            case arnPattern(topic) => complete {
              (actorRef ? CmdPublish(topic, message)).mapTo[HttpResponse]
            }
            case _ => complete(HttpResponse(400, entity = "Invalid topic ARN"))
          }
        } ~
        complete(HttpResponse(400, entity = "TopicArn is required"))
      }
    }
  }
}

object PublishResponses extends XmlHttpResponse {
  def publish(message: Message) = {
    response(
      <PublishResponse xmlns="http://sns.amazonaws.com/doc/2010-03-31/">
        <PublishResult>
          <MessageId>{message.uuid}</MessageId>
        </PublishResult>
        <ResponseMetadata>
          <RequestId>{UUID.randomUUID}</RequestId>
        </ResponseMetadata>
      </PublishResponse>
    )
  }
}

object PublishActor {
  def props(actor: ActorRef) = Props(new PublishActor(actor))

  case class CmdPublish(topicArn: String, message: String)
}

class PublishActor(subscribeActor: ActorRef) extends Actor with ActorLogging {
  private def publish(topicArn: String, messageString: String): HttpResponse = {
    val message = Message(messageString)
    
    subscribeActor ! CmdFanOut(topicArn, message)
    
    PublishResponses.publish(message)
  }
  
  override def receive = {
    case CmdPublish(topicArn, message) => sender ! publish(topicArn, message)
    case _ => sender ! HttpResponse(500, entity = "Invalid message")
  }
}