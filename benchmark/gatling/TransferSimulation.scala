package com.fcp.benchmark

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

/** Gatling 多账户随机转账；与 k6/JMeter 场景口径一致，便于横向对比。 */
class TransferSimulation extends Simulation {
  val httpProtocol = http
    .baseUrl("http://localhost:8080")
    .contentTypeHeader("application/json")

  val feeder = Iterator.continually(Map(
    "idempotencyKey" -> java.util.UUID.randomUUID().toString,
    "payer" -> (10001 + scala.util.Random.nextInt(100)),
    "payee" -> (10001 + scala.util.Random.nextInt(100)),
    "amount" -> 100
  ))

  val scn = scenario("Transfer")
    .feed(feeder)
    .exec(http("transfer")
      .post("/v1/transactions")
      .header("Idempotency-Key", "#{idempotencyKey}")
      .body(StringBody("""{"tenantId":1,"payerAccountId":#{payer},"payeeAccountId":#{payee},"amountMinor":#{amount},"currency":"CNY"}"""))
      .check(status.is(200)))

  setUp(
    scn.inject(
      rampUsersPerSec(10).to(500).during(2.minutes),
      constantUsersPerSec(500).during(5.minutes)
    )
  ).protocols(httpProtocol)
   .assertions(global().responseTime().percentile(99).lt(200))
}