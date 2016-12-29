package responses

import java.time.Instant

import models.product._
import responses.AlbumResponses._
import responses.ObjectResponses._
import responses.ProductVariantResponses._
import responses.TaxonomyResponses.SingleTaxonResponse
import responses.ProductOptionResponses._
import utils.aliases._

object ProductResponses {

  object ProductHeadResponse {

    case class Root(id: Int) extends ResponseItem

    //Product here is a placeholder for future. Using only form
    def build(p: Product): Root = Root(id = p.formId)
  }

  // New Product Response
  object ProductResponse {

    case class Root(id: Int,
                    slug: String,
                    context: ObjectContextResponse.Root,
                    attributes: Json,
                    albums: Seq[AlbumResponse.Root],
                    variants: Seq[ProductVariantResponse.Root],
                    options: Seq[ProductOptionResponse.Root],
                    archivedAt: Option[Instant],
                    taxons: Seq[SingleTaxonResponse])
        extends ResponseItem

    def build(product: IlluminatedProduct,
              albums: Seq[AlbumResponse.Root],
              variants: Seq[ProductVariantResponse.Root],
              options: Seq[ProductOptionResponse.Root],
              taxons: Seq[SingleTaxonResponse]): Root =
      Root(id = product.id,
           slug = product.slug,
           attributes = product.attributes,
           context = ObjectContextResponse.build(product.context),
           albums = albums,
           variants = variants,
           options = options,
           archivedAt = product.archivedAt,
           taxons = taxons)
  }
}
