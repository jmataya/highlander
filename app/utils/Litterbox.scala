package utils

import cats.implicits._
import cats.{SemigroupK, Semigroup}
import cats.data.NonEmptyList
import services._

// useful extensions to cats
object Litterbox {
  /* this is a required implicit conversion for apply (|@|) syntax on a NonEmptyList */
  implicit val nelSemigroup: Semigroup[NonEmptyList[String]] = SemigroupK[NonEmptyList].algebra[String]

  implicit val failureSemigroup: Semigroup[NonEmptyList[Failure]] = SemigroupK[NonEmptyList].algebra[Failure]
}
