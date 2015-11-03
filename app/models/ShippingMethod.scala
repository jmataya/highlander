package models

import models.rules.QueryStatement
import org.json4s.JValue
import utils.ExPostgresDriver.api._
import monocle.macros.GenLens
import utils.{GenericTable, ModelWithIdParameter, TableQueryWithId}

final case class ShippingMethod(id: Int = 0, parentId: Option[Int] = None, adminDisplayName: String,
  storefrontDisplayName: String, shippingCarrierId: Option[Int] = None, price: Int, isActive: Boolean = true,
  conditions: Option[QueryStatement] = None, restrictions: Option[QueryStatement] = None)
  extends ModelWithIdParameter[ShippingMethod]

object ShippingMethod

class ShippingMethods(tag: Tag) extends GenericTable.TableWithId[ShippingMethod](tag, "shipping_methods")  {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def parentId = column[Option[Int]]("parent_id")
  def adminDisplayName = column[String]("admin_display_name")
  def storefrontDisplayName = column[String]("storefront_display_name")
  def shippingCarrierId = column[Option[Int]]("shipping_carrier_id")
  def price = column[Int]("price")
  def isActive = column[Boolean]("is_active")
  def conditions = column[Option[QueryStatement]]("conditions")
  def restrictions = column[Option[QueryStatement]]("restrictions")

  def * = (id, parentId, adminDisplayName, storefrontDisplayName, shippingCarrierId, price,
    isActive, conditions, restrictions) <> ((ShippingMethod.apply _).tupled, ShippingMethod.unapply)
}

object ShippingMethods extends TableQueryWithId[ShippingMethod, ShippingMethods](
  idLens = GenLens[ShippingMethod](_.id)
)(new ShippingMethods(_)) {

  def findActive(implicit db: Database): Query[ShippingMethods, ShippingMethod, Seq] = filter(_.isActive === true)

  def findActiveById(id: Int)(implicit db: Database): QuerySeq = findActive.filter(_.id === id)
}
