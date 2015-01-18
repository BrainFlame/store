all:
	sbt compile && sbt +publish-local
	cd examples/finagle && sbt compile && sbt assembly

run:
	cd examples/finagle && java -jar target/scala-2.11/finagle-server.jar serve -solo db/store.3kv
