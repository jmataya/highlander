package models


import utils.RichTable
import utils.{ GenericTable, TableQueryWithId, ModelWithIdParameter }
import monocle.macros.GenLens
import slick.driver.PostgresDriver.api._
import slick.driver.PostgresDriver.backend.{DatabaseDef => Database}


import scala.concurrent.{ExecutionContext, Future}


case class Sku(id: Int = 0, name: Option[String] = None) extends ModelWithIdParameter{

}

// This table mostly acts a placeholder in our system.  We may or may not import skus from 'origin' into this.
class Skus(tag: Tag) extends GenericTable.TableWithId[Sku](tag, "skus") with RichTable {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name = column[Option[String]]("name")

  def * = (id, name) <> ((Sku.apply _).tupled, Sku.unapply)


}

object Skus extends TableQueryWithId[Sku, Skus](
  idLens = GenLens[Sku](_.id)
)(new Skus(_)) {
  val inventorySummaries = TableQuery[InventorySummaries]


  def isAvailableOnHand(id: Int): Boolean = {
    true
  }

  def qtyAvailableOnHand(id: Int)(implicit ec: ExecutionContext, db: Database): Future[Int] = {
    db.run(_qtyAvailableOnHand(id).result.head)
  }

  def _qtyAvailableOnHand(id: Int) = {
    for {
      iSum <- inventorySummaries.filter(_.skuId === id)
    } yield (iSum.availableOnHand)
  }
}