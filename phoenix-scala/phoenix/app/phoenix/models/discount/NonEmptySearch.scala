package phoenix.models.discount

/**
  * Some of our discount have parameter `search: Seq[T <: SearchReference]`
  * And this parameter can't be empty actually in all cases for now.
  * This trait should be added to all Offers or Qualifiers with this parameter
  * to be ensure search is presented.
  *
  * Note: json4s create empty Seq in case classes even if field not presented in json
  *
  * TODO: Looks hacky, need to refactor to NonEmptyList for this Seq-s.
  * TODO: Need to add json4s serializer for NonEmptyList // narma 02.12.2016
  * TODO: Or, it's better - replace json4s with circle with native support of NEL
  */
trait NonEmptySearch {
  val search: Seq[_]
}
