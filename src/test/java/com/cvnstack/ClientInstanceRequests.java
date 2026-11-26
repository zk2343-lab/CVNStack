package com.cvnstack;

import static io.restassured.RestAssured.given;

import io.restassured.specification.RequestSpecification;

/**
 * Adds {@code X-Client-Instance} on mutating HTTP tests.
 *
 * <p>AI-assisted.
 */
public final class ClientInstanceRequests {

  /** Header name used by the service. */
  public static final String HEADER = "X-Client-Instance";

  /** Instance id used by tests that do not need two clients. */
  public static final String VALUE = "cli-1";

  private ClientInstanceRequests() {}

  /**
   * Starts a REST Assured request with the required instance header.
   *
   * @param port local server port
   * @return request with {@code X-Client-Instance}
   */
  public static RequestSpecification givenClient(int port) {
    return given().port(port).header(HEADER, VALUE);
  }
}
