package io.openshift.booster.routing;

import io.vertx.core.Future;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;


public class VertxRoutingServiceA extends AbstractVerticle {

  @Override
  public void start(Future<Void> future) {
    Router router = Router.router(vertx);
    router.get("/data").handler(this::random);
    router.get("/health").handler(rc -> rc.response().end("OK"));

    vertx
      .createHttpServer()
      .requestHandler(router::accept)
      .listen(
        // Retrieve the port from the configuration, default to 8080.
        config().getInteger("http.port", 8080), ar -> {
          if (ar.succeeded()) {
            System.out.println("Server started on port " + ar.result().actualPort());
          }
          future.handle(ar.mapEmpty());
        });
  }

  private void random(RoutingContext rc) {
    long started = System.currentTimeMillis();
    long delay = (long) (Math.random() * 1000);
    vertx.setTimer(delay, x ->
      rc.response().end("Hello from Service A! Operation completed in " + (System.currentTimeMillis() - started) + "ms."));
  }
}
