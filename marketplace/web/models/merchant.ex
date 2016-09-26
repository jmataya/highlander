defmodule Marketplace.Merchant do
  use Marketplace.Web, :model

  schema "merchants" do
    field :name, :string
    field :business_name, :string
    field :phone_number, :string
    field :email_address, :string
    field :description, :string
    field :site_url, :string
    field :state, :string, default: "new"
    field :scope_id, :integer #Scopes live in Permissions/Solomon
    field :organization_id, :integer #Organizations live in Permissions/Solomon

    timestamps

    has_one :merchant_application, Marketplace.MerchantApplication
    has_many :merchant_addresses, Marketplace.MerchantAddress
    has_many :merchant_accounts, Marketplace.MerchantAccount
    has_one :merchant_social_profile, Marketplace.MerchantSocialProfile
    has_one :merchant_business_profile, Marketplace.MerchantBusinessProfile
    has_one :social_profile, through: [:merchant_social_profile, :social_profile]
    has_one :business_profile, through: [:merchant_business_profile, :business_profile]
  end

  @states ~w(new approved suspended cancelled)s
  @required_fields ~w(name description state)a
  @optional_fields ~w(business_name phone_number email_address site_url scope_id organization_id)a

  def changeset(model, params \\ :empty) do
    model 
    |> cast(params, @required_fields ++ @optional_fields)
    |> validate_required(@required_fields)
    |> validate_inclusion(:state, @states)
  end

  def update_changeset(model, params \\ :empty) do
    model 
    |> cast(params, @required_fields ++ @optional_fields)
    |> validate_required(@required_fields)
    |> validate_inclusion(:state, @states)
  end

end
