package services

import scala.collection.immutable.Seq
import scala.concurrent.Future

import models.location._
import Country._
import Region._
import responses.CountryWithRegions
import slick.driver.PostgresDriver.api._
import utils.aliases._
import utils.db._
import utils.db.DbResultT._

object PublicService {

  def findCountry(countryId: Int)(implicit ec: EC, db: DB): Result[CountryWithRegions] = (for {
    country ← * <~ Countries.mustFindById404(countryId)
    regions ← * <~ Regions.filter(_.countryId === country.id).result.toXor
  } yield CountryWithRegions(country, sortRegions(regions.to[Seq]))).run()

  def listCountries(implicit ec: EC, db: DB): Future[Seq[Country]] =
    db.run(Countries.result).map { countries ⇒
      val usa = countries.filter(_.id == unitedStatesId)
      val othersSorted = countries.filterNot(_.id == unitedStatesId).sortBy(_.name)
      (usa ++ othersSorted).to[Seq]
    }

  def listRegions(implicit ec: EC, db: DB): Future[Seq[Region]] =
    db.run(Regions.result.map(rs ⇒ sortRegions(rs.to[Seq])))

  private def sortRegions(regions: Seq[Region]): Seq[Region] = {
    regions.filter(r ⇒ regularUsRegions.contains(r.id)).sortBy(_.name) ++
    regions.filter(r ⇒ armedRegions.contains(r.id)).sortBy(_.name) ++
    regions.filterNot(r ⇒ usRegions.contains(r.id)).sortBy(_.name)
  }
}
