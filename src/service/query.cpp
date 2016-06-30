#include "service/query.hpp"
#include "util/dbc.hpp"

namespace isaac
{
    namespace net
    {
        void query_request_handler::onRequest(std::unique_ptr<proxygen::HTTPMessage> headers) noexcept
        {
            _headers = std::move(headers);
        }

        void query_request_handler::onBody(std::unique_ptr<folly::IOBuf> body) noexcept
        { 
            if (_body) _body->prependChain(std::move(body));
            else _body = std::move(body);
        }

        void query_request_handler::onEOM() noexcept
        try
        {
            if(!(_headers && _body)) return;

            if(_headers->getPath() == "/validate") 
                validate(*_headers, *_body);
            else
            {
                proxygen::ResponseBuilder{downstream_}
                .status(404, "Not Found")
                    .sendWithEOM();
            }
        }
        catch(std::exception& e)
        {
            proxygen::ResponseBuilder{downstream_}
            .status(500, e.what())
                .sendWithEOM();
        }
        catch(...)
        {
            proxygen::ResponseBuilder{downstream_}
            .status(500, "Unknown Error")
                .sendWithEOM();
        }

        void query_request_handler::onUpgrade(proxygen::UpgradeProtocol proto) noexcept 
        {
        }

        void query_request_handler::requestComplete() noexcept
        { 
            delete this;
        }

        void query_request_handler::onError(proxygen::ProxygenError err) noexcept
        { 
            delete this;
        }

        bool query_request_handler::check_signature(const util::jwt_parts& parts)
        {
            REQUIRE(_c.verifier);
            return util::check_jwt_signature(parts, *_c.verifier);
        }

        bool query_request_handler::verify_header(const folly::dynamic& header)
        {
            return header["alg"].asString() == "RS256";
        }

        bool query_request_handler::verify_user(const folly::dynamic& user)
        {
            auto id = user["id"].asInt();
            auto is_admin = user["admin"].asBool();
            auto ratchet = user.count("ratchet") ? user["ratchet"].asInt() : 0;

            //TODO
            //Hit cache, otherwise hit DB
            return true;
        }


        void query_request_handler::validate(proxygen::HTTPMessage& headers, folly::IOBuf& body) 
        {
            util::jwt_parts parts;
            if(!util::get_jwt_parts(parts, body)) 
            {
                invalid_jwt();
                return;
            }

            if(!check_signature(parts))
            {
                signature_not_verified();
                return;
            }

            auto decoded_header = util::base64url_decode(parts.header);
            auto header = folly::parseJson(decoded_header);

            if(!verify_header(header))
            {
                invalid_header();
                return;
            }

            auto decoded_payload = util::base64url_decode(parts.payload);
            auto payload = folly::parseJson(decoded_payload);

            if(!verify_user(payload))
            {
                invalid_user();
                return;
            }

            proxygen::ResponseBuilder{downstream_}
                .status(200, "OK")
                .sendWithEOM();
        }

        void query_request_handler::invalid_jwt()
        {
            proxygen::ResponseBuilder{downstream_}
                .status(500, "Invalid JWT")
                .sendWithEOM();
        }

        void query_request_handler::invalid_header()
        {
            proxygen::ResponseBuilder{downstream_}
                .status(500, "Only RS256 signatures are supported")
                .sendWithEOM();
        }

        void query_request_handler::invalid_user()
        {
            proxygen::ResponseBuilder{downstream_}
                .status(500, "Invalid User")
                .sendWithEOM();
        }

        void query_request_handler::signature_not_verified()
        {
            proxygen::ResponseBuilder{downstream_}
                .status(500, "Signature is not valid")
                .sendWithEOM();
        }
    }
}
