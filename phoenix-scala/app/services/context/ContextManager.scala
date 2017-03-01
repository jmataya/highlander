package services.context

import cats.implicits._
import failures.ObjectFailures._
import models.objects._
import payloads.ContextPayloads._
import responses.ObjectResponses.ObjectContextResponse._
import utils.aliases._
import utils.db._

object ContextManager {
  def getContext(id: Int)(implicit db: DB, ec: EC): DbResultT[Root] =
    for {
      context ← * <~ ObjectContexts.mustFindById404(id)
    } yield build(context)

  def getContextByName(name: String)(implicit ec: EC, db: DB): DbResultT[Root] =
    for {
      context ← * <~ mustFindByName404(name)
    } yield build(context)

  def createContext(payload: CreateObjectContext)(implicit ec: EC, db: DB): DbResultT[Root] =
    for {
      context ← * <~ ObjectContexts.create(
                   ObjectContext(name = payload.name, attributes = payload.attributes))
    } yield build(context)

  def updateContextByName(name: String, payload: UpdateObjectContext)(implicit ec: EC,
                                                                      db: DB): DbResultT[Root] =
    for {
      context ← * <~ mustFindByName404(name)
      update ← * <~ ObjectContexts.update(
                  context,
                  context.copy(name = payload.name, attributes = payload.attributes))
    } yield build(update)

  def mustFindByName404(name: String)(implicit ec: EC): DbResultT[ObjectContext] =
    ObjectContexts.filterByName(name).mustFindOneOr(ObjectContextNotFound(name))
}