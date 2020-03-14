import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object DistributedWordCounter extends App {

    val system = ActorSystem("distributed-word-counter")

    /**
      * This is the companion object for the WordCounterMaster class
      */
    object WordCounterMaster {

        /**
          * The Initialize class makes the Master initiate the process of creating the Workers
          * @param nChildren The number of workers to be created
          */
        case class Initialize(nChildren: Int)

        /**
          * The WordCountTask class sends the task to the workers
          * @param id The ID number of the task
          * @param text The String which the worker will count the words
          */
        case class WordCountTask(id: Int, text: String)

        /**
          * The WordCountReply sends the count of each word to the sender
          * @param id The ID number of the task
          * @param count The list with the word and it's count
          */
        case class WordCountReply(id: Int, count: List[(String, Int)])
    }

    /**
      * This is the main class for the WordCounterMaster Actor
      */
    class WordCounterMaster extends Actor {

        import WordCounterMaster._

        /**
          * This is the main method of an Actor. It receives a message and performs an action.
          * @return Creates the number of workers defined in the Initialize case class
          *         and hotswap to withChildren method
          */
        override def receive: Receive = {
            case Initialize(n) =>
                println("Initializing...\n")
                val childrenRefs =
                    for (i <- 1 to n) yield context.actorOf(Props[WordCounterWorker], s"worker-$i")
                context.become(withChildren(childrenRefs, 0, 0, Map()))
        }

        /**
          * The withChildren method uses the workers to run the given tasks
          * @param childrenRefs A sequence of workers
          * @param currentChildIndex The ID of the current worker
          * @param currentTaskId The ID of the current task
          * @param requestMap A map containing Task ID and the Worker who is going to run it
          * @return Returns a List containing the count of each word in the String to the Task Manager
          */
        def withChildren(childrenRefs: Seq[ActorRef], currentChildIndex: Int, currentTaskId: Int,
                         requestMap: Map[Int, ActorRef]): Receive = {
            case text: String              =>
                println(s"Sending task #$currentTaskId to worker-$currentChildIndex")
                val originalSender = sender()
                val task = WordCountTask(currentTaskId, text)
                val childRef = childrenRefs(currentChildIndex)
                childRef ! task
                val next = (currentChildIndex + 1) % childrenRefs.length
                val nextTaskId = currentTaskId + 1
                val newRequestMap = requestMap + (currentTaskId -> originalSender)
                context.become(withChildren(childrenRefs, next, nextTaskId, newRequestMap))
            case WordCountReply(id, count) =>
                println(s"The count for task #$id is:")
                count foreach { x =>
                    println(x)
                    val originalSender = requestMap(id)
                    originalSender ! x
                }
                context.become(withChildren(childrenRefs, currentChildIndex, currentTaskId, requestMap - 1))
        }
    }

    /**
      * This is the main class for the WordCounterWorker Actor
      */
    class WordCounterWorker extends Actor {

        import WordCounterMaster._

        /**
          * This is the main method of an Actor. It receives a message and performs an action.
          * @return Returns a message to the sender, with the Task ID and a List with the text and it's count
          */
        override def receive: Receive = {
            case WordCountTask(id, text) =>
                println(s"Received task #$id")
                val actualText = text.replace(",", "")
                                     .replace(".", "")
                val textToLower = actualText.toLowerCase
                val splitText: Array[String] = textToLower.split(" ")
                val mapText = splitText.map(x => (x, 1))
                val mapReduceText: List[(String, Int)] = mapText.groupBy(_._1).mapValues(_.map(_._2).sum).toList
                sender() ! WordCountReply(id, mapReduceText)
        }
    }

    /**
      * This is the companion object to the class TaskManager
      */
    object TaskManager {

        /**
          * Makes a request to the Master Node
          * @param task The text to be passed as task
          * @param nWorkers The number of workers to create
          * @tparam A The type of the task. It can be a String or a List of Strings
          */
        case class requestTask[A](task: A, nWorkers: Int)
    }

    /**
      * This is the main class for the TaskManager Actor
      */
    class TaskManager extends Actor {

        import TaskManager._
        import WordCounterMaster._

        /**
          * This is the main method of an Actor. It receives a message and performs an action.
          * @return Sends the message to the Master Node
          */
        override def receive: Receive = {
            case requestTask(task, nWorkers) =>
                val master = context.actorOf(Props[WordCounterMaster], "master")
                master ! Initialize(nWorkers)
                task match {
                    case x: String       => master ! x
                    case x: List[String] => x.foreach(text => master ! text)
                }
        }
    }

    val taskManager = system.actorOf(Props[TaskManager], "manager")

    taskManager ! TaskManager.requestTask(List("distributed word counter", "two words words", "Ok oK ok OK"), 6)

}
