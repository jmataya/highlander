import java.time.{Duration, ZoneId, Instant}

package object time {
  val UTC = ZoneId.of("UTC")

  implicit class RichInstant(val instant: Instant) extends AnyVal {
    def minuteOfHour: Int          = utc.getMinute

    // The follow methods assume UTC
    def plusMinutes(minutes: Long)  = instant.plusSeconds(minutes * 60)
    def minusMinutes(minutes: Long) = plusMinutes(0 - minutes)

    def durationUntil(other: Instant) = Duration.between(instant, other)

    def isBeforeNow: Boolean = instant.toEpochMilli <= Instant.now.toEpochMilli
    def utc = instant.atZone(UTC)
  }

  implicit class RichDuration(val duration: Duration) extends AnyVal {
    def getMinutes: Int = (duration.getSeconds.toDouble / 60).toInt
  }
}
