package failures

import utils.friendlyClassName
import Util._

case class AlreadyAssignedFailure(message: String) extends Failure {
  override def description = message
}

object AlreadyAssignedFailure {
  def apply[A](a: A, searchKey: Any, storeAdminId: Int): AlreadyAssignedFailure = {
    val msg =
      s"storeAdmin with id=$storeAdminId is already assigned to ${friendlyClassName(a)} with ${searchTerm(
          a)}=$searchKey"
    AlreadyAssignedFailure(msg)
  }
}

case class NotAssignedFailure(message: String) extends Failure {
  override def description = message
}

object NotAssignedFailure {
  def apply[A](a: A, searchKey: Any, storeAdminId: Int): NotAssignedFailure = {
    val msg =
      s"storeAdmin with id=$storeAdminId is not assigned to ${friendlyClassName(a)} with ${searchTerm(
          a)}=$searchKey"
    NotAssignedFailure(msg)
  }
}

case class AssigneeNotFoundFailure(message: String) extends Failure {
  override def description = message
}

object AssigneeNotFoundFailure {
  def apply[A](a: A, searchKey: Any, assigneeId: Int): AssigneeNotFoundFailure = {
    AssigneeNotFoundFailure(
        s"storeAdmin with id=$assigneeId is not assigned to ${friendlyClassName(a)} " +
          s"with ${searchTerm(a)}=$searchKey")
  }
}
