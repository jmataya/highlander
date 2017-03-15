defmodule Hyperion.ProductFactory do
  defmacro __using__(_opts) do
    quote do
      def product_without_varians_factory do
        %{body: %{"albums" => [%{"createdAt" => "2017-02-28T10:38:33.711Z", "id" => 27,
               "images" => [%{"alt" => "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Quay_Side.jpg",
                  "id" => 7,
                  "src" => "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Quay_Side.jpg",
                  "title" => "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Quay_Side.jpg"}],
               "name" => "Fox", "updatedAt" => "2017-02-28T10:38:33.711Z"}],
            "attributes" => %{"activeFrom" => %{"t" => "date",
                "v" => "2017-02-28T10:38:32.559Z"},
              "description" => %{"t" => "richText",
                "v" => "Stylish fit, stylish finish."},
              "tags" => %{"t" => "tags", "v" => ["sunglasses"]},
              "title" => %{"t" => "string", "v" => "Fox"}},
            "context" => %{"attributes" => %{"lang" => "en", "modality" => "desktop"},
              "name" => "default"}, "id" => 25,
            "skus" => [%{"albums" => [],
               "attributes" => %{"activeFrom" => %{"t" => "date",
                   "v" => "2017-02-28T10:38:33.627Z"},
                 "code" => %{"t" => "string", "v" => "SKU-TRL"},
                 "retailPrice" => %{"t" => "price",
                   "v" => %{"currency" => "USD", "value" => 10500}},
                 "salePrice" => %{"t" => "price",
                   "v" => %{"currency" => "USD", "value" => 10000}},
                 "tags" => %{"t" => "tags", "v" => ["sunglasses"]},
                 "title" => %{"t" => "string", "v" => "Fox"}}, "id" => 26}],
            "slug" => "fox", "taxons" => [], "variants" => []}}
      end

      def product_with_variants_factory do
        %{body: %{"albums" => [],
            "attributes" => %{"activeFrom" => %{"t" => "datetime",
                "v" => "2017-03-09T02:21:07.763Z"},
              "activeTo" => %{"t" => "datetime", "v" => nil},
              "description" => %{"t" => "richText", "v" => "<p>bar baz</p>"},
              "tags" => %{"t" => "tags", "v" => ["a", "b", "c", "d"]},
              "title" => %{"t" => "string", "v" => "foo"}},
            "context" => %{"attributes" => %{"lang" => "en", "modality" => "desktop"},
              "name" => "default"}, "id" => 468,
            "skus" => [%{"albums" => [],
               "attributes" => %{"activeFrom" => %{"t" => "datetime",
                   "v" => "2017-03-09T02:21:07.763Z"},
                 "activeTo" => %{"t" => "datetime", "v" => nil},
                 "code" => %{"t" => "string", "v" => "SKU123"},
                 "retailPrice" => %{"t" => "price",
                   "v" => %{"currency" => "USD", "value" => 1000}},
                 "salePrice" => %{"t" => "price",
                   "v" => %{"currency" => "USD", "value" => 0}},
                 "title" => %{"t" => "string", "v" => ""}},
               "context" => %{"attributes" => %{"lang" => "en",
                   "modality" => "desktop"}, "name" => "default"}, "id" => 469}],
            "slug" => "foo", "taxons" => [],
            "variants" => [%{"attributes" => %{"name" => %{"t" => "string",
                   "v" => "color"}, "type" => %{"t" => "string", "v" => ""}},
               "id" => 470,
               "values" => [%{"id" => 471, "name" => "white", "skuCodes" => ["SKU123"],
                  "swatch" => "ffffff"}]},
             %{"attributes" => %{"name" => %{"t" => "string", "v" => "size"},
                 "type" => %{"t" => "string", "v" => ""}}, "id" => 472,
               "values" => [%{"id" => 473, "name" => "S", "skuCodes" => ["SKU123"],
                  "swatch" => ""}]}]}}
      end

      def sku_with_images_factory do
        %{body: %{"albums" => [%{"createdAt" => "2017-02-28T10:38:33.217Z", "id" => 11,
               "images" => [%{"alt" => "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Gold_Blue_Three_Quarter.jpg",
                  "id" => 3,
                  "src" => "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Gold_Blue_Three_Quarter.jpg",
                  "title" => "https://s3-us-west-2.amazonaws.com/fc-firebird-public/images/product/Gold_Blue_Three_Quarter.jpg"}],
               "name" => "Sharkling", "updatedAt" => "2017-02-28T10:38:33.217Z"}],
            "attributes" => %{"Brand" => %{"t" => "string", "v" => "FoxBrand"},
              "activeFrom" => %{"t" => "date", "v" => "2017-02-28T10:38:32.553Z"},
              "bullet point" => %{"t" => "string", "v" => "A great stuff"},
              "bullet point1" => %{"t" => "string", "v" => "Must have"},
              "bullet point2" => %{"t" => "string", "v" => "Really cool"},
              "bullet point3" => %{"t" => "string", "v" => "Only here"},
              "bullet point4" => %{"t" => "string", "v" => "Get your item now"},
              "description" => %{"t" => "richText", "v" => "Designed for the beach."},
              "node id" => %{"t" => "string", "v" => ""},
              "nodeId" => %{"t" => "string", "v" => "2474995011"},
              "tags" => %{"t" => "tags", "v" => ["sunglasses"]},
              "tax code" => %{"t" => "string", "v" => "A_GEN_NO_TAX"},
              "title" => %{"t" => "string", "v" => "Sharkling"}},
            "context" => %{"attributes" => %{"lang" => "en", "modality" => "desktop"},
              "name" => "default"}, "id" => 9,
            "skus" => [%{"albums" => [%{"createdAt" => "2017-03-15T07:03:53.095Z",
                  "id" => 487,
                  "images" => [%{"alt" => "DeathStar_400x390.jpg", "id" => 147,
                     "src" => "https://s3-us-west-1.amazonaws.com/foxcomm-images/albums/1/487/DeathStar_400x390.jpg",
                     "title" => "DeathStar_400x390.jpg"},
                   %{"baseUrl" => "CuCeQ0IXEAAnpVx.jpg", "id" => 146,
                     "src" => "https://s3-us-west-1.amazonaws.com/foxcomm-images/albums/1/487/CuCeQ0IXEAAnpVx.jpg",
                     "title" => "CuCeQ0IXEAAnpVx.jpg"}], "name" => "main",
                  "updatedAt" => "2017-03-15T07:03:53.095Z"},
                %{"createdAt" => "2017-03-15T07:04:53.396Z", "id" => 490,
                  "images" => [%{"id" => 148,
                     "src" => "https://s3-us-west-1.amazonaws.com/foxcomm-images/albums/1/490/DeathStar2-1-1.jpg"},
                   %{"baseUrl" => "DeathStar2-2-0.jpg", "id" => 149,
                     "src" => "https://s3-us-west-1.amazonaws.com/foxcomm-images/albums/1/490/DeathStar2-2-0.jpg"},
                   %{"baseUrl" => "DeathStar2-2-2.jpg", "id" => 150,
                     "src" => "https://s3-us-west-1.amazonaws.com/foxcomm-images/albums/1/490/DeathStar2-2-2.jpg",
                     "title" => "DeathStar2-2-2.jpg"},
                   %{"alt" => "DeathStar2-0-1.jpg", "id" => 151,
                     "src" => "https://s3-us-west-1.amazonaws.com/foxcomm-images/albums/1/490/DeathStar2-0-1.jpg",
                     "title" => "DeathStar2-0-1.jpg"}], "name" => "swatches",
                  "updatedAt" => "2017-03-15T07:04:53.396Z"}],
               "attributes" => %{"activeFrom" => %{"t" => "date",
                   "v" => "2017-02-28T10:38:32.553Z"},
                 "code" => %{"t" => "string", "v" => "SKU-ABC"},
                 "retailPrice" => %{"t" => "price",
                   "v" => %{"currency" => "USD", "value" => 5000}},
                 "salePrice" => %{"t" => "price",
                   "v" => %{"currency" => "USD", "value" => 4500}},
                 "tags" => %{"t" => "tags", "v" => ["sunglasses"]},
                 "title" => %{"t" => "string", "v" => "Sharkling"},
                 "upc" => %{"t" => "string", "v" => "1234567890"}},
               "context" => %{"attributes" => %{"lang" => "en",
                   "modality" => "desktop"}, "name" => "default"}, "id" => 10}],
            "slug" => "sharkling", "taxons" => [],
            "variants" => [%{"attributes" => %{"name" => %{"t" => "string",
                   "v" => "color"}, "type" => %{"t" => "string", "v" => ""}},
               "id" => 485,
               "values" => [%{"id" => 486, "name" => "white", "skuCodes" => ["SKU-ABC"],
                  "swatch" => "FFFFFF"}]}]}}
      end

      def product_by_title_factory do
        %{"ListMatchingProductsResponse" =>
          %{"ListMatchingProductsResult" =>
            %{"Products" =>
              %{"Product" => [
                  %{"AttributeSets" => %{"ItemAttributes" => %{"Binding" => "Apparel",
                        "Brand" => "Gilden", "Color" => "Small,black",
                        "Department" => "mens",
                        "Feature" => ["Wolverine", "X-Men", "Marvel Comics",
                         "100% Cotten", "100% Satisfaction Guaranteed"],
                        "PackageDimensions" => %{"Height" => "1.60", "Length" => "8.20",
                          "Weight" => "0.30", "Width" => "4.60"},
                        "PackageQuantity" => "1", "PartNumber" => "ycbvb635053",
                        "ProductGroup" => "Apparel", "ProductTypeName" => "SHIRT",
                        "Size" => "Small,Black",
                        "SmallImage" => %{"Height" => "75",
                          "URL" => "http://ecx.images-amazon.com/images/I/41SXDS9-wDL._SL75_.jpg",
                          "Width" => "62"},
                        "Title" => "Wolverine T-shirt Different Colors (Small, Black)",
                        "{http://www.w3.org/XML/1998/namespace}lang" => "en-US"}},
                    "Identifiers" => %{"MarketplaceASIN" => %{"ASIN" => "B01C26TGYA",
                        "MarketplaceId" => "ATVPDKIKX0DER"}},
                    "Relationships" => %{"VariationParent" => %{"Identifiers" => %{"MarketplaceASIN" => %{"ASIN" => "B01C26TG90",
                            "MarketplaceId" => "ATVPDKIKX0DER"}}}},
                    "SalesRankings" => %{}}]}},
             "ResponseMetadata" => %{"RequestId" => "06e0cea7-4e06-4ced-9cc1-e130cb1a2735"}}}
      end

      def categories_by_asin_factory do
        %{"GetProductCategoriesForASINResponse" =>
          %{"GetProductCategoriesForASINResult" =>
            %{"Self" => [%{"Parent" => %{"Parent" => %{"Parent" => %{"ProductCategoryId" => "7141123011",
                        "ProductCategoryName" => "Clothing, Shoes & Jewelry"},
                      "ProductCategoryId" => "7141124011",
                      "ProductCategoryName" => "Departments"},
                    "ProductCategoryId" => "7147441011",
                    "ProductCategoryName" => "Men"},
                  "ProductCategoryId" => "7581669011",
                  "ProductCategoryName" => "Shops"},
                %{"Parent" => %{"Parent" => %{"Parent" => %{"Parent" => %{"Parent" => %{"ProductCategoryId" => "7141123011",
                            "ProductCategoryName" => "Clothing, Shoes & Jewelry"},
                          "ProductCategoryId" => "7141124011",
                          "ProductCategoryName" => "Departments"},
                        "ProductCategoryId" => "7147445011",
                        "ProductCategoryName" => "Novelty & More"},
                      "ProductCategoryId" => "12035955011",
                      "ProductCategoryName" => "Clothing"},
                    "ProductCategoryId" => "7586148011",
                    "ProductCategoryName" => "Band & Music Fan"},
                  "ProductCategoryId" => "1252178011",
                  "ProductCategoryName" => "T-Shirts"}]},
             "ResponseMetadata" => %{"RequestId" => "cb4f02da-6c5d-4014-ba4b-dd9d3914dba5"}}}
      end

      def submit_product_feed_data do
        [{[parentage: "parent", color: "white", size: "S",
           activefrom: "2017-03-09T02:21:07.763Z", description: "<p>bar baz</p>",
           tags: ["a", "b", "c", "d"], title: "foo",
           activefrom: "2017-03-09T02:21:07.763Z", code: "PARENTSKU123",
           retailprice: %{"currency" => "USD", "value" => 1000},
           saleprice: %{"currency" => "USD", "value" => 0}], 1},
         {[parentage: "child", color: "white", size: "S",
           activefrom: "2017-03-09T02:21:07.763Z", description: "<p>bar baz</p>",
           tags: ["a", "b", "c", "d"], title: "foo",
           activefrom: "2017-03-09T02:21:07.763Z", code: "SKU123",
           retailprice: %{"currency" => "USD", "value" => 1000},
           saleprice: %{"currency" => "USD", "value" => 0}], 2}]
      end

      def submit_images_feed_data do
        [[{[sku: "SKU-ABC", type: "Main",
            location: "http://s3-us-west-1.amazonaws.com/foxcomm-images/albums/1/487/DeathStar_400x390.jpg"],
           1},
          {[sku: "SKU-ABC", type: "PT",
            location: "http://s3-us-west-1.amazonaws.com/foxcomm-images/albums/1/487/CuCeQ0IXEAAnpVx.jpg",
            id: 1], 2}],
         [[sku: "SKU-ABC", type: "Swatch",
           location: "http://s3-us-west-1.amazonaws.com/foxcomm-images/albums/1/490/DeathStar2-1-1.jpg",
           idx: 3],
          [sku: "SKU-ABC", type: "Swatch",
           location: "http://s3-us-west-1.amazonaws.com/foxcomm-images/albums/1/490/DeathStar2-2-0.jpg",
           idx: 4],
          [sku: "SKU-ABC", type: "Swatch",
           location: "http://s3-us-west-1.amazonaws.com/foxcomm-images/albums/1/490/DeathStar2-2-2.jpg",
           idx: 5],
          [sku: "SKU-ABC", type: "Swatch",
           location: "http://s3-us-west-1.amazonaws.com/foxcomm-images/albums/1/490/DeathStar2-0-1.jpg",
           idx: 6]]]
      end
    end
  end
end
