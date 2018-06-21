package io.openshift.booster.routing;

import io.vertx.core.Future;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.client.HttpResponse;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.ext.web.handler.StaticHandler;


public class VertxRoutingClientVerticle extends AbstractVerticle {

  private WebClient client;

  @Override
  public void start(Future<Void> future) {
    client = WebClient.create(vertx, new WebClientOptions()
      .setDefaultHost("vertx-istio-routing-service")
      .setDefaultPort(8080));

    Router router = Router.router(vertx);
    router.get("/request-data").handler(this::request);
    router.get("/*").handler(StaticHandler.create());

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

  private void request(RoutingContext rc) {
    client.get("/data")
      .rxSend()
      .map(HttpResponse::bodyAsString)
      .subscribe(payload -> rc.response().end(payload), rc::fail);
  }
}
