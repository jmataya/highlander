# Highlander

Highlander is the brand-spanking-new FoxCommerce mono-repo.

_This project is currently in pre-release form, so the upstream repositories
(a.k.a. the old ones) should be used for any current feature work._

## The Projects

###[phoenix-scala](https://github.com/FoxComm/phoenix-scala)

Our main API that handles the business logic for the customer, merchandising,
and order management systems. Written in Scala.

###[green-river](https://github.com/FoxComm/green-river)

An event-sourcing system based on Kafka that utilizes
[bottledwater](https://github.com/confluentinc/bottledwater-pg) to capture all
of the changes that occur in Postgres and piple them into Kafka. It's built in
Scala and powers logging and searching capabilities in the system.

###[middlewarehouse](https://github.com/FoxComm/middlewarehouse)

A lightweight and fast shipping and inventory management service written in Go.

###[isaac](https://github.com/FoxComm/isaac)

Our C++ authentication service.

###[ashes](http://github.com/FoxComm/ashes)

The Admin UI, written in React.js.

###[api-js](https://github.com/FoxComm/api-js)

A JavaScript library for interacting with the FoxCommerce API.

###[firebrand](https://github.com/FoxComm/firebrand)

A demo storefront used to show off the capabilities of FoxCommerce APIs.

###[integration-tests](https://github.com/FoxComm/integration-tests)

Our tests for hitting the system as a black box at the API level. They test
both functionality and that every piece in the system is talking correctly to
each other.

###[prov-shit](https://github.com/FoxComm/prov-shit)

All of our DevOps tools for deploying the application to both development and
production.

## Usage

### Updating from Upstream

Updating from upstream will pull in all commits on each project's `master`
branch. All commit history in each repository will be cloned into this repo.

```
$ make update
```

## Development Environment

_TODO: Add the instructions_
