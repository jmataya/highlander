defmodule Hyperion.Amazon.Workers.CustomersOrdersWorker do
  use GenServer
  require Logger

  alias Hyperion.PhoenixScala.Client
  alias Hyperion.Amazon

  def start_link do
    GenServer.start_link(__MODULE__, %{})
  end

  def init(state) do
    schedule_work()
    {:ok, state}
  end

  def handle_info(:work, state) do
    do_work()
    schedule_work()
    {:noreply, state}
  end

  def do_work() do
    try do
      get_credentials()
      |> fetch_amazon_orders
      |> store_customers_and_orders()
    rescue e in RuntimeError ->
      Logger.error "Error while fetching orders from Amazon: #{e.message}"
    end
  end

  defp get_credentials() do
    cfg = Amazon.fetch_config()
    if String.strip(cfg.seller_id) != "" do
      cfg
    else
      raise "Credentials not set. Exiting."
    end
  end

  defp fetch_amazon_orders(cfg) do
    date = PullWorkerHistory.last_run_for(cfg.seller_id)
           |>Timex.format!("%Y-%m-%dT%H:%M:%SZ", :strftime)
    list = [fulfillment_channel: ["MFN", "AFN"],
            created_after: [date]]
    Logger.info("Fetching order with params: #{inspect(list)}")

    case MWSClient.list_orders(list, cfg) do
      {:error, error} -> raise inspect(error)
      {:warn, warn} -> raise warn["ErrorResponse"]["Error"]["Message"]
      {_, resp} ->
        Logger.info("Orders fetched: #{inspect(resp)}")
        PullWorkerHistory.insert_run_mark(cfg.seller_id)
        resp["ListOrdersResponse"]["ListOrdersResult"]
    end
  end

  defp store_customers_and_orders(orders) do
    case orders["Orders"]["Order"] do
      list when is_list(list) -> Enum.each(list, fn order ->
                                  Client.create_order_and_customer(order)
                                 end)
      map when is_map(map) -> Client.create_order_and_customer(map)
      nil -> Logger.info "No orders present: #{inspect(orders)}"
      _ -> Logger.error "Some error occured! #{inspect(orders)}"
    end
  end

  defp schedule_work do
    Process.send_after(self(), :work, 24 * 60 * 60 * 1000) # In 24 hours
  end
end