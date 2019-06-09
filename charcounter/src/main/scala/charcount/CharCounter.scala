package charcount

import akka.actor._
import akka.event.Logging
import scala.io.BufferedSource


object LineReaderActor {
  def props(fileStream: BufferedSource) = Props(new LineReaderActor(fileStream))
  // Tells the LineReaderActor to read the next N lines
  case class ReadMore(lines: Int)
  // Send batches of rows, containing columns.
  case class Lines(lines: List[String])
  // Data structure to save the resulting letter count. 
  case class CharCount(map: Map[Char, Int])
}

object CharCounterActor {
  def props(reader: ActorRef) = Props(new CharCounterActor(reader))
}

class LineReaderActor(fileStream: BufferedSource) extends Actor with ActorLogging {
  import akka.actor.{ Address, AddressFromURIString }

  val rows = fileStream.getLines

  val workerRefs = for (i <- 1 to 16) yield {
    context.actorOf(CharCounterActor.props(self))
  }

  import LineReaderActor._

  override def receive = {
    case ReadMore(value) => {
      if (!rows.hasNext) {
        workerRefs.foreach(_ ! PoisonPill)
        self ! PoisonPill
      } else {
        sender() ! Lines(rows.take(value).toList)
      }
    }
    case CharCount(counts) => {
      println(counts)
    }
  }

  override def postStop = {
    log.info("Done!")
  }
}

class CharCounterActor(source: ActorRef) extends Actor with ActorLogging {
  import LineReaderActor._
  import CharCounterActor._

  final val DEFAULT_BATCH_SIZE = 15

  override def preStart = {
    notifyReadyToWork()
  }

  override def receive = {
    case Lines(rows) => {
      // println(rows)
      source ! CharCount(Map('T' -> 5, 'B' -> 7))
      notifyReadyToWork()
    }
  }

  private def notifyReadyToWork() {
    source ! ReadMore(DEFAULT_BATCH_SIZE)
  }
}