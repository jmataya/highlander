package utils

import scala.concurrent.{Future, ExecutionContext}

import services.Failures
import cats.{Monad, Applicative, Functor}
import cats.data.{Validated, XorT, Xor}
import scala.collection.generic.CanBuildFrom
import slick.driver.PostgresDriver.api._
import slick.profile.SqlAction

object DbResultT {
  type DbResultT[A] = XorT[DBIO, Failures, A]

  object implicits {
    implicit def dbioApplicative(implicit ec: ExecutionContext): Applicative[DBIO] = new Applicative[DBIO] {
      def ap[A, B](fa: DBIO[A])(f: DBIO[A => B]): DBIO[B] =
        fa.flatMap(a ⇒ f.map(ff ⇒ ff(a)))

      def pure[A](a: A): DBIO[A] = DBIO.successful(a)
    }

    implicit def dbioMonad(implicit ec: ExecutionContext, app: Applicative[DBIO]) = new Functor[DBIO] with Monad[DBIO] {
      override def map[A, B](fa: DBIO[A])(f: A ⇒ B): DBIO[B] = fa.map(f)

      override def pure[A](a: A): DBIO[A] = DBIO.successful(a)

      override def flatMap[A, B](fa: DBIO[A])(f: A => DBIO[B]): DBIO[B] = fa.flatMap(f)
    }
  }

  import implicits._

  def apply[A](v: DBIO[Failures Xor A])(implicit ec: ExecutionContext): DbResultT[A] =
    XorT[DBIO, Failures, A](v)

  def pure[A](v: A)(implicit ec: ExecutionContext): DbResultT[A] =
    XorT.pure[DBIO, Failures, A](v)

  def fromXor[A](v: Failures Xor A)(implicit ec: ExecutionContext): DbResultT[A] =
    v.fold(leftLift, rightLift)

  def right[A](v: DBIO[A])(implicit ec: ExecutionContext): DbResultT[A] =
    XorT.right[DBIO, Failures, A](v)

  def rightLift[A](v: A)(implicit ec: ExecutionContext): DbResultT[A] =
    XorT.right[DBIO, Failures, A](DBIO.successful(v))

  def left[A](v: DBIO[Failures])(implicit ec: ExecutionContext): DbResultT[A] =
    XorT.left[DBIO, Failures, A](v)

  def leftLift[A](v: Failures)(implicit ec: ExecutionContext): DbResultT[A] =
    left(DBIO.successful(v))

  def sequence[A, M[X] <: TraversableOnce[X]](in: M[DbResultT[A]])
    (implicit cbf: CanBuildFrom[M[DbResultT[A]], A, M[A]], ec: ExecutionContext): DbResultT[M[A]] =
    in.foldLeft(rightLift(cbf(in))) {
      (fr, fa) ⇒ for (r ← fr; a ← fa) yield r += a
    }.map(_.result())

  object * {
    def <~[A](v: DBIO[Failures Xor A])(implicit ec: ExecutionContext): DbResultT[A] =
      DbResultT(v)

    def <~[A](v: SqlAction[A, NoStream, Effect.All])(implicit ec: ExecutionContext): DbResultT[A] =
      DbResultT(v.map(Xor.right))

    def <~[A](v: Failures Xor A)(implicit ec: ExecutionContext): DbResultT[A] =
      DbResultT.fromXor(v)

    def <~[A](v: A)(implicit ec: ExecutionContext): DbResultT[A] =
      DbResultT.pure(v)

    def <~[A](v: Validated[Failures, A])(implicit ec: ExecutionContext): DbResultT[A] =
      DbResultT.fromXor(v.toXor)
  }
}
