defmodule Solomon.Plug.JWTScope do
  import Plug.Conn

  def init(default), do: default

  def call(conn, _default) do
    scope_or_error =  get_jwt_string(conn)
                      |> extract_jwt_body
                      |> decode
                      |> extract_scope
    case scope_or_error do
      {:ok, scope} ->
        put_resp_header(conn, "scope", scope)
      {:error, msg} ->
        put_resp_header(conn, "scope_err", msg)
    end
  end

  defp get_jwt_string(conn) do
    case Map.fetch(fetch_cookies(conn).cookies, "jwt") do
      {:ok, bin} -> {:ok, [bin]}
      :error -> get_header_jwt(conn)
    end
  end

  defp get_header_jwt(conn) do
    case get_req_header(conn, "jwt") do
      [] -> {:error, "no jwt"}
      val ->  {:ok, val}
    end
  end

  defp extract_jwt_body({:error, bin}), do: {:error, bin}
  defp extract_jwt_body({:ok, jwt}) do
    {:ok, jwt
    |> Enum.flat_map(fn s -> String.split(s, ".") end)
    |> Enum.at(1)}
  end

  defp decode({:error, bin}), do: {:error, bin}
  defp decode({:ok, bin}) do
    case Base.url_decode64(bin, padding: false) do
      {:ok, decoded} -> {:ok, decoded}
      :error -> {:error, "base64 encoding: " <> bin}
    end
  end

  defp extract_scope({:error, bin}), do: {:error, bin}
  defp extract_scope({:ok, bin}) do
    case Poison.decode(bin) do
      {:ok, map} ->
        case Map.get(map, "scope") do
          nil -> "no scope"
          scope -> {:ok, scope}
        end
      {:error, _} ->
        {:error, "poison failed: " <> bin}
    end
  end
end
